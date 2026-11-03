/*
 * Copyright (C) 2026 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.phlox.cli;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.pdflayout.base.PLColor;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.html.HtmlInvoiceWriter;
import com.helger.phlox.html.HtmlRenderContext;
import com.helger.phlox.pdf.PdfInvoiceWriter;
import com.helger.phlox.pdf.PdfPalette;
import com.helger.phlox.pdf.PdfRenderContext;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.theme.PhloxTheme;
import com.helger.phlox.theme.PhloxThemeReader;
import com.helger.phlox.ubl.UblInvoiceReader;

import picocli.CommandLine;

/**
 * Test that the theme <code>blue-theme.json</code> restyles the HTML and the PDF output, so that
 * blue is the only hue of the layout. The theme is the example of the wiki page "Styling".
 *
 * @author Philip Helger
 */
public final class BlueThemeTest
{
  private static final String THEME_PATH = "blue-theme.json";
  private static final PLColor BLUE = new PLColor (0x1a, 0x56, 0xdb);
  private static final PLColor SKY = new PLColor (0xe6, 0xef, 0xfd);
  private static final PLColor NAVY = new PLColor (0x0b, 0x1f, 0x3a);
  private static final PLColor MIST = new PLColor (0x8a, 0xa0, 0xbf);

  private static PhloxTheme _readTheme ()
  {
    return PhloxThemeReader.read (new ClassPathResource (THEME_PATH));
  }

  private static PhloxInvoice _readInvoice ()
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH +
                                                        "2017/ubl/invoice/ubl-tc434-example1.xml").getInputStream ())
    {
      return UblInvoiceReader.fromUbl21 ().read (aIS, new ErrorList ());
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to read the invoice", ex);
    }
  }

  private static boolean _isGray (final PLColor aColor)
  {
    return aColor.getRed () == aColor.getGreen () && aColor.getGreen () == aColor.getBlue ();
  }

  private static boolean _isBluish (final PLColor aColor)
  {
    return aColor.getBlue () >= aColor.getRed () && aColor.getBlue () >= aColor.getGreen ();
  }

  private static int _toByte (final COSNumber aOperand)
  {
    return Math.round (aOperand.floatValue () * 255f);
  }

  /** Get all colours that are set as fill or stroke colour on any page of the PDF. */
  private static ICommonsSet <PLColor> _getColors (final byte [] aPdf) throws IOException
  {
    final ICommonsSet <PLColor> ret = new CommonsHashSet <> ();
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      for (final PDPage aPage : aDoc.getPages ())
      {
        // The operands in front of the next operator
        final ICommonsList <COSNumber> aOperands = new CommonsArrayList <> ();
        for (final Object aToken : new PDFStreamParser (aPage).parse ())
        {
          if (aToken instanceof final COSNumber aNumber)
            aOperands.add (aNumber);
          else
            if (aToken instanceof final Operator aOperator)
            {
              final String sName = aOperator.getName ();
              final boolean bColor = "rg".equals (sName) ||
                                     "RG".equals (sName) ||
                                     "g".equals (sName) ||
                                     "G".equals (sName) ||
                                     "sc".equals (sName) ||
                                     "SC".equals (sName) ||
                                     "scn".equals (sName) ||
                                     "SCN".equals (sName);
              if (bColor && aOperands.size () == 3)
                ret.add (new PLColor (_toByte (aOperands.get (0)),
                                      _toByte (aOperands.get (1)),
                                      _toByte (aOperands.get (2))));
              else
                if (bColor && aOperands.size () == 1)
                  ret.add (PLColor.gray (_toByte (aOperands.get (0))));
              aOperands.clear ();
            }
            else
            {
              // A name or an array: not the operand of a colour operator
              aOperands.clear ();
            }
        }
      }
    }
    return ret;
  }

  @Test
  public void testPaletteIsBlue ()
  {
    final PdfPalette aPalette = new PdfRenderContext ().applyTheme (_readTheme ()).getPalette ();
    assertEquals (BLUE, aPalette.getAccent ());
    assertEquals (SKY, aPalette.getAccentWash ());
    for (final PLColor aColor : new PLColor [] { aPalette.getInk (),
                                                 aPalette.getMuted (),
                                                 aPalette.getFaint (),
                                                 aPalette.getRule (),
                                                 aPalette.getWash (),
                                                 aPalette.getAccent (),
                                                 aPalette.getAccentWash () })
    {
      assertTrue ("Not blue: " + aColor, _isBluish (aColor));
      // No colour of the default layout is left
      assertFalse ("Not changed: " + aColor, aColor.equals (PdfPalette.DEFAULT.getInk ()));
    }
  }

  @Test
  public void testHtmlUsesBlue ()
  {
    final String sHtml = new HtmlInvoiceWriter ().writeAsString (_readInvoice (),
                                                                 new HtmlRenderContext ().applyTheme (_readTheme ()));
    SimpleFileIO.writeFile (new File ("generated/blue/blue.html"), sHtml, StandardCharsets.UTF_8);

    // The blue scale and the variables of the layout come after the default stylesheet, so they win
    final int nDefault = sHtml.indexOf ("--phlox-accent:#245c64");
    final int nBlue = sHtml.indexOf ("--blue:#1a56db");
    final int nAccent = sHtml.indexOf ("--phlox-accent:var(--blue)");
    assertTrue (nDefault >= 0);
    assertTrue (nBlue > nDefault);
    assertTrue (nAccent > nDefault);
    // The default layout uses the variables, not the colours
    assertTrue (sHtml.contains ("background:var(--phlox-accent)"));
    // The page header and footer of the printed page use the faint colour of the theme
    assertTrue (sHtml, sHtml.contains ("@top-left"));
    assertTrue (sHtml, sHtml.contains ("counter(pages)"));
    assertTrue (sHtml, sHtml.contains ("color:var(--phlox-faint,"));
  }

  @Test
  public void testPdfUsesBlueAsTheOnlyHue () throws Exception
  {
    final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
    new PdfInvoiceWriter ().writeTo (_readInvoice (), new PdfRenderContext ().applyTheme (_readTheme ()), aBaos);
    final byte [] aPdf = aBaos.toByteArray ();
    SimpleFileIO.writeFile (new File ("generated/blue/blue.pdf"), aPdf);

    final ICommonsSet <PLColor> aColors = _getColors (aPdf);
    // The amount due is filled with the accent, the payment block with the light accent
    assertTrue ("Colours: " + aColors, aColors.contains (BLUE));
    assertTrue ("Colours: " + aColors, aColors.contains (SKY));
    // The line below the table headers is navy
    assertTrue ("Colours: " + aColors, aColors.contains (NAVY));
    // The page footer is mist (the header starts on the second page, which this invoice does not have)
    assertTrue ("Colours: " + aColors, aColors.contains (MIST));
    // The accent of the default layout is gone
    assertFalse (aColors.contains (PdfPalette.DEFAULT.getAccent ()));
    assertFalse (aColors.contains (PdfPalette.DEFAULT.getAccentWash ()));
    // Everything that is not gray is blue
    for (final PLColor aColor : aColors)
      assertTrue ("Not blue: " + aColor, _isGray (aColor) || _isBluish (aColor));
  }

  @Test
  public void testDefaultLayoutIsNotBlue () throws Exception
  {
    // Guards the colour extraction: the default layout must show its own accent
    final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
    new PdfInvoiceWriter ().writeTo (_readInvoice (), new PdfRenderContext (), aBaos);
    final ICommonsSet <PLColor> aColors = _getColors (aBaos.toByteArray ());
    assertTrue ("Colours: " + aColors, aColors.contains (PdfPalette.DEFAULT.getAccent ()));
    assertFalse (aColors.contains (BLUE));
  }

  @Test
  public void testCommandLine () throws IOException
  {
    final Path aInvoice = Files.createTempFile ("phlox-blue-invoice-", ".xml");
    final Path aTheme = Files.createTempFile ("phlox-blue-theme-", ".json");
    final Path aHtml = Files.createTempFile ("phlox-blue-", ".html");
    final Path aPdf = Files.createTempFile ("phlox-blue-", ".pdf");
    for (final Path aFile : new Path [] { aInvoice, aTheme, aHtml, aPdf })
      aFile.toFile ().deleteOnExit ();
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH +
                                                        "2017/ubl/invoice/ubl-tc434-example1.xml").getInputStream ();
         final InputStream aThemeIS = new ClassPathResource (THEME_PATH).getInputStream ())
    {
      Files.write (aInvoice, aIS.readAllBytes ());
      Files.write (aTheme, aThemeIS.readAllBytes ());
    }

    assertEquals (0,
                  new CommandLine (new PhloxCli ()).execute ("-i",
                                                             aInvoice.toString (),
                                                             "-o",
                                                             aHtml.toString (),
                                                             "--theme",
                                                             aTheme.toString ()));
    assertTrue (Files.readString (aHtml, StandardCharsets.UTF_8).contains ("--phlox-accent:var(--blue)"));

    assertEquals (0,
                  new CommandLine (new PhloxCli ()).execute ("-i",
                                                             aInvoice.toString (),
                                                             "-f",
                                                             "PDF",
                                                             "-o",
                                                             aPdf.toString (),
                                                             "--theme",
                                                             aTheme.toString ()));
    assertTrue (_getColors (Files.readAllBytes (aPdf)).contains (BLUE));
  }
}
