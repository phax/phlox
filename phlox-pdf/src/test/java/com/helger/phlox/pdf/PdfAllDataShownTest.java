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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringReplace;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.font.roboto.EFontResourceRoboto;
import com.helger.phlox.api.i18n.DefaultPhloxLabelResolver;
import com.helger.phlox.api.i18n.PhloxFormat;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.ubl.UblInvoiceReader;
import com.helger.xml.serialize.read.DOMReader;
import com.helger.xml.serialize.write.XMLWriter;

/**
 * Test class for class {@link PdfInvoiceWriter}: everything that the XML of an invoice holds has to
 * appear in the PDF of the default template. For all invoices of <code>phlox-testfiles</code> the
 * test collects each text of an element and each value of an attribute with its XPath, renders the
 * PDF, extracts its text and looks for the values. The comparison does not depend on line breaks.
 * An amount or a date is found in its format, a code also by the name that the code list has for
 * it. A value that occurs several times in the XML has to occur as often in the PDF.
 * <p>
 * Only the XPaths of {@link #ALLOWED} are exempt, with the values and the reason given there. The
 * result of the last run is in <code>target/all-data-shown.tsv</code>. A failure means that a reader,
 * the model or the default template lost data: fix that, and do not extend the list unless the
 * value really is not data of the invoice.
 *
 * @author Philip Helger
 */
public final class PdfAllDataShownTest
{
  private static final File FIXTURE_DIR = new File ("../phlox-testfiles/src/main/resources/external/en16931");
  private static final File REPORT_FILE = new File ("target/all-data-shown.tsv");
  private static final File PROBE_REPORT_FILE = new File ("target/all-data-probed.tsv");
  private static final String PROBE_OK = "OK";
  private static final String REPORT_TAB = "\t";
  private static final Pattern NUMBER = Pattern.compile ("[+-]?(?:\\d+\\.?\\d*|\\.\\d+)");
  private static final Pattern NUMBER_IN_TEXT = Pattern.compile ("(?<![\\p{L}\\p{N}])(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?");
  private static final Pattern DATE_ISO = Pattern.compile ("(\\d{4})-(\\d{2})-(\\d{2}).*");
  private static final Pattern DATE_102 = Pattern.compile ("(\\d{4})(\\d{2})(\\d{2})");
  private static final Pattern NOTE_SUBJECT = Pattern.compile ("#([^#]+)#(.*)", Pattern.DOTALL);
  private static final Pattern TIME = Pattern.compile ("\\d{2}:\\d{2}:\\d{2}.*");
  private static final Pattern DATE_TIME_208 = Pattern.compile ("(\\d{8})\\d{6}[+-]\\d{4}");
  private static final Map <String, String []> CODE_LISTS = new TreeMap <> ();
  private static final String ANY_VALUE = ".*";
  // The fillers of the UBL binding, which count as no data. CII has them in BT-11-1
  private static final String FILLERS = "None|NA";
  private static final ICommonsList <AllowedValue> ALLOWED = new CommonsArrayList <> ();
  private static final AtomicInteger MERGED_KEY_ID = new AtomicInteger ();

  @NonNull
  private static BigDecimal _getKey (@NonNull final BigDecimal aNumber)
  {
    return aNumber.abs ().stripTrailingZeros ();
  }

  @NonNull
  private static String _getCompact (@NonNull final String sValue)
  {
    final String sNormalised = Normalizer.normalize (sValue, Normalizer.Form.NFKC);
    final StringBuilder aSB = new StringBuilder (sNormalised.length ());
    for (final char c : sNormalised.toCharArray ())
      if (!Character.isWhitespace (c) && !Character.isSpaceChar (c))
        aSB.append (c);
    return aSB.toString ();
  }

  /** The values of the XPaths with this ending are no data of the invoice. */
  private static final class AllowedValue
  {
    private final String m_sXPathSuffix;
    private final Pattern m_aValues;
    private final String m_sReason;
    private boolean m_bUsed;

    AllowedValue (@NonNull final String sXPathSuffix, @NonNull final String sValues, @NonNull final String sReason)
    {
      m_sXPathSuffix = sXPathSuffix;
      m_aValues = Pattern.compile (sValues, Pattern.DOTALL);
      m_sReason = sReason;
    }
  }

  /** A value of the XML with the place where it was found and the node that holds it. */
  private record XmlValue (String sXPath, String sValue, Node aNode)
  {}

  /** The text of a PDF, searchable independently of the line breaks. */
  private static final class PdfText
  {
    private final String m_sText;
    private final String m_sCompact;
    private final int [] m_aPositions;
    private final Map <BigDecimal, Integer> m_aNumbers = new TreeMap <> ();

    PdfText (@NonNull final String sRawText)
    {
      m_sText = Normalizer.normalize (sRawText, Normalizer.Form.NFKC);
      final StringBuilder aCompact = new StringBuilder (m_sText.length ());
      m_aPositions = new int [m_sText.length ()];
      for (int i = 0; i < m_sText.length (); ++i)
      {
        final char c = m_sText.charAt (i);
        if (!_isSpace (c))
        {
          m_aPositions[aCompact.length ()] = i;
          aCompact.append (c);
        }
      }
      m_sCompact = aCompact.toString ();
      final Matcher aMatcher = NUMBER_IN_TEXT.matcher (m_sText);
      while (aMatcher.find ())
      {
        final BigDecimal aNumber = _getKey (new BigDecimal (StringReplace.replaceAll (aMatcher.group (), ",", "")));
        m_aNumbers.merge (aNumber, Integer.valueOf (1), Integer::sum);
      }
    }

    private static boolean _isSpace (final char c)
    {
      return Character.isWhitespace (c) || Character.isSpaceChar (c);
    }

    private boolean _isTokenBoundary (final int nPositionInText)
    {
      if (nPositionInText < 0 || nPositionInText >= m_sText.length ())
        return true;
      return !Character.isLetterOrDigit (m_sText.charAt (nPositionInText));
    }

    int getTextCount (@NonNull final String sValue)
    {
      final String sNeedle = _getCompact (sValue);
      if (sNeedle.isEmpty ())
        return 0;
      int ret = 0;
      int nIndex = m_sCompact.indexOf (sNeedle);
      while (nIndex >= 0)
      {
        final int nFirst = m_aPositions[nIndex];
        final int nLast = m_aPositions[nIndex + sNeedle.length () - 1];
        if (_isTokenBoundary (nFirst - 1) && _isTokenBoundary (nLast + 1))
          ret++;
        nIndex = m_sCompact.indexOf (sNeedle, nIndex + sNeedle.length ());
      }
      return ret;
    }

    int getNumberCount (@NonNull final BigDecimal aNumber)
    {
      final Integer aCount = m_aNumbers.get (_getKey (aNumber));
      return aCount == null ? 0 : aCount.intValue ();
    }
  }

  private static void _allow (@NonNull final String sXPathSuffix,
                              @NonNull final String sValues,
                              @NonNull final String sReason)
  {
    ALLOWED.add (new AllowedValue (sXPathSuffix, sValues, sReason));
  }

  static
  {
    _allow ("/@xsi:schemaLocation", ANY_VALUE, "(f) not part of the invoice");
    _allow ("/@format", "102|208|610", "(f) format of a date, which is shown formatted");
    _allow ("/cbc:ChargeIndicator", "true|false|1|0", "(f) discriminator of BG-20/21 and BG-27/28, shown by the table and the sign");
    _allow ("/ram:ChargeIndicator/udt:Indicator", "true|false", "(f) as cbc:ChargeIndicator");
    _allow ("/cbc:AllowanceChargeReasonCode/@listID", ANY_VALUE, "(f) names the code list, 5153 is shown as BT-177-1");
    _allow ("/ram:ReasonCode/@listID", ANY_VALUE, "(f) names the code list, 5153 is shown as BT-177-1");
    _allow ("/cbc:AllowanceChargeReasonCode/@listAgencyID", "6", "(f) agency of a fixed code list");
    _allow ("/ram:ReasonCode/@listAgencyID", "6", "(f) agency of a fixed code list");
    _allow ("/cbc:DocumentTypeCode/@listID", "1001", "(f) BT-122-1-1, the only list");
    _allow ("/ram:SpecifiedTaxRegistration/ram:ID/@schemeID", "VA", "(f) BT-31-1, BT-48-1, BT-63-1: fixed, the label says it");
    _allow ("/cac:TaxScheme/cbc:ID", "VAT|LOC", "(f) BT-31-2, BT-32-2, BT-48-2, BT-63-2: fixed, the label says it");
    _allow ("/ram:ApplicableTradeTax/ram:TypeCode", "VAT", "(f) fixed tax type");
    _allow ("/ram:CategoryTradeTax/ram:TypeCode", "VAT", "(f) fixed tax type");
    _allow ("/cac:AdditionalDocumentReference/cbc:DocumentTypeCode", "130|916", "(f) BT-18-2, BT-122-1: fixed discriminators");
    _allow ("/cac:DocumentReference/cbc:DocumentTypeCode", "130", "(f) BT-128-2: fixed discriminator");
    _allow ("/cac:AdditionalDocumentReference/cbc:DocumentType", "ATS", "(f) discriminator of BT-18 in the 2017 credit note");
    _allow ("/ram:AdditionalReferencedDocument/ram:TypeCode", "50|130|916", "(f) BT-17-1, BT-18-2, BT-122-1, BT-128-2: fixed discriminators");
    _allow ("/cbc:EmbeddedDocumentBinaryObject", ANY_VALUE, "(f) binary content of BT-125: --embed-attachments covers it");
    _allow ("/ram:AttachmentBinaryObject", ANY_VALUE, "(f) binary content of BT-125: --embed-attachments covers it");
    _allow ("/cbc:ID", FILLERS, "(f) filler of the UBL binding");
    _allow ("/cbc:LineID", FILLERS, "(f) filler of the UBL binding");
    _allow ("/cbc:SalesOrderLineID", FILLERS, "(f) filler of the UBL binding");

    CODE_LISTS.put ("cbc:InvoiceTypeCode", new String [] { "doctype" });
    CODE_LISTS.put ("cbc:CreditNoteTypeCode", new String [] { "doctype" });
    CODE_LISTS.put ("rsm:ExchangedDocument/ram:TypeCode", new String [] { "doctype" });
    CODE_LISTS.put ("cbc:PaymentMeansCode", new String [] { "paymentmeans" });
    CODE_LISTS.put ("ram:SpecifiedTradeSettlementPaymentMeans/ram:TypeCode", new String [] { "paymentmeans" });
    CODE_LISTS.put ("cbc:IdentificationCode", new String [] { "country" });
    CODE_LISTS.put ("ram:CountryID", new String [] { "country" });
    CODE_LISTS.put ("ram:OriginTradeCountry/ram:ID", new String [] { "country" });
    CODE_LISTS.put ("@unitCode", new String [] { "unit" });
    CODE_LISTS.put ("cac:TaxCategory/cbc:ID", new String [] { "vatcategory" });
    CODE_LISTS.put ("cac:ClassifiedTaxCategory/cbc:ID", new String [] { "vatcategory" });
    CODE_LISTS.put ("ram:CategoryCode", new String [] { "vatcategory" });
    CODE_LISTS.put ("cbc:TaxExemptionReasonCode", new String [] { "vatexemption" });
    CODE_LISTS.put ("ram:ExemptionReasonCode", new String [] { "vatexemption" });
    CODE_LISTS.put ("cbc:AllowanceChargeReasonCode", new String [] { "allowancereason", "chargereason" });
    CODE_LISTS.put ("ram:ReasonCode", new String [] { "allowancereason", "chargereason" });
    CODE_LISTS.put ("cbc:DescriptionCode", new String [] { "vatpointdate" });
    CODE_LISTS.put ("ram:DueDateTypeCode", new String [] { "vatpointdate" });
    CODE_LISTS.put ("ram:SubjectCode", new String [] { "notesubject" });
    CODE_LISTS.put ("cbc:SubjectCode", new String [] { "notesubject" });
    CODE_LISTS.put ("cbc:Note#subject", new String [] { "notesubject" });
  }

  private static boolean _isAllowed (@NonNull final XmlValue aValue, @NonNull final PhloxInvoice aInvoice)
  {
    // (f) BT-5 and BT-6 are shown once, and an amount shows the symbol of its currency
    if ((aValue.sXPath ().endsWith ("/@currencyID") || aValue.sXPath ().endsWith ("/ram:CurrencyCode")) &&
        (aValue.sValue ().equals (aInvoice.getCurrency ()) ||
         aValue.sValue ().equals (aInvoice.getVatAccountingCurrency ())))
      return true;
    boolean ret = false;
    for (final AllowedValue aAllowed : ALLOWED)
      if (aValue.sXPath ().endsWith (aAllowed.m_sXPathSuffix) && aAllowed.m_aValues.matcher (aValue.sValue ()).matches ())
      {
        aAllowed.m_bUsed = true;
        ret = true;
      }
    return ret;
  }

  @NonNull
  private static String _getPrefix (@NonNull final Node aNode)
  {
    final String sNamespaceURI = aNode.getNamespaceURI ();
    if (sNamespaceURI == null)
      return "";
    if (sNamespaceURI.endsWith ("CommonBasicComponents-2"))
      return "cbc:";
    if (sNamespaceURI.endsWith ("CommonAggregateComponents-2"))
      return "cac:";
    if (sNamespaceURI.endsWith ("CommonExtensionComponents-2"))
      return "ext:";
    if (sNamespaceURI.endsWith ("CrossIndustryInvoice:100"))
      return "rsm:";
    if (sNamespaceURI.endsWith ("ReusableAggregateBusinessInformationEntity:100"))
      return "ram:";
    if (sNamespaceURI.endsWith ("UnqualifiedDataType:100"))
      return "udt:";
    if (sNamespaceURI.endsWith ("QualifiedDataType:100"))
      return "qdt:";
    if (sNamespaceURI.endsWith ("XMLSchema-instance"))
      return "xsi:";
    if (sNamespaceURI.endsWith ("Invoice-2") || sNamespaceURI.endsWith ("CreditNote-2"))
      return "";
    return aNode.getPrefix () == null ? "" : aNode.getPrefix () + ":";
  }

  /**
   * The readers merge equal values of these places: BT-7 is repeated with each VAT breakdown, and
   * BT-149 of the gross price is the one of the net price.
   *
   * @return A key that is the same for such values, and unique otherwise.
   */
  @NonNull
  private static String _getMergedKey (@NonNull final XmlValue aValue)
  {
    final String sXPath = aValue.sXPath ();
    if (sXPath.contains ("/ram:TaxPointDate/"))
      return sXPath + "=" + aValue.sValue ();
    if (sXPath.contains ("/ram:GrossPriceProductTradePrice/ram:BasisQuantity"))
      return StringReplace.replaceAll (sXPath, "/ram:GrossPriceProductTradePrice/", "/ram:NetPriceProductTradePrice/") +
             "=" +
             aValue.sValue ();
    if (sXPath.contains ("/ram:NetPriceProductTradePrice/ram:BasisQuantity"))
      return sXPath + "=" + aValue.sValue ();
    return sXPath + "#" + MERGED_KEY_ID.incrementAndGet ();
  }

  private static void _addValue (@NonNull final String sXPath,
                                 @NonNull final String sValue,
                                 @NonNull final Node aNode,
                                 @NonNull final List <XmlValue> aOut)
  {
    final Matcher aMatcher = NOTE_SUBJECT.matcher (sValue);
    if (sXPath.endsWith ("/cbc:Note") && aMatcher.matches ())
    {
      aOut.add (new XmlValue (sXPath + "#subject", aMatcher.group (1), aNode));
      aOut.add (new XmlValue (sXPath, aMatcher.group (2).trim (), aNode));
    }
    else
      aOut.add (new XmlValue (sXPath, sValue, aNode));
  }

  private static void _collectValuesRecursive (@NonNull final Element aElement,
                                               @NonNull final String sParentPath,
                                               @NonNull final List <XmlValue> aOut)
  {
    final String sPath = sParentPath + "/" + _getPrefix (aElement) + aElement.getLocalName ();
    final NamedNodeMap aAttributes = aElement.getAttributes ();
    // The scheme of a value that is not there is no data
    final boolean bEmpty = aElement.getFirstChild () == null || aElement.getTextContent ().isBlank ();
    for (int i = 0; i < aAttributes.getLength (); ++i)
    {
      final Attr aAttribute = (Attr) aAttributes.item (i);
      if (!"http://www.w3.org/2000/xmlns/".equals (aAttribute.getNamespaceURI ()) && !bEmpty)
        _addValue (sPath + "/@" + _getPrefix (aAttribute) + aAttribute.getLocalName (),
                   aAttribute.getValue ().trim (),
                   aAttribute,
                   aOut);
    }
    final StringBuilder aText = new StringBuilder ();
    for (Node aChild = aElement.getFirstChild (); aChild != null; aChild = aChild.getNextSibling ())
      if (aChild instanceof final Element aChildElement)
        _collectValuesRecursive (aChildElement, sPath, aOut);
      else
        if (aChild.getNodeType () == Node.TEXT_NODE || aChild.getNodeType () == Node.CDATA_SECTION_NODE)
          aText.append (aChild.getNodeValue ());
    final String sText = aText.toString ().trim ();
    if (!sText.isEmpty ())
      _addValue (sPath, sText, aElement, aOut);
  }

  @Nullable
  private static LocalDate _getDate (@NonNull final String sValue)
  {
    final Matcher aISO = DATE_ISO.matcher (sValue);
    final Matcher a102 = DATE_102.matcher (sValue);
    final Matcher a208 = DATE_TIME_208.matcher (sValue);
    final String sDate;
    if (aISO.matches ())
      sDate = aISO.group (1) + aISO.group (2) + aISO.group (3);
    else
      if (a102.matches ())
        sDate = sValue;
      else
        if (a208.matches ())
          sDate = a208.group (1);
        else
          return null;
    try
    {
      return LocalDate.parse (sDate, DateTimeFormatter.BASIC_ISO_DATE);
    }
    catch (final DateTimeParseException ex)
    {
      return null;
    }
  }

  private static int _getFoundCount (@NonNull final XmlValue aValue,
                                     @NonNull final PdfText aPdf,
                                     @NonNull final PdfRenderContext aCtx)
  {
    final String sValue = aValue.sValue ();
    final TreeSet <String> aCandidates = new TreeSet <> ();
    aCandidates.add (sValue);
    final LocalDate aDate = _getDate (sValue);
    if (aDate != null)
      aCandidates.add (PhloxFormat.date (aDate, aCtx.getLocale (), aCtx.getFormatOptions ()));
    for (final Map.Entry <String, String []> aEntry : CODE_LISTS.entrySet ())
      if (aValue.sXPath ().endsWith (aEntry.getKey ()))
        for (final String sList : aEntry.getValue ())
          aCandidates.add (DefaultPhloxLabelResolver.INSTANCE.getCodeLabel (sList, sValue, aCtx.getLocale ()));
    int ret = 0;
    for (final String sCandidate : aCandidates)
      ret += aPdf.getTextCount (sCandidate);
    if (NUMBER.matcher (sValue).matches ())
      ret = Math.max (ret, aPdf.getNumberCount (new BigDecimal (sValue)));
    return ret;
  }

  @NonNull
  private static String _getPdfText (final byte @NonNull [] aPdf) throws IOException
  {
    try (final PDDocument aDoc = Loader.loadPDF (aPdf))
    {
      return new PDFTextStripper ().getText (aDoc);
    }
  }

  @Nullable
  private static PhloxInvoice _read (@NonNull final String sPath, final byte @NonNull [] aXml)
  {
    final ErrorList aErrors = new ErrorList ();
    try (final InputStream aIS = new NonBlockingByteArrayInputStream (aXml))
    {
      if (sPath.contains ("/cii/") || sPath.startsWith ("cii/"))
        return (sPath.startsWith ("2026/") || sPath.contains ("d25a") ? CiiInvoiceReader.fromD25A ()
                                                                       : CiiInvoiceReader.fromD16B ()).read (aIS,
                                                                                                             aErrors);
      return (sPath.startsWith ("2026/") || sPath.contains ("ubl25") ? UblInvoiceReader.fromUbl25 ()
                                                                      : UblInvoiceReader.fromUbl21 ()).read (aIS,
                                                                                                             aErrors);
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to read " + sPath, ex);
    }
  }

  @NonNull
  private static String _getShort (@NonNull final String sValue)
  {
    final String sOneLine = new String (StringReplace.replaceMultiple (sValue, new char [] { '\n', '\r', '\t' }, ' '));
    return sOneLine.length () <= 60 ? sOneLine : sOneLine.substring (0, 60) + "...";
  }

  private static byte @NonNull [] _getPdf (@NonNull final PhloxInvoice aInvoice,
                                           @NonNull final PdfRenderContext aCtx) throws Exception
  {
    final NonBlockingByteArrayOutputStream aBAOS = new NonBlockingByteArrayOutputStream ();
    new PdfInvoiceWriter ().writeTo (aInvoice, aCtx, aBAOS);
    return aBAOS.toByteArray ();
  }

  @NonNull
  private static PdfRenderContext _createContext (@NonNull final String sPath)
  {
    final PdfRenderContext ret = new PdfRenderContext ().setLocale (Locale.ENGLISH);
    // The standard fonts of PDF have no Cyrillic and Greek letters
    if (sPath.contains ("cyrillic-greek"))
      ret.setFontFamily (PdfFontFamily.of (EFontResourceRoboto.ROBOTO_NORMAL,
                                           EFontResourceRoboto.ROBOTO_BOLD,
                                           EFontResourceRoboto.ROBOTO_NORMAL_ITALIC,
                                           EFontResourceRoboto.ROBOTO_BOLD_ITALIC));
    return ret;
  }

  /**
   * @return A value of the same kind that occurs nowhere else, to replace all values of one XPath.
   */
  @NonNull
  private static String _getProbe (@NonNull final String sValue, final int nIndex)
  {
    if (DATE_ISO.matcher (sValue).matches ())
      return "2031-07-23";
    if (DATE_102.matcher (sValue).matches ())
      return "20310723";
    if (DATE_TIME_208.matcher (sValue).matches ())
      return "20310723101112+0200";
    if (TIME.matcher (sValue).matches ())
      return "21:43:05";
    if (NUMBER.matcher (sValue).matches ())
      return (73000 + nIndex) + ".2468";
    return "ZqxProbe" + nIndex;
  }

  private static void _setValue (@NonNull final Node aNode, @NonNull final String sValue)
  {
    if (aNode instanceof final Attr aAttribute)
      aAttribute.setValue (sValue);
    else
      aNode.setTextContent (sValue);
  }

  private static void _checkFixture (@NonNull final Path aFile, @NonNull final List <String> aReport) throws Exception
  {
    final String sPath = FIXTURE_DIR.toPath ().relativize (aFile).toString ();
    final byte [] aXml = Files.readAllBytes (aFile);
    final PhloxInvoice aInvoice = _read (sPath, aXml);
    if (aInvoice == null)
    {
      aReport.add (String.join (REPORT_TAB, "unreadable", "", sPath, "", "", ""));
      return;
    }

    final PdfRenderContext aCtx = _createContext (sPath);
    final PdfText aPdf = new PdfText (_getPdfText (_getPdf (aInvoice, aCtx)));

    final Document aDoc = DOMReader.readXMLDOM (aXml);
    final ICommonsList <XmlValue> aValues = new CommonsArrayList <> ();
    _collectValuesRecursive (aDoc.getDocumentElement (), "", aValues);

    final Map <String, ICommonsList <XmlValue>> aByValue = new TreeMap <> ();
    final TreeSet <String> aMerged = new TreeSet <> ();
    for (final XmlValue aValue : aValues)
      if (!_isAllowed (aValue, aInvoice) && aMerged.add (_getMergedKey (aValue)))
        aByValue.computeIfAbsent (aValue.sValue (), aKey -> new CommonsArrayList <> ()).add (aValue);
    for (final Map.Entry <String, ICommonsList <XmlValue>> aEntry : aByValue.entrySet ())
    {
      // Each XPath is checked on its own, because a code has other candidates in another place
      final int nExpected = aEntry.getValue ().size ();
      int nBest = 0;
      for (final XmlValue aValue : aEntry.getValue ())
        nBest = Math.max (nBest, _getFoundCount (aValue, aPdf, aCtx));
      for (final XmlValue aValue : aEntry.getValue ())
      {
        final int nFound = _getFoundCount (aValue, aPdf, aCtx);
        if (nFound == 0)
          aReport.add (String.join (REPORT_TAB, "missing", aValue.sXPath (), sPath, _getShort (aValue.sValue ()), "0", "1"));
        else
          if (nBest < nExpected)
            aReport.add (String.join (REPORT_TAB,
                                      "fewer",
                                      aValue.sXPath (),
                                      sPath,
                                      _getShort (aValue.sValue ()),
                                      Integer.toString (nBest),
                                      Integer.toString (nExpected)));
      }
    }
  }

  private static void _probeFixture (@NonNull final Path aFile,
                                     @NonNull final Map <String, String> aProbed) throws Exception
  {
    final String sPath = FIXTURE_DIR.toPath ().relativize (aFile).toString ();
    final byte [] aXml = Files.readAllBytes (aFile);
    final PhloxInvoice aOriginal = _read (sPath, aXml);
    if (aOriginal == null)
      return;

    final Document aDoc = DOMReader.readXMLDOM (aXml);
    final ICommonsList <XmlValue> aValues = new CommonsArrayList <> ();
    _collectValuesRecursive (aDoc.getDocumentElement (), "", aValues);
    final Map <String, ICommonsList <XmlValue>> aByXPath = new TreeMap <> ();
    for (final XmlValue aValue : aValues)
      if (!_isAllowed (aValue, aOriginal) && !aValue.sXPath ().endsWith ("#subject"))
        aByXPath.computeIfAbsent (aValue.sXPath (), aKey -> new CommonsArrayList <> ()).add (aValue);

    for (final Map.Entry <String, ICommonsList <XmlValue>> aEntry : aByXPath.entrySet ())
      if (!PROBE_OK.equals (aProbed.get (aEntry.getKey ())))
      {
        final XmlValue aFirst = aEntry.getValue ().getFirstOrNull ();
        final String sProbe = _getProbe (aFirst.sValue (), aProbed.size ());
        final ICommonsList <String> aOldValues = new CommonsArrayList <> ();
        for (final XmlValue aValue : aEntry.getValue ())
        {
          aOldValues.add (aValue.aNode () instanceof final Attr aAttribute ? aAttribute.getValue ()
                                                                           : aValue.aNode ().getTextContent ());
          _setValue (aValue.aNode (), sProbe);
        }
        final PhloxInvoice aInvoice = _read (sPath, XMLWriter.getNodeAsBytes (aDoc));
        if (aInvoice == null)
          aProbed.putIfAbsent (aEntry.getKey (), "not readable with a changed value: " + sPath);
        else
        {
          final PdfRenderContext aCtx = _createContext (sPath);
          final PdfText aPdf = new PdfText (_getPdfText (_getPdf (aInvoice, aCtx)));
          final XmlValue aProbeValue = new XmlValue (aFirst.sXPath (), sProbe, aFirst.aNode ());
          if (_getFoundCount (aProbeValue, aPdf, aCtx) > 0)
            aProbed.put (aEntry.getKey (), PROBE_OK);
          else
            aProbed.put (aEntry.getKey (), "changed value '" + sProbe + "' is not in the PDF: " + sPath);
        }
        for (int i = 0; i < aEntry.getValue ().size (); ++i)
          _setValue (aEntry.getValue ().get (i).aNode (), aOldValues.get (i));
      }
  }

  @NonNull
  private static ICommonsList <Path> _getAllFixtures () throws IOException
  {
    final ICommonsList <Path> ret = new CommonsArrayList <> ();
    try (final Stream <Path> aStream = Files.walk (FIXTURE_DIR.toPath ()))
    {
      aStream.filter (aPath -> aPath.toString ().endsWith (".xml")).sorted ().forEach (ret::add);
    }
    assertTrue (ret.size () > 100);
    return ret;
  }

  /**
   * Check that each XPath really reaches the PDF, with a value that nothing else can match. A short
   * value such as "1" could otherwise be found by chance. Each XPath is proven once, by the first
   * invoice where it works.
   *
   * @throws Exception
   *         on error
   */
  @Test
  public void testChangedValueReachesThePdf () throws Exception
  {
    final Map <String, String> aProbed = new TreeMap <> ();
    for (final Path aFile : _getAllFixtures ())
      _probeFixture (aFile, aProbed);
    final ICommonsList <String> aReport = new CommonsArrayList <> ();
    for (final Map.Entry <String, String> aEntry : aProbed.entrySet ())
      if (!PROBE_OK.equals (aEntry.getValue ()))
        aReport.add (aEntry.getKey () + REPORT_TAB + aEntry.getValue ());
    REPORT_FILE.getParentFile ().mkdirs ();
    Files.write (PROBE_REPORT_FILE.toPath (), aReport, StandardCharsets.UTF_8);
    assertTrue (aReport.size () +
                " of " +
                aProbed.size () +
                " XPaths do not reach the PDF, see " +
                PROBE_REPORT_FILE.getAbsolutePath () +
                ":\n" +
                String.join ("\n", aReport.subList (0, Math.min (aReport.size (), 20))),
                aReport.isEmpty ());
  }

  /**
   * A bank assigned creditor identifier on a credit transfer invoice is a plain line, and there is
   * no direct debit block.
   *
   * @throws Exception
   *         on error
   */
  @Test
  public void testLoneCreditorID () throws Exception
  {
    for (final String sFile : new String [] { "synthetic/ubl/invoice/phlox-reader-ubl21-invoice.xml",
                                              "synthetic/ubl/invoice/phlox-reader-ubl25-invoice.xml" })
    {
      final byte [] aXml = Files.readAllBytes (new File (FIXTURE_DIR, sFile).toPath ());
      final String sXml = StringReplace.replaceAll (new String (aXml, StandardCharsets.UTF_8),
                                                    "<cbc:PaymentMeansCode>59<",
                                                    "<cbc:PaymentMeansCode>58<");
      final PhloxInvoice aInvoice = _read (sFile, sXml.getBytes (StandardCharsets.UTF_8));
      assertNotNull (sFile, aInvoice);
      final String sPdf = _getPdfText (_getPdf (aInvoice, _createContext (sFile)));
      assertTrue (sPdf, sPdf.contains ("Bank assigned creditor identifier: DE98ZZZ09999999999"));
      assertFalse (sPdf, sPdf.contains ("Direct debit"));
    }
  }

  /**
   * Check that the values of all invoices appear in their PDF.
   *
   * @throws Exception
   *         on error
   */
  @Test
  public void testAllDataShown () throws Exception
  {
    final ICommonsList <String> aReport = new CommonsArrayList <> ();
    for (final Path aFile : _getAllFixtures ())
      _checkFixture (aFile, aReport);
    REPORT_FILE.getParentFile ().mkdirs ();
    Files.write (REPORT_FILE.toPath (), aReport, StandardCharsets.UTF_8);
    assertTrue (aReport.size () +
                " values are not shown, see " +
                REPORT_FILE.getAbsolutePath () +
                ":\n" +
                String.join ("\n", aReport.subList (0, Math.min (aReport.size (), 20))),
                aReport.isEmpty ());
    for (final AllowedValue aAllowed : ALLOWED)
      assertTrue ("The exemption of '" +
                  aAllowed.m_sXPathSuffix +
                  "' is not needed any more (" +
                  aAllowed.m_sReason +
                  ")",
                  aAllowed.m_bUsed);
  }
}
