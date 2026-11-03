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
package com.helger.phlox.api.i18n;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.concurrent.Immutable;
import com.helger.datetime.format.EDTFormatterMode;
import com.helger.datetime.format.PDTFormatter;
import com.helger.phlox.api.ENegativeStyle;
import com.helger.phlox.api.PhloxFormatOptions;

/**
 * Locale-aware formatting helpers used by both writers. Pure functions — no state.
 *
 * @author Philip Helger
 */
@Immutable
public final class PhloxFormat
{
  /** The non-breaking space. */
  private static final char NBSP = '\u00A0';

  /**
   * A currency symbol or a currency code that is directly followed by a digit, as in
   * <code>&euro;10.00</code> or <code>NOK1,273.00</code>.
   */
  private static final Pattern CURRENCY_BEFORE_DIGIT = Pattern.compile ("(\\p{Sc}+|\\p{Lu}{3})(?=\\p{Nd})");

  private PhloxFormat ()
  {}

  /**
   * Make sure that the number and its currency or percent sign are never separated at the end of a
   * line. Every space that the locale puts into the text becomes a non-breaking space, and a space
   * is inserted between a currency in front of the number and the number, so that they do not touch.
   *
   * @param sText
   *        The formatted amount or percentage. Never <code>null</code>.
   * @return The text with non-breaking spaces. Never <code>null</code>.
   */
  @NonNull
  private static String _keepTogether (@NonNull final String sText)
  {
    final StringBuilder aSB = new StringBuilder (sText.length () + 2);
    for (final char c : sText.toCharArray ())
    {
      // Includes the narrow no-break space of some locales, which many fonts do not have
      aSB.append (Character.getType (c) == Character.SPACE_SEPARATOR ? NBSP : c);
    }
    return CURRENCY_BEFORE_DIGIT.matcher (aSB).replaceAll ("$1" + NBSP);
  }

  /**
   * @return The number of fraction digits that the value needs, not counting trailing zeros.
   */
  private static int _getFractionDigits (@NonNull final BigDecimal aValue)
  {
    return Math.max (aValue.stripTrailingZeros ().scale (), 0);
  }

  /**
   * Format a date in the style of the format options.
   *
   * @param aDate
   *        The date, may be {@code null}.
   * @param aLocale
   *        The target locale.
   * @param aOptions
   *        Optional format overrides; may be {@code null} for defaults.
   * @return Formatted date string, or empty if {@code aDate} is {@code null}.
   */
  @NonNull
  public static String date (@Nullable final LocalDate aDate,
                             @NonNull final Locale aLocale,
                             @Nullable final PhloxFormatOptions aOptions)
  {
    if (aDate == null)
      return "";
    final FormatStyle eStyle = aOptions != null && aOptions.getDateStyle () != null ? aOptions.getDateStyle ()
                                                                                    : PhloxFormatOptions.DEFAULT_DATE_STYLE;
    return PDTFormatter.getFormatterDate (eStyle, aLocale, EDTFormatterMode.PRINT).format (aDate);
  }

  /**
   * Format an amount with the given currency in the locale's number style. An amount is never
   * rounded: it has the fraction digits of the currency, or of
   * {@link PhloxFormatOptions#getAmountMinFractionDigits()}, and more if its value needs them. A
   * negative amount is shown as {@link PhloxFormatOptions#getNegativeStyle()} asks for.
   *
   * @param aAmount
   *        The amount, may be {@code null}.
   * @param sCurrencyCode
   *        The ISO 4217 currency code (e.g. {@code "EUR"}), may be {@code null} for no currency
   *        symbol.
   * @param aLocale
   *        The target locale.
   * @param aOptions
   *        Optional format overrides; may be {@code null} for defaults.
   * @return Formatted amount, or empty if {@code aAmount} is {@code null}.
   */
  @NonNull
  public static String amount (@Nullable final BigDecimal aAmount,
                               @Nullable final String sCurrencyCode,
                               @NonNull final Locale aLocale,
                               @Nullable final PhloxFormatOptions aOptions)
  {
    if (aAmount == null)
      return "";

    final NumberFormat aFmt = NumberFormat.getCurrencyInstance (aLocale);
    if (sCurrencyCode != null)
    {
      try
      {
        final Currency aCurrency = Currency.getInstance (sCurrencyCode);
        aFmt.setCurrency (aCurrency);
        // Setting the currency does not change the number of fraction digits, which are the ones of
        // the currency of the locale (2 for EUR, 0 for JPY, 3 for BHD)
        final int nDigits = aCurrency.getDefaultFractionDigits ();
        if (nDigits >= 0)
        {
          aFmt.setMinimumFractionDigits (nDigits);
          aFmt.setMaximumFractionDigits (nDigits);
        }
      }
      catch (final IllegalArgumentException ex)
      {
        // Unknown currency code — keep the locale default
      }
    }
    if (aOptions != null && aOptions.getAmountMinFractionDigits () != null)
    {
      aFmt.setMinimumFractionDigits (aOptions.getAmountMinFractionDigits ().intValue ());
      aFmt.setMaximumFractionDigits (aOptions.getAmountMinFractionDigits ().intValue ());
    }
    aFmt.setMaximumFractionDigits (Math.max (aFmt.getMaximumFractionDigits (), _getFractionDigits (aAmount)));
    if (aAmount.signum () < 0 && aOptions != null && aOptions.getNegativeStyle () == ENegativeStyle.PARENTHESES)
      return "(" + _keepTogether (aFmt.format (aAmount.negate ())) + ")";
    return _keepTogether (aFmt.format (aAmount));
  }

  /**
   * Format a percentage value (e.g. {@code 19} → {@code "19.00%"} in en-US, {@code "19,00 %"} in
   * de-DE). A percentage is never rounded: it has the fraction digits of the options and more if its
   * value needs them.
   *
   * @param aRate
   *        The value expressed in whole-percent (i.e. {@code 19} for 19%). May be {@code null}.
   * @param aLocale
   *        The target locale.
   * @param aOptions
   *        Optional format overrides; may be {@code null} for defaults.
   * @return Formatted percentage, or empty if {@code aRate} is {@code null}.
   */
  @NonNull
  public static String percent (@Nullable final BigDecimal aRate,
                                @NonNull final Locale aLocale,
                                @Nullable final PhloxFormatOptions aOptions)
  {
    if (aRate == null)
      return "";

    final NumberFormat aFmt = NumberFormat.getPercentInstance (aLocale);
    final int nDigits = aOptions != null ? aOptions.getPercentMinFractionDigits () : 2;
    aFmt.setMinimumFractionDigits (nDigits);
    aFmt.setMaximumFractionDigits (Math.max (nDigits, _getFractionDigits (aRate)));
    // From 19 to 0,19
    return _keepTogether (aFmt.format (aRate.movePointLeft (2)));
  }
}
