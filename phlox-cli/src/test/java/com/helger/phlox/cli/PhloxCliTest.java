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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.font.roboto.EFontResourceRoboto;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.CPhloxVersion;
import com.helger.phlox.testfiles.MockPhloxTestFiles;

import picocli.CommandLine;

/**
 * Test class for class {@link PhloxCli}.
 *
 * @author Philip Helger
 */
public final class PhloxCliTest
{
  @NonNull
  private static Path _copyInvoice () throws IOException
  {
    final Path aFile = Files.createTempFile ("phlox-cli-invoice-", ".xml");
    aFile.toFile ().deleteOnExit ();
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH +
                                                        "2017/ubl/invoice/ubl-tc434-example1.xml").getInputStream ())
    {
      Files.write (aFile, aIS.readAllBytes ());
    }
    return aFile;
  }

  @NonNull
  private static Path _tempFile (final String sSuffix) throws IOException
  {
    final Path aFile = Files.createTempFile ("phlox-cli-", sSuffix);
    aFile.toFile ().deleteOnExit ();
    return aFile;
  }

  private static int _run (final String... aArgs)
  {
    return new CommandLine (new PhloxCli ()).execute (aArgs);
  }

  @Test
  public void testVersion ()
  {
    final StringWriter aSW = new StringWriter ();
    final CommandLine aCmd = new CommandLine (new PhloxCli ()).setOut (new PrintWriter (aSW));
    assertEquals (0, aCmd.execute ("--version"));
    assertEquals ("phlox " + CPhloxVersion.BUILD_VERSION, aSW.toString ().strip ());
    assertFalse (CPhloxVersion.BUILD_VERSION.equals ("undefined"));
  }

  @Test
  public void testDefaultTemplateIsTheDefault () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-o", aOut.toString ()));
    final String sHtml = Files.readString (aOut, StandardCharsets.UTF_8);
    assertTrue (sHtml.contains ("class=\"phlox-table\""));
  }

  @Test
  public void testDefaultTemplate () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-o", aOut.toString (), "--template", "default"));
    final String sHtml = Files.readString (aOut, StandardCharsets.UTF_8);
    assertTrue (sHtml.contains ("class=\"phlox-table\""));
  }

  @Test
  public void testTemplateFile () throws IOException
  {
    final Path aTemplate = _tempFile (".ptpl");
    Files.writeString (aTemplate, "# Mine {{BT-1}}\n", StandardCharsets.UTF_8);
    final Path aOut = _tempFile (".html");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-o", aOut.toString (), "-t", aTemplate.toString ()));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).contains (">Mine 12115118</h1>"));
  }

  @Test
  public void testClasspathTemplate () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-o",
                        aOut.toString (),
                        "--template",
                        "classpath:templates/default.ptpl"));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).contains ("class=\"phlox-table\""));
  }

  @Test
  public void testTemplateForPdf () throws IOException
  {
    final Path aOut = _tempFile (".pdf");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-f",
                        "PDF",
                        "-o",
                        aOut.toString (),
                        "--template",
                        "default"));
    final byte [] aBytes = Files.readAllBytes (aOut);
    assertTrue (aBytes.length > 500 && aBytes[0] == '%' && aBytes[1] == 'P' && aBytes[2] == 'D' && aBytes[3] == 'F');
  }

  @Test
  public void testMissingTemplateIsAnError () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (2,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-o",
                        aOut.toString (),
                        "--template",
                        "/no/such/template.ptpl"));
  }

  @Test
  public void testTemplateCoverage () throws IOException
  {
    final Path aTemplate = _tempFile (".ptpl");
    Files.writeString (aTemplate, "# {{BT-1}}", StandardCharsets.UTF_8);
    final Path aOut = _tempFile (".html");
    final String sInvoice = _copyInvoice ().toString ();

    // WARN is the default
    assertEquals (0, _run ("-i", sInvoice, "-o", aOut.toString (), "-t", aTemplate.toString ()));
    assertEquals (2,
                  _run ("-i",
                        sInvoice,
                        "-o",
                        aOut.toString (),
                        "-t",
                        aTemplate.toString (),
                        "--template-coverage",
                        "REJECT"));
    assertEquals (0, _run ("-i", sInvoice, "-o", aOut.toString (), "--template-coverage", "REJECT"));

    // Also for the template of a theme
    final Path aTheme = _tempFile (".json");
    Files.writeString (aTheme,
                       "{\"common\": {\"template\": \"" + aTemplate.toString ().replace ("\\", "/") + "\"}}",
                       StandardCharsets.UTF_8);
    assertEquals (2,
                  _run ("-i",
                        sInvoice,
                        "-o",
                        aOut.toString (),
                        "--theme",
                        aTheme.toString (),
                        "--template-coverage",
                        "REJECT"));
  }

  @Test
  public void testTheme () throws IOException
  {
    final Path aTheme = _tempFile (".json");
    Files.writeString (aTheme,
                       "{\"$comment\":\"test\",\"variables\":{\"--phlox-accent\":\"#8a1538\"},\"common\":{\"locale\":\"de\"}}",
                       StandardCharsets.UTF_8);
    final Path aOut = _tempFile (".html");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-o", aOut.toString (), "--theme", aTheme.toString ()));
    final String sHtml = Files.readString (aOut, StandardCharsets.UTF_8);
    // The language and the colour come from the theme
    assertTrue (sHtml.contains ("Rechnung"));
    assertTrue (sHtml.contains ("--phlox-accent:#8a1538;"));

    // The command line wins over the theme
    final Path aOutEn = _tempFile (".html");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-o",
                        aOutEn.toString (),
                        "--theme",
                        aTheme.toString (),
                        "-l",
                        "en"));
    assertTrue (Files.readString (aOutEn, StandardCharsets.UTF_8).contains ("Invoice"));
  }

  @Test
  public void testInvalidTheme () throws IOException
  {
    final Path aTheme = _tempFile (".json");
    Files.writeString (aTheme, "{\"common\":{\"colour\":\"red\"}}", StandardCharsets.UTF_8);
    final Path aOut = _tempFile (".html");
    Files.deleteIfExists (aOut);
    assertEquals (2, _run ("-i", _copyInvoice ().toString (), "-o", aOut.toString (), "--theme", aTheme.toString ()));
    assertEquals (2, _run ("-i", _copyInvoice ().toString (), "--theme", "does-not-exist.json"));
  }

  @NonNull
  private static Path _fontFile (final EFontResourceRoboto eFont, final String sName) throws IOException
  {
    final Path aFile = Files.createTempFile ("phlox-cli-font-", "-" + sName);
    aFile.toFile ().deleteOnExit ();
    try (final InputStream aIS = eFont.getFontResource ().getInputStream ())
    {
      Files.write (aFile, aIS.readAllBytes ());
    }
    return aFile;
  }

  @NonNull
  private static ICommonsList <String> _getFontNames (@NonNull final Path aPdf) throws IOException
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    try (final PDDocument aDoc = Loader.loadPDF (aPdf.toFile ()))
    {
      for (final PDPage aPage : aDoc.getPages ())
        for (final COSName aName : aPage.getResources ().getFontNames ())
          ret.add (aPage.getResources ().getFont (aName).getName ());
    }
    return ret;
  }

  @Test
  public void testFontIsUsedInHtml () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "--font",
                        _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString (),
                        "--font-bold",
                        _fontFile (EFontResourceRoboto.ROBOTO_BOLD, "Bold.ttf").toString (),
                        "-o",
                        aOut.toString ()));
    final String sHtml = Files.readString (aOut, StandardCharsets.UTF_8);
    assertTrue (sHtml.contains ("@font-face{font-family:\"Phlox Custom\";font-weight:400;font-style:normal;src:url(data:font/ttf;base64,"));
    assertTrue (sHtml.contains ("font-weight:700;font-style:normal"));
    assertTrue (sHtml.contains ("body{font-family:\"Phlox Custom\""));
  }

  @Test
  public void testFontIsUsedInPdf () throws IOException
  {
    final Path aWithout = _tempFile (".pdf");
    final Path aWith = _tempFile (".pdf");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-f", "PDF", "-o", aWithout.toString ()));
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--font",
                        _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString (),
                        "-o",
                        aWith.toString ()));
    assertTrue (_getFontNames (aWithout).stream ().noneMatch (x -> x.contains ("Roboto")));
    assertTrue (_getFontNames (aWith).stream ().anyMatch (x -> x.contains ("Roboto")));
  }

  @Test
  public void testFontErrors () throws IOException
  {
    final Path aOut = _tempFile (".html");
    final Path aInvoice = _copyInvoice ();
    assertEquals (2, _run ("-i", aInvoice.toString (), "--font-bold", aInvoice.toString (), "-o", aOut.toString ()));
    assertEquals (2, _run ("-i", aInvoice.toString (), "--font", "does-not-exist.ttf", "-o", aOut.toString ()));
    // Not a font file
    assertEquals (2, _run ("-i", aInvoice.toString (), "--font", aInvoice.toString (), "-o", aOut.toString ()));
    assertEquals (2, _run ("-i", aInvoice.toString (), "-f", "PDF", "--font", aInvoice.toString (), "-o", aOut.toString ()));
  }

  @Test
  public void testWatermarkInHtml () throws IOException
  {
    final Path aWithout = _tempFile (".html");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-o", aWithout.toString ()));
    assertFalse (Files.readString (aWithout, StandardCharsets.UTF_8).contains ("<div class=\"phlox-watermark-user\""));

    // Defaults: opacity 0.10, rotation -45
    final Path aDefault = _tempFile (".html");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "--watermark", "DRAFT", "-o", aDefault.toString ()));
    assertTrue (Files.readString (aDefault, StandardCharsets.UTF_8)
                     .contains ("<div class=\"phlox-watermark-user\" style=\"color:rgba(128,128,128,0.1);font-size:72.0pt;position:fixed;top:50%;left:50%;transform:translate(-50%,-50%) rotate(-45.0deg);\" aria-hidden=\"true\">DRAFT</div>"));

    // Explicit values
    final Path aExplicit = _tempFile (".html");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "--watermark",
                        "DRAFT",
                        "--watermark-opacity",
                        "0.5",
                        "--watermark-rotation",
                        "30",
                        "-o",
                        aExplicit.toString ()));
    assertTrue (Files.readString (aExplicit, StandardCharsets.UTF_8)
                     .contains ("<div class=\"phlox-watermark-user\" style=\"color:rgba(128,128,128,0.5);font-size:72.0pt;position:fixed;top:50%;left:50%;transform:translate(-50%,-50%) rotate(30.0deg);\" aria-hidden=\"true\">DRAFT</div>"));
  }

  /**
   * Find the operands of the last operator with the given name that precedes the text drawing
   * operator of the given text on the first page.
   *
   * @return <code>null</code> if the text is not drawn on the first page.
   */
  @Nullable
  private static ICommonsList <COSBase> _getOperandsBeforeText (@NonNull final Path aPdf,
                                                                @NonNull final String sText,
                                                                @NonNull final String sOperator) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf.toFile ()))
    {
      final ICommonsList <COSBase> aOperands = new CommonsArrayList <> ();
      ICommonsList <COSBase> aLastMatch = null;
      for (final Object aToken : new PDFStreamParser (aDoc.getPage (0)).parse ())
      {
        if (aToken instanceof final Operator aOp)
        {
          if (aOp.getName ().equals (sOperator))
            aLastMatch = aOperands.getClone ();
          else
            if (aOp.getName ().equals ("Tj") &&
                aOperands.size () == 1 &&
                aOperands.getFirstOrNull () instanceof final COSString aString &&
                aString.getString ().equals (sText))
              return aLastMatch;
          aOperands.clear ();
        }
        else
          if (aToken instanceof final COSBase aOperand)
            aOperands.add (aOperand);
      }
      return null;
    }
  }

  private static float _getFloat (@NonNull final ICommonsList <COSBase> aOperands, final int nIndex)
  {
    return ((COSNumber) aOperands.get (nIndex)).floatValue ();
  }

  @Test
  public void testWatermarkInPdf () throws IOException
  {
    final Path aWithout = _tempFile (".pdf");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-f", "PDF", "-o", aWithout.toString ()));
    assertNull (_getOperandsBeforeText (aWithout, "DRAFT", "Tm"));

    // Defaults: opacity 0.10, rotation -45
    final Path aDefault = _tempFile (".pdf");
    assertEquals (0,
                  _run ("-i", _copyInvoice ().toString (), "-f", "PDF", "--watermark", "DRAFT", "-o", aDefault.toString ()));
    ICommonsList <COSBase> aMatrix = _getOperandsBeforeText (aDefault, "DRAFT", "Tm");
    assertNotNull (aMatrix);
    assertEquals (Math.cos (Math.toRadians (-45)), _getFloat (aMatrix, 0), 0.001);
    assertEquals (Math.sin (Math.toRadians (-45)), _getFloat (aMatrix, 1), 0.001);
    // The opacity lightens the grey (128) towards white: 255 - 127 * 0.1 = 242
    ICommonsList <COSBase> aColour = _getOperandsBeforeText (aDefault, "DRAFT", "rg");
    assertNotNull (aColour);
    assertEquals (242 / 255f, _getFloat (aColour, 0), 0.001);

    // Explicit values
    final Path aExplicit = _tempFile (".pdf");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--watermark",
                        "DRAFT",
                        "--watermark-opacity",
                        "1.0",
                        "--watermark-rotation",
                        "30",
                        "-o",
                        aExplicit.toString ()));
    aMatrix = _getOperandsBeforeText (aExplicit, "DRAFT", "Tm");
    assertNotNull (aMatrix);
    assertEquals (Math.cos (Math.toRadians (30)), _getFloat (aMatrix, 0), 0.001);
    assertEquals (Math.sin (Math.toRadians (30)), _getFloat (aMatrix, 1), 0.001);
    aColour = _getOperandsBeforeText (aExplicit, "DRAFT", "rg");
    assertNotNull (aColour);
    assertEquals (128 / 255f, _getFloat (aColour, 0), 0.001);
  }

  @NonNull
  private static String _loadSynthetic (@NonNull final String sFile) throws IOException
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH +
                                                        "synthetic/" +
                                                        sFile).getInputStream ())
    {
      return new String (aIS.readAllBytes (), StandardCharsets.UTF_8);
    }
  }

  /**
   * Run the command line and keep what it prints to stderr.
   */
  @NonNull
  private static String _runWithStderr (final int nExpectedExit, final String... aArgs)
  {
    final PrintStream aOldErr = System.err;
    final ByteArrayOutputStream aErr = new ByteArrayOutputStream ();
    System.setErr (new PrintStream (aErr, true, StandardCharsets.UTF_8));
    try
    {
      assertEquals (nExpectedExit, _run (aArgs));
    }
    finally
    {
      System.setErr (aOldErr);
    }
    return aErr.toString (StandardCharsets.UTF_8);
  }

  @Test
  public void testSniffSyntax ()
  {
    final String sUbl = "<Invoice xmlns='urn:oasis:names:specification:ubl:schema:xsd:Invoice-2'>" +
                        "<Note>rsm:CrossIndustryInvoice urn:un:unece:uncefact:data:standard</Note></Invoice>";
    assertEquals (PhloxCli.ESourceSyntax.UBL, PhloxCli._sniffSyntax (sUbl.getBytes (StandardCharsets.UTF_8)));
    final String sCii = "<rsm:CrossIndustryInvoice xmlns:rsm='urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100' />";
    assertEquals (PhloxCli.ESourceSyntax.CII, PhloxCli._sniffSyntax (sCii.getBytes (StandardCharsets.UTF_8)));
    assertEquals (PhloxCli.ESourceSyntax.CII,
                  PhloxCli._sniffSyntax ("<CrossIndustryInvoice />".getBytes (StandardCharsets.UTF_8)));
    // A DOCTYPE stays refused, and the UBL reader reports the problem
    assertEquals (PhloxCli.ESourceSyntax.UBL,
                  PhloxCli._sniffSyntax ("<!DOCTYPE a [<!ENTITY b 'c'>]><CrossIndustryInvoice>&b;</CrossIndustryInvoice>".getBytes (StandardCharsets.UTF_8)));
    assertEquals (PhloxCli.ESourceSyntax.UBL, PhloxCli._sniffSyntax ("no xml".getBytes (StandardCharsets.UTF_8)));
  }

  @Test
  public void testUblNoteMentioningCiiIsReadAsUbl () throws IOException
  {
    final Path aIn = _tempFile (".xml");
    final String sXml = _loadSynthetic ("ubl/invoice/phlox-reader-ubl21-invoice.xml");
    Files.writeString (aIn,
                       sXml.replace ("#AAI#Header note", "See rsm:CrossIndustryInvoice"),
                       StandardCharsets.UTF_8);
    final Path aOut = _tempFile (".txt");
    assertEquals (0, _run ("-i", aIn.toString (), "-f", "TEXT", "-o", aOut.toString ()));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).contains ("READER-UBL21"));
  }

  @Test
  public void testReaderWarningsGoToStderr () throws IOException
  {
    final Path aIn = _tempFile (".xml");
    final String sXml = _loadSynthetic ("cii/phlox-reader-d16b.xml").replace (">notadate<", ">not&#10;a date<");
    Files.writeString (aIn, sXml, StandardCharsets.UTF_8);
    final String sErr = _runWithStderr (0, "-i", aIn.toString (), "-f", "TEXT", "-o", _tempFile (".txt").toString ());
    assertTrue (sErr, sErr.contains ("Could not parse CII date 'not_a date' with format code '102'"));
    assertTrue (sErr, sErr.contains ("Unsupported CII date format code '610'"));
  }

  @Test
  public void testXsdErrorGoesToStderr () throws IOException
  {
    final Path aIn = _tempFile (".xml");
    final String sXml = _loadSynthetic ("ubl/invoice/phlox-reader-ubl21-invoice.xml");
    Files.writeString (aIn,
                       sXml.replace ("<cbc:DocumentCurrencyCode>", "<cbc:Nonsense>x</cbc:Nonsense><cbc:DocumentCurrencyCode>"),
                       StandardCharsets.UTF_8);
    final String sErr = _runWithStderr (1, "-i", aIn.toString (), "-f", "TEXT", "-o", _tempFile (".txt").toString ());
    assertTrue (sErr, sErr.contains ("Failed to parse input"));
    assertTrue (sErr, sErr.contains ("cvc-complex-type"));
    assertTrue (sErr, sErr.contains ("Source could not be parsed as UBL 2.1"));
  }

  @Test
  public void testNoEpcQrCodeForCreditNote () throws IOException
  {
    final String sXml = _loadSynthetic ("ubl/invoice/phlox-reader-ubl21-invoice.xml");

    final Path aInvoice = _tempFile (".xml");
    Files.writeString (aInvoice, sXml.replace (">381<", ">380<"), StandardCharsets.UTF_8);
    final Path aInvoiceOut = _tempFile (".html");
    assertEquals (0, _run ("-i", aInvoice.toString (), "-o", aInvoiceOut.toString ()));
    assertTrue (Files.readString (aInvoiceOut, StandardCharsets.UTF_8).contains ("<img"));

    // An Invoice root element with a credit note type code
    final Path aCreditNote = _tempFile (".xml");
    Files.writeString (aCreditNote, sXml, StandardCharsets.UTF_8);
    final Path aCreditNoteOut = _tempFile (".html");
    assertEquals (0, _run ("-i", aCreditNote.toString (), "-o", aCreditNoteOut.toString ()));
    assertFalse (Files.readString (aCreditNoteOut, StandardCharsets.UTF_8).contains ("<img"));
  }

  @Test
  public void testTextOutput () throws IOException
  {
    final Path aOut = _tempFile (".txt");
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-f", "TEXT", "-o", aOut.toString ()));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).startsWith ("Invoice\n\n12115118\n========\n"));

    // Language, watermark and template work as for the other formats
    final Path aTemplate = _tempFile (".ptpl");
    Files.writeString (aTemplate, "# {{label:section.lines.title}} **{{BT-1}}**\n", StandardCharsets.UTF_8);
    final Path aOutDe = _tempFile (".txt");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-f",
                        "TEXT",
                        "-l",
                        "de",
                        "--watermark",
                        "ENTWURF",
                        "-t",
                        aTemplate.toString (),
                        "-o",
                        aOutDe.toString ()));
    assertTrue (Files.readString (aOutDe, StandardCharsets.UTF_8)
                     .startsWith ("*** ENTWURF ***\n\nRechnungspositionen 12115118\n============================\n"));
  }

  @Test
  public void testWatermarkErrors () throws IOException
  {
    final Path aOut = _tempFile (".html");
    final Path aInvoice = _copyInvoice ();
    assertEquals (2,
                  _run ("-i",
                        aInvoice.toString (),
                        "--watermark",
                        "DRAFT",
                        "--watermark-opacity",
                        "abc",
                        "-o",
                        aOut.toString ()));
    assertEquals (2,
                  _run ("-i",
                        aInvoice.toString (),
                        "--watermark",
                        "DRAFT",
                        "--watermark-rotation",
                        "abc",
                        "-o",
                        aOut.toString ()));
  }

  @NonNull
  private static Path _copyCiiInvoice () throws IOException
  {
    final Path aFile = Files.createTempFile ("phlox-cli-cii-", ".xml");
    aFile.toFile ().deleteOnExit ();
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH + "2017/cii/CII_example1.xml").getInputStream ())
    {
      Files.write (aFile, aIS.readAllBytes ());
    }
    return aFile;
  }

  @Test
  public void testPdfAEmbedSource () throws IOException
  {
    final Path aInvoice = _copyInvoice ();
    final Path aOut = _tempFile (".pdf");
    assertEquals (0,
                  _run ("-i",
                        aInvoice.toString (),
                        "-f",
                        "PDF",
                        "--font",
                        _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString (),
                        "--embed-source",
                        "--embed-attachments",
                        "-o",
                        aOut.toString ()));
    try (final PDDocument aDoc = Loader.loadPDF (aOut.toFile ()))
    {
      final String sXmp = new String (aDoc.getDocumentCatalog ().getMetadata ().toByteArray (), StandardCharsets.UTF_8);
      assertTrue (sXmp, sXmp.contains ("<pdfaid:part>3</pdfaid:part>"));
      assertTrue (sXmp, sXmp.contains ("<pdfaid:conformance>B</pdfaid:conformance>"));
      final PDComplexFileSpecification aFileSpec = aDoc.getDocumentCatalog ()
                                                       .getNames ()
                                                       .getEmbeddedFiles ()
                                                       .getNames ()
                                                       .get (aInvoice.getFileName ().toString ());
      assertNotNull (aFileSpec);
      assertEquals ("Source", aFileSpec.getCOSObject ().getNameAsString (COSName.AF_RELATIONSHIP));
    }
  }

  @Test
  public void testZugferd () throws IOException
  {
    final Path aOut = _tempFile (".pdf");
    assertEquals (0,
                  _run ("-i",
                        _copyCiiInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--font",
                        _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString (),
                        "--zugferd",
                        "-o",
                        aOut.toString ()));
    try (final PDDocument aDoc = Loader.loadPDF (aOut.toFile ()))
    {
      final String sXmp = new String (aDoc.getDocumentCatalog ().getMetadata ().toByteArray (), StandardCharsets.UTF_8);
      assertTrue (sXmp, sXmp.contains ("<fx:ConformanceLevel>EN 16931</fx:ConformanceLevel>"));
      final PDComplexFileSpecification aFileSpec = aDoc.getDocumentCatalog ()
                                                       .getNames ()
                                                       .getEmbeddedFiles ()
                                                       .getNames ()
                                                       .get ("factur-x.xml");
      assertNotNull (aFileSpec);
      assertEquals ("Alternative", aFileSpec.getCOSObject ().getNameAsString (COSName.AF_RELATIONSHIP));
    }

    // Explicit profile
    assertEquals (0,
                  _run ("-i",
                        _copyCiiInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--font",
                        _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString (),
                        "--zugferd",
                        "--zugferd-profile",
                        "extended",
                        "-o",
                        aOut.toString ()));
    try (final PDDocument aDoc = Loader.loadPDF (aOut.toFile ()))
    {
      final String sXmp = new String (aDoc.getDocumentCatalog ().getMetadata ().toByteArray (), StandardCharsets.UTF_8);
      assertTrue (sXmp, sXmp.contains ("<fx:ConformanceLevel>EXTENDED</fx:ConformanceLevel>"));
    }
  }

  @Test
  public void testPdfAErrors () throws IOException
  {
    final Path aOut = _tempFile (".pdf");
    final String sUbl = _copyInvoice ().toString ();
    final String sFont = _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString ();
    // Not PDF
    assertEquals (2, _run ("-i", sUbl, "--pdfa", "3b", "-o", aOut.toString ()));
    assertEquals (2, _run ("-i", sUbl, "--embed-attachments", "-o", aOut.toString ()));
    assertEquals (2, _run ("-i", sUbl, "--pdfua", "-o", aOut.toString ()));
    // PDF/UA requires embedded fonts
    assertEquals (2, _run ("-i", sUbl, "-f", "PDF", "--pdfua", "-o", aOut.toString ()));
    // PDF/A-2 does not allow the attachments
    assertEquals (2,
                  _run ("-i", sUbl, "-f", "PDF", "--font", sFont, "--pdfa", "2b", "--embed-attachments", "-o", aOut.toString ()));
    // Unknown or tagged level
    assertEquals (2, _run ("-i", sUbl, "-f", "PDF", "--font", sFont, "--pdfa", "4x", "-o", aOut.toString ()));
    // Standard fonts
    assertEquals (2, _run ("-i", sUbl, "-f", "PDF", "--pdfa", "3b", "-o", aOut.toString ()));
    // PDF/A-2 does not allow embedded files
    assertEquals (2, _run ("-i", sUbl, "-f", "PDF", "--font", sFont, "--pdfa", "2b", "--embed-source", "-o", aOut.toString ()));
    // ZUGFeRD requires CII
    assertEquals (2, _run ("-i", sUbl, "-f", "PDF", "--font", sFont, "--zugferd", "-o", aOut.toString ()));
    // Contradicting options
    assertEquals (2,
                  _run ("-i",
                        _copyCiiInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--font",
                        sFont,
                        "--zugferd",
                        "--embed-source",
                        "-o",
                        aOut.toString ()));
    assertEquals (2, _run ("-i", sUbl, "-f", "PDF", "--font", sFont, "--zugferd-profile", "BASIC", "-o", aOut.toString ()));
    assertEquals (2,
                  _run ("-i",
                        _copyCiiInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--font",
                        sFont,
                        "--zugferd",
                        "--zugferd-profile",
                        "UNKNOWN",
                        "-o",
                        aOut.toString ()));
  }

  @Test
  public void testPdfUA () throws IOException
  {
    final Path aOut = _tempFile (".pdf");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "-f",
                        "PDF",
                        "--font",
                        _fontFile (EFontResourceRoboto.ROBOTO_NORMAL, "Regular.ttf").toString (),
                        "--pdfua",
                        "--pdfa",
                        "3a",
                        "-o",
                        aOut.toString ()));
    try (final PDDocument aDoc = Loader.loadPDF (aOut.toFile ()))
    {
      final String sXmp = new String (aDoc.getDocumentCatalog ().getMetadata ().toByteArray (), StandardCharsets.UTF_8);
      assertTrue (sXmp, sXmp.contains ("<pdfuaid:part>1</pdfuaid:part>"));
      assertTrue (sXmp, sXmp.contains ("<pdfaid:conformance>A</pdfaid:conformance>"));
      assertNotNull (aDoc.getDocumentCatalog ().getStructureTreeRoot ());
    }
  }

  @NonNull
  private static Path _themeWithWatermark () throws IOException
  {
    final Path aTheme = _tempFile (".json");
    Files.writeString (aTheme,
                       "{\"common\":{\"watermark\":{\"text\":\"THEME\",\"opacity\":0.5,\"rotation\":10,\"color\":\"#102030\",\"position\":\"top\",\"fontSize\":30}}}",
                       StandardCharsets.UTF_8);
    return aTheme;
  }

  @Test
  public void testWatermarkTextMergesOverTheTheme () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "--theme",
                        _themeWithWatermark ().toString (),
                        "--watermark",
                        "DRAFT",
                        "-o",
                        aOut.toString ()));
    final String sHtml = Files.readString (aOut, StandardCharsets.UTF_8);
    assertTrue (sHtml, sHtml.contains (">DRAFT</div>"));
    assertTrue (sHtml, sHtml.contains ("color:rgba(16,32,48,0.5)"));
    assertTrue (sHtml, sHtml.contains ("font-size:30.0pt"));
    assertTrue (sHtml, sHtml.contains ("top:4mm"));
  }

  @Test
  public void testWatermarkOptionsApplyToTheThemeWatermark () throws IOException
  {
    final Path aOut = _tempFile (".html");
    final Path aTheme = _tempFile (".json");
    Files.writeString (aTheme, "{\"common\":{\"watermark\":{\"text\":\"THEME\",\"opacity\":0.5,\"rotation\":10}}}", StandardCharsets.UTF_8);
    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "--theme",
                        aTheme.toString (),
                        "--watermark-opacity",
                        "0.25",
                        "-o",
                        aOut.toString ()));
    final String sOpacity = Files.readString (aOut, StandardCharsets.UTF_8);
    assertTrue (sOpacity, sOpacity.contains (">THEME</div>"));
    assertTrue (sOpacity, sOpacity.contains ("rgba(128,128,128,0.25)"));
    assertTrue (sOpacity, sOpacity.contains ("rotate(10.0deg)"));

    assertEquals (0,
                  _run ("-i",
                        _copyInvoice ().toString (),
                        "--theme",
                        aTheme.toString (),
                        "--watermark-rotation",
                        "20",
                        "-o",
                        aOut.toString ()));
    final String sRotation = Files.readString (aOut, StandardCharsets.UTF_8);
    assertTrue (sRotation, sRotation.contains ("rgba(128,128,128,0.5)"));
    assertTrue (sRotation, sRotation.contains ("rotate(20.0deg)"));

    // There is nothing to change
    assertEquals (2, _run ("-i", _copyInvoice ().toString (), "--watermark-opacity", "0.25", "-o", aOut.toString ()));
  }

  @Test
  public void testLineWidth () throws IOException
  {
    final Path aTemplate = _tempFile (".ptpl");
    Files.writeString (aTemplate, "aaaa bbbb cccc\n", StandardCharsets.UTF_8);
    final Path aOut = _tempFile (".txt");
    final String sInvoice = _copyInvoice ().toString ();
    assertEquals (0, _run ("-i", sInvoice, "-f", "TEXT", "-t", aTemplate.toString (), "--line-width", "6", "-o", aOut.toString ()));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).startsWith ("aaaa\nbbbb\ncccc\n"));

    // The theme, and the option on top of it
    final Path aTheme = _tempFile (".json");
    Files.writeString (aTheme, "{\"text\":{\"lineWidth\":10}}", StandardCharsets.UTF_8);
    assertEquals (0, _run ("-i", sInvoice, "-f", "TEXT", "-t", aTemplate.toString (), "--theme", aTheme.toString (), "-o", aOut.toString ()));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).startsWith ("aaaa bbbb\ncccc\n"));
    assertEquals (0,
                  _run ("-i",
                        sInvoice,
                        "-f",
                        "TEXT",
                        "-t",
                        aTemplate.toString (),
                        "--theme",
                        aTheme.toString (),
                        "--line-width",
                        "0",
                        "-o",
                        aOut.toString ()));
    assertTrue (Files.readString (aOut, StandardCharsets.UTF_8).startsWith ("aaaa bbbb cccc\n"));

    assertEquals (2, _run ("-i", sInvoice, "-f", "TEXT", "--line-width", "-1", "-o", aOut.toString ()));
    assertEquals (2, _run ("-i", sInvoice, "-f", "HTML", "--line-width", "10", "-o", aOut.toString ()));
  }

  @Test
  public void testUnsupportedLanguageIsAUsageError () throws IOException
  {
    final Path aOut = _tempFile (".html");
    assertEquals (2, _run ("-i", _copyInvoice ().toString (), "-l", "xx", "-o", aOut.toString ()));
    assertEquals (0, _run ("-i", _copyInvoice ().toString (), "-l", "de_AT", "-o", aOut.toString ()));
  }

  @Test
  public void testFontsAreRefusedForText () throws IOException
  {
    final Path aOut = _tempFile (".txt");
    final Path aInvoice = _copyInvoice ();
    assertEquals (2, _run ("-i", aInvoice.toString (), "-f", "TEXT", "--font", aInvoice.toString (), "-o", aOut.toString ()));
    assertEquals (2,
                  _run ("-i",
                        aInvoice.toString (),
                        "-f",
                        "TEXT",
                        "--font-bold",
                        aInvoice.toString (),
                        "-o",
                        aOut.toString ()));
  }

  @Test
  public void testSniffOfMalformedInputLogsNothing ()
  {
    final PrintStream aOldErr = System.err;
    final ByteArrayOutputStream aErr = new ByteArrayOutputStream ();
    try (final PrintStream aNewErr = new PrintStream (aErr, true, StandardCharsets.UTF_8))
    {
      System.setErr (aNewErr);
      assertEquals (PhloxCli.ESourceSyntax.UBL, PhloxCli._sniffSyntax ("<a><b>".getBytes (StandardCharsets.UTF_8)));
    }
    finally
    {
      System.setErr (aOldErr);
    }
    assertEquals ("", aErr.toString (StandardCharsets.UTF_8));
  }
}
