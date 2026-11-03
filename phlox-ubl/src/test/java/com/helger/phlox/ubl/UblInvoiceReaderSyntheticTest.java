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
package com.helger.phlox.ubl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringReplace;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxDirectDebit;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxItem;
import com.helger.phlox.api.model.PhloxLineCharge;
import com.helger.phlox.testfiles.MockPhloxTestFiles;

/**
 * Test class for class {@link UblInvoiceReader} with the edge cases of the synthetic reader files.
 * Every test covers all files the case applies to, so that the readers of both UBL versions and
 * both root elements cannot drift apart.
 *
 * @author Philip Helger
 */
public final class UblInvoiceReaderSyntheticTest
{
  private static final String UBL21_INVOICE = "ubl/invoice/phlox-reader-ubl21-invoice.xml";
  private static final String UBL21_CREDIT_NOTE = "ubl/creditnote/phlox-reader-ubl21-creditnote.xml";
  private static final String UBL25_INVOICE = "ubl/invoice/phlox-reader-ubl25-invoice.xml";
  private static final String UBL25_CREDIT_NOTE = "ubl/creditnote/phlox-reader-ubl25-creditnote.xml";
  private static final String [] ALL_FILES = { UBL21_INVOICE, UBL21_CREDIT_NOTE, UBL25_INVOICE, UBL25_CREDIT_NOTE };
  private static final String SEPA_ID = "DE98ZZZ09999999999";

  @NonNull
  private static String _load (@NonNull final String sFile)
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH + "synthetic/" + sFile)
                                                                                                            .getInputStream ())
    {
      return new String (aIS.readAllBytes (), StandardCharsets.UTF_8);
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sFile, ex);
    }
  }

  @NonNull
  private static String _load (@NonNull final String sFile,
                               @NonNull final String sSearch,
                               @NonNull final String sReplace)
  {
    return StringReplace.replaceAll (_load (sFile), sSearch, sReplace);
  }

  @NonNull
  private static UblInvoiceReader _readerOf (@NonNull final String sFile)
  {
    return sFile.equals (UBL21_INVOICE) || sFile.equals (UBL21_CREDIT_NOTE) ? UblInvoiceReader.fromUbl21 ()
                                                                            : UblInvoiceReader.fromUbl25 ();
  }

  @Nullable
  private static PhloxInvoice _readXml (@NonNull final UblInvoiceReader aReader,
                                        @NonNull final String sXml,
                                        @NonNull final ErrorList aErrors)
  {
    return aReader.read (new NonBlockingByteArrayInputStream (sXml.getBytes (StandardCharsets.UTF_8)), aErrors);
  }

  @NonNull
  private static PhloxInvoice _read (@NonNull final String sFile, @NonNull final String sXml)
  {
    final ErrorList aErrors = new ErrorList ();
    final PhloxInvoice ret = _readXml (_readerOf (sFile), sXml, aErrors);
    assertNotNull (sFile + ": " + aErrors, ret);
    assertFalse (sFile + ": " + aErrors, aErrors.containsAtLeastOneError ());
    return ret;
  }

  @NonNull
  private static PhloxInvoice _read (@NonNull final String sFile)
  {
    return _read (sFile, _load (sFile));
  }

  @NonNull
  private static String _typeCodeReplaced (@NonNull final String sFile, @NonNull final String sTypeCode)
  {
    return StringReplace.replaceAll (_load (sFile, "TypeCode>381<", "TypeCode>" + sTypeCode + "<"),
                                     "TypeCode>380<",
                                     "TypeCode>" + sTypeCode + "<");
  }

  @Test
  public void testConstructorDetectsTheEdition ()
  {
    assertNull (new UblInvoiceReader ().getEdition ());
    final PhloxInvoice aModel = _readXml (new UblInvoiceReader (), _load (UBL25_INVOICE), new ErrorList ());
    assertNotNull (aModel);
    assertEquals (EEN16931Edition.EN2026, aModel.getEdition ());
  }

  @Test
  public void testTypeCodeDecidesTheKind ()
  {
    // The root element and BT-3 of the files disagree
    assertTrue (_read (UBL21_INVOICE).isCreditNote ());
    assertTrue (_read (UBL25_INVOICE).isCreditNote ());
    assertTrue (_read (UBL21_CREDIT_NOTE).isInvoice ());
    assertTrue (_read (UBL25_CREDIT_NOTE).isInvoice ());

    for (final String sFile : ALL_FILES)
    {
      assertTrue (sFile, _read (sFile, _typeCodeReplaced (sFile, "381")).isCreditNote ());
      assertTrue (sFile, _read (sFile, _typeCodeReplaced (sFile, "502")).isCreditNote ());
      assertTrue (sFile, _read (sFile, _typeCodeReplaced (sFile, "380")).isInvoice ());
    }
  }

  @Test
  public void testRootElementDecidesWithoutTypeCode ()
  {
    for (final String sFile : ALL_FILES)
    {
      final boolean bCreditNote = sFile.endsWith ("creditnote.xml");

      // Unknown code: same warning as in CII
      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aUnknown = _readXml (_readerOf (sFile), _typeCodeReplaced (sFile, "999"), aErrors);
      assertNotNull (sFile, aUnknown);
      assertTrue (sFile, bCreditNote == aUnknown.isCreditNote ());
      assertEquals (sFile, 1, aErrors.getCount (x -> x.getErrorText (Locale.ROOT).startsWith ("Unknown BT-3")));
      assertFalse (sFile, aErrors.containsAtLeastOneError ());

      // Missing code: no warning
      final ErrorList aErrors2 = new ErrorList ();
      final String sXml = RegExHelper.stringReplacePattern ("<cbc:(Invoice|CreditNote)TypeCode>999</cbc:\\w+>",
                                                            _typeCodeReplaced (sFile, "999"),
                                                            "");
      final PhloxInvoice aMissing = _readXml (_readerOf (sFile), sXml, aErrors2);
      assertNotNull (sFile, aMissing);
      assertNull (sFile, aMissing.getTypeCode ());
      assertTrue (sFile, bCreditNote == aMissing.isCreditNote ());
      assertEquals (sFile, 0, aErrors2.getCount (x -> x.getErrorText (Locale.ROOT).startsWith ("Unknown BT-3")));
    }
  }

  @NonNull
  private static PhloxLineCharge _readLineChargeWithCode (@NonNull final String sCode)
  {
    final String sCharge = "<cac:AllowanceCharge><cbc:ChargeIndicator>true</cbc:ChargeIndicator>" +
                           sCode +
                           "<cbc:Amount currencyID=\"EUR\">1.00</cbc:Amount></cac:AllowanceCharge>";
    final PhloxInvoice aModel = _read (UBL25_INVOICE, _load (UBL25_INVOICE, "<cac:Item>", sCharge + "<cac:Item>"));
    final ICommonsList <PhloxLineCharge> aCharges = aModel.lines ().get (0).charges ();
    return aCharges.getLastOrNull ();
  }

  @Test
  public void testLineNonVatTaxCodeWithOptionalListID ()
  {
    // With the identifier of the code list
    PhloxLineCharge aCharge = _readLineChargeWithCode ("<cbc:AllowanceChargeReasonCode listID=\"5153\">AAA</cbc:AllowanceChargeReasonCode>");
    assertEquals ("AAA", aCharge.getNonVatTaxTypeCode ());
    assertEquals ("5153", aCharge.getNonVatTaxTypeCodeListID ());
    assertNull (aCharge.getReasonCode ());

    // The identifier may be omitted: only part of UNTDID 5153, and the identifier has only one
    // value
    aCharge = _readLineChargeWithCode ("<cbc:AllowanceChargeReasonCode>ENV</cbc:AllowanceChargeReasonCode>");
    assertEquals ("ENV", aCharge.getNonVatTaxTypeCode ());
    assertEquals ("5153", aCharge.getNonVatTaxTypeCodeListID ());
    assertNull (aCharge.getReasonCode ());

    // Added to UNTDID 5153 after D15B
    aCharge = _readLineChargeWithCode ("<cbc:AllowanceChargeReasonCode>AAO</cbc:AllowanceChargeReasonCode>");
    assertEquals ("AAO", aCharge.getNonVatTaxTypeCode ());
    assertNull (aCharge.getReasonCode ());

    // Part of UNTDID 7161
    aCharge = _readLineChargeWithCode ("<cbc:AllowanceChargeReasonCode>FC</cbc:AllowanceChargeReasonCode>");
    assertEquals ("FC", aCharge.getReasonCode ());
    assertNull (aCharge.getNonVatTaxTypeCode ());

    // Part of both code lists
    aCharge = _readLineChargeWithCode ("<cbc:AllowanceChargeReasonCode>AAA</cbc:AllowanceChargeReasonCode>");
    assertEquals ("AAA", aCharge.getReasonCode ());
    assertNull (aCharge.getNonVatTaxTypeCode ());

    // Excluded by the binding, and an unknown code
    for (final String sCode : new String [] { "VAT", "XYZ" })
    {
      aCharge = _readLineChargeWithCode ("<cbc:AllowanceChargeReasonCode>" +
                                         sCode +
                                         "</cbc:AllowanceChargeReasonCode>");
      assertEquals (sCode, aCharge.getReasonCode ());
      assertNull (aCharge.getNonVatTaxTypeCode ());
    }
  }

  @Test
  public void testProjectReferenceInCreditNote21 ()
  {
    assertNull (_read (UBL21_CREDIT_NOTE).getProjectReference ());

    final String sProject = "<cac:AdditionalDocumentReference><cbc:ID>PROJECT-1</cbc:ID></cac:AdditionalDocumentReference>";
    final String sOther = "<cac:AdditionalDocumentReference><cbc:ID>OTHER</cbc:ID></cac:AdditionalDocumentReference>";
    final String sAnchor = "<cac:AccountingSupplierParty>";

    // A reference with only an ID is BT-11
    PhloxInvoice aModel = _read (UBL21_CREDIT_NOTE, _load (UBL21_CREDIT_NOTE, sAnchor, sProject + sAnchor));
    assertEquals ("PROJECT-1", aModel.getProjectReference ());
    assertEquals (1, aModel.supportingDocuments ().size ());
    assertEquals ("SUPPORT-1", aModel.supportingDocuments ().get (0).getReference ());

    // There is only one BT-11
    aModel = _read (UBL21_CREDIT_NOTE, _load (UBL21_CREDIT_NOTE, sAnchor, sProject + sOther + sAnchor));
    assertEquals ("PROJECT-1", aModel.getProjectReference ());
    assertEquals (2, aModel.supportingDocuments ().size ());
    assertEquals ("OTHER", aModel.supportingDocuments ().get (1).getReference ());
  }

  @Test
  public void testInvoicedObjectInCreditNote21 ()
  {
    final PhloxInvoice aModel = _read (UBL21_CREDIT_NOTE);
    assertEquals ("OBJECT-1", aModel.getInvoicedObjectIdentifier ().getValue ());
    assertEquals ("AVE", aModel.getInvoicedObjectIdentifier ().getSchemeID ());
    assertEquals (1, aModel.supportingDocuments ().size ());
    assertEquals ("SUPPORT-1", aModel.supportingDocuments ().get (0).getReference ());

    // The invoice code is accepted as well
    final PhloxInvoice aModel2 = _read (UBL21_CREDIT_NOTE,
                                        _load (UBL21_CREDIT_NOTE,
                                               "<cbc:DocumentType>ATS</cbc:DocumentType>",
                                               "<cbc:DocumentTypeCode>130</cbc:DocumentTypeCode>"));
    assertEquals ("OBJECT-1", aModel2.getInvoicedObjectIdentifier ().getValue ());
  }

  @Test
  public void testInvoicedObjectInCreditNote25 ()
  {
    // The 2026 binding still accepts the form of the 2017 binding, see the migration table
    final PhloxInvoice aModel = _read (UBL25_CREDIT_NOTE,
                                       _load (UBL25_CREDIT_NOTE,
                                              "<cbc:DocumentTypeCode>130</cbc:DocumentTypeCode>",
                                              "<cbc:DocumentType>ATS</cbc:DocumentType>"));
    assertEquals ("OBJECT-1", aModel.getInvoicedObjectIdentifier ().getValue ());
    assertEquals ("AVE", aModel.getInvoicedObjectIdentifier ().getSchemeID ());
    assertEquals (1, aModel.supportingDocuments ().size ());
    assertEquals ("SUPPORT-1", aModel.supportingDocuments ().get (0).getReference ());
  }

  @Test
  public void testInvoicedObjectNeedsType130Elsewhere ()
  {
    // The Invoice of both bindings only knows code 130
    for (final String sFile : new String [] { UBL21_INVOICE, UBL25_INVOICE })
    {
      assertEquals (sFile, "OBJECT-1", _read (sFile).getInvoicedObjectIdentifier ().getValue ());

      final PhloxInvoice aModel = _read (sFile,
                                         _load (sFile,
                                                "<cbc:DocumentTypeCode>130</cbc:DocumentTypeCode>",
                                                "<cbc:DocumentType>ATS</cbc:DocumentType>"));
      assertNull (sFile, aModel.getInvoicedObjectIdentifier ());
    }
  }

  @Test
  public void testFillerIsNotPresent ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      // BT-13 next to BT-14
      assertNull (sFile, aModel.getPurchaseOrderReference ());
      assertEquals (sFile, "SO-1", aModel.getSalesOrderReference ());
      // BT-132
      assertNull (sFile, aModel.lines ().get (0).getPurchaseOrderLineRef ());
    }
    for (final String sFile : new String [] { UBL25_INVOICE, UBL25_CREDIT_NOTE })
    {
      final PhloxInvoiceLine aLine = _read (sFile).lines ().get (0);
      assertNull (sFile, aLine.getSalesOrderLineReference ());
      assertNull (sFile, aLine.getDespatchAdviceLineReference ());
      assertNull (sFile, aLine.getReceivingAdviceLineReference ());
      assertNull (sFile, aLine.getDeliveryNoteLineReference ());
      // The terms the filler stands next to are kept
      assertEquals (sFile, "LINE-PO", aLine.getPurchaseOrderReference ());
      assertEquals (sFile, "LINE-SO", aLine.getSalesOrderReference ());
      assertEquals (sFile, "LINE-DESP", aLine.getDespatchAdviceReference ());
      assertEquals (sFile, "LINE-RECV", aLine.getReceivingAdviceReference ());
      assertEquals (sFile, "LINE-DELN", aLine.getDeliveryNoteReference ());
    }
  }

  @Test
  public void testPeppolFillerIsAbsent ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile, _load (sFile, ">None<", ">NA<"));
      assertNull (sFile, aModel.getPurchaseOrderReference ());
      assertNull (sFile, aModel.lines ().get (0).getPurchaseOrderLineRef ());
    }
  }

  @Test
  public void testOtherValuesThanTheFillerAreKept ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile, _load (sFile, ">None<", ">NA1<"));
      assertEquals (sFile, "NA1", aModel.getPurchaseOrderReference ());
      assertEquals (sFile, "NA1", aModel.lines ().get (0).getPurchaseOrderLineRef ());
    }
  }

  @Test
  public void testCreditorIDWithoutMandate ()
  {
    for (final String sFile : ALL_FILES)
    {
      // The second payment means says direct debit
      final PhloxInvoice aModel = _read (sFile);
      final PhloxDirectDebit aDD = aModel.getPaymentInstructions ().getDirectDebit ();
      assertNotNull (sFile, aDD);
      assertNull (sFile, aDD.getMandateReferenceID ());
      assertEquals (sFile, SEPA_ID, aDD.getBankAssignedCreditorID ());
      // It is never a seller identifier
      assertEquals (sFile, 1, aModel.getSeller ().identifiers ().size ());
      assertEquals (sFile, "4012345000009", aModel.getSeller ().identifiers ().get (0).getValue ());

      // Code 49 is a direct debit as well
      final PhloxInvoice aModel49 = _read (sFile,
                                           _load (sFile, "<cbc:PaymentMeansCode>59<", "<cbc:PaymentMeansCode>49<"));
      assertNotNull (sFile, aModel49.getPaymentInstructions ().getDirectDebit ());

      // Same rule as in CII: the other means are no direct debit
      final PhloxInvoice aModel58 = _read (sFile,
                                           _load (sFile, "<cbc:PaymentMeansCode>59<", "<cbc:PaymentMeansCode>58<"));
      assertNull (sFile, aModel58.getPaymentInstructions ().getDirectDebit ());
      assertEquals (sFile, 1, aModel58.getSeller ().identifiers ().size ());
    }
  }

  @Test
  public void testAllPaymentTerms ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, 2, aModel.paymentTerms ().size ());
      assertEquals (sFile, "First terms", aModel.paymentTerms ().get (0).getText ());
      assertEquals (sFile, "Second terms", aModel.paymentTerms ().get (1).getText ());
    }
  }

  @Test
  public void testLineObjectNeedsType130 ()
  {
    for (final String sFile : ALL_FILES)
    {
      assertEquals (sFile, "LINE-OBJECT", _read (sFile).lines ().get (0).getObjectIdentifier ().getValue ());
      assertEquals (sFile, "AVE", _read (sFile).lines ().get (0).getObjectIdentifier ().getSchemeID ());

      final PhloxInvoice aModel = _read (sFile,
                                         _load (sFile,
                                                "<cbc:ID schemeID=\"AVE\">LINE-OBJECT</cbc:ID>\n      <cbc:DocumentTypeCode>130<",
                                                "<cbc:ID schemeID=\"AVE\">LINE-OBJECT</cbc:ID>\n      <cbc:DocumentTypeCode>916<"));
      assertNull (sFile, aModel.lines ().get (0).getObjectIdentifier ());
    }
  }

  @Test
  public void testLegacyElementsWithForcedEdition ()
  {
    // A document of the 2017 binding read with the reader of the 2026 edition
    for (final String sFile : new String [] { UBL21_INVOICE, UBL21_CREDIT_NOTE })
    {
      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aModel = _readXml (UblInvoiceReader.fromUbl25 (), _load (sFile), aErrors);
      assertNotNull (sFile + ": " + aErrors, aModel);
      assertEquals (sFile, EEN16931Edition.EN2026, aModel.getEdition ());
      // cbc:Note with subject code
      assertEquals (sFile, 1, aModel.invoiceNotes ().size ());
      assertEquals (sFile, "AAI", aModel.invoiceNotes ().get (0).getSubjectCode ());
      assertEquals (sFile, "Header note", aModel.invoiceNotes ().get (0).getContent ());
      // cbc:BuyerReference
      assertEquals (sFile, 1, aModel.buyerReferences ().size ());
      assertEquals (sFile, "LEGACY-BUYER-REF", aModel.buyerReferences ().get (0).getValue ());
      // cbc:DueDate of the Invoice, cac:PaymentMeans/cbc:PaymentDueDate of the CreditNote
      assertEquals (sFile, LocalDate.of (2026, 2, 15), aModel.getPaymentDueDate ());
    }
  }

  @Test
  public void testLegacyElementsFollowTheBinding ()
  {
    final String sLegacy = "<cbc:Note>Legacy note</cbc:Note>\n  <cbc:DocumentCurrencyCode>";
    final PhloxInvoice aModel = _read (UBL25_INVOICE, _load (UBL25_INVOICE, "<cbc:DocumentCurrencyCode>", sLegacy));
    // Nothing gets lost: the subject code without a content, the note, the legacy note
    assertEquals (3, aModel.invoiceNotes ().size ());
    assertEquals ("AAI", aModel.invoiceNotes ().get (0).getSubjectCode ());
    assertEquals ("Header note", aModel.invoiceNotes ().get (1).getContent ());
    assertEquals ("Legacy note", aModel.invoiceNotes ().get (2).getContent ());
  }

  @Test
  public void testXsdErrorsAreInTheErrorList ()
  {
    for (final String sFile : ALL_FILES)
    {
      final ErrorList aErrors = new ErrorList ();
      final String sXml = _load (sFile,
                                 "<cbc:DocumentCurrencyCode>",
                                 "<cbc:Nonsense>x</cbc:Nonsense><cbc:DocumentCurrencyCode>");
      assertNull (sFile, _readXml (_readerOf (sFile), sXml, aErrors));
      assertEquals (sFile + ": " + aErrors, 2, aErrors.size ());
      assertTrue (aErrors.toString (), aErrors.get (0).getErrorText (Locale.ROOT).contains ("cvc-complex-type"));
      assertTrue (aErrors.toString (),
                  aErrors.get (1).getErrorText (Locale.ROOT).startsWith ("Source could not be parsed"));
    }
  }

  @Test
  public void testTrimming ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, "EUR", aModel.getCurrency ());
      final PhloxInvoiceLine aLine = aModel.lines ().get (0);
      assertEquals (sFile, "C62", aLine.getQuantityUnit ());
      assertEquals (sFile, "Item description", aLine.getItem ().getDescription ());
      assertEquals (sFile, "12345", aLine.getItem ().classifications ().get (0).getCode ());
      assertEquals (sFile, "STI", aLine.getItem ().classifications ().get (0).getListID ());
    }
    assertEquals ("LEGACY-BUYER-REF", _read (UBL21_INVOICE).buyerReferences ().get (0).getValue ());
    assertEquals ("AAI", _read (UBL21_INVOICE).invoiceNotes ().get (0).getSubjectCode ());
  }

  @Test
  public void testFirstPriceDiscount ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, 0, new BigDecimal ("5").compareTo (aModel.lines ().get (0).getPrice ().getDiscount ()));
      assertEquals (sFile, 0, new BigDecimal ("30").compareTo (aModel.lines ().get (0).getPrice ().getGrossPrice ()));
    }
  }

  @Test
  public void testNoEmptyRows ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      // A blank note without a subject code is no row, one with a subject code is
      assertEquals (sFile, sFile.contains ("ubl25") ? 2 : 1, aModel.invoiceNotes ().size ());
      assertEquals (sFile, 1, aModel.precedingInvoiceRefs ().size ());
      assertEquals (sFile, "PREV-1", aModel.precedingInvoiceRefs ().get (0).getNumber ());
      assertEquals (sFile, 1, aModel.supportingDocuments ().size ());
      assertTrue (sFile, aModel.getBuyer ().identifiers ().isEmpty ());

      final PhloxInvoiceLine aLine = aModel.lines ().get (0);
      assertTrue (sFile, aLine.precedingInvoiceRefs ().isEmpty ());
      final PhloxItem aItem = aLine.getItem ();
      assertEquals (sFile, 1, aItem.attributes ().size ());
      assertEquals (sFile, 1, aItem.classifications ().size ());
    }
  }

  @Test
  public void testFirstTaxRegistration ()
  {
    for (final String sFile : ALL_FILES)
      assertEquals (sFile, "TAX-FIRST", _read (sFile).getSeller ().getTaxRegistration ());
  }

  @Test
  public void testSubtotalInOtherCurrency ()
  {
    // BT-184 carries it in EN 16931:2026. EN 16931:2017 has no such term, but the value is shown
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, 2, aModel.vatBreakdowns ().size ());
      assertNull (sFile, aModel.vatBreakdowns ().get (0).getCurrency ());
      assertEquals (sFile, "USD", aModel.vatBreakdowns ().get (1).getCurrency ());
      // BT-116-1 is the currency of the breakdown in both
      assertNull (sFile, aModel.vatBreakdowns ().get (0).getTaxableAmountCurrency ());
      assertNull (sFile, aModel.vatBreakdowns ().get (1).getTaxableAmountCurrency ());
    }
  }

  @Test
  public void testTaxableAmountInOtherCurrency ()
  {
    for (final String sFile : ALL_FILES)
    {
      // Differs from BT-5
      PhloxInvoice aModel = _read (sFile,
                                   _load (sFile,
                                          "<cbc:TaxableAmount currencyID=\"EUR\">",
                                          "<cbc:TaxableAmount currencyID=\"CHF\">"));
      assertEquals (sFile, "CHF", aModel.vatBreakdowns ().get (0).getTaxableAmountCurrency ());

      // Differs from BT-184
      aModel = _read (sFile,
                      _load (sFile, "<cbc:TaxableAmount currencyID=\"USD\">", "<cbc:TaxableAmount currencyID=\"EUR\">"));
      assertEquals (sFile, "EUR", aModel.vatBreakdowns ().get (1).getTaxableAmountCurrency ());
    }
  }
}
