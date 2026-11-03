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
package com.helger.phlox.api;

import java.time.format.FormatStyle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.clone.ICloneable;

/**
 * Optional fine-tuning of locale-driven formatting. Held by {@link AbstractRenderContext} and shared
 * across writers.
 *
 * @author Philip Helger
 */
public class PhloxFormatOptions implements ICloneable <PhloxFormatOptions>
{
  /** Default style of formatted dates. */
  public static final FormatStyle DEFAULT_DATE_STYLE = FormatStyle.LONG;
  /** Default number of fraction digits of percentages. */
  public static final int DEFAULT_PERCENT_MIN_FRACTION_DIGITS = 2;
  /** Default representation of negative amounts. */
  public static final ENegativeStyle DEFAULT_NEGATIVE_STYLE = ENegativeStyle.MINUS_SIGN;

  private FormatStyle m_eDateStyle = DEFAULT_DATE_STYLE;
  private Integer m_aAmountMinFractionDigits;
  private int m_nPercentMinFractionDigits = DEFAULT_PERCENT_MIN_FRACTION_DIGITS;
  private ENegativeStyle m_eNegativeStyle = DEFAULT_NEGATIVE_STYLE;

  /** Create a new instance with default options. */
  public PhloxFormatOptions ()
  {}

  /**
   * Copy constructor.
   *
   * @param aOther
   *        The options to copy. Never {@code null}.
   */
  public PhloxFormatOptions (@NonNull final PhloxFormatOptions aOther)
  {
    m_eDateStyle = aOther.m_eDateStyle;
    m_aAmountMinFractionDigits = aOther.m_aAmountMinFractionDigits;
    m_nPercentMinFractionDigits = aOther.m_nPercentMinFractionDigits;
    m_eNegativeStyle = aOther.m_eNegativeStyle;
  }

  /**
   * Get the new instance with all options at their defaults.
   *
   * @return A new instance with all options at their defaults.
   */
  @NonNull
  public static PhloxFormatOptions defaults ()
  {
    return new PhloxFormatOptions ();
  }

  /**
   * Get the date format style (LONG by default).
   *
   * @return The date format style (LONG by default). Never {@code null}.
   */
  @NonNull
  public FormatStyle getDateStyle ()
  {
    return m_eDateStyle;
  }

  /**
   * Set the date format style.
   *
   * @param eDateStyle
   *        Date format style; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxFormatOptions setDateStyle (@NonNull final FormatStyle eDateStyle)
  {
    m_eDateStyle = eDateStyle;
    return this;
  }

  /**
   * Get the override for the number of fraction digits used when formatting amounts. An amount that
   * has more fraction digits shows them, because no value is rounded.
   *
   * @return Override for the number of fraction digits used when formatting amounts. When
   *         {@code null} (the default), the currency's natural fraction-digits count is used.
   */
  @Nullable
  public Integer getAmountMinFractionDigits ()
  {
    return m_aAmountMinFractionDigits;
  }

  /**
   * Override the number of fraction digits used when formatting amounts. It is a minimum: an amount
   * that has more fraction digits shows them.
   *
   * @param aAmountMinFractionDigits
   *        Digit count or {@code null} to use the currency default.
   * @return this for chaining.
   */
  @NonNull
  public PhloxFormatOptions setAmountMinFractionDigits (@Nullable final Integer aAmountMinFractionDigits)
  {
    m_aAmountMinFractionDigits = aAmountMinFractionDigits;
    return this;
  }

  /**
   * Get the fraction digits for percentage values (default {@value #DEFAULT_PERCENT_MIN_FRACTION_DIGITS}).
   * A percentage that has more fraction digits shows them, because no value is rounded.
   *
   * @return Fraction digits for percentage values (default {@value #DEFAULT_PERCENT_MIN_FRACTION_DIGITS}).
   */
  public int getPercentMinFractionDigits ()
  {
    return m_nPercentMinFractionDigits;
  }

  /**
   * Set the fraction digits for percentage values. It is a minimum: a percentage that has more
   * fraction digits shows them.
   *
   * @param nPercentMinFractionDigits
   *        Digit count (≥ 0).
   * @return this for chaining.
   */
  @NonNull
  public PhloxFormatOptions setPercentMinFractionDigits (final int nPercentMinFractionDigits)
  {
    m_nPercentMinFractionDigits = nPercentMinFractionDigits;
    return this;
  }

  /**
   * Get the negative-amount style (minus-sign by default).
   *
   * @return The negative-amount style (minus-sign by default). Never {@code null}.
   */
  @NonNull
  public ENegativeStyle getNegativeStyle ()
  {
    return m_eNegativeStyle;
  }

  /**
   * Set the negative-amount style.
   *
   * @param eNegativeStyle
   *        Negative style; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxFormatOptions setNegativeStyle (@NonNull final ENegativeStyle eNegativeStyle)
  {
    m_eNegativeStyle = eNegativeStyle;
    return this;
  }

  /**
   * Get an independent copy of these options.
   *
   * @return A new instance with the same values. Never {@code null}.
   */
  @NonNull
  public PhloxFormatOptions getClone ()
  {
    return new PhloxFormatOptions (this);
  }
}
