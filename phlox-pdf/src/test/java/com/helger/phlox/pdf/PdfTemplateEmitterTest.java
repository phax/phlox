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
import static org.junit.Assert.fail;

import java.awt.image.BufferedImage;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringHelper;
import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.SimpleFileIO;
import com.helger.font.roboto.EFontResourceRoboto;
import com.helger.io.resource.ClassPathResource;
import com.helger.pdflayout.base.PLColor;
import com.helger.pdflayout.pdfa.EPdfALevel;
import com.helger.phlox.api.PhloxRuntimeException;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.api.watermark.WatermarkSpec;
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
          assertFalse ("Unresolved sentinel in " + aRes.getPath (), sText.contains ("PHLOX:"));
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

  private static int _fillOperators (final byte [] aPdf)
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      int nCount = 0;
      for (final Object aToken : new PDFStreamParser (aDoc.getPage (0)).parse ())
        if (aToken instanceof final Operator aOperator && "f".equals (aOperator.getName ()))
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
    assertNotNull (aCtx.getBlockStyle ("due"));
    assertEquals (null, aCtx.getBlockStyle ("unknown"));
    aCtx.putBlockStyle ("warn", new PdfBlockStyle (PLColor.YELLOW, PLColor.RED));
    assertEquals (PLColor.YELLOW, aCtx.getBlockStyle ("warn").getFillColor ());
    assertEquals (PLColor.RED, aCtx.getBlockStyle ("warn").getTextColor ());
  }

  @Test
  public void testDataImage () throws IOException
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final BufferedImage aSource = new BufferedImage (8, 8, BufferedImage.TYPE_INT_RGB);
    final NonBlockingByteArrayOutputStream aPng = new NonBlockingByteArrayOutputStream ();
    ImageIO.write (aSource, "png", aPng);
    final String sData = "data:image/png;base64," + Base64.getEncoder ().encodeToString (aPng.toByteArray ());

    final byte [] aPdf = _render (aModel, PhloxTemplate.fromString ("before\n\n![Stamp](" + sData + ")\n"), Locale.ENGLISH);
    int nImages = 0;
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      for (final COSName aName : aDoc.getPage (0).getResources ().getXObjectNames ())
        if (aDoc.getPage (0).getResources ().getXObject (aName) instanceof PDImageXObject)
          nImages++;
    }
    assertEquals (1, nImages);
  }

  @Test
  public void testAllStylesOfABlockApply () throws Exception
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final PdfRenderContext aCtx = new PdfRenderContext ().setTemplate (PhloxTemplate.fromString ("{{class:big strong}}\n\nStyled\n"));
    aCtx.putBlockStyle ("big", PdfBlockStyle.builder ().fontSize (30).build ());
    aCtx.putBlockStyle ("strong", PdfBlockStyle.builder ().bold (true).build ());
    final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
    new PdfInvoiceWriter ().writeTo (aModel, aCtx, aBaos);

    final Wrapper <TextPosition> aFirst = new Wrapper <> ();
    try (final PDDocument aDoc = Loader.loadPDF (aBaos.toByteArray ()))
    {
      new PDFTextStripper ()
      {
        @Override
        protected void processTextPosition (final TextPosition aText)
        {
          if (aFirst.get () == null)
            aFirst.set (aText);
          super.processTextPosition (aText);
        }
      }.getText (aDoc);
    }
    assertEquals (30f, aFirst.get ().getFontSizeInPt (), 0.01f);
    assertTrue (aFirst.get ().getFont ().getName (), aFirst.get ().getFont ().getName ().contains ("Bold"));
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

  private static PhloxInvoice _example1 ()
  {
    return _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
  }

  private static byte [] _render (final PdfRenderContext aCtx) throws Exception
  {
    final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
    new PdfInvoiceWriter ().writeTo (_example1 (), aCtx, aBaos);
    return aBaos.toByteArray ();
  }

  private static byte [] _render (final String sTemplate) throws Exception
  {
    return _render (new PdfRenderContext ().setTemplate (PhloxTemplate.fromString (sTemplate)));
  }

  private static int _pageCount (final byte [] aPdf) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      return aDoc.getNumberOfPages ();
    }
  }

  private static String _pageText (final byte [] aPdf, final int nPage) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      final PDFTextStripper aStripper = new PDFTextStripper ();
      aStripper.setStartPage (nPage);
      aStripper.setEndPage (nPage);
      return aStripper.getText (aDoc);
    }
  }

  /** @return The first character of a text, with the font and the size it is drawn in. */
  private static TextPosition _findChar (final byte [] aPdf, final String sChar) throws IOException
  {
    final Wrapper <TextPosition> ret = new Wrapper <> ();
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      new PDFTextStripper ()
      {
        @Override
        protected void processTextPosition (final TextPosition aText)
        {
          if (ret.get () == null && sChar.equals (aText.getUnicode ()))
            ret.set (aText);
          super.processTextPosition (aText);
        }
      }.getText (aDoc);
    }
    assertNotNull ("Did not find " + sChar, ret.get ());
    return ret.get ();
  }

  private static void _writePngChunk (final DataOutputStream aOut, final String sType, final byte [] aData) throws IOException
  {
    final byte [] aTypeBytes = sType.getBytes (StandardCharsets.ISO_8859_1);
    final CRC32 aCRC = new CRC32 ();
    aCRC.update (aTypeBytes);
    aCRC.update (aData);
    aOut.writeInt (aData.length);
    aOut.write (aTypeBytes);
    aOut.write (aData);
    aOut.writeInt ((int) aCRC.getValue ());
  }

  /** @return A valid PNG start with the given size, but with hardly any pixel data. */
  private static byte [] _createPngWithoutPixels (final int nWidth, final int nHeight) throws IOException
  {
    final NonBlockingByteArrayOutputStream aHeader = new NonBlockingByteArrayOutputStream ();
    try (final DataOutputStream aOut = new DataOutputStream (aHeader))
    {
      aOut.writeInt (nWidth);
      aOut.writeInt (nHeight);
      // 8 bit truecolour with alpha
      aOut.write (new byte [] { 8, 6, 0, 0, 0 });
    }
    final NonBlockingByteArrayOutputStream aPixels = new NonBlockingByteArrayOutputStream ();
    try (final DeflaterOutputStream aOut = new DeflaterOutputStream (aPixels))
    {
      aOut.write (new byte [1000]);
    }

    final NonBlockingByteArrayOutputStream ret = new NonBlockingByteArrayOutputStream ();
    try (final DataOutputStream aOut = new DataOutputStream (ret))
    {
      aOut.write (new byte [] { (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a });
      _writePngChunk (aOut, "IHDR", aHeader.toByteArray ());
      _writePngChunk (aOut, "IDAT", aPixels.toByteArray ());
      _writePngChunk (aOut, "IEND", new byte [0]);
    }
    return ret.toByteArray ();
  }

  /** @return A complete PNG with 1 bit per pixel, so that it needs little memory. */
  private static byte [] _createBlackAndWhitePng (final int nWidth, final int nHeight) throws IOException
  {
    final NonBlockingByteArrayOutputStream aHeader = new NonBlockingByteArrayOutputStream ();
    try (final DataOutputStream aOut = new DataOutputStream (aHeader))
    {
      aOut.writeInt (nWidth);
      aOut.writeInt (nHeight);
      // 1 bit greyscale
      aOut.write (new byte [] { 1, 0, 0, 0, 0 });
    }
    final NonBlockingByteArrayOutputStream aPixels = new NonBlockingByteArrayOutputStream ();
    try (final DeflaterOutputStream aOut = new DeflaterOutputStream (aPixels))
    {
      // One filter byte per row
      final byte [] aRow = new byte [1 + (nWidth + 7) / 8];
      for (int i = 0; i < nHeight; ++i)
        aOut.write (aRow);
    }

    final NonBlockingByteArrayOutputStream ret = new NonBlockingByteArrayOutputStream ();
    try (final DataOutputStream aOut = new DataOutputStream (ret))
    {
      aOut.write (new byte [] { (byte) 0x89, 'P', 'N', 'G', 0x0d, 0x0a, 0x1a, 0x0a });
      _writePngChunk (aOut, "IHDR", aHeader.toByteArray ());
      _writePngChunk (aOut, "IDAT", aPixels.toByteArray ());
      _writePngChunk (aOut, "IEND", new byte [0]);
    }
    return ret.toByteArray ();
  }

  private static int _imageCount (final byte [] aPdf) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      int nCount = 0;
      for (final COSName aName : aDoc.getPage (0).getResources ().getXObjectNames ())
        if (aDoc.getPage (0).getResources ().isImageXObject (aName))
          nCount++;
      return nCount;
    }
  }

  /** @return The largest height any image is drawn with on the first page. */
  private static float _maxImageHeight (final byte [] aPdf) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      float ret = 0;
      final List <Object> aTokens = new PDFStreamParser (aDoc.getPage (0)).parse ();
      for (int i = 6; i < aTokens.size (); ++i)
        if (aTokens.get (i) instanceof final Operator aOp &&
            "cm".equals (aOp.getName ()) &&
            aTokens.get (i - 3) instanceof final COSNumber aScale)
          ret = Math.max (ret, aScale.floatValue ());
      return ret;
    }
  }

  private static String _dataImage (final byte [] aImage)
  {
    return "![Pic](data:image/png;base64," + Base64.getEncoder ().encodeToString (aImage) + ")";
  }

  @Test (timeout = 20_000)
  public void testRunOfSpacesIsLinear () throws Exception
  {
    // Took minutes in the regular expressions of the markup parser
    final byte [] aPdf = _render ("Before" + StringHelper.getRepeated (' ', 200_000) + "After `code" + StringHelper.getRepeated (' ', 200_000) + "x`");
    final String sText = _text (aPdf);
    assertTrue (sText.contains ("Before"));
    assertTrue (sText.contains ("After"));
  }

  @Test
  public void testBlocksHigherThanAPageAreSplit () throws Exception
  {
    final String sLong = StringHelper.getRepeated ('x', 40_000) + " THEEND";
    final String sWords = StringHelper.getRepeated ("word ", 8_000) + "THEEND";
    final PdfRenderContext aKeepCtx = new PdfRenderContext ().putBlockStyle ("keep", PdfBlockStyle.builder ().keepTogether (true).build ());

    for (final String sTemplate : new String [] { "# " + sLong,
                                                  "{{row:r}}\n\n{{group:a}}\n\n" + sLong + "\n\n{{end}}\n\n{{group:b}}\n\nb\n\n{{end}}\n\n{{end}}\n",
                                                  "{{row:r}}\n\n{{group:a}}\n\n## " + sWords + "\n\n{{end}}\n\n{{end}}\n",
                                                  "{{group:keep}}\n\n" + sWords + "\n\n{{end}}\n",
                                                  "| A | B |\n|---|---|\n| " + sWords + " | b |\n",
                                                  "| A | B |\n|---|---|\n| " + sLong + " | b |\n" })
    {
      final byte [] aPdf = _render (aKeepCtx.setTemplate (PhloxTemplate.fromString (sTemplate)));
      assertTrue (_pageCount (aPdf) > 1);
      assertTrue (sTemplate.substring (0, 16), _text (aPdf).contains ("THEEND"));
    }
  }

  @Test
  public void testBlockThatFitsIsKeptTogether () throws Exception
  {
    final PdfRenderContext aCtx = new PdfRenderContext ().putBlockStyle ("keep", PdfBlockStyle.builder ().keepTogether (true).build ());
    final String sFill = StringHelper.getRepeated ("fill\n\n", 45);
    final byte [] aPdf = _render (aCtx.setTemplate (PhloxTemplate.fromString (sFill +
                                                                              "{{group:keep}}\n\nFIRSTLINE\n\nx\n\nx\n\nx\n\nx\n\nLASTLINE\n\n{{end}}\n")));
    assertEquals (2, _pageCount (aPdf));
    assertTrue (_pageText (aPdf, 2).contains ("FIRSTLINE"));
    assertTrue (_pageText (aPdf, 2).contains ("LASTLINE"));
  }

  @Test
  public void testCodeBlockUsesTheFontFamily () throws Exception
  {
    final PdfRenderContext aCtx = new PdfRenderContext ().setFontFamily (PdfFontFamily.of (EFontResourceRoboto.ROBOTO_NORMAL,
                                                                                           EFontResourceRoboto.ROBOTO_BOLD,
                                                                                           EFontResourceRoboto.ROBOTO_NORMAL_ITALIC,
                                                                                           EFontResourceRoboto.ROBOTO_BOLD_ITALIC))
                                                         .setPdfALevel (EPdfALevel.PDF_A_3B)
                                                         .setTemplate (PhloxTemplate.fromString ("```\nZ\n```\n\n    indented\n"));
    final byte [] aPdf = _render (aCtx);
    assertTrue (_findChar (aPdf, "Z").getFont ().getName (), _findChar (aPdf, "Z").getFont ().getName ().contains ("Roboto"));
    assertFalse (_fontNames (aPdf).toString (), _fontNames (aPdf).toString ().contains ("Courier"));

    // Without a family it stays the monospace font of PDF
    final byte [] aStandard = _render ("```\nZ\n```\n");
    assertTrue (_findChar (aStandard, "Z").getFont ().getName ().contains ("Courier"));
  }

  @Test
  public void testPageBreak () throws Exception
  {
    assertEquals (2, _pageCount (_render ("a\n\n{{pagebreak}}\n\nb\n")));

    // No empty page at the edges of the document
    assertEquals (1, _pageCount (_render ("{{pagebreak}}\n\na\n")));
    assertEquals (1, _pageCount (_render ("a\n\n{{pagebreak}}\n")));
    assertEquals (1, _pageCount (_render ("{{group:g}}\n\na\n\n{{pagebreak}}\n\n{{end}}\n")));

    // A group of the top level is split
    final byte [] aPdf = _render ("{{group:g}}\n\nFIRST\n\n{{pagebreak}}\n\nSECOND\n\n{{end}}\n");
    assertEquals (2, _pageCount (aPdf));
    assertTrue (_pageText (aPdf, 1).contains ("FIRST"));
    assertTrue (_pageText (aPdf, 2).contains ("SECOND"));

    // Both parts have the style of the group
    final PdfRenderContext aCtx = new PdfRenderContext ().putBlockStyle ("g", PdfBlockStyle.builder ().fontSize (21).build ());
    final byte [] aStyled = _render (aCtx.setTemplate (PhloxTemplate.fromString ("{{group:g}}\n\nX\n\n{{pagebreak}}\n\nY\n\n{{end}}\n")));
    assertEquals (21f, _findChar (aStyled, "X").getFontSizeInPt (), 0.01f);
    assertEquals (21f, _findChar (aStyled, "Y").getFontSizeInPt (), 0.01f);

    // At the edges of a group no empty group is left
    assertEquals (2, _pageCount (_render ("{{group:g}}\n\nFIRST\n\n{{pagebreak}}\n\n{{end}}\n\nnext\n")));

    // Where it cannot work it is ignored
    assertEquals (1, _pageCount (_render ("{{group:g}}\n\n{{group:h}}\n\na\n\n{{pagebreak}}\n\nb\n\n{{end}}\n\n{{end}}\n")));
    assertEquals (1, _pageCount (_render ("{{row:r}}\n\n{{group:g}}\n\na\n\n{{pagebreak}}\n\nb\n\n{{end}}\n\n{{end}}\n")));
  }

  @Test
  public void testGroupPassesTheTextStyleToItsBlocks () throws Exception
  {
    final PdfRenderContext aCtx = new PdfRenderContext ().putBlockStyle ("g",
                                                                         PdfBlockStyle.builder ().fontSize (21).bold (true).build ());
    final byte [] aPdf = _render (aCtx.setTemplate (PhloxTemplate.fromString ("{{group:g}}\n\nP\n\n| A | B |\n|---|---|\n| T | c |\n\n- L\n\n> Q\n\n### H\n\n{{end}}\n\nO\n")));
    for (final String sChar : new String [] { "P", "T", "L", "Q" })
    {
      assertEquals (sChar, 21f, _findChar (aPdf, sChar).getFontSizeInPt (), 0.01f);
      assertTrue (sChar, _findChar (aPdf, sChar).getFont ().getName ().contains ("Bold"));
    }
    // A heading keeps its size
    assertEquals (11f, _findChar (aPdf, "H").getFontSizeInPt (), 0.01f);
    // And the group ends with the group
    assertEquals (10f, _findChar (aPdf, "O").getFontSizeInPt (), 0.01f);
    assertFalse (_findChar (aPdf, "O").getFont ().getName ().contains ("Bold"));
  }

  @Test
  public void testAmountDueIsBoldInTheDefaultTemplate () throws Exception
  {
    final byte [] aPdf = _render (new PdfRenderContext ().setLocale (Locale.ENGLISH));
    final Wrapper <Boolean> aFound = new Wrapper <> (Boolean.FALSE);
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      new PDFTextStripper ()
      {
        @Override
        protected void writeString (final String sText, final List <TextPosition> aPositions) throws IOException
        {
          // The amount due of the example
          if (sText.contains ("Amount due") && aPositions.get (0).getFont ().getName ().contains ("Bold"))
            aFound.set (Boolean.TRUE);
          super.writeString (sText, aPositions);
        }
      }.getText (aDoc);
    }
    assertTrue (_text (aPdf), aFound.get ().booleanValue ());
  }

  @Test
  public void testBlockStylesOfBlocks () throws Exception
  {
    final PdfRenderContext aCtx = new PdfRenderContext ().putBlockStyle ("s", PdfBlockStyle.builder ().fontSize (21).build ());
    for (final String sTemplate : new String [] { "{{class:s}}\n| A | B |\n|---|---|\n| X | c |\n",
                                                  "{{class:s}}\n- X\n",
                                                  "{{class:s}}\n1. X\n",
                                                  "{{class:s}}\n> X\n",
                                                  "{{class:s}}\n```\nX\n```\n" })
      assertEquals (sTemplate,
                    21f,
                    _findChar (_render (aCtx.setTemplate (PhloxTemplate.fromString (sTemplate))), "X").getFontSizeInPt (),
                    0.01f);
  }

  @Test
  public void testBlockStyleDecoratesTablesListsQuotesAndRows () throws Exception
  {
    final PdfRenderContext aCtx = new PdfRenderContext ().putBlockStyle ("s", PdfBlockStyle.fill (PLColor.YELLOW));
    final int nPlain = _fillOperators (_render ("{{row:other}}\n\n{{group:a}}\n\nX\n\n{{end}}\n\n{{end}}\n"));
    for (final String sTemplate : new String [] { "{{class:s}}\n| A | B |\n|---|---|\n| X | c |\n",
                                                  "{{class:s}}\n- X\n",
                                                  "{{class:s}}\n> X\n",
                                                  "{{row:s}}\n\n{{group:a}}\n\nX\n\n{{end}}\n\n{{end}}\n" })
    {
      final int nFilled = _fillOperators (_render (aCtx.setTemplate (PhloxTemplate.fromString (sTemplate))));
      assertTrue (sTemplate + " has " + nFilled + " fills", nFilled > 0);
    }
    assertEquals (0, nPlain);
  }

  @Test
  public void testTableRowStyle () throws Exception
  {
    final PdfRenderContext aCtx = new PdfRenderContext ().putBlockStyle ("s",
                                                                         PdfBlockStyle.builder ()
                                                                                      .fontSize (21)
                                                                                      .padding (9)
                                                                                      .build ());
    final byte [] aStyled = _render (aCtx.setTemplate (PhloxTemplate.fromString ("| A | B |\n|---|---|\n{{class:s}}\n| X | c |\n| Y | d |\n")));
    assertEquals (21f, _findChar (aStyled, "X").getFontSizeInPt (), 0.01f);
    assertEquals (10f, _findChar (aStyled, "Y").getFontSizeInPt (), 0.01f);

    // The padding moves the next row down
    final byte [] aPlain = _render (aCtx.putBlockStyle ("s", PdfBlockStyle.builder ().fontSize (21).build ()));
    final float fPlain = _findChar (aPlain, "Y").getY () - _findChar (aPlain, "X").getY ();
    final float fStyled = _findChar (aStyled, "Y").getY () - _findChar (aStyled, "X").getY ();
    assertEquals (6f, fStyled - fPlain, 0.5f);
  }

  @Test
  public void testInlineHtmlIsShownAsText () throws Exception
  {
    final String sText = _text (_render ("Before <span>inner</span> after<br>next"));
    assertTrue (sText, sText.contains ("Before <span>inner</span> after"));
    assertFalse (sText, sText.contains ("<br>"));
    assertTrue (sText, sText.contains ("next"));
  }

  @Test
  public void testImageWithHugeDimensionsIsSkipped () throws Exception
  {
    // A few bytes that would need 3.6 GB when decoded
    final String sText = _text (_render ("Text\n\n" + _dataImage (_createPngWithoutPixels (30_000, 30_000)) + "\n\nAfter\n"));
    assertTrue (sText, sText.contains ("After"));
  }

  @Test
  public void testImageAboveThePixelLimitIsNotDecoded () throws Exception
  {
    assertEquals (1, _imageCount (_render (_dataImage (_createBlackAndWhitePng (100, 100)) + "\n\nAfter\n")));
    // 36 million pixels, a little above the limit
    assertEquals (0, _imageCount (_render (_dataImage (_createBlackAndWhitePng (6000, 6000)) + "\n\nAfter\n")));
  }

  @Test
  public void testDamagedImagesAreSkipped () throws Exception
  {
    // The header is valid, most pixels are missing
    final String sTruncated = _text (_render ("Text\n\n" + _dataImage (_createPngWithoutPixels (10, 10)) + "\n\nAfter\n"));
    assertTrue (sTruncated, sTruncated.contains ("After"));
    final String sGarbage = _text (_render ("Text\n\n" + _dataImage (new byte [] { 1, 2, 3 }) + "\n\nAfter\n"));
    assertTrue (sGarbage, sGarbage.contains ("After"));
  }

  @Test
  public void testVeryTallImageFitsThePage () throws Exception
  {
    final BufferedImage aImage = new BufferedImage (10, 8000, BufferedImage.TYPE_INT_RGB);
    final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
    assertTrue (ImageIO.write (aImage, "png", aBaos));
    final byte [] aPdf = _render (_dataImage (aBaos.toByteArray ()) + "\n");
    assertEquals (1, _pageCount (aPdf));
    final float fHeight = _maxImageHeight (aPdf);
    assertTrue (Float.toString (fHeight), fHeight > 100f && fHeight < PdfRenderContext.DEFAULT_PAGE_SIZE.getHeight ());
  }

  @Test
  public void testTemplateWithoutPdfContentHasAClearError () throws Exception
  {
    for (final String sTemplate : new String [] { "![Gone](attachment:logo)\n",
                                                  "![Svg](data:image/svg+xml;base64,PHN2Zy8+)\n" })
      try
      {
        _render (sTemplate);
        fail ();
      }
      catch (final PhloxRuntimeException ex)
      {
        assertTrue (ex.getMessage (), ex.getMessage ().contains ("no content"));
      }
  }

  @Test
  public void testUserWatermarkOnFirstPageOnly () throws Exception
  {
    final String sTemplate = "one\n\n{{pagebreak}}\n\ntwo\n\n{{pagebreak}}\n\nthree\n";
    final byte [] aAll = _render (new PdfRenderContext ().setTemplate (PhloxTemplate.fromString (sTemplate))
                                                         .setUserWatermark (WatermarkSpec.text ("STAMP")));
    assertEquals (3, _pageCount (aAll));
    assertTrue (_pageText (aAll, 1).contains ("STAMP"));
    assertTrue (_pageText (aAll, 3).contains ("STAMP"));

    final byte [] aFirst = _render (new PdfRenderContext ().setTemplate (PhloxTemplate.fromString (sTemplate))
                                                           .setUserWatermark (WatermarkSpec.text ("STAMP").setRepeatPerPage (false)));
    assertEquals (3, _pageCount (aFirst));
    assertTrue (_pageText (aFirst, 1).contains ("STAMP"));
    assertFalse (_pageText (aFirst, 2).contains ("STAMP"));
    assertFalse (_pageText (aFirst, 3).contains ("STAMP"));
    // The order of the pages stays
    assertTrue (_pageText (aFirst, 2).contains ("two"));
    assertTrue (_pageText (aFirst, 3).contains ("three"));
  }
}
