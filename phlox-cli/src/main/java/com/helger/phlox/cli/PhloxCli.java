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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.io.nonblocking.NonBlockingBufferedInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.pdflayout.PDFCreationException;
import com.helger.pdflayout.pdfa.EPdfALevel;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.CPhloxVersion;
import com.helger.phlox.api.PhloxRuntimeException;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.html.HtmlFontFamily;
import com.helger.phlox.html.HtmlInvoiceWriter;
import com.helger.phlox.html.HtmlRenderContext;
import com.helger.phlox.pdf.EFacturXConformanceLevel;
import com.helger.phlox.pdf.PdfEmbeddedSource;
import com.helger.phlox.pdf.PdfFontFamily;
import com.helger.phlox.pdf.PdfInvoiceWriter;
import com.helger.phlox.pdf.PdfRenderContext;
import com.helger.phlox.template.EPhloxTemplateCoverageMode;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.text.TextInvoiceWriter;
import com.helger.phlox.text.TextRenderContext;
import com.helger.phlox.theme.PhloxTheme;
import com.helger.phlox.theme.PhloxThemeReader;
import com.helger.phlox.ubl.UblInvoiceReader;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine;

/**
 * Command-line entry point for {@code phlox}. Reads an electronic invoice from XML and renders it
 * to HTML, PDF or plain text.
 *
 * @author Philip Helger
 */
@Command (name = "phlox",
          mixinStandardHelpOptions = true,
          versionProvider = PhloxCli.VersionProvider.class,
          description = "Render EN 16931 electronic invoices (UBL / CII) to HTML, PDF or plain text.")
public class PhloxCli implements Callable <Integer>
{
  /**
   * Provides the version for <code>--version</code> from {@link CPhloxVersion}, which the Maven
   * build fills from the project version.
   */
  public static final class VersionProvider implements CommandLine.IVersionProvider
  {
    /** Create a new version provider. Called by picocli. */
    public VersionProvider ()
    {}

    @NonNull
    public String [] getVersion ()
    {
      return new String [] { "phlox " + CPhloxVersion.BUILD_VERSION };
    }
  }

  /** The supported output formats. */
  public enum EOutputFormat
  {
    /** HTML5 output. */
    HTML,
    /** PDF output. */
    PDF,
    /** Plain-text output. */
    TEXT
  }

  /** The supported source syntaxes. */
  public enum ESourceSyntax
  {
    /** Detect the syntax from the document. */
    AUTO,
    /** UBL Invoice or CreditNote. */
    UBL,
    /** UN/CEFACT Cross Industry Invoice. */
    CII
  }

  @Option (names = { "-i", "--in" },
           description = "Input invoice XML file. Read from stdin when omitted.",
           required = false)
  Path m_aInputFile;

  @Option (names = { "-o", "--out" },
           description = "Output file. Written to stdout when omitted (HTML and TEXT only).",
           required = false)
  Path m_aOutputFile;

  @Option (names = { "-f", "--format" },
           description = "Output format: HTML, PDF or TEXT. Default: HTML.",
           defaultValue = "HTML")
  EOutputFormat m_eFormat;

  @Option (names = { "-s", "--syntax" },
           description = "Source syntax: AUTO, UBL, or CII. Default: AUTO (sniffs the root element).",
           defaultValue = "AUTO")
  ESourceSyntax m_eSyntax;

  @Option (names = "--en-version",
           description = "EN 16931 edition of the input: auto, 2017 or 2026. Default: auto (reads BT-24 Specification identifier).",
           defaultValue = "auto")
  String m_sEnVersion;

  @Option (names = { "-l", "--lang" },
           description = "Output language code (e.g. 'en', 'de', 'es', 'fr', 'nl'). Default: the language of the theme or en.")
  String m_sLang;

  @Option (names = { "-t", "--template" },
           description = "The template to render through: 'default' for the template shipped with phlox, 'classpath:<path>' for a template on the classpath, or the path of a .ptpl file. Default: 'default'.")
  String m_sTemplate;

  @Option (names = "--template-coverage",
           description = "What to do with a template that does not show all BTs of EN 16931: WARN logs a warning, REJECT refuses the template. Default: WARN.",
           defaultValue = "WARN")
  EPhloxTemplateCoverageMode m_eTemplateCoverage;

  @Option (names = "--theme",
           description = "A JSON file with the layout parameters of the HTML and PDF output (colours, fonts, page size, language, watermark, ...). The options of the command line override the theme.")
  Path m_aThemeFile;

  @Option (names = "--logo",
           description = "Optional logo image (PNG, JPEG or GIF) that the template shows in the header.")
  Path m_aLogoFile;

  @Option (names = "--font",
           description = "Regular font file (TTF or OTF; WOFF and WOFF2 for HTML only) that replaces the default fonts in HTML and PDF. Needed for characters outside Western European languages, e.g. Cyrillic or Greek. The font is embedded into the output.")
  Path m_aFontFile;

  @Option (names = "--font-bold", description = "Bold font file for --font. Default: the regular font.")
  Path m_aFontBoldFile;

  @Option (names = "--font-italic", description = "Italic font file for --font. Default: the regular font.")
  Path m_aFontItalicFile;

  @Option (names = "--font-bold-italic", description = "Bold italic font file for --font. Default: the bold font.")
  Path m_aFontBoldItalicFile;

  @Option (names = "--watermark", description = "Optional user watermark text (e.g. 'DRAFT').")
  String m_sWatermark;

  @Option (names = "--watermark-opacity",
           description = "Watermark opacity 0.0-1.0. Default: 0.10.",
           defaultValue = "0.10")
  float m_fWatermarkOpacity;

  @Option (names = "--watermark-rotation",
           description = "Watermark rotation in degrees. Default: -45.",
           defaultValue = "-45")
  float m_fWatermarkRotation;

  @Option (names = "--pdfa",
           description = "PDF only: create PDF/A at the given level: 2b, 2u, 3a, 3b or 3u. 3a is a tagged (accessible) PDF. PDF/A requires embedded fonts (--font or 'pdf.fonts' in the theme).")
  String m_sPdfALevel;

  @Option (names = "--pdfua",
           description = "PDF only: create a PDF/UA-1 (accessible, tagged PDF). Can be combined with --pdfa. Requires embedded fonts (--font or 'pdf.fonts' in the theme).")
  boolean m_bPdfUA;

  @Option (names = "--embed-source",
           description = "PDF only: embed the input XML into the PDF (relationship 'Source'). Requires PDF/A-3; without --pdfa, 3b is used.")
  boolean m_bEmbedSource;

  @Option (names = "--embed-attachments",
           description = "PDF only: embed the attached documents of the invoice (BG-24) into the PDF (relationship 'Supplement'). Requires PDF/A-3; without --pdfa, 3b is used.")
  boolean m_bEmbedAttachments;

  @Option (names = "--zugferd",
           description = "PDF only: create a ZUGFeRD / Factur-X hybrid invoice. The CII input is embedded as factur-x.xml (xrechnung.xml for XRechnung) with the Factur-X metadata. Only for CII invoices of EN 16931:2017. Requires PDF/A-3; without --pdfa, 3b is used.")
  boolean m_bZugferd;

  @Option (names = "--zugferd-profile",
           description = "The ZUGFeRD / Factur-X profile for --zugferd: MINIMUM, 'BASIC WL', BASIC, 'EN 16931', EXTENDED or XRECHNUNG. Default: derived from BT-24 (Specification identifier).")
  String m_sZugferdProfile;

  @FunctionalInterface
  private interface FontApplier <CTX>
  {
    void apply (@NonNull CTX aCtx) throws IOException;
  }

  private static final String TEMPLATE_DEFAULT = "default";
  private static final String TEMPLATE_CLASSPATH_PREFIX = "classpath:";

  /** Create a new CLI instance. */
  public PhloxCli ()
  {}

  @NonNull
  private static String _getMimeCode (@NonNull final String sFilename)
  {
    final String sLower = sFilename.toLowerCase (Locale.ROOT);
    if (sLower.endsWith (".jpg") || sLower.endsWith (".jpeg"))
      return "image/jpeg";
    if (sLower.endsWith (".gif"))
      return "image/gif";
    return "image/png";
  }

  @NonNull
  private static PhloxTemplate _loadTemplate (@Nullable final String sTemplate,
                                              @NonNull final EPhloxTemplateCoverageMode eCoverageMode)
  {
    if (sTemplate == null || TEMPLATE_DEFAULT.equals (sTemplate))
      return PhloxTemplate.getDefault ();
    if (sTemplate.startsWith (TEMPLATE_CLASSPATH_PREFIX))
      return PhloxTemplate.fromClasspath (sTemplate.substring (TEMPLATE_CLASSPATH_PREFIX.length ()), eCoverageMode);
    return PhloxTemplate.fromFile (new File (sTemplate), eCoverageMode);
  }

  private static byte [] _readAllStdin () throws IOException
  {
    try (final NonBlockingBufferedInputStream aIn = new NonBlockingBufferedInputStream (System.in))
    {
      return aIn.readAllBytes ();
    }
  }

  @Nullable
  private Integer _checkFonts ()
  {
    if (m_aFontFile == null && (m_aFontBoldFile != null || m_aFontItalicFile != null || m_aFontBoldItalicFile != null))
    {
      System.err.println ("--font-bold, --font-italic and --font-bold-italic require --font");
      return Integer.valueOf (2);
    }
    for (final Path aFile : new Path [] { m_aFontFile, m_aFontBoldFile, m_aFontItalicFile, m_aFontBoldItalicFile })
      if (aFile != null && !Files.isRegularFile (aFile))
      {
        System.err.println ("Font file '" + aFile + "' does not exist");
        return Integer.valueOf (2);
      }
    return null;
  }

  /**
   * Apply the theme and then the options of the command line, which win over the theme.
   *
   * @return <code>null</code> if everything is fine, otherwise the exit code.
   */
  @Nullable
  private <CTX extends AbstractRenderContext <CTX>> Integer _prepare (@NonNull final CTX aCtx,
                                                                      @Nullable final Consumer <CTX> aThemeApplier,
                                                                      @NonNull final FontApplier <CTX> aFontApplier)
  {
    try
    {
      if (aThemeApplier != null)
        aThemeApplier.accept (aCtx);
      if (m_aFontFile != null)
        aFontApplier.apply (aCtx);
      if (m_sLang != null)
        aCtx.setLocale (Locale.forLanguageTag (m_sLang.replace ('_', '-')));
      if (m_sTemplate != null)
        aCtx.setTemplate (_loadTemplate (m_sTemplate, m_eTemplateCoverage));
      // The template of a theme is loaded without the coverage mode of the command line
      if (m_eTemplateCoverage == EPhloxTemplateCoverageMode.REJECT &&
          aCtx.getTemplate () instanceof final PhloxTemplate aTemplate)
        aTemplate.checkCoverage (m_eTemplateCoverage);
      if (m_aLogoFile != null)
      {
        if (!Files.isRegularFile (m_aLogoFile))
        {
          System.err.println ("Logo file '" + m_aLogoFile + "' does not exist");
          return Integer.valueOf (2);
        }
        final String sFilename = m_aLogoFile.getFileName ().toString ();
        aCtx.setLogo (new PhloxAttachment ().setBinary (Files.readAllBytes (m_aLogoFile))
                                            .setMimeCode (_getMimeCode (sFilename))
                                            .setFilename (sFilename));
      }
      if (m_sWatermark != null)
        aCtx.setUserWatermark (WatermarkSpec.text (m_sWatermark)
                                            .setOpacity (m_fWatermarkOpacity)
                                            .setRotation (m_fWatermarkRotation));
    }
    catch (final PhloxRuntimeException | IOException | IllegalArgumentException ex)
    {
      System.err.println ("Failed to prepare the rendering: " + ex.getMessage ());
      return Integer.valueOf (2);
    }
    return null;
  }

  /**
   * Write the rendered text as UTF-8 to the output file, or to stdout if there is none.
   */
  private void _writeText (@NonNull final String sText) throws IOException
  {
    final byte [] aBytes = sText.getBytes (StandardCharsets.UTF_8);
    if (m_aOutputFile == null)
      System.out.write (aBytes);
    else
      try (final OutputStream aOut = new FileOutputStream (m_aOutputFile.toFile ()))
      {
        aOut.write (aBytes);
      }
  }

  /**
   * Command line entry point.
   *
   * @param args
   *        The command line arguments.
   */
  public static void main (final String [] args)
  {
    final int nExit = new CommandLine (new PhloxCli ()).execute (args);
    System.exit (nExit);
  }

  /** Decide source syntax by sniffing the root element / namespace. */
  static ESourceSyntax _sniffSyntax (final byte [] aBytes)
  {
    final String sHead = new String (aBytes, StandardCharsets.UTF_8);
    if (sHead.contains ("<CrossIndustryInvoice") ||
        sHead.contains (":CrossIndustryInvoice") ||
        sHead.contains ("uncefact:data:standard"))
      return ESourceSyntax.CII;
    return ESourceSyntax.UBL;
  }

  /**
   * Apply PDF/A and the embedding of the source XML. The options are checked before.
   *
   * @return {@code null} on success or the exit code of the error.
   */
  @Nullable
  private Integer _applyArchiveOptions (@NonNull final PdfRenderContext aCtx,
                                        final byte @NonNull [] aBytes,
                                        @NonNull final ESourceSyntax eSyntax)
  {
    EPdfALevel ePdfALevel = null;
    if (m_sPdfALevel != null)
    {
      ePdfALevel = EPdfALevel.getFromIDOrNull (m_sPdfALevel.trim ().toLowerCase (Locale.ROOT));
      if (ePdfALevel == null)
      {
        System.err.println ("Unsupported --pdfa '" + m_sPdfALevel + "'. Use 2b, 2u, 3a, 3b or 3u.");
        return Integer.valueOf (2);
      }
    }

    if (m_bEmbedAttachments)
    {
      if (ePdfALevel == null)
        ePdfALevel = EPdfALevel.PDF_A_3B;
      aCtx.setEmbedAttachments (true);
    }
    if (m_bEmbedSource || m_bZugferd)
    {
      if (ePdfALevel == null)
        ePdfALevel = EPdfALevel.PDF_A_3B;
      if (m_bZugferd)
      {
        if (eSyntax != ESourceSyntax.CII)
        {
          System.err.println ("--zugferd requires a CII invoice");
          return Integer.valueOf (2);
        }
        PdfEmbeddedSource aSource = PdfEmbeddedSource.ofFacturX (aBytes);
        if (m_sZugferdProfile != null)
        {
          final EFacturXConformanceLevel eLevel = EFacturXConformanceLevel.getFromIDOrNull (m_sZugferdProfile.trim ()
                                                                                                             .toUpperCase (Locale.ROOT));
          if (eLevel == null)
          {
            System.err.println ("Unsupported --zugferd-profile '" +
                                m_sZugferdProfile +
                                "'. Use MINIMUM, 'BASIC WL', BASIC, 'EN 16931', EXTENDED or XRECHNUNG.");
            return Integer.valueOf (2);
          }
          aSource = aSource.withConformanceLevel (eLevel);
        }
        aCtx.setEmbeddedSource (aSource);
      }
      else
      {
        final String sFileName = m_aInputFile != null ? m_aInputFile.getFileName ().toString () : "invoice.xml";
        aCtx.setEmbeddedSource (PdfEmbeddedSource.ofSource (aBytes, sFileName));
      }
    }
    aCtx.setPdfALevel (ePdfALevel);
    aCtx.setPdfUA (m_bPdfUA);
    return null;
  }

  public Integer call () throws Exception
  {
    if (m_aInputFile != null && !Files.isRegularFile (m_aInputFile))
    {
      System.err.println ("Input file '" + m_aInputFile + "' does not exist");
      return Integer.valueOf (2);
    }
    final Integer aFontError = _checkFonts ();
    if (aFontError != null)
      return aFontError;
    if ((m_sPdfALevel != null ||
         m_bPdfUA ||
         m_bEmbedSource ||
         m_bEmbedAttachments ||
         m_bZugferd ||
         m_sZugferdProfile != null) && m_eFormat != EOutputFormat.PDF)
    {
      System.err.println ("--pdfa, --pdfua, --embed-source, --embed-attachments, --zugferd and --zugferd-profile require --format PDF");
      return Integer.valueOf (2);
    }
    if (m_bEmbedSource && m_bZugferd)
    {
      System.err.println ("Use either --embed-source or --zugferd");
      return Integer.valueOf (2);
    }
    if (m_sZugferdProfile != null && !m_bZugferd)
    {
      System.err.println ("--zugferd-profile requires --zugferd");
      return Integer.valueOf (2);
    }
    final byte [] aBytes = m_aInputFile != null ? Files.readAllBytes (m_aInputFile) : _readAllStdin ();
    final ESourceSyntax eSyntax = m_eSyntax == ESourceSyntax.AUTO ? _sniffSyntax (aBytes) : m_eSyntax;

    if (!"auto".equalsIgnoreCase (m_sEnVersion) && !"2017".equals (m_sEnVersion) && !"2026".equals (m_sEnVersion))
    {
      System.err.println ("Unsupported --en-version '" + m_sEnVersion + "'. Use auto, 2017 or 2026.");
      return Integer.valueOf (2);
    }

    final ErrorList aErrors = new ErrorList ();
    final PhloxInvoice aInvoice;
    if (eSyntax == ESourceSyntax.CII)
    {
      final CiiInvoiceReader aReader = "2017".equals (m_sEnVersion) ? CiiInvoiceReader.fromD16B ()
                                                                 : "2026".equals (m_sEnVersion) ? CiiInvoiceReader.fromD25A ()
                                                                                             : CiiInvoiceReader.autoDetect ();
      aInvoice = aReader.read (new NonBlockingByteArrayInputStream (aBytes), aErrors);
    }
    else
    {
      final UblInvoiceReader aReader = "2017".equals (m_sEnVersion) ? UblInvoiceReader.fromUbl21 ()
                                                                 : "2026".equals (m_sEnVersion) ? UblInvoiceReader.fromUbl25 ()
                                                                                             : UblInvoiceReader.autoDetect ();
      aInvoice = aReader.read (new NonBlockingByteArrayInputStream (aBytes), aErrors);
    }

    if (aInvoice == null || aErrors.containsAtLeastOneError ())
    {
      System.err.println ("Failed to parse input: " + aErrors);
      return Integer.valueOf (1);
    }

    final PhloxTheme aTheme;
    if (m_aThemeFile == null)
      aTheme = null;
    else
    {
      try
      {
        aTheme = PhloxThemeReader.read (m_aThemeFile);
      }
      catch (final PhloxRuntimeException ex)
      {
        System.err.println ("Failed to read the theme '" + m_aThemeFile + "': " + ex.getMessage ());
        return Integer.valueOf (2);
      }
    }

    if (m_eFormat == EOutputFormat.PDF)
    {
      if (m_aOutputFile == null)
      {
        System.err.println ("PDF output requires --out");
        return Integer.valueOf (2);
      }
      final PdfRenderContext aCtx = new PdfRenderContext ();
      final Integer aError = _prepare (aCtx, aTheme == null ? null : (final PdfRenderContext aTarget) -> aTarget.applyTheme (aTheme),
                                       aTarget -> aTarget.setFontFamily (PdfFontFamily.ofFiles (m_aFontFile,
                                                                                                 m_aFontBoldFile,
                                                                                                 m_aFontItalicFile,
                                                                                                 m_aFontBoldItalicFile)));
      if (aError != null)
        return aError;
      final Integer aArchiveError = _applyArchiveOptions (aCtx, aBytes, eSyntax);
      if (aArchiveError != null)
        return aArchiveError;
      // Render completely before touching the output file, so a failure leaves no empty file
      final NonBlockingByteArrayOutputStream aBuffer = new NonBlockingByteArrayOutputStream ();
      try
      {
        new PdfInvoiceWriter ().writeTo (aInvoice, aCtx, aBuffer);
      }
      catch (final PhloxRuntimeException | PDFCreationException ex)
      {
        System.err.println ("Failed to render the invoice: " + ex.getMessage ());
        return Integer.valueOf (2);
      }
      try (final OutputStream aOut = new FileOutputStream (m_aOutputFile.toFile ()))
      {
        aBuffer.writeTo (aOut);
      }
    }
    else
      if (m_eFormat == EOutputFormat.TEXT)
      {
        final TextRenderContext aCtx = new TextRenderContext ();
        // Fonts have no meaning in plain text
        final Integer aError = _prepare (aCtx,
                                         aTheme == null ? null : (final TextRenderContext aTarget) -> aTarget.applyTheme (aTheme),
                                         aTarget -> {});
        if (aError != null)
          return aError;
        final String sText;
        try
        {
          sText = new TextInvoiceWriter ().writeAsString (aInvoice, aCtx);
        }
        catch (final PhloxRuntimeException ex)
        {
          System.err.println ("Failed to render the invoice: " + ex.getMessage ());
          return Integer.valueOf (2);
        }
        _writeText (sText);
      }
      else
      {
        final HtmlRenderContext aCtx = new HtmlRenderContext ();
        final Integer aError = _prepare (aCtx,
                                         aTheme == null ? null : (final HtmlRenderContext aTarget) -> aTarget.applyTheme (aTheme),
                                         aTarget -> aTarget.setFontFamily (HtmlFontFamily.ofFiles (m_aFontFile,
                                                                                                    m_aFontBoldFile,
                                                                                                    m_aFontItalicFile,
                                                                                                    m_aFontBoldItalicFile)));
        if (aError != null)
          return aError;
        final HtmlInvoiceWriter aHtmlWriter = new HtmlInvoiceWriter ();
        final String sHtml;
        try
        {
          sHtml = aHtmlWriter.writeAsString (aInvoice, aCtx);
        }
        catch (final PhloxRuntimeException ex)
        {
          System.err.println ("Failed to render the invoice: " + ex.getMessage ());
          return Integer.valueOf (2);
        }
        _writeText (sHtml);
      }
    return Integer.valueOf (0);
  }
}
