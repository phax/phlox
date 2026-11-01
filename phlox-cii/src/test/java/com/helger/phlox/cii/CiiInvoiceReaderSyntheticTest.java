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
package com.helger.phlox.cii;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.junit.Test;

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringReplace;
import com.helger.cache.regex.RegExHelper;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxDirectDebit;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxPaymentInstructions;
import com.helger.phlox.testfiles.MockPhloxTestFiles;

/**
 * Test class for class {@link CiiInvoiceReader} with the edge cases of the synthetic reader files.
 * Every test covers both CII versions, so that the two readers cannot drift apart.
 *
 * @author Philip Helger
 */
public final class CiiInvoiceReaderSyntheticTest
{
  private static final String D16B = "cii/phlox-reader-d16b.xml";
  private static final String D25A = "cii/phlox-reader-d25a.xml";
  private static final String [] ALL_FILES = { D16B, D25A };
  private static final String CREDITOR_ID = "DE98ZZZ09999999999";
  private static final String PAYER_ACCOUNT = "<ram:PayerPartyDebtorFinancialAccount>\n          <ram:ProprietaryID>DEBTOR-PROP</ram:ProprietaryID>\n        </ram:PayerPartyDebtorFinancialAccount>\n";

  @NonNull
  private static String _load (@NonNull final String sFile)
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH +
                                                        "synthetic/" +
                                                        sFile).getInputStream ())
    {
      return new String (aIS.readAllBytes (), StandardCharsets.UTF_8);
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sFile, ex);
    }
  }

  @NonNull
  private static String _load (@NonNull final String sFile, @NonNull final String sSearch, @NonNull final String sReplace)
  {
    return StringReplace.replaceAll (_load (sFile), sSearch, sReplace);
  }

  @NonNull
  private static CiiInvoiceReader _readerOf (@NonNull final String sFile)
  {
    return sFile.equals (D16B) ? CiiInvoiceReader.fromD16B () : CiiInvoiceReader.fromD25A ();
  }

  @Nullable
  private static PhloxInvoice _readXml (@NonNull final String sFile,
                                        @NonNull final String sXml,
                                        @NonNull final ErrorList aErrors)
  {
    return _readerOf (sFile).read (new NonBlockingByteArrayInputStream (sXml.getBytes (StandardCharsets.UTF_8)),
                                   aErrors);
  }

  @NonNull
  private static PhloxInvoice _read (@NonNull final String sFile, @NonNull final String sXml)
  {
    final ErrorList aErrors = new ErrorList ();
    final PhloxInvoice ret = _readXml (sFile, sXml, aErrors);
    assertNotNull (sFile + ": " + aErrors, ret);
    assertFalse (sFile + ": " + aErrors, aErrors.containsAtLeastOneError ());
    return ret;
  }

  @NonNull
  private static PhloxInvoice _read (@NonNull final String sFile)
  {
    return _read (sFile, _load (sFile));
  }

  @Test
  public void testKindFromTheCodeList ()
  {
    for (final String sFile : ALL_FILES)
    {
      // Both code 503 and 502 are credit notes in EN16931CodeLists
      assertTrue (sFile, _read (sFile).isCreditNote ());
      assertTrue (sFile, _read (sFile, _load (sFile, "TypeCode>503<", "TypeCode>502<")).isCreditNote ());
      assertTrue (sFile, _read (sFile, _load (sFile, "TypeCode>503<", "TypeCode>380<")).isInvoice ());

      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aUnknown = _readXml (sFile, _load (sFile, "TypeCode>503<", "TypeCode>999<"), aErrors);
      assertNotNull (sFile, aUnknown);
      assertTrue (sFile, aUnknown.isInvoice ());
      assertEquals (sFile, 1, aErrors.getCount (x -> x.getErrorText (Locale.ROOT).startsWith ("Unknown BT-3")));
    }
  }

  @Test
  public void testVatPointDateCode ()
  {
    for (final String sFile : ALL_FILES)
    {
      // UNTDID 2475 in CII, the model has the code of the UBL binding
      assertEquals (sFile, "3", _read (sFile).getVatPointDateCode ());
      assertEquals (sFile,
                    "35",
                    _read (sFile, _load (sFile, "DueDateTypeCode>5<", "DueDateTypeCode>29<")).getVatPointDateCode ());
      assertEquals (sFile,
                    "432",
                    _read (sFile, _load (sFile, "DueDateTypeCode>5<", "DueDateTypeCode>72<")).getVatPointDateCode ());
    }
  }

  @Test
  public void testCardInSecondPaymentMeans ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxPaymentInstructions aPI = _read (sFile).getPaymentInstructions ();
      assertEquals (sFile, "30", aPI.getPaymentMeansCode ());
      assertNotNull (sFile, aPI.getPaymentCard ());
      assertEquals (sFile, "1234", aPI.getPaymentCard ().getPan ());
      assertEquals (sFile, "Card Holder", aPI.getPaymentCard ().getHolderName ());
    }
  }

  @Test
  public void testContactDepartmentName ()
  {
    for (final String sFile : ALL_FILES)
      assertEquals (sFile, "Sales department", _read (sFile).getSeller ().getContact ().getName ());
  }

  @Test
  public void testDirectDebitWithoutMandate ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxDirectDebit aDD = _read (sFile).getPaymentInstructions ().getDirectDebit ();
      assertNotNull (sFile, aDD);
      assertNull (sFile, aDD.getMandateReferenceID ());
      // BT-91 as ram:ProprietaryID
      assertEquals (sFile, "DEBTOR-PROP", aDD.getDebitedAccountID ());
      assertEquals (sFile, CREDITOR_ID, aDD.getBankAssignedCreditorID ());
    }
  }

  @Test
  public void testCreditorIDWithoutMandateAndAccount ()
  {
    for (final String sFile : ALL_FILES)
    {
      final String sXml = _load (sFile, PAYER_ACCOUNT, "");
      assertFalse (sFile, sXml.contains ("DEBTOR-PROP"));

      // Code 59 is in the second payment means
      final PhloxDirectDebit aDD = _read (sFile, sXml).getPaymentInstructions ().getDirectDebit ();
      assertNotNull (sFile, aDD);
      assertNull (sFile, aDD.getDebitedAccountID ());
      assertEquals (sFile, CREDITOR_ID, aDD.getBankAssignedCreditorID ());

      // Same rule as in UBL: the other means are no direct debit
      assertNull (sFile,
                  _read (sFile, StringReplace.replaceAll (sXml, "TypeCode>59<", "TypeCode>58<")).getPaymentInstructions ().getDirectDebit ());
    }
  }

  @Test
  public void testRemittanceWithoutPaymentMeans ()
  {
    for (final String sFile : ALL_FILES)
    {
      final String sXml = RegExHelper.stringReplacePattern ("(?s)<ram:SpecifiedTradeSettlementPaymentMeans>.*</ram:SpecifiedTradeSettlementPaymentMeans>",
                                                            _load (sFile),
                                                            "");
      assertFalse (sFile, sXml.contains ("SpecifiedTradeSettlementPaymentMeans"));

      final PhloxPaymentInstructions aPI = _read (sFile, sXml).getPaymentInstructions ();
      assertNotNull (sFile, aPI);
      assertNull (sFile, aPI.getPaymentMeansCode ());
      assertEquals (sFile, "Remittance one\nRemittance two", aPI.getRemittanceInformation ());
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
  public void testSupportingDocumentWithOtherTypeCode ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, 2, aModel.supportingDocuments ().size ());
      assertEquals (sFile, "SUPPORT-OTHER-CODE", aModel.supportingDocuments ().get (0).getReference ());
      assertEquals (sFile, "SUPPORT-916", aModel.supportingDocuments ().get (1).getReference ());
    }
  }

  @Test
  public void testBreakdownCurrency ()
  {
    final PhloxInvoice aModel = _read (D25A);
    // BT-184 only if it differs from BT-5, like in UBL
    assertNull (aModel.vatBreakdowns ().get (0).getCurrency ());
    assertEquals ("USD", aModel.vatBreakdowns ().get (1).getCurrency ());
  }

  @Test
  public void testDateFailuresAreInTheErrorList ()
  {
    for (final String sFile : ALL_FILES)
    {
      final ErrorList aErrors = new ErrorList ();
      final PhloxInvoice aModel = _readXml (sFile, _load (sFile), aErrors);
      assertNotNull (sFile, aModel);
      assertEquals (sFile + ": " + aErrors, 2, aErrors.size ());
      assertFalse (sFile, aErrors.containsAtLeastOneError ());
      assertEquals (sFile,
                    "Could not parse CII date 'notadate' with format code '102'",
                    aErrors.get (0).getErrorText (Locale.ROOT));
      assertEquals (sFile,
                    "Unsupported CII date format code '610' on value '202601'",
                    aErrors.get (1).getErrorText (Locale.ROOT));
      assertNull (sFile, aModel.lines ().get (0).getPeriod ());
    }
  }

  @Test
  public void testDateValueInMessageIsSanitised ()
  {
    for (final String sFile : ALL_FILES)
    {
      final ErrorList aErrors = new ErrorList ();
      _readXml (sFile, _load (sFile, ">notadate<", ">not&#10;a&#13;date<"), aErrors);
      assertEquals (sFile,
                    "Could not parse CII date 'not_a_date' with format code '102'",
                    aErrors.get (0).getErrorText (Locale.ROOT));
    }
  }

  @Test
  public void testFormat208 ()
  {
    for (final String sFile : ALL_FILES)
      assertEquals (sFile, LocalDate.of (2026, 2, 15), _read (sFile).getPaymentDueDate ());
  }

  @Test
  public void testLegacyBuyerReferenceWithForcedEdition ()
  {
    final PhloxInvoice aModel = _read (D25A);
    assertEquals (1, aModel.buyerReferences ().size ());
    assertEquals ("LEGACY-BUYER-REF", aModel.buyerReferences ().get (0).getValue ());

    // The element of the binding has priority
    final PhloxInvoice aModel2 = _read (D25A,
                                        _load (D25A, "</ram:BuyerReference>",
                                                              "</ram:BuyerReference>\n      <ram:BuyerReferenceID>NEW-REF</ram:BuyerReferenceID>"));
    assertEquals (1, aModel2.buyerReferences ().size ());
    assertEquals ("NEW-REF", aModel2.buyerReferences ().get (0).getValue ());
  }

  @Test
  public void testXsdErrorsAreInTheErrorList ()
  {
    for (final String sFile : ALL_FILES)
    {
      final ErrorList aErrors = new ErrorList ();
      final String sXml = _load (sFile, "<ram:InvoiceCurrencyCode>", "<ram:Nonsense>x</ram:Nonsense><ram:InvoiceCurrencyCode>");
      assertNull (sFile, _readXml (sFile, sXml, aErrors));
      assertEquals (sFile + ": " + aErrors, 2, aErrors.size ());
      assertTrue (aErrors.toString (), aErrors.get (0).getErrorText (Locale.ROOT).contains ("cvc-complex-type"));
      assertTrue (aErrors.toString (), aErrors.get (1).getErrorText (Locale.ROOT).startsWith ("Source could not be parsed"));
    }
  }

  @Test
  public void testTrimming ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, "EUR", aModel.getCurrency ());
      assertEquals (sFile, "SELLER-ID", aModel.getSeller ().identifiers ().get (0).getValue ());
      final PhloxInvoiceLine aLine = aModel.lines ().get (0);
      assertEquals (sFile, "C62", aLine.getQuantityUnit ());
      assertEquals (sFile, "SELLER-ITEM", aLine.getItem ().getSellerItemID ());
      assertEquals (sFile, "12345", aLine.getItem ().classifications ().get (0).getCode ());
      assertEquals (sFile, "STI", aLine.getItem ().classifications ().get (0).getListID ());
    }
  }

  @Test
  public void testNoEmptyRows ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, 1, aModel.getSeller ().identifiers ().size ());
      assertEquals (sFile, 1, aModel.lines ().get (0).getItem ().classifications ().size ());
    }

    final String sSummationEnd = "</ram:SpecifiedTradeSettlementHeaderMonetarySummation>";
    final PhloxInvoice aModel = _read (D25A,
                                       _load (D25A, sSummationEnd,
                                                             sSummationEnd + "\n      <ram:SpecifiedFinancialAdjustment />"));
    assertTrue (aModel.getDocumentTotals ().thirdPartyCharges ().isEmpty ());
  }

  @Test
  public void testFirstTaxRegistration ()
  {
    for (final String sFile : ALL_FILES)
      assertEquals (sFile, "TAX-FIRST", _read (sFile).getSeller ().getTaxRegistration ());
  }

  @Test
  public void testPayeeIdentifierPrefersGlobalID ()
  {
    for (final String sFile : ALL_FILES)
    {
      final PhloxInvoice aModel = _read (sFile);
      assertEquals (sFile, "PAYEE-GLOBAL-ID", aModel.getPayee ().getIdentifier ().getValue ());
      assertEquals (sFile, "0088", aModel.getPayee ().getIdentifier ().getSchemeID ());
    }
  }

  @Test
  public void testFirstLineObject ()
  {
    for (final String sFile : ALL_FILES)
      assertEquals (sFile, "LINE-OBJECT-1", _read (sFile).lines ().get (0).getObjectIdentifier ().getValue ());
  }
}
