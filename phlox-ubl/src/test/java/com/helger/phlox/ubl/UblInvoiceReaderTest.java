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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetTime;
import java.time.ZoneOffset;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxDirectDebit;
import com.helger.phlox.api.model.PhloxDocumentCharge;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxItemAttribute;
import com.helger.phlox.api.model.PhloxPaymentTerms;
import com.helger.phlox.api.model.PhloxSupportingDocument;
import com.helger.phlox.testfiles.MockPhloxTestFiles;

/**
 * Tests for {@link UblInvoiceReader} — make sure every file in the corpus parses without fatal
 * errors and produces a non-empty model, and that the terms new in EN 16931:2026 end up in the
 * model.
 */
public final class UblInvoiceReaderTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (UblInvoiceReaderTest.class);

  private static final String BASE = MockPhloxTestFiles.BASE_PATH + "2026/ubl/invoice/";
  private static final String BASE_CN = MockPhloxTestFiles.BASE_PATH + "2026/ubl/creditnote/";

  @NonNull
  private static PhloxInvoice _read (final UblInvoiceReader aReader, final String sPath)
  {
    try (final InputStream aIS = new ClassPathResource (sPath).getInputStream ())
    {
      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aModel = aReader.read (aIS, aErrors);
      assertNotNull ("Failed to parse " + sPath + " — errors: " + aErrors, aModel);
      assertFalse ("Parsed " + sPath + " but produced errors: " + aErrors, aErrors.containsAtLeastOneError ());
      return aModel;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sPath, ex);
    }
  }

  @NonNull
  private static PhloxInvoice _readWithoutDocumentDescription (final UblInvoiceReader aReader, final String sPath)
  {
    try (final InputStream aIS = new ClassPathResource (sPath).getInputStream ())
    {
      // BT-123 is optional
      final String sXml = RegExHelper.stringReplacePattern ("<cbc:DocumentDescription>[^<]*</cbc:DocumentDescription>",
                                                            new String (aIS.readAllBytes (), StandardCharsets.UTF_8),
                                                            "");
      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aModel = aReader.read (new NonBlockingByteArrayInputStream (sXml.getBytes (StandardCharsets.UTF_8)),
                                                aErrors);
      assertNotNull ("Failed to parse " + sPath + " — errors: " + aErrors, aModel);
      assertFalse ("Parsed " + sPath + " but produced errors: " + aErrors, aErrors.containsAtLeastOneError ());
      return aModel;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sPath, ex);
    }
  }

  private static int _readAll (final UblInvoiceReader aReader,
                               final ICommonsList <ClassPathResource> aFiles,
                               final EEN16931Edition eExpectedEdition,
                               final boolean bCreditNote)
  {
    int nCount = 0;
    for (final ClassPathResource aRes : aFiles)
    {
      final PhloxInvoice aModel = _read (aReader, aRes.getPath ());
      assertNotNull ("Missing invoice number in " + aRes.getPath (), aModel.getNumber ());
      assertSame ("Wrong edition in " + aRes.getPath (), eExpectedEdition, aModel.getEdition ());
      assertTrue ("Wrong kind in " + aRes.getPath (), bCreditNote == aModel.isCreditNote ());
      nCount++;
    }
    return nCount;
  }

  @Test
  public void testReadAllInvoices ()
  {
    final int nCount = _readAll (UblInvoiceReader.fromUbl21 (),
                                 MockPhloxTestFiles.getUbl21InvoiceFiles (),
                                 EEN16931Edition.EN2017,
                                 false);
    LOGGER.info ("Parsed " + nCount + " UBL 2.1 Invoice example files");
    assertTrue ("Expected at least 10 invoice examples", nCount >= 10);
  }

  @Test
  public void testReadAllCreditNotes ()
  {
    final int nCount = _readAll (UblInvoiceReader.fromUbl21 (),
                                 MockPhloxTestFiles.getUbl21CreditNoteFiles (),
                                 EEN16931Edition.EN2017,
                                 true);
    LOGGER.info ("Parsed " + nCount + " UBL 2.1 CreditNote example files");
    assertTrue (nCount >= 1);
  }

  @Test
  public void testReadAllInvoices25 ()
  {
    final int nCount = _readAll (UblInvoiceReader.fromUbl25 (),
                                 MockPhloxTestFiles.getUbl25InvoiceFiles (),
                                 EEN16931Edition.EN2026,
                                 false);
    LOGGER.info ("Parsed " + nCount + " UBL 2.5 Invoice example files");
    assertEquals (MockPhloxTestFiles.getUbl25InvoiceFiles ().size (), nCount);
  }

  @Test
  public void testReadAllCreditNotes25 ()
  {
    final int nCount = _readAll (UblInvoiceReader.fromUbl25 (),
                                 MockPhloxTestFiles.getUbl25CreditNoteFiles (),
                                 EEN16931Edition.EN2026,
                                 true);
    LOGGER.info ("Parsed " + nCount + " UBL 2.5 CreditNote example files");
    assertEquals (MockPhloxTestFiles.getUbl25CreditNoteFiles ().size (), nCount);
  }

  @Test
  public void testAutoDetect ()
  {
    final UblInvoiceReader aReader = UblInvoiceReader.autoDetect ();
    assertNull (aReader.getEdition ());
    assertEquals (MockPhloxTestFiles.getUbl21InvoiceFiles ().size (),
                  _readAll (aReader, MockPhloxTestFiles.getUbl21InvoiceFiles (), EEN16931Edition.EN2017, false));
    assertEquals (MockPhloxTestFiles.getUbl21CreditNoteFiles ().size (),
                  _readAll (aReader, MockPhloxTestFiles.getUbl21CreditNoteFiles (), EEN16931Edition.EN2017, true));
    assertEquals (MockPhloxTestFiles.getUbl25InvoiceFiles ().size (),
                  _readAll (aReader, MockPhloxTestFiles.getUbl25InvoiceFiles (), EEN16931Edition.EN2026, false));
    assertEquals (MockPhloxTestFiles.getUbl25CreditNoteFiles ().size (),
                  _readAll (aReader, MockPhloxTestFiles.getUbl25CreditNoteFiles (), EEN16931Edition.EN2026, true));
  }

  @Test
  public void testAutoDetectWithoutBT24 ()
  {
    final String sXml = "<Invoice xmlns=\"urn:oasis:names:specification:ubl:schema:xsd:Invoice-2\" " +
                        "xmlns:cbc=\"urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2\">" +
                        "<cbc:ID>1</cbc:ID></Invoice>";
    final ErrorList aErrors = new ErrorList ();
    final PhloxInvoice aModel = UblInvoiceReader.autoDetect ()
                                                .read (new NonBlockingByteArrayInputStream (sXml.getBytes (StandardCharsets.UTF_8)),
                                                       aErrors);
    assertNull (aModel);
    assertTrue (aErrors.containsAtLeastOneError ());
    assertTrue (aErrors.toString ().contains ("BT-24"));
  }

  @Test
  public void testAutoDetectNoXml ()
  {
    final ErrorList aErrors = new ErrorList ();
    assertNull (UblInvoiceReader.autoDetect ()
                                .read (new NonBlockingByteArrayInputStream ("no xml".getBytes (StandardCharsets.UTF_8)),
                                       aErrors));
    assertTrue (aErrors.containsAtLeastOneError ());
  }

  @Test
  public void testEdition21Defaults ()
  {
    // BT-10 and BT-20 are mapped to the lists of the superset model
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (),
                                       MockPhloxTestFiles.BASE_PATH + "2017/ubl/invoice/guide-example1.xml");
    assertSame (EEN16931Edition.EN2017, aModel.getEdition ());
    assertTrue (aModel.getProcessControl ().getSpecificationIdentifier ().startsWith ("urn:cen.eu:en16931:2017"));
    for (final PhloxPaymentTerms aTerms : aModel.paymentTerms ())
    {
      assertTrue (aTerms.earlyDiscounts ().isEmpty ());
      assertTrue (aTerms.latePenalties ().isEmpty ());
    }
  }

  @Test
  public void testNewHeaderTerms25 ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-new-header-invoice-ubl.xml");

    // BT-10 + BT-10-1
    assertEquals (1, aModel.buyerReferences ().size ());
    assertEquals ("BUYER-REF-4711", aModel.buyerReferences ().get (0).getValue ());
    assertEquals ("ADE", aModel.buyerReferences ().get (0).getSchemeID ());
    // BT-21 + BT-22
    assertEquals (1, aModel.invoiceNotes ().size ());
    assertEquals ("AAI", aModel.invoiceNotes ().get (0).getSubjectCode ());
    assertEquals ("Header note", aModel.invoiceNotes ().get (0).getContent ());
    // BT-166
    assertEquals (OffsetTime.of (12, 5, 3, 0, ZoneOffset.ofHours (1)), aModel.getIssueTime ());
    // BT-167 + BT-167-1 + BT-167-2
    assertEquals (0, new BigDecimal ("1.1").compareTo (aModel.getVatExchangeRate ()));
    assertEquals ("EUR", aModel.getVatExchangeRateTargetCurrency ());
    assertEquals ("USD", aModel.getVatExchangeRateSourceCurrency ());
    // BT-197
    assertEquals ("DELNOTE-8", aModel.getDeliveryNoteReference ());
    // BT-11, BT-9
    assertEquals ("PROJECT-7", aModel.getProjectReference ());
    assertEquals (LocalDate.of (2026, 2, 14), aModel.getPaymentDueDate ());
    // BT-18 / BG-24
    assertEquals ("METER-9", aModel.getInvoicedObjectIdentifier ().getValue ());
    assertEquals (1, aModel.supportingDocuments ().size ());
    // BT-202
    assertEquals (2, aModel.precedingInvoiceRefs ().size ());
    assertEquals ("380", aModel.precedingInvoiceRefs ().get (0).getTypeCode ());
    assertNull (aModel.precedingInvoiceRefs ().get (1).getTypeCode ());
    // BT-90 from the payee, BT-215/BT-216
    assertEquals ("SEPA-CRED-1", aModel.getPaymentInstructions ().getDirectDebit ().getBankAssignedCreditorID ());
    assertEquals ("Buyer Account", aModel.getPaymentInstructions ().getDirectDebit ().getDebitedAccountName ());
    assertEquals ("SPSBAT2SXXX", aModel.getPaymentInstructions ().getDirectDebit ().getDebitedAccountProviderID ());
    // BT-184: tax subtotals are in the invoice currency
    assertNull (aModel.vatBreakdowns ().get (0).getCurrency ());
    // BT-111
    assertEquals (0, new BigDecimal ("24.2").compareTo (aModel.getDocumentTotals ().getTaxTotalAccountingCurrency ()));
    // BT-120/BT-121
    assertEquals ("VATEX-EU-AE", aModel.vatBreakdowns ().get (1).getExemptionReasonCode ());
  }

  @Test
  public void testPaymentTerms25 ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-new-paymentterms-invoice-ubl.xml");
    assertEquals (3, aModel.paymentTerms ().size ());
    // BG-33 / BT-20
    assertEquals ("Net 30 days", aModel.paymentTerms ().get (0).getText ());
    // BG-35
    assertEquals (1, aModel.paymentTerms ().get (1).earlyDiscounts ().size ());
    assertEquals (LocalDate.of (2026, 1, 25), aModel.paymentTerms ().get (1).earlyDiscounts ().get (0).getEndDate ());
    assertEquals (0,
                  new BigDecimal ("2.00").compareTo (aModel.paymentTerms ()
                                                           .get (1)
                                                           .earlyDiscounts ()
                                                           .get (0)
                                                           .getPercentage ()));
    assertEquals (0,
                  new BigDecimal ("2.64").compareTo (aModel.paymentTerms ()
                                                           .get (1)
                                                           .earlyDiscounts ()
                                                           .get (0)
                                                           .getAmount ()));
    // BG-36
    assertEquals (1, aModel.paymentTerms ().get (2).latePenalties ().size ());
    assertEquals (LocalDate.of (2026, 2, 15), aModel.paymentTerms ().get (2).latePenalties ().get (0).getStartDate ());
    assertEquals (0,
                  new BigDecimal ("9.20").compareTo (aModel.paymentTerms ()
                                                           .get (2)
                                                           .latePenalties ()
                                                           .get (0)
                                                           .getYearlyInterestPercentage ()));
    assertEquals (0,
                  new BigDecimal ("12.5").compareTo (aModel.paymentTerms ()
                                                           .get (2)
                                                           .latePenalties ()
                                                           .get (0)
                                                           .getAmount ()));
  }

  @Test
  public void testThirdPartyCharges25 ()
  {
    for (final String sPath : new String [] { BASE + "d25a-new-bg34-invoice-ubl.xml",
                                              BASE_CN + "d25a-new-bg34-creditnote-ubl.xml" })
    {
      final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), sPath);
      assertEquals (2, aModel.getDocumentTotals ().thirdPartyCharges ().size ());
      assertEquals (0,
                    new BigDecimal ("3.2").compareTo (aModel.getDocumentTotals ()
                                                            .thirdPartyCharges ()
                                                            .get (0)
                                                            .getAmount ()));
      assertEquals ("Copyright levy", aModel.getDocumentTotals ().thirdPartyCharges ().get (0).getSpecification ());
      assertEquals ("Recycling fee", aModel.getDocumentTotals ().thirdPartyCharges ().get (1).getSpecification ());
    }
  }

  @Test
  public void testAllowanceCharge25 ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-new-allowchg-invoice-ubl.xml");
    // BT-173, BT-174, BT-213
    assertEquals ("Intra-community supply", aModel.documentAllowances ().get (0).getExemptionReasonText ());
    assertEquals ("VATEX-EU-IC", aModel.documentAllowances ().get (0).getExemptionReasonCode ());
    assertEquals ("SUPPLY-A", aModel.documentAllowances ().get (0).getGoodsServicesCode ());
    // BT-175, BT-176, BT-177 + BT-177-1, BT-214 — BT-177 replaces BT-105
    final PhloxDocumentCharge aCharge = aModel.documentCharges ().get (0);
    assertEquals ("Not subject to VAT", aCharge.getExemptionReasonText ());
    assertEquals ("VATEX-EU-O", aCharge.getExemptionReasonCode ());
    assertEquals ("ENV", aCharge.getNonVatTaxCode ());
    assertEquals ("5153", aCharge.getNonVatTaxCodeListID ());
    assertNull (aCharge.getReasonCode ());
    assertEquals ("SUPPLY-B", aCharge.getGoodsServicesCode ());
    // BT-210
    assertNull (aModel.vatBreakdowns ().get (0).getGoodsServicesCode ());
    assertEquals ("SUPPLY-C", aModel.vatBreakdowns ().get (1).getGoodsServicesCode ());
    // BT-184: only the second VAT breakdown is stated in another currency than BT-5
    assertNull (aModel.vatBreakdowns ().get (0).getCurrency ());
    assertEquals ("USD", aModel.vatBreakdowns ().get (1).getCurrency ());
  }

  @Test
  public void testLineDelivery25 ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-new-linedelivery-invoice-ubl.xml");
    final PhloxInvoiceLine aLine = aModel.lines ().get (0);
    // BG-37
    assertNotNull (aLine.getDelivery ());
    assertEquals ("Line Delivery Site", aLine.getDelivery ().getPartyName ());
    assertEquals ("4035813333333", aLine.getDelivery ().getLocationIdentifier ().getValue ());
    assertEquals ("0088", aLine.getDelivery ().getLocationIdentifier ().getSchemeID ());
    assertEquals (LocalDate.of (2026, 1, 13), aLine.getDelivery ().getActualDeliveryDate ());
    // BG-38
    assertEquals ("Line Delivery Road 1", aLine.getDelivery ().getPostalAddress ().getLine1 ());
    assertEquals ("Gate 5", aLine.getDelivery ().getPostalAddress ().getLine2 ());
    assertEquals ("Dock C", aLine.getDelivery ().getPostalAddress ().getLine3 ());
    assertEquals ("Innsbruck", aLine.getDelivery ().getPostalAddress ().getCity ());
    assertEquals ("6020", aLine.getDelivery ().getPostalAddress ().getPostCode ());
    assertEquals ("Tirol", aLine.getDelivery ().getPostalAddress ().getCountrySubdivision ());
    assertEquals ("AT", aLine.getDelivery ().getPostalAddress ().getCountryCode ());
    // BT-193 + BT-193-1 replace BT-145
    assertEquals ("ENV", aLine.charges ().get (0).getNonVatTaxTypeCode ());
    assertEquals ("5153", aLine.charges ().get (0).getNonVatTaxTypeCodeListID ());
    assertNull (aLine.charges ().get (0).getReasonCode ());
    // BT-211 + BT-161b + BT-212
    final PhloxItemAttribute aAttr = aLine.getItem ().attributes ().getLastOrNull ();
    assertEquals ("Humidity", aAttr.getName ());
    assertEquals ("AAO", aAttr.getCode ());
    assertEquals ("65", aAttr.getValue ());
    assertEquals ("P1", aAttr.getValueUnit ());
  }

  @Test
  public void testLineReferences25 ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-new-lineref-invoice-ubl.xml");
    final PhloxInvoiceLine aLine = aModel.lines ().get (0);
    // BT-132, BT-188, BT-200, BT-201
    assertEquals ("PO-LINE-5", aLine.getPurchaseOrderLineRef ());
    assertEquals ("LINE-PO-1", aLine.getPurchaseOrderReference ());
    assertEquals ("LINE-SO-1", aLine.getSalesOrderReference ());
    assertEquals ("SO-LINE-9", aLine.getSalesOrderLineReference ());
    // BT-189, BT-190
    assertEquals ("LINE-DESP-1", aLine.getDespatchAdviceReference ());
    assertEquals ("DESP-LINE-2", aLine.getDespatchAdviceLineReference ());
    // BT-191, BT-192
    assertEquals ("LINE-RECV-1", aLine.getReceivingAdviceReference ());
    assertEquals ("RECV-LINE-3", aLine.getReceivingAdviceLineReference ());
    // BT-198, BT-199
    assertEquals ("LINE-DELN-1", aLine.getDeliveryNoteReference ());
    assertEquals ("DELN-LINE-4", aLine.getDeliveryNoteLineReference ());
    // BG-39
    assertEquals (1, aLine.precedingInvoiceRefs ().size ());
    assertEquals ("LINE-PREV-INV-1", aLine.precedingInvoiceRefs ().get (0).getNumber ());
    assertEquals (LocalDate.of (2025, 12, 10), aLine.precedingInvoiceRefs ().get (0).getIssueDate ());
    assertEquals ("380", aLine.precedingInvoiceRefs ().get (0).getTypeCode ());
    assertEquals ("PREV-LINE-7", aLine.precedingInvoiceRefs ().get (0).getLineReference ());
    // no BG-37 without delivery terms
    assertNull (aLine.getDelivery ());
  }

  @Test
  public void testVatAccountingAmountOnlyIfCurrencyDiffers ()
  {
    // BT-6 equals BT-5: the VAT amount is BT-110 only, there is no BT-111
    final PhloxInvoice aSame = _read (UblInvoiceReader.fromUbl25 (),
                                      BASE + "d25a-edge-tax-currency-same-as-document-currency-invoice-ubl.xml");
    assertEquals ("EUR", aSame.getVatAccountingCurrency ());
    assertNotNull (aSame.getDocumentTotals ().getTaxTotal ());
    assertNull (aSame.getDocumentTotals ().getTaxTotalAccountingCurrency ());

    // BT-6 differs from BT-5: both amounts are set
    final PhloxInvoice aDiffers = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-edge-payment-invoice-ubl.xml");
    assertEquals ("USD", aDiffers.getVatAccountingCurrency ());
    assertNotNull (aDiffers.getDocumentTotals ().getTaxTotal ());
    assertNotNull (aDiffers.getDocumentTotals ().getTaxTotalAccountingCurrency ());
  }

  @Test
  public void testDirectDebitWithoutMandateReference ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), BASE + "d25a-edge-directdebit-invoice-ubl.xml");
    final PhloxDirectDebit aDD = aModel.getPaymentInstructions ().getDirectDebit ();
    assertNotNull (aDD);
    assertNull (aDD.getMandateReferenceID ());
    assertEquals ("SEPA-CRED-1", aDD.getBankAssignedCreditorID ());
    assertEquals ("DEBTOR-12345", aDD.getDebitedAccountID ());
    assertEquals ("Buyer Account", aDD.getDebitedAccountName ());

    // The SEPA creditor identifier is BT-90 and not one of the seller identifiers BT-29
    for (final PhloxIdentifier aID : aModel.getSeller ().identifiers ())
      assertFalse ("SEPA".equals (aID.getSchemeID ()));
  }

  @Test
  public void testSupportingDocumentWithoutDescription ()
  {
    // Issue #5: BG-24 with an attachment but without BT-123
    final PhloxInvoice aModel21 = _readWithoutDocumentDescription (UblInvoiceReader.fromUbl21 (),
                                                                   MockPhloxTestFiles.BASE_PATH +
                                                                                                  "2017/ubl/invoice/ubl-tc434-example2.xml");
    assertEquals (2, aModel21.supportingDocuments ().size ());
    for (final PhloxSupportingDocument aDoc : aModel21.supportingDocuments ())
      assertNull (aDoc.getDescription ());
    assertEquals ("http://www.suppliersite.eu/sheet001.html",
                  aModel21.supportingDocuments ().get (0).getExternalLocation ());
    assertNotNull (aModel21.supportingDocuments ().get (1).getAttachment ());
    assertEquals ("test.pdf", aModel21.supportingDocuments ().get (1).getAttachment ().getFilename ());

    final PhloxInvoice aModel25 = _readWithoutDocumentDescription (UblInvoiceReader.fromUbl25 (),
                                                                   BASE + "d25a-full-invoice-ubl.xml");
    assertEquals (1, aModel25.supportingDocuments ().size ());
    assertNull (aModel25.supportingDocuments ().get (0).getDescription ());
    assertEquals ("https://example.org/doc", aModel25.supportingDocuments ().get (0).getExternalLocation ());
  }
}
