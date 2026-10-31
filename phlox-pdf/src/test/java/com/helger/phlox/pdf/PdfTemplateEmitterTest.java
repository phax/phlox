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
package com.helger.phlox.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.pdflayout.base.PLColor;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Test class for class {@link PdfTemplateEmitter}.
 *
 * @author Philip Helger
 */
public final class PdfTemplateEmitterTest
{
  private static PhloxInvoice _read (final IInvoiceReader <InputStream> aReader, final String sPath)
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH + sPath).getInputStream ())
    {
      final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
      assertNotNull ("Failed to parse " + sPath, aModel);
      return aModel;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sPath, ex);
    }
  }

  private static byte [] _render (final PhloxInvoice aModel, final PhloxTemplate aTemplate, final Locale aLocale)
  {
    try
    {
      final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
      new PdfInvoiceWriter ().writeTo (aModel,
                                       new PdfRenderContext ().setLocale (aLocale).setTemplate (aTemplate),
                                       aBaos);
      return aBaos.toByteArray ();
    }
    catch (final Exception ex)
    {
      throw new IllegalStateException ("Failed to render the PDF", ex);
    }
  }

  private static String _text (final byte [] aPdf)
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      return new PDFTextStripper ().getText (aDoc);
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to read the PDF", ex);
    }
  }

  private static void _renderAll (final IInvoiceReader <InputStream> aReader,
                                  final ICommonsList <ClassPathResource> aFiles,
                                  final int nMinCount)
  {
    int nCount = 0;
    for (final ClassPathResource aRes : aFiles)
    {
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
        assertNotNull ("Failed to parse " + aRes.getPath (), aModel);
        for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN })
        {
          final byte [] aBytes = _render (aModel, PhloxTemplate.getDefault (), aLocale);
          assertTrue ("Output is not a PDF: " + aRes.getPath (),
                      aBytes.length > 500 && aBytes[0] == '%' && aBytes[1] == 'P' && aBytes[2] == 'D' && aBytes[3] == 'F');
          final String sText = _text (aBytes);
          assertTrue ("Missing invoice number for " + aRes.getPath (), sText.contains (aModel.getNumber ()));
          assertFalse ("Unresolved sentinel in " + aRes.getPath (), sText.contains ("[[PHLOX:"));
          assertFalse ("Unresolved placeholder in " + aRes.getPath (), sText.contains ("{{"));

          final String sOutputPath = "generated/template/" +
                                     StringHelper.trimStart (aRes.getPath (), "external/") +
                                     "-" +
                                     aLocale.getLanguage () +
                                     ".pdf";
          SimpleFileIO.writeFile (new File (sOutputPath), aBytes);
        }
        nCount++;
      }
      catch (final IOException ex)
      {
        throw new IllegalStateException ("Failed to open " + aRes.getPath (), ex);
      }
    }
    assertTrue ("Expected at least " + nMinCount + " documents rendered", nCount >= nMinCount);
  }

  private static int _strokeOperators (final byte [] aPdf)
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      int nCount = 0;
      for (final Object aToken : new PDFStreamParser (aDoc.getPage (0)).parse ())
        if (aToken instanceof final Operator aOperator && "S".equals (aOperator.getName ()))
          nCount++;
      return nCount;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to read the PDF", ex);
    }
  }

  private static List <String> _fontNames (final byte [] aPdf)
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      final ICommonsList <String> ret = new CommonsArrayList <> ();
      for (final COSName aName : aDoc.getPage (0).getResources ().getFontNames ())
        ret.add (aDoc.getPage (0).getResources ().getFont (aName).getName ());
      return ret;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to read the PDF", ex);
    }
  }

  @Test
  public void testDefaultTemplateUbl21 ()
  {
    _renderAll (UblInvoiceReader.fromUbl21 (), MockPhloxTestFiles.getUbl21InvoiceFiles (), 10);
  }

  @Test
  public void testDefaultTemplateUbl25 ()
  {
    _renderAll (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getAllUbl25Files (), 10);
  }

  @Test
  public void testDefaultTemplateCiiD16B ()
  {
    _renderAll (CiiInvoiceReader.fromD16B (), MockPhloxTestFiles.getCiiD16BFiles (), 100);
  }

  @Test
  public void testDefaultTemplateCiiD25A ()
  {
    _renderAll (CiiInvoiceReader.fromD25A (), MockPhloxTestFiles.getCiiD25AFiles (), 10);
  }

  @Test
  public void testStyledRunsStayOnOneLine ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sText = _text (_render (aModel, PhloxTemplate.getDefault (), Locale.ENGLISH));
    // "**Label:** value" is a bold run followed by a regular run
    assertTrue (sText.contains ("Seller VAT identifier: NL8200.98.395.B.01"));
    // A line break in a table cell
    assertTrue (sText.contains ("PATAT FRITES 10MM 10KG"));
    assertTrue (sText.contains ("Item seller's identifier: 166022"));
  }

  @Test
  public void testMarkupInInvoiceDataStaysText ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    aModel.setNumber ("{link[https://evil.example/pay]}CLICK{link} {bg:#ff0000}RED{bg}");
    final String sText = _text (_render (aModel, PhloxTemplate.fromString ("{{BT-1}} [x](https://example.org/my_file_name.pdf)"), Locale.ENGLISH));
    assertTrue (sText, sText.contains ("(link[https://evil.example/pay])CLICK(link)"));
    assertTrue (sText, sText.contains ("(bg:#ff0000)RED(bg)"));
    assertTrue (sText, sText.contains ("x"));
  }

  @Test
  public void testMarkdownNodes ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sSource = "# {{BT-1}}\n\n" +
                           "## Section\n\n" +
                           "### Sub\n\n" +
                           "Text **bold** *em* ~~del~~ `code` [link](https://example.org/x)\nsecond line<br>third line\n\n" +
                           "- one\n- two\n\n" +
                           "3. three\n4. four\n\n" +
                           "> quote\n\n" +
                           "---\n\n" +
                           "| A | B | C |\n|:--|:-:|--:|\n| 1 | 2 | 3 |\n\n" +
                           "{{pagebreak}}\n\n" +
                           "    indented code\n";
    final byte [] aPdf = _render (aModel, PhloxTemplate.fromString (sSource), Locale.ENGLISH);
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      // The page break creates a second page
      assertEquals (2, aDoc.getNumberOfPages ());
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException (ex);
    }
    final String sText = _text (aPdf);
    for (final String sExpected : new String [] { aModel.getNumber (),
                                                  "SECTION",
                                                  "Sub",
                                                  "Text bold em del code link",
                                                  "second line",
                                                  "third line",
                                                  "one",
                                                  "two",
                                                  "3.",
                                                  "4.",
                                                  "quote",
                                                  "indented code" })
      assertTrue ("Missing '" + sExpected + "' in: " + sText, sText.contains (sExpected));
  }

  @Test
  public void testCurrencySymbolIsSeparatedFromTheNumber ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sText = _text (_render (aModel, PhloxTemplate.getDefault (), Locale.ENGLISH));
    // Non-breaking space between the symbol and the number
    assertTrue (sText.contains ("\u20AC\u00A0250.33") || sText.contains ("\u20AC 250.33"));
    assertFalse (sText.contains ("\u20AC250.33"));
  }

  @Test
  public void testStrikethroughIsDrawn ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final byte [] aStruck = _render (aModel, PhloxTemplate.fromString ("Text ~~deleted~~ end"), Locale.ENGLISH);
    final byte [] aPlain = _render (aModel, PhloxTemplate.fromString ("Text deleted end"), Locale.ENGLISH);
    assertTrue (_text (aStruck).contains ("Text deleted end"));
    // The only difference is the line through the struck word
    assertEquals (_strokeOperators (aPlain) + 1, _strokeOperators (aStruck));
  }

  @Test
  public void testInlineCodeUsesCourier ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final byte [] aWithCode = _render (aModel, PhloxTemplate.fromString ("Call `getFoo()` now"), Locale.ENGLISH);
    final byte [] aWithout = _render (aModel, PhloxTemplate.fromString ("Call getFoo() now"), Locale.ENGLISH);
    assertTrue (_text (aWithCode).contains ("Call getFoo() now"));
    assertTrue (_fontNames (aWithCode).contains ("Courier"));
    assertFalse (_fontNames (aWithout).contains ("Courier"));
  }

  @Test
  public void testBacktickInTextIsNotAMarker ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    // Written as a literal backtick in the template: CommonMark keeps a lone backtick as text
    final byte [] aPdf = _render (aModel, PhloxTemplate.fromString ("It costs 5 `a unit"), Locale.ENGLISH);
    assertTrue (_text (aPdf).contains ("It costs 5 `a unit"));
    assertFalse (_fontNames (aPdf).contains ("Courier"));
  }

  @Test
  public void testTwoTildesInTextAreNotAMarker ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final byte [] aPdf = _render (aModel, PhloxTemplate.fromString ("Range 5~~7 and about ~3"), Locale.ENGLISH);
    assertTrue (_text (aPdf).contains ("Range 5~~7 and about ~3"));
  }

  @Test
  public void testBlockStyleIsPartOfTheContext ()
  {
    final PdfRenderContext aCtx = new PdfRenderContext ();
    assertNotNull (aCtx.getBlockStyle ("totals-due"));
    assertEquals (null, aCtx.getBlockStyle ("unknown"));
    aCtx.putBlockStyle ("warn", new PdfBlockStyle (PLColor.YELLOW, PLColor.RED));
    assertEquals (PLColor.YELLOW, aCtx.getBlockStyle ("warn").getFillColor ());
    assertEquals (PLColor.RED, aCtx.getBlockStyle ("warn").getTextColor ());
  }

  @Test
  public void testQrCode () throws IOException, ReaderException
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sText = "Müller & Co\nLine 2";
    final byte [] aPdf = _render (aModel,
                                  PhloxTemplate.fromString ("![Code](qrcode:M%C3%BCller%20%26%20Co%0ALine%202 \"height-px:80\")"),
                                  Locale.ENGLISH);
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      final PDPage aPage = aDoc.getPage (0);
      PDImageXObject aImage = null;
      for (final COSName aName : aPage.getResources ().getXObjectNames ())
        if (aPage.getResources ().getXObject (aName) instanceof final PDImageXObject aXObject)
          aImage = aXObject;
      assertNotNull (aImage);
      // Lossless, so the code can be read back from the PDF
      assertEquals ("png", aImage.getSuffix ());
      final BufferedImage aBuffered = aImage.getImage ();
      final int nWidth = aBuffered.getWidth ();
      final int nHeight = aBuffered.getHeight ();
      final int [] aPixels = aBuffered.getRGB (0, 0, nWidth, nHeight, null, 0, nWidth);
      final BinaryBitmap aBitmap = new BinaryBitmap (new HybridBinarizer (new RGBLuminanceSource (nWidth,
                                                                                                  nHeight,
                                                                                                  aPixels)));
      assertEquals (sText,
                    new QRCodeReader ().decode (aBitmap, Map.of (DecodeHintType.PURE_BARCODE, Boolean.TRUE)).getText ());

      // 80 CSS pixels are 60 points
      final List <Object> aTokens = new PDFStreamParser (aPage).parse ();
      boolean bFound = false;
      for (int i = 6; i < aTokens.size (); ++i)
        if (aTokens.get (i) instanceof final Operator aOp &&
            "cm".equals (aOp.getName ()) &&
            aTokens.get (i - 6) instanceof final COSNumber aScale &&
            aScale.floatValue () == 60f)
          bFound = true;
      assertTrue (bFound);
    }
  }
}
