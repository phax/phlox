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
import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.base.enforce.ValueEnforcer;

/**
 * A value that the model has no place for, but that must not get lost: the second and every later
 * occurrence of an element that EN 16931 has only once, a date that cannot be read, or an element
 * of the syntax without a business term of the same party. It is shown with the label of the term.
 * <p>
 * An amount is a {@link BigDecimal} in the invoice currency (BT-5). Any other value is a
 * {@link String}, a {@link LocalDate}, a {@link PhloxIdentifier} or a {@link PhloxPostalAddress}.
 *
 * @author Philip Helger
 */
public class PhloxAdditionalTerm
{
  private final String m_sTerm;
  private final Object m_aValue;
  private final String m_sCodeList;

  /**
   * Create a new term with a value that is not a code.
   *
   * @param sTerm
   *        The key of the label, usually the BT, e.g. {@code BT-12}. Neither {@code null} nor empty.
   * @param aValue
   *        The value. Never {@code null}.
   */
  public PhloxAdditionalTerm (@NonNull @Nonempty final String sTerm, @NonNull final Object aValue)
  {
    this (sTerm, aValue, null);
  }

  /**
   * Create a new term.
   *
   * @param sTerm
   *        The key of the label, usually the BT, e.g. {@code BT-81}. Neither {@code null} nor empty.
   * @param aValue
   *        The value. Never {@code null}.
   * @param sCodeList
   *        The name of the code list that names the value, e.g. {@code paymentmeans}. May be
   *        {@code null}.
   */
  public PhloxAdditionalTerm (@NonNull @Nonempty final String sTerm,
                              @NonNull final Object aValue,
                              @Nullable final String sCodeList)
  {
    ValueEnforcer.notEmpty (sTerm, "Term");
    ValueEnforcer.notNull (aValue, "Value");
    m_sTerm = sTerm;
    m_aValue = aValue;
    m_sCodeList = sCodeList;
  }

  /**
   * Get the key of the label.
   *
   * @return The key of the label, usually the BT. Neither {@code null} nor empty.
   */
  @NonNull
  @Nonempty
  public String getTerm ()
  {
    return m_sTerm;
  }

  /**
   * Get the value.
   *
   * @return The value. Never {@code null}.
   */
  @NonNull
  public Object getValue ()
  {
    return m_aValue;
  }

  /**
   * Get the name of the code list.
   *
   * @return The name of the code list that names the value, or {@code null} if it is no code.
   */
  @Nullable
  public String getCodeList ()
  {
    return m_sCodeList;
  }
}
