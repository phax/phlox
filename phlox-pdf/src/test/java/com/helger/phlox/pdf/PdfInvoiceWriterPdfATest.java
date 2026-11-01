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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Map;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider;
import org.verapdf.pdfa.Foundries;
import org.verapdf.pdfa.PDFAParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.flavours.PDFAFlavour;
import org.verapdf.pdfa.results.TestAssertion;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.io.stream.StreamHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.font.roboto.EFontResourceRoboto;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.kaltblut.core.extract.HybridExtractor;
import com.helger.kaltblut.core.inspect.HybridInspector;
import com.helger.kaltblut.core.model.EAFRelationship;
import com.helger.kaltblut.core.model.EZugferdCountry;
import com.helger.kaltblut.core.model.EZugferdProfile;
import com.helger.kaltblut.core.model.HybridMetadata;
import com.helger.kaltblut.core.source.HybridSource;
import com.helger.kaltblut.core.source.IHybridSource;
import com.helger.kaltblut.core.validate.HybridFinding;
import com.helger.kaltblut.core.validate.HybridValidationResult;
import com.helger.kaltblut.core.validate.HybridValidator;
import com.helger.kaltblut.verapdf.VeraPdfA3ValidatorSPI;
import com.helger.pdflayout.pdfa.EPdfALevel;
import com.helger.phlox.api.PhloxRuntimeException;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Test class for the PDF/A, embedded source and ZUGFeRD / Factur-X output of class
 * {@link PdfInvoiceWriter}. The PDFs are checked with veraPDF and kaltblut and written to
 * <code>generated/pdfa</code>.
 *
 * @author Philip Helger
 */
public final class PdfInvoiceWriterPdfATest
{
  private static final String UBL_FILE = "2017/ubl/invoice/guide-example1.xml";
  private static final String CII_FILE = "2017/cii/CII_example1.xml";
  private static final String CII_XRECHNUNG_FILE = "2017/cii/xrechnung/3.0.2/01.01a-INVOICE_uncefact.xml";
  private static final String CII_D25A_FILE = "2026/cii/d25a-edge-payment-invoice.xml";
  private static final String UBL_ATTACHMENT_FILE = "2017/ubl/invoice/ubl-tc434-example2.xml";
  private static final String CII_ATTACHMENT_FILE = "2017/cii/CII_example2.xml";

  private static byte @NonNull [] _readBytes (@NonNull final String sPath)
  {
    final byte [] ret = StreamHelper.getAllBytes (new ClassPathResource (MockPhloxTestFiles.BASE_PATH + sPath).getInputStream ());
    assertNotNull (sPath, ret);
    return ret;
  }

  @NonNull
  private static PhloxInvoice _readUbl (final byte @NonNull [] aXml)
  {
    final PhloxInvoice ret = UblInvoiceReader.autoDetect ().read (new NonBlockingByteArrayInputStream (aXml),
                                                                  new ErrorList ());
    assertNotNull (ret);
    return ret;
  }

  @NonNull
  private static PhloxInvoice _readCii (final byte @NonNull [] aXml)
  {
    final PhloxInvoice ret = CiiInvoiceReader.autoDetect ().read (new NonBlockingByteArrayInputStream (aXml),
                                                                  new ErrorList ());
    assertNotNull (ret);
    return ret;
  }

  @NonNull
  private static PdfRenderContext _createContext (@NonNull final EPdfALevel eLevel)
  {
    return new PdfRenderContext ().setFontFamily (PdfFontFamily.of (EFontResourceRoboto.ROBOTO_NORMAL,
                                                                    EFontResourceRoboto.ROBOTO_BOLD,
                                                                    EFontResourceRoboto.ROBOTO_NORMAL_ITALIC,
                                                                    EFontResourceRoboto.ROBOTO_BOLD_ITALIC))
                                  .setPdfALevel (eLevel)
                                  .setDocumentCreationDateTime (ZonedDateTime.of (2026, 10, 28, 12, 0, 0, 0, ZoneOffset.UTC));
  }

  private static byte @NonNull [] _render (@NonNull final PhloxInvoice aInvoice,
                                           @NonNull final PdfRenderContext aCtx,
                                           @NonNull final String sName) throws Exception
  {
    final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
    new PdfInvoiceWriter ().writeTo (aInvoice, aCtx, aBaos);
    final byte [] ret = aBaos.toByteArray ();
    SimpleFileIO.writeFile (new File ("generated/pdfa/" + sName + ".pdf"), ret);
    return ret;
  }

  private static void _assertPdfA3Valid (final byte @NonNull [] aPdf) throws Exception
  {
    final ICommonsList <HybridFinding> aFindings = new VeraPdfA3ValidatorSPI ().validatePdfA3 (HybridSource.fromBytes (aPdf));
    assertTrue (aFindings.toString (), aFindings.isEmpty ());
  }

  @Test
  public void testPdfA3b () throws Exception
  {
    final PhloxInvoice aInvoice = _readUbl (_readBytes (UBL_FILE));
    _assertPdfA3Valid (_render (aInvoice, _createContext (EPdfALevel.PDF_A_3B), "ubl-3b"));
    _assertPdfA3Valid (_render (aInvoice, _createContext (EPdfALevel.PDF_A_3U), "ubl-3u"));

    // With the logo of the tests (PNG)
    final PdfRenderContext aCtx = _createContext (EPdfALevel.PDF_A_3B);
    try (final InputStream aIS = MockPhloxTestFiles.getLogo ().getInputStream ())
    {
      aCtx.setLogo (new PhloxAttachment ().setBinary (aIS.readAllBytes ()).setMimeCode ("image/png").setFilename ("phoss.png"));
    }
    _assertPdfA3Valid (_render (aInvoice, aCtx, "ubl-3b-logo"));

    // With a user watermark
    _assertPdfA3Valid (_render (aInvoice,
                                _createContext (EPdfALevel.PDF_A_3B).setUserWatermark (WatermarkSpec.text ("DRAFT")),
                                "ubl-3b-watermark"));

    // With a QR code, also as a tagged PDF
    final PhloxTemplate aQrTemplate = PhloxTemplate.fromString ("# {{BT-1}}\n\n![Payment](qrcode:{{BT-1 | url}} \"height-px:80\")");
    _assertPdfA3Valid (_render (aInvoice, _createContext (EPdfALevel.PDF_A_3B).setTemplate (aQrTemplate), "ubl-3b-qr"));
    _assertPdfA3Valid (_render (aInvoice, _createContext (EPdfALevel.PDF_A_3A).setTemplate (aQrTemplate), "ubl-3a-qr"));
  }

  @Test
  public void testEmbeddedSource () throws Exception
  {
    final byte [] aXml = _readBytes (UBL_FILE);
    final byte [] aPdf = _render (_readUbl (aXml),
                                  _createContext (EPdfALevel.PDF_A_3B).setEmbeddedSource (PdfEmbeddedSource.ofSource (aXml,
                                                                                                                      "invoice.xml")),
                                  "ubl-3b-source");
    _assertPdfA3Valid (aPdf);

    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      final PDComplexFileSpecification aFileSpec = aDoc.getDocumentCatalog ()
                                                       .getNames ()
                                                       .getEmbeddedFiles ()
                                                       .getNames ()
                                                       .get ("invoice.xml");
      assertNotNull (aFileSpec);
      assertEquals ("Source", aFileSpec.getCOSObject ().getNameAsString (COSName.AF_RELATIONSHIP));
      assertArrayEquals (aXml, aFileSpec.getEmbeddedFileUnicode ().toByteArray ());
    }
  }

  private static void _assertFacturX (final byte @NonNull [] aXml,
                                      final byte @NonNull [] aPdf,
                                      @NonNull final EZugferdProfile eExpectedProfile,
                                      @NonNull final String sExpectedFileName) throws Exception
  {
    final IHybridSource aSource = HybridSource.fromBytes (aPdf);

    final HybridValidator aValidator = new HybridValidator ();
    aValidator.getSettings ().setCountry (EZugferdCountry.DE).setCheckPdfA3 (true);
    final HybridValidationResult aResult = aValidator.validate (aSource);
    assertTrue (aResult.getAllFindings ().toString (), aResult.isValid ());

    final HybridMetadata aMeta = HybridInspector.readMetadata (aSource);
    assertTrue (aMeta.toString (), aMeta.isRecognisedHybridInvoice ());
    assertEquals (eExpectedProfile, aMeta.getProfile ());
    assertEquals (sExpectedFileName, aMeta.getEmbeddedFileName ());
    assertEquals (EAFRelationship.ALTERNATIVE, aMeta.getAFRelationship ());
    assertArrayEquals (aXml, HybridExtractor.extractInvoiceXml (aSource));
  }

  @Test
  public void testFacturX () throws Exception
  {
    final byte [] aXml = _readBytes (CII_FILE);
    final byte [] aPdf = _render (_readCii (aXml),
                                  _createContext (EPdfALevel.PDF_A_3B).setEmbeddedSource (PdfEmbeddedSource.ofFacturX (aXml)),
                                  "cii-factur-x");
    _assertFacturX (aXml, aPdf, EZugferdProfile.EN_16931, "factur-x.xml");
  }

  @Test
  public void testFacturXXRechnung () throws Exception
  {
    final byte [] aXml = _readBytes (CII_XRECHNUNG_FILE);
    final byte [] aPdf = _render (_readCii (aXml),
                                  _createContext (EPdfALevel.PDF_A_3B).setEmbeddedSource (PdfEmbeddedSource.ofFacturX (aXml)),
                                  "cii-xrechnung");
    _assertFacturX (aXml, aPdf, EZugferdProfile.XRECHNUNG, "xrechnung.xml");
  }

  private static void _assertFails (@NonNull final PhloxInvoice aInvoice,
                                    @NonNull final PdfRenderContext aCtx,
                                    @NonNull final String sExpectedText) throws Exception
  {
    try
    {
      new PdfInvoiceWriter ().writeTo (aInvoice, aCtx, new NonBlockingByteArrayOutputStream ());
      fail ();
    }
    catch (final PhloxRuntimeException ex)
    {
      assertTrue (ex.getMessage (), ex.getMessage ().contains (sExpectedText));
    }
  }

  @Test
  public void testErrors () throws Exception
  {
    final byte [] aUbl = _readBytes (UBL_FILE);
    final PhloxInvoice aUblInvoice = _readUbl (aUbl);

    // Standard fonts
    _assertFails (aUblInvoice, new PdfRenderContext ().setPdfALevel (EPdfALevel.PDF_A_3B), "embedded fonts");
    _assertFails (aUblInvoice, new PdfRenderContext ().setPdfUA (true), "embedded fonts");
    // Embedding requires PDF/A-3
    _assertFails (aUblInvoice,
                  _createContext (EPdfALevel.PDF_A_3B).setPdfALevel (null)
                                                      .setEmbeddedSource (PdfEmbeddedSource.ofSource (aUbl, "a.xml")),
                  "PDF/A-3");
    _assertFails (aUblInvoice,
                  _createContext (EPdfALevel.PDF_A_2B).setEmbeddedSource (PdfEmbeddedSource.ofSource (aUbl, "a.xml")),
                  "PDF/A-3");
    // Factur-X requires CII
    _assertFails (aUblInvoice,
                  _createContext (EPdfALevel.PDF_A_3B).setEmbeddedSource (PdfEmbeddedSource.ofFacturX (aUbl)),
                  "CII");
    // Factur-X requires EN 16931:2017
    final byte [] aD25A = _readBytes (CII_D25A_FILE);
    _assertFails (_readCii (aD25A),
                  _createContext (EPdfALevel.PDF_A_3B).setEmbeddedSource (PdfEmbeddedSource.ofFacturX (aD25A)),
                  "2026");
  }

  @Test
  public void testEmbedAttachments () throws Exception
  {
    final PhloxInvoice aInvoice = _readUbl (_readBytes (UBL_ATTACHMENT_FILE));
    final int nAttachments = aInvoice.supportingDocuments ()
                                     .getCount (x -> x.getAttachment () != null && x.getAttachment ().getBinary () != null);
    assertTrue (nAttachments > 0);

    final byte [] aPdf = _render (aInvoice,
                                  _createContext (EPdfALevel.PDF_A_3B).setEmbedAttachments (true),
                                  "ubl-3b-attachments");
    _assertPdfA3Valid (aPdf);
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      final Map <String, PDComplexFileSpecification> aFiles = aDoc.getDocumentCatalog ()
                                                                  .getNames ()
                                                                  .getEmbeddedFiles ()
                                                                  .getNames ();
      assertEquals (nAttachments, aFiles.size ());
      for (final PDComplexFileSpecification aFileSpec : aFiles.values ())
        assertEquals ("Supplement", aFileSpec.getCOSObject ().getNameAsString (COSName.AF_RELATIONSHIP));
    }

    // Requires PDF/A-3
    _assertFails (aInvoice, _createContext (EPdfALevel.PDF_A_2B).setEmbedAttachments (true), "PDF/A-3");
  }

  @Test
  public void testCodeBlockIsValidPdfA () throws Exception
  {
    final byte [] aPdf = _render (_readUbl (_readBytes (UBL_FILE)),
                                  _createContext (EPdfALevel.PDF_A_3B).setTemplate (PhloxTemplate.fromString ("Text\n\n```\ncode block\n```\n\n    indented\n")),
                                  "ubl-3b-code-block");
    _assertPdfA3Valid (aPdf);
  }

  @Test
  public void testFacturXWithAttachments () throws Exception
  {
    final byte [] aXml = _readBytes (CII_ATTACHMENT_FILE);
    final byte [] aPdf = _render (_readCii (aXml),
                                  _createContext (EPdfALevel.PDF_A_3B).setEmbeddedSource (PdfEmbeddedSource.ofFacturX (aXml))
                                                                      .setEmbedAttachments (true),
                                  "cii-factur-x-attachments");
    _assertFacturX (aXml, aPdf, EZugferdProfile.EN_16931, "factur-x.xml");
  }

  @Test
  public void testAllMasterInvoices () throws Exception
  {
    final ICommonsList <String> aFailures = new CommonsArrayList <> ();
    for (final String sPath : MockPhloxTestFiles.getMasterFiles ())
    {
      // Some of the examples do not contain BT-24, so the edition is selected by the folder
      final byte [] aXml = _readBytes (sPath);
      final boolean b2026 = sPath.startsWith ("2026/");
      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aInvoice = sPath.contains ("/cii/") ? (b2026 ? CiiInvoiceReader.fromD25A ()
                                                                      : CiiInvoiceReader.fromD16B ()).read (new NonBlockingByteArrayInputStream (aXml),
                                                                                                            aErrors)
                                                             : (b2026 ? UblInvoiceReader.fromUbl25 ()
                                                                      : UblInvoiceReader.fromUbl21 ()).read (new NonBlockingByteArrayInputStream (aXml),
                                                                                                             aErrors);
      assertNotNull (sPath + ": " + aErrors, aInvoice);
      for (final String sLang : new String [] { "en", "de" })
      {
        final PdfRenderContext aCtx = _createContext (EPdfALevel.PDF_A_3A).setLocale (Locale.forLanguageTag (sLang))
                                                                          .setEmbeddedSource (PdfEmbeddedSource.ofSource (aXml,
                                                                                                                          "invoice.xml"))
                                                                          .setEmbedAttachments (true);
        final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
        new PdfInvoiceWriter ().writeTo (aInvoice, aCtx, aBaos);
        final ICommonsList <HybridFinding> aFindings = new VeraPdfA3ValidatorSPI ().validatePdfA3 (HybridSource.fromBytes (aBaos.toByteArray ()));
        if (aFindings.isNotEmpty ())
          aFailures.add (sPath + " (" + sLang + "): " + aFindings);
      }
    }
    assertTrue (String.join ("\n", aFailures), aFailures.isEmpty ());
  }

  @Test
  public void testTaggedPdfUA () throws Exception
  {
    VeraGreenfieldFoundryProvider.initialise ();
    final ICommonsSortedSet <String> aRules = new CommonsTreeSet <> ();
    for (final String sPath : new String [] { UBL_FILE, CII_FILE, CII_XRECHNUNG_FILE })
    {
      final byte [] aXml = _readBytes (sPath);
      final PhloxInvoice aInvoice = sPath.contains ("/cii/") ? _readCii (aXml) : _readUbl (aXml);
      final NonBlockingByteArrayOutputStream aBaos = new NonBlockingByteArrayOutputStream ();
      // PDF/A-3a and PDF/UA-1 together, and PDF/UA-1 alone
      final PdfRenderContext aCtx = UBL_FILE.equals (sPath) ? _createContext (EPdfALevel.PDF_A_3A).setPdfUA (true)
                                                      : _createContext (EPdfALevel.PDF_A_3A).setPdfALevel (null)
                                                                                            .setPdfUA (true);
      new PdfInvoiceWriter ().writeTo (aInvoice, aCtx, aBaos);
      try (final PDFAParser aParser = Foundries.defaultInstance ()
                                               .createParser (new NonBlockingByteArrayInputStream (aBaos.toByteArray ()),
                                                              PDFAFlavour.PDFUA_1);
           final PDFAValidator aValidator = Foundries.defaultInstance ().createValidator (PDFAFlavour.PDFUA_1, false))
      {
        for (final TestAssertion aTA : aValidator.validate (aParser).getTestAssertions ())
          if (aTA.getStatus () == TestAssertion.Status.FAILED)
            aRules.add (aTA.getRuleId ().getClause () + "-" + aTA.getRuleId ().getTestNumber () + ": " + aTA.getMessage ());
      }
    }
    assertTrue (String.join ("\n", aRules), aRules.isEmpty ());
  }
}
