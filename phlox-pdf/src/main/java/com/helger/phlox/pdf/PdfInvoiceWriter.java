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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.WillClose;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.pdflayout.PDFCreationException;
import com.helger.pdflayout.PageLayoutPDF;
import com.helger.pdflayout.base.IPLRenderableObject;
import com.helger.pdflayout.base.PLColor;
import com.helger.pdflayout.base.PLPageSet;
import com.helger.pdflayout.element.box.PLBox;
import com.helger.pdflayout.pdfa.EPdfAFRelationship;
import com.helger.pdflayout.pdfa.EPdfALevel;
import com.helger.pdflayout.pdfa.PdfAXmpExtensionSchema;
import com.helger.pdflayout.pdfa.PdfAssociatedFile;
import com.helger.pdflayout.richtext.element.PLRichText;
import com.helger.pdflayout.spec.EHorzAlignment;
import com.helger.pdflayout.spec.IPLMissingCodePointCallback;
import com.helger.pdflayout.spec.PreloadFont;
import com.helger.pdflayout.watermark.EPLWatermarkPosition;
import com.helger.pdflayout.watermark.PLTextWatermark;
import com.helger.phlox.api.PhloxDocumentProperties;
import com.helger.phlox.api.PhloxLogHelper;
import com.helger.phlox.api.PhloxRuntimeException;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxSupportingDocument;
import com.helger.phlox.api.template.IPhloxTemplate;
import com.helger.phlox.api.watermark.EWatermarkPosition;
import com.helger.phlox.api.watermark.PhloxProgramWatermark;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.api.write.IInvoiceWriter;
import com.helger.phlox.template.PhloxTemplate;

/**
 * Renders a {@link PhloxInvoice} as a PDF using {@code ph-pdf-layout}. The content comes from a
 * template; if the render context has none, the default template is used.
 * <p>
 * phlox sets no limit on the size of its input, and PDF is the most expensive output: the layout of
 * a note of 1 MB takes several seconds. An application that renders invoices from untrusted sources
 * must cap the size of the XML before it reads it.
 *
 * @author Philip Helger
 */
public class PdfInvoiceWriter implements IInvoiceWriter <PdfRenderContext>
{
  /** The namespace URI of the Factur-X XMP extension schema */
  public static final String FACTUR_X_XMP_NAMESPACE_URI = "urn:factur-x:pdfa:CrossIndustryDocument:invoice:1p0#";
  /** The namespace prefix of the Factur-X XMP extension schema */
  public static final String FACTUR_X_XMP_PREFIX = "fx";
  /** The value of <code>fx:Version</code> */
  public static final String FACTUR_X_VERSION = "1.0";
  /** The value of <code>fx:DocumentType</code> */
  public static final String FACTUR_X_DOCUMENT_TYPE = "INVOICE";

  private static final Logger LOGGER = LoggerFactory.getLogger (PdfInvoiceWriter.class);

  /** Create a new PDF invoice writer. */
  public PdfInvoiceWriter ()
  {}

  /**
   * The opacity is simulated by lightening the colour towards white, so that the PDF uses no
   * transparency.
   */
  @NonNull
  private static PLColor _getOpaqueColor (@NonNull final WatermarkSpec aSpec)
  {
    final Color aRGB = aSpec.getColor ();
    final float fO = Math.max (0f, Math.min (1f, aSpec.getOpacity ()));
    return new PLColor (Math.round (255 - (255 - aRGB.getRed ()) * fO),
                        Math.round (255 - (255 - aRGB.getGreen ()) * fO),
                        Math.round (255 - (255 - aRGB.getBlue ()) * fO));
  }

  /** Create the user watermark. */
  @NonNull
  private static PLTextWatermark _createWatermark (@NonNull final PdfFontFamily aFamily,
                                                   @NonNull final WatermarkSpec aSpec)
  {
    // Bold for the large centred watermark, regular for the small ones at the edges
    final PreloadFont aFont = aSpec.getPosition () == EWatermarkPosition.CENTER ? aFamily.getBold ()
                                                                                : aFamily.getRegular ();
    final PLTextWatermark ret = new PLTextWatermark (aSpec.getText (), aFont).setFontSize (aSpec.getFontSizePt ())
                                                                             .setColor (_getOpaqueColor (aSpec))
                                                                             .setRotation (aSpec.getRotation ());
    switch (aSpec.getPosition ())
    {
      case CENTER -> ret.setPosition (EPLWatermarkPosition.CENTER);
      case TOP -> ret.setPosition (EPLWatermarkPosition.TOP).setEdgeDistance (12f);
      case BOTTOM -> ret.setPosition (EPLWatermarkPosition.BOTTOM).setEdgeDistance (12f);
      case FOOTER_RIGHT -> ret.setPosition (EPLWatermarkPosition.BOTTOM_RIGHT).setEdgeDistance (10f);
      default -> ret.setPosition (EPLWatermarkPosition.BOTTOM_RIGHT).setEdgeDistance (10f);
    }
    return ret;
  }

  /**
   * Create the footer with the attribution of the program. The text links to the phlox project. It
   * is a part of the page layout, so it is on every page, right-aligned, and the rotation, the
   * position and <code>repeatPerPage</code> of the spec have no effect.
   */
  @NonNull
  private static IPLRenderableObject <?> _createProgramFooter (@NonNull final PdfFontFamily aFamily,
                                                               @NonNull final WatermarkSpec aSpec)
  {
    // PLColor is RGB-only - approximate the opacity by lightening the colour towards white
    final PLRichText aText = PLRichText.createFromMarkup ("{link[" +
                                                          PhloxDocumentProperties.PROJECT_URL +
                                                          "]}" +
                                                          PdfTextSupport.escapeMarkup (aSpec.getText ()) +
                                                          "{link}",
                                                          aFamily.getAsPLFontFamily (),
                                                          aSpec.getFontSizePt (),
                                                          _getOpaqueColor (aSpec));
    aText.setHorzAlign (EHorzAlignment.RIGHT);
    return new PLBox (aText).setMarginTop (4).setHorzAlign (EHorzAlignment.RIGHT);
  }

  /**
   * Set the metadata of the PDF: title, author, subject, keywords, language and the creator
   * (phlox).
   */
  private static void _setDocumentProperties (@NonNull final PageLayoutPDF aPdf,
                                              @NonNull final PhloxInvoice aInvoice,
                                              @NonNull final PdfRenderContext aCtx)
  {
    aPdf.setDocumentTitle (PhloxDocumentProperties.getTitle (aInvoice, aCtx));
    aPdf.setDocumentAuthor (PhloxDocumentProperties.getAuthor (aInvoice));
    aPdf.setDocumentSubject (PhloxDocumentProperties.getSubject (aInvoice, aCtx));
    aPdf.setDocumentKeywords (PhloxDocumentProperties.getKeywords (aInvoice, aCtx));
    aPdf.setDocumentLanguage (aCtx.getLocale ().toLanguageTag ());
    aPdf.setDocumentCreator (PhloxDocumentProperties.getCreator ());
  }

  private static void _addPlainSource (@NonNull final PageLayoutPDF aPdf, @NonNull final PdfEmbeddedSource aSource)
  {
    final EPdfAFRelationship eRelationship = aSource.getRelationship () != null ? aSource.getRelationship ()
                                                                                : EPdfAFRelationship.SOURCE;
    aPdf.addAssociatedFile (new PdfAssociatedFile (aSource.getContent (),
                                                   aSource.getFileName (),
                                                   PdfEmbeddedSource.MIME_TYPE_XML,
                                                   "Source of the invoice",
                                                   eRelationship));
  }

  /**
   * Embed a CII invoice as ZUGFeRD / Factur-X hybrid invoice: <code>factur-x.xml</code> (or
   * <code>xrechnung.xml</code>), the relationship of the profile and the Factur-X XMP extension
   * schema. Only plausibility checks are done - the XML itself is not validated.
   */
  private static void _addFacturX (@NonNull final PageLayoutPDF aPdf,
                                   @NonNull final PhloxInvoice aInvoice,
                                   @NonNull final PdfEmbeddedSource aSource)
  {
    if (aInvoice.getEdition () == EEN16931Edition.EN2026)
      throw new PhloxRuntimeException ("ZUGFeRD / Factur-X is only defined for invoices of EN 16931:2017, but the invoice is of EN 16931:2026. Embed it as plain source instead.");
    // Cheap plausibility check, that the XML is CII and not UBL
    if (!new String (aSource.getContent (), StandardCharsets.UTF_8).contains ("CrossIndustryInvoice"))
      throw new PhloxRuntimeException ("ZUGFeRD / Factur-X requires a CII invoice, but the embedded XML is no CrossIndustryInvoice. Embed it as plain source instead.");

    EFacturXConformanceLevel eLevel = aSource.getConformanceLevel ();
    if (eLevel == null)
    {
      final String sBT24 = aInvoice.getProcessControl ().getSpecificationIdentifier ();
      eLevel = EFacturXConformanceLevel.getFromSpecificationIdentifierOrNull (sBT24);
      if (eLevel == null)
        throw new PhloxRuntimeException ("The specification identifier (BT-24) '" +
                                         sBT24 +
                                         "' is not the one of a ZUGFeRD / Factur-X profile. Set the profile explicitly with PdfEmbeddedSource.withConformanceLevel.");
    }
    final EPdfAFRelationship eRelationship = aSource.getRelationship () != null ? aSource.getRelationship ()
                                                                                : eLevel.getDefaultRelationship ();
    final String sFileName = eLevel.getFileName ();

    aPdf.addAssociatedFile (new PdfAssociatedFile (aSource.getContent (),
                                                   sFileName,
                                                   PdfEmbeddedSource.MIME_TYPE_XML,
                                                   "Factur-X/ZUGFeRD invoice",
                                                   eRelationship));
    aPdf.addXmpExtensionSchema (new PdfAXmpExtensionSchema (FACTUR_X_XMP_NAMESPACE_URI,
                                                            FACTUR_X_XMP_PREFIX,
                                                            "Factur-X PDFA Extension Schema").addTextProperty ("DocumentFileName",
                                                                                                               "The name of the embedded XML document",
                                                                                                               sFileName)
                                                                                             .addTextProperty ("DocumentType",
                                                                                                               "The type of the hybrid document in capital letters, e.g. INVOICE or ORDER",
                                                                                                               FACTUR_X_DOCUMENT_TYPE)
                                                                                             .addTextProperty ("Version",
                                                                                                               "The actual version of the standard applying to the embedded XML document",
                                                                                                               FACTUR_X_VERSION)
                                                                                             .addTextProperty ("ConformanceLevel",
                                                                                                               "The conformance level of the embedded XML document",
                                                                                                               eLevel.getID ()));
  }

  /**
   * Embed the attached documents (BT-125 of BG-24) with the relationship <code>Supplement</code>.
   * The file name and the MIME type come from the invoice, so they are reduced to a safe base name
   * and a syntactically valid type. A missing or duplicate file name is replaced by a unique one.
   */
  private static void _addAttachments (@NonNull final PageLayoutPDF aPdf, @NonNull final PhloxInvoice aInvoice)
  {
    final ICommonsSet <String> aUsedNames = new CommonsHashSet <> ();
    for (final PdfAssociatedFile aFile : aPdf.getAllAssociatedFiles ())
      aUsedNames.add (aFile.getFileName ());

    int nIndex = 0;
    for (final PhloxSupportingDocument aDoc : aInvoice.supportingDocuments ())
    {
      nIndex++;
      final PhloxAttachment aAttachment = aDoc.getAttachment ();
      if (aAttachment == null || aAttachment.getBinary () == null)
        continue;

      String sFileName = PdfTextSupport.getSafeFileName (aAttachment.getFilename (), "attachment-" + nIndex);
      if (aUsedNames.contains (sFileName))
        sFileName = nIndex + "-" + sFileName;
      aUsedNames.add (sFileName);

      final String sMimeType = PdfTextSupport.getSafeMimeType (aAttachment.getMimeCode ());
      final String sDescription = StringHelper.isNotEmpty (aDoc.getDescription ()) ? aDoc.getDescription ()
                                                                                   : aDoc.getReference ();
      aPdf.addAssociatedFile (new PdfAssociatedFile (aAttachment.getBinary (),
                                                     sFileName,
                                                     sMimeType,
                                                     sDescription,
                                                     EPdfAFRelationship.SUPPLEMENT));
    }
  }

  /**
   * Set PDF/A, the creation date, the embedded source and the embedded attachments of the render
   * context.
   */
  private static void _applyArchiveOptions (@NonNull final PageLayoutPDF aPdf,
                                            @NonNull final PhloxInvoice aInvoice,
                                            @NonNull final PdfRenderContext aCtx)
  {
    final EPdfALevel ePdfALevel = aCtx.getPdfALevel ();
    if ((ePdfALevel != null || aCtx.isPdfUA ()) && aCtx.getFontFamily () == PdfFontFamily.STANDARD)
      throw new PhloxRuntimeException ("PDF/A and PDF/UA require embedded fonts, but the standard fonts of PDF are used. Set a font family (PdfRenderContext.setFontFamily, 'pdf.fonts' in a theme or --font in the CLI).");
    if (ePdfALevel != null)
      aPdf.setPdfALevel (ePdfALevel);
    if (aCtx.isPdfUA ())
      aPdf.setCreatePdfUA (true);
    if (aCtx.getDocumentCreationDateTime () != null)
      aPdf.setDocumentCreationDateTime (aCtx.getDocumentCreationDateTime ());

    final PdfEmbeddedSource aSource = aCtx.getEmbeddedSource ();
    if (aSource != null)
    {
      if (ePdfALevel == null || !ePdfALevel.isArbitraryEmbeddedFilesAllowed ())
        throw new PhloxRuntimeException ("Embedding the source XML requires PDF/A-3 (PdfRenderContext.setPdfALevel)");
      if (aSource.isFacturX ())
        _addFacturX (aPdf, aInvoice, aSource);
      else
        _addPlainSource (aPdf, aSource);
    }

    if (aCtx.isEmbedAttachments ())
    {
      if (ePdfALevel == null || !ePdfALevel.isArbitraryEmbeddedFilesAllowed ())
        throw new PhloxRuntimeException ("Embedding the attached documents requires PDF/A-3 (PdfRenderContext.setPdfALevel)");
      _addAttachments (aPdf, aInvoice);
    }
  }

  /**
   * Draw a watermark on the first page only. {@link PLTextWatermark} only knows all pages of a
   * document, so the other pages leave the document for that time.
   */
  private static void _drawOnFirstPage (@NonNull final PDDocument aDoc,
                                        @NonNull final PLTextWatermark aWatermark,
                                        final boolean bCompress) throws IOException
  {
    final ICommonsList <PDPage> aOtherPages = new CommonsArrayList <> ();
    for (final PDPage aPage : aDoc.getPages ())
      aOtherPages.add (aPage);
    aOtherPages.removeFirstOrNull ();

    for (final PDPage aPage : aOtherPages)
      aDoc.removePage (aPage);
    aWatermark.drawOnAllPages (aDoc, bCompress);
    for (final PDPage aPage : aOtherPages)
      aDoc.addPage (aPage);
  }

  /**
   * Create the default action for a character that a font cannot show. A text in another script
   * misses many characters in several fonts, so only the first one of a document is logged.
   */
  @NonNull
  private static IPLMissingCodePointCallback _createMissingCodePointLogger ()
  {
    final AtomicBoolean aLogged = new AtomicBoolean (false);
    return (aFont, nCodePoint, nFallbackCodePoint) -> {
      if (aLogged.compareAndSet (false, true))
        LOGGER.warn ("The font '" +
                     PhloxLogHelper.sanitise (aFont.getName ()) +
                     "' does not have the character U+" +
                     Integer.toHexString (nCodePoint).toUpperCase (Locale.ROOT) +
                     ", which is lost in the PDF, and maybe more. Set a font family (PdfRenderContext.setFontFamily, 'pdf.fonts' in a theme or --font in the CLI).");
    };
  }

  @NonNull
  private PageLayoutPDF _build (@NonNull final PhloxInvoice aInvoice, @NonNull final PdfRenderContext aCtx)
  {
    final PageLayoutPDF aPdf = new PageLayoutPDF ();
    final IPLMissingCodePointCallback aMissingCodePointCallback = aCtx.getMissingCodePointCallback ();
    aPdf.setMissingCodePointCallback (aMissingCodePointCallback != null ? aMissingCodePointCallback
                                                                        : _createMissingCodePointLogger ());
    _setDocumentProperties (aPdf, aInvoice, aCtx);
    _applyArchiveOptions (aPdf, aInvoice, aCtx);

    final IPhloxTemplate aTemplate = aCtx.getTemplate () != null ? aCtx.getTemplate () : PhloxTemplate.getDefault ();
    if (!(aTemplate instanceof final PhloxTemplate aPhloxTemplate))
      throw new PhloxRuntimeException ("Unsupported template implementation " + aTemplate.getClass ().getName ());
    final PLPageSet aPS = new PdfTemplateEmitter (aCtx).emit (aPhloxTemplate.renderAst (aInvoice, aCtx));

    aPdf.addPageSet (aPS);

    // The watermark is drawn behind the page content after all pages are laid out
    final WatermarkSpec aUW = aCtx.getUserWatermark ();
    if (aUW != null)
    {
      final PLTextWatermark aWatermark = _createWatermark (aCtx.getFontFamily (), aUW);
      if (aUW.isRepeatPerPage ())
        aPdf.addWatermark (aWatermark);
      else
        aPdf.setDocumentCustomizer (aDoc -> _drawOnFirstPage (aDoc, aWatermark, aPdf.isCompressPDF ()));
    }

    // The attribution of the program is a link to the project, so it is part of the layout
    final WatermarkSpec aPW = PhloxProgramWatermark.getInstance ();
    if (aPW != null)
      aPS.setPageFooter (_createProgramFooter (aCtx.getFontFamily (), aPW));

    return aPdf;
  }

  /**
   * Render the given invoice into a PDF written to the given file.
   *
   * @param aInvoice
   *        The invoice. Never {@code null}.
   * @param aCtx
   *        The PDF render context. Never {@code null}.
   * @param aFile
   *        The output file. Never {@code null}.
   * @throws PDFCreationException
   *         If PDF creation fails.
   */
  public void writeTo (@NonNull final PhloxInvoice aInvoice,
                       @NonNull final PdfRenderContext aCtx,
                       @NonNull final File aFile) throws PDFCreationException
  {
    _build (aInvoice, aCtx).renderTo (aFile);
  }

  /**
   * Render the given invoice into a PDF written to the given stream.
   *
   * @param aInvoice
   *        The invoice.
   * @param aCtx
   *        The PDF render context.
   * @param aOut
   *        The output stream (closed by the renderer).
   * @throws PDFCreationException
   *         If PDF creation fails.
   */
  public void writeTo (@NonNull final PhloxInvoice aInvoice,
                       @NonNull final PdfRenderContext aCtx,
                       @NonNull @WillClose final OutputStream aOut) throws PDFCreationException
  {
    _build (aInvoice, aCtx).renderTo (aOut);
  }
}
