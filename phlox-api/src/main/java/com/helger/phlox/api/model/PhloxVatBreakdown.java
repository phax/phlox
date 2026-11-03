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

import java.math.BigDecimal;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * BG-23 VAT BREAKDOWN (1..n). Carries BT-116 (taxable amount), BT-117 (tax amount, optional since
 * EN 16931:2026 to support margin schemes), BT-118 (VAT category code), BT-119 (VAT rate), BT-120
 * (exemption reason text), BT-121 (exemption reason and specification code) and, since EN
 * 16931:2026, BT-184 (VAT breakdown currency) and BT-210 (goods/services code). BT-116-1 (taxable
 * amount currency) is only set if it differs from the currency of the breakdown.
 *
 * @author Philip Helger
 */
public class PhloxVatBreakdown
{
  private BigDecimal m_aTaxableAmount;
  private BigDecimal m_aTaxAmount;
  private String m_sCategoryCode;
  private BigDecimal m_aRate;
  private String m_sExemptionReason;
  private String m_sExemptionReasonCode;
  private String m_sCurrency;
  private String m_sTaxableAmountCurrency;
  private String m_sGoodsServicesCode;

  /** Create an empty VAT breakdown. */
  public PhloxVatBreakdown ()
  {}

  /**
   * Get the VAT category taxable amount (BT-116).
   *
   * @return VAT category taxable amount (BT-116). May be {@code null}.
   */
  @Nullable
  public BigDecimal getTaxableAmount ()
  {
    return m_aTaxableAmount;
  }

  /**
   * Set the VAT category taxable amount (BT-116).
   *
   * @param aTaxableAmount
   *        Amount; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setTaxableAmount (@Nullable final BigDecimal aTaxableAmount)
  {
    m_aTaxableAmount = aTaxableAmount;
    return this;
  }

  /**
   * Get the VAT category tax amount (BT-117).
   *
   * @return VAT category tax amount (BT-117). May be {@code null}.
   */
  @Nullable
  public BigDecimal getTaxAmount ()
  {
    return m_aTaxAmount;
  }

  /**
   * Set the VAT category tax amount (BT-117).
   *
   * @param aTaxAmount
   *        Amount; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setTaxAmount (@Nullable final BigDecimal aTaxAmount)
  {
    m_aTaxAmount = aTaxAmount;
    return this;
  }

  /**
   * Get the VAT category code (BT-118) — UNTDID 5305. May be {@code null}.
   *
   * @return VAT category code (BT-118) — UNTDID 5305. May be {@code null}.
   */
  @Nullable
  public String getCategoryCode ()
  {
    return m_sCategoryCode;
  }

  /**
   * Set the VAT category code (BT-118).
   *
   * @param sCategoryCode
   *        UNTDID 5305 code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setCategoryCode (@Nullable final String sCategoryCode)
  {
    m_sCategoryCode = sCategoryCode;
    return this;
  }

  /**
   * Get the VAT category rate (BT-119).
   *
   * @return VAT category rate (BT-119). May be {@code null}.
   */
  @Nullable
  public BigDecimal getRate ()
  {
    return m_aRate;
  }

  /**
   * Set the VAT category rate (BT-119).
   *
   * @param aRate
   *        Rate as a {@link BigDecimal} (e.g. {@code 19} for 19 %); may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setRate (@Nullable final BigDecimal aRate)
  {
    m_aRate = aRate;
    return this;
  }

  /**
   * Get the VAT exemption reason text (BT-120).
   *
   * @return VAT exemption reason text (BT-120). May be {@code null}.
   */
  @Nullable
  public String getExemptionReason ()
  {
    return m_sExemptionReason;
  }

  /**
   * Set the VAT exemption reason text (BT-120).
   *
   * @param sExemptionReason
   *        Reason text; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setExemptionReason (@Nullable final String sExemptionReason)
  {
    m_sExemptionReason = sExemptionReason;
    return this;
  }

  /**
   * Get the VAT exemption reason code (BT-121).
   *
   * @return VAT exemption reason code (BT-121). May be {@code null}.
   */
  @Nullable
  public String getExemptionReasonCode ()
  {
    return m_sExemptionReasonCode;
  }

  /**
   * Set the VAT exemption reason code (BT-121).
   *
   * @param sExemptionReasonCode
   *        Reason code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setExemptionReasonCode (@Nullable final String sExemptionReasonCode)
  {
    m_sExemptionReasonCode = sExemptionReasonCode;
    return this;
  }

  /**
   * Get the VAT breakdown currency (BT-184).
   *
   * @return VAT breakdown currency (BT-184). May be {@code null}.
   */
  @Nullable
  public String getCurrency ()
  {
    return m_sCurrency;
  }

  /**
   * Set the VAT breakdown currency (BT-184).
   *
   * @param sCurrency
   *        VAT breakdown currency; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setCurrency (@Nullable final String sCurrency)
  {
    m_sCurrency = sCurrency;
    return this;
  }

  /**
   * Get the VAT category taxable amount currency (BT-116-1). It is only set if it differs from the
   * currency of the breakdown, which is BT-184 or, without it, the invoice currency BT-5.
   *
   * @return VAT category taxable amount currency (BT-116-1). May be {@code null}.
   */
  @Nullable
  public String getTaxableAmountCurrency ()
  {
    return m_sTaxableAmountCurrency;
  }

  /**
   * Set the VAT category taxable amount currency (BT-116-1).
   *
   * @param sTaxableAmountCurrency
   *        VAT category taxable amount currency; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setTaxableAmountCurrency (@Nullable final String sTaxableAmountCurrency)
  {
    m_sTaxableAmountCurrency = sTaxableAmountCurrency;
    return this;
  }

  /**
   * Get the VAT breakdown goods/services code (BT-210).
   *
   * @return VAT breakdown goods/services code (BT-210). May be {@code null}.
   */
  @Nullable
  public String getGoodsServicesCode ()
  {
    return m_sGoodsServicesCode;
  }

  /**
   * Set the VAT breakdown goods/services code (BT-210).
   *
   * @param sGoodsServicesCode
   *        VAT breakdown goods/services code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxVatBreakdown setGoodsServicesCode (@Nullable final String sGoodsServicesCode)
  {
    m_sGoodsServicesCode = sGoodsServicesCode;
    return this;
  }
}
