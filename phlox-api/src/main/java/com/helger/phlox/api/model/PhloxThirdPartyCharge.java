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
 * BG-34 CHARGES ON BEHALF OF A THIRD PARTY (0..n within BG-22). Charges, e.g. statutory fees,
 * that flow through the invoice but are not part of the seller's own revenue: the collected
 * amount (BT-179) with the line identifier of UBL (BT-179-1) and the specification of the charge
 * (BT-180). Introduced by EN 16931:2026.
 *
 * @author Philip Helger
 */
public class PhloxThirdPartyCharge
{
  private String m_sLineID;
  private BigDecimal m_aAmount;
  private String m_sSpecification;

  /** Create an empty instance. */
  public PhloxThirdPartyCharge ()
  {}

  /**
   * Get the line identifier (BT-179-1). Only UBL has it.
   *
   * @return Line identifier (BT-179-1). May be {@code null}.
   */
  @Nullable
  public String getLineID ()
  {
    return m_sLineID;
  }

  /**
   * Set the line identifier (BT-179-1).
   *
   * @param sLineID
   *        The line identifier; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxThirdPartyCharge setLineID (@Nullable final String sLineID)
  {
    m_sLineID = sLineID;
    return this;
  }

  /**
   * Get the charge amount collected on behalf of a third party (BT-179).
   *
   * @return Charge amount collected on behalf of a third party (BT-179). May be {@code null}.
   */
  @Nullable
  public BigDecimal getAmount ()
  {
    return m_aAmount;
  }

  /**
   * Set the charge amount collected on behalf of a third party (BT-179).
   *
   * @param aAmount
   *        Charge amount collected on behalf of a third party; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxThirdPartyCharge setAmount (@Nullable final BigDecimal aAmount)
  {
    m_aAmount = aAmount;
    return this;
  }

  /**
   * Get the charges specification (BT-180).
   *
   * @return Charges specification (BT-180). May be {@code null}.
   */
  @Nullable
  public String getSpecification ()
  {
    return m_sSpecification;
  }

  /**
   * Set the charges specification (BT-180).
   *
   * @param sSpecification
   *        Charges specification; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxThirdPartyCharge setSpecification (@Nullable final String sSpecification)
  {
    m_sSpecification = sSpecification;
    return this;
  }
}
