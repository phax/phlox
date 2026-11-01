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
package com.helger.phlox.api.model;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * BG-16 PAYMENT INSTRUCTIONS (0..1) plus the nested BG-17 (credit transfers), BG-18 (payment
 * card) and BG-19 (direct debit) groups. Carries BT-81 (payment means type code), BT-82
 * (payment means text) and BT-83 (remittance information).
 *
 * @author Philip Helger
 */
public class PhloxPaymentInstructions
{
  private String m_sPaymentMeansCode;
  private String m_sPaymentMeansText;
  private String m_sRemittanceInformation;
  private final ICommonsList <PhloxCreditTransfer> m_aCreditTransfers = new CommonsArrayList <> ();
  private PhloxPaymentCard m_aPaymentCard;
  private PhloxDirectDebit m_aDirectDebit;
  private final ICommonsList <PhloxAdditionalTerm> m_aAdditionalTerms = new CommonsArrayList <> ();

  /** Create an empty payment instructions record. */
  public PhloxPaymentInstructions ()
  {}

  /**
   * Get the mutable list of the values of the payment, among them all payment means after the first one, that the model has no place for.
   *
   * @return Mutable list of the additional terms. Never {@code null}; empty when there are none.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxAdditionalTerm> additionalTerms ()
  {
    return m_aAdditionalTerms;
  }

  /**
   * Get the payment means type code (BT-81) — UNTDID 4461. May be {@code null}.
   *
   * @return Payment means type code (BT-81) — UNTDID 4461. May be {@code null}.
   */
  @Nullable
  public String getPaymentMeansCode ()
  {
    return m_sPaymentMeansCode;
  }

  /**
   * Set the payment means type code (BT-81).
   *
   * @param sPaymentMeansCode
   *        UNTDID 4461 code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxPaymentInstructions setPaymentMeansCode (@Nullable final String sPaymentMeansCode)
  {
    m_sPaymentMeansCode = sPaymentMeansCode;
    return this;
  }

  /**
   * Get the payment means text (BT-82).
   *
   * @return Payment means text (BT-82). May be {@code null}.
   */
  @Nullable
  public String getPaymentMeansText ()
  {
    return m_sPaymentMeansText;
  }

  /**
   * Set the payment means text (BT-82).
   *
   * @param sPaymentMeansText
   *        Free-form text; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxPaymentInstructions setPaymentMeansText (@Nullable final String sPaymentMeansText)
  {
    m_sPaymentMeansText = sPaymentMeansText;
    return this;
  }

  /**
   * Get the remittance information (BT-83).
   *
   * @return Remittance information (BT-83). May be {@code null}.
   */
  @Nullable
  public String getRemittanceInformation ()
  {
    return m_sRemittanceInformation;
  }

  /**
   * Set the remittance information (BT-83).
   *
   * @param sRemittanceInformation
   *        Remittance text; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxPaymentInstructions setRemittanceInformation (@Nullable final String sRemittanceInformation)
  {
    m_sRemittanceInformation = sRemittanceInformation;
    return this;
  }

  /**
   * Get the mutable list of credit transfers (BG-17).
   *
   * @return Mutable list of credit transfers (BG-17). Never {@code null}; empty when none are
   *         present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxCreditTransfer> creditTransfers ()
  {
    return m_aCreditTransfers;
  }

  /**
   * Get the payment card information (BG-18).
   *
   * @return Payment card information (BG-18). May be {@code null}.
   */
  @Nullable
  public PhloxPaymentCard getPaymentCard ()
  {
    return m_aPaymentCard;
  }

  /**
   * Set the payment card information (BG-18).
   *
   * @param aPaymentCard
   *        Card details; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxPaymentInstructions setPaymentCard (@Nullable final PhloxPaymentCard aPaymentCard)
  {
    m_aPaymentCard = aPaymentCard;
    return this;
  }

  /**
   * Get the direct debit information (BG-19).
   *
   * @return Direct debit information (BG-19). May be {@code null}.
   */
  @Nullable
  public PhloxDirectDebit getDirectDebit ()
  {
    return m_aDirectDebit;
  }

  /**
   * Set the direct debit information (BG-19).
   *
   * @param aDirectDebit
   *        Direct debit details; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxPaymentInstructions setDirectDebit (@Nullable final PhloxDirectDebit aDirectDebit)
  {
    m_aDirectDebit = aDirectDebit;
    return this;
  }
}
