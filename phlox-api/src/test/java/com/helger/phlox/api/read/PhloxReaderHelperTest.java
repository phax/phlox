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
package com.helger.phlox.api.read;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Locale;

import org.junit.Test;

import com.helger.base.wrapper.Wrapper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.codelist.EEN16931InvoiceTypeCode;
import com.helger.en16931.basics.codelist.EEN16931InvoiceTypeCodeRole;
import com.helger.phlox.api.model.EPhloxDocumentKind;
import com.helger.phlox.api.model.PhloxAdditionalTerm;
import com.helger.phlox.api.model.PhloxDirectDebit;
import com.helger.phlox.api.model.PhloxPaymentInstructions;

/**
 * Test class for class {@link PhloxReaderHelper}.
 *
 * @author Philip Helger
 */
public final class PhloxReaderHelperTest
{
  private static final String [] CREDIT_NOTE_CODES = { "81",
                                                       "83",
                                                       "261",
                                                       "262",
                                                       "296",
                                                       "308",
                                                       "381",
                                                       "396",
                                                       "420",
                                                       "458",
                                                       "502",
                                                       "503",
                                                       "532" };

  @Test
  public void testAllCodes ()
  {
    final ErrorList aErrors = new ErrorList ();
    int nCreditNotes = 0;
    for (final EEN16931InvoiceTypeCode eCode : EEN16931InvoiceTypeCode.values ())
    {
      final boolean bCreditNote = eCode.getRole () == EEN16931InvoiceTypeCodeRole.CREDIT_NOTE;
      if (bCreditNote)
        nCreditNotes++;

      // The default must have no influence for a known code
      for (final EPhloxDocumentKind eDefault : EPhloxDocumentKind.values ())
        assertTrue (eCode.getID (),
                    bCreditNote == PhloxReaderHelper.getDocumentKind (eCode.getID (), eDefault, aErrors)
                                                    .isCreditNote ());
    }
    assertEquals (CREDIT_NOTE_CODES.length, nCreditNotes);
    for (final String sCode : CREDIT_NOTE_CODES)
      assertSame (sCode,
                  EPhloxDocumentKind.CREDIT_NOTE,
                  PhloxReaderHelper.getDocumentKind (sCode, EPhloxDocumentKind.INVOICE, aErrors));
    assertTrue (aErrors.isEmpty ());
  }

  @Test
  public void testMissingCode ()
  {
    final ErrorList aErrors = new ErrorList ();
    assertSame (EPhloxDocumentKind.CREDIT_NOTE,
                PhloxReaderHelper.getDocumentKind (null, EPhloxDocumentKind.CREDIT_NOTE, aErrors));
    assertSame (EPhloxDocumentKind.INVOICE,
                PhloxReaderHelper.getDocumentKind ("", EPhloxDocumentKind.INVOICE, aErrors));
    assertTrue (aErrors.isEmpty ());
  }

  @Test
  public void testUnknownCode ()
  {
    final ErrorList aErrors = new ErrorList ();
    assertSame (EPhloxDocumentKind.CREDIT_NOTE,
                PhloxReaderHelper.getDocumentKind ("99\n9", EPhloxDocumentKind.CREDIT_NOTE, aErrors));
    assertEquals (1, aErrors.size ());
    assertFalse (aErrors.containsAtLeastOneError ());
    assertEquals ("Unknown BT-3 invoice type code '99_9' — defaulting to CREDIT_NOTE",
                  aErrors.getFirstOrNull ().getErrorText (Locale.ROOT));
  }

  @Test
  public void testSetAndAddRepeated ()
  {
    final Wrapper <String> aFirst = new Wrapper <> ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = new CommonsArrayList <> ();
    PhloxReaderHelper.setAndAddRepeated (Arrays.asList (null, "a", null, "b", "c"), "BT-12", aFirst::set, aTerms);
    assertEquals ("a", aFirst.get ());
    assertEquals (2, aTerms.size ());
    assertEquals ("BT-12", aTerms.get (0).getTerm ());
    assertEquals ("b", aTerms.get (0).getValue ());
    assertEquals ("c", aTerms.get (1).getValue ());
  }

  @Test
  public void testAddCreditorIDs ()
  {
    // On a credit transfer invoice BT-90 alone is no direct debit
    final PhloxPaymentInstructions aCreditTransfer = new PhloxPaymentInstructions ();
    PhloxReaderHelper.addCreditorIDs (Arrays.asList ("CRED-1"), false, aCreditTransfer);
    assertNull (aCreditTransfer.getDirectDebit ());
    assertEquals ("BT-90", aCreditTransfer.additionalTerms ().getFirstOrNull ().getTerm ());
    assertEquals ("CRED-1", aCreditTransfer.additionalTerms ().getFirstOrNull ().getValue ());

    final PhloxPaymentInstructions aMeans = new PhloxPaymentInstructions ();
    PhloxReaderHelper.addCreditorIDs (Arrays.asList ("CRED-1", "CRED-2"), true, aMeans);
    assertNotNull (aMeans.getDirectDebit ());
    assertEquals ("CRED-1", aMeans.getDirectDebit ().getBankAssignedCreditorID ());
    assertEquals ("CRED-2", aMeans.additionalTerms ().getFirstOrNull ().getValue ());

    final PhloxPaymentInstructions aMandate = new PhloxPaymentInstructions ();
    aMandate.setDirectDebit (new PhloxDirectDebit ().setMandateReferenceID ("M"));
    PhloxReaderHelper.addCreditorIDs (Arrays.asList ((String) null), false, aMandate);
    assertNull (aMandate.getDirectDebit ().getBankAssignedCreditorID ());
    assertTrue (aMandate.additionalTerms ().isEmpty ());
  }
}
