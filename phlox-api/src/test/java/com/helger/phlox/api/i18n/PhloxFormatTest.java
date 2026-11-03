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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.Test;

import com.helger.phlox.api.ENegativeStyle;
import com.helger.phlox.api.PhloxFormatOptions;

/**
 * Test class for class {@link PhloxFormat}.
 *
 * @author Philip Helger
 */
public final class PhloxFormatTest
{
  private static void _assertNoBreakingSpace (final String sText)
  {
    for (final char c : sText.toCharArray ())
      if (Character.getType (c) == Character.SPACE_SEPARATOR)
        assertEquals ("Only the non-breaking space is allowed in '" + sText + "'", '\u00A0', c);
  }

  @Test
  public void testDateUsesLocaleAndDefaultStyle ()
  {
    final LocalDate aDate = LocalDate.of (2026, 3, 15);
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ();
    final String sEn = PhloxFormat.date (aDate, Locale.US, aOpts);
    final String sDe = PhloxFormat.date (aDate, Locale.GERMANY, aOpts);
    assertNotNull (sEn);
    assertNotNull (sDe);
    // Locale formatting must actually differ — catches a regression where the format options
    // override the locale-driven style.
    assertTrue ("expected en-US and de-DE to format differently for medium-style date, got '" +
                sEn +
                "' vs '" +
                sDe +
                "'",
                !sEn.equals (sDe));
  }

  @Test
  public void testAmountUsesCurrencyAndLocale ()
  {
    final BigDecimal aValue = new BigDecimal ("123.45");
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ();
    final String sUsd = PhloxFormat.amount (aValue, "USD", Locale.US, aOpts);
    final String sEur = PhloxFormat.amount (aValue, "EUR", Locale.GERMANY, aOpts);
    assertNotNull (sUsd);
    assertNotNull (sEur);
    assertTrue ("expected US-formatted USD amount '" + sUsd + "' to contain 123.45", sUsd.contains ("123.45"));
    assertTrue ("expected DE-formatted EUR amount '" + sEur + "' to contain 123,45", sEur.contains ("123,45"));
  }

  @Test
  public void testAmountUsesTheFractionDigitsOfTheCurrency ()
  {
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ();
    // No decimals, whatever the locale uses
    final String sJpyEn = PhloxFormat.amount (new BigDecimal ("1250000"), "JPY", Locale.US, aOpts);
    assertTrue (sJpyEn, sJpyEn.contains ("1,250,000"));
    assertFalse (sJpyEn, sJpyEn.contains ("."));
    final String sJpyDe = PhloxFormat.amount (new BigDecimal ("1250000"), "JPY", Locale.GERMANY, aOpts);
    assertTrue (sJpyDe, sJpyDe.contains ("1.250.000"));
    assertFalse (sJpyDe, sJpyDe.contains (","));
    // Three decimals
    final String sBhd = PhloxFormat.amount (new BigDecimal ("1.5"), "BHD", Locale.US, aOpts);
    assertTrue (sBhd, sBhd.contains ("1.500"));
    // The override wins
    final String sOverride = PhloxFormat.amount (new BigDecimal ("1250000"),
                                                 "JPY",
                                                 Locale.US,
                                                 PhloxFormatOptions.defaults ().setAmountMinFractionDigits (Integer.valueOf (2)));
    assertTrue (sOverride, sOverride.contains ("1,250,000.00"));
  }

  @Test
  public void testAmountMinFractionDigitsOverride ()
  {
    final BigDecimal aValue = new BigDecimal ("1.234");
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ().setAmountMinFractionDigits (Integer.valueOf (3));
    final String sOut = PhloxFormat.amount (aValue, "EUR", Locale.US, aOpts);
    assertTrue ("expected fraction-digit override to keep 3 decimals, got: " + sOut, sOut.contains ("1.234"));
  }

  @Test
  public void testNegativeStyle ()
  {
    final BigDecimal aNegative = new BigDecimal ("-1234.5");
    final PhloxFormatOptions aMinus = PhloxFormatOptions.defaults ();
    final PhloxFormatOptions aParentheses = PhloxFormatOptions.defaults ().setNegativeStyle (ENegativeStyle.PARENTHESES);
    assertTrue (PhloxFormat.amount (aNegative, "EUR", Locale.US, aMinus).contains ("-"));
    assertEquals ("(€ 1,234.50)", PhloxFormat.amount (aNegative, "EUR", Locale.US, aParentheses));
    assertEquals ("(1.234,50 €)", PhloxFormat.amount (aNegative, "EUR", Locale.GERMANY, aParentheses));
    // Only negative amounts
    assertEquals ("€ 1,234.50", PhloxFormat.amount (aNegative.negate (), "EUR", Locale.US, aParentheses));
    assertEquals ("€ 0.00", PhloxFormat.amount (BigDecimal.ZERO, "EUR", Locale.US, aParentheses));
    // No options are the defaults
    assertEquals (PhloxFormat.amount (aNegative, "EUR", Locale.US, aMinus),
                  PhloxFormat.amount (aNegative, "EUR", Locale.US, null));
  }

  @Test
  public void testPercentUsesConfiguredFractionDigits ()
  {
    final BigDecimal aRate = new BigDecimal ("19");
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ();
    final String sEn = PhloxFormat.percent (aRate, Locale.US, aOpts);
    assertNotNull (sEn);
    // BT-X percentages are stored as whole-percent (19 == 19%), so the rendered text must
    // contain "19" — not "1900" (raw multiplication) and not "0.19" (raw fraction).
    assertTrue ("expected '19' in formatted percent, got: " + sEn, sEn.contains ("19"));
    assertEquals ("19.00%", sEn);
    assertEquals ("19,00\u00a0%", PhloxFormat.percent (aRate, Locale.GERMANY, aOpts));
  }

  @Test
  public void testAmountAndPercentNeverBreak ()
  {
    final BigDecimal aValue = new BigDecimal ("1273.5");
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ();
    for (final Locale aLocale : new Locale [] { Locale.US, Locale.GERMANY, Locale.FRANCE, Locale.forLanguageTag ("sv-SE"), Locale.forLanguageTag ("de-CH") })
      for (final String sCurrency : new String [] { "EUR", "NOK", "USD", "CHF" })
      {
        final String sAmount = PhloxFormat.amount (aValue, sCurrency, aLocale, aOpts);
        _assertNoBreakingSpace (sAmount);
        _assertNoBreakingSpace (PhloxFormat.percent (new BigDecimal ("19"), aLocale, aOpts));
      }

    // The currency does not touch the number, also if it is in front of it
    assertTrue (PhloxFormat.amount (aValue, "EUR", Locale.US, aOpts).startsWith ("\u20AC\u00A0"));
    assertTrue (PhloxFormat.amount (aValue, "NOK", Locale.US, aOpts).startsWith ("NOK\u00A0"));
    // The currency behind the number
    assertTrue (PhloxFormat.amount (aValue, "EUR", Locale.GERMANY, aOpts).endsWith ("\u00A0\u20AC"));
    assertTrue (PhloxFormat.percent (new BigDecimal ("19"), Locale.GERMANY, aOpts).endsWith ("\u00A0%"));
  }

  @Test
  public void testNeverRounded ()
  {
    final PhloxFormatOptions aOptions = new PhloxFormatOptions ();
    assertEquals ("€\u00A00.1212", PhloxFormat.amount (new BigDecimal ("0.1212"), "EUR", Locale.ENGLISH, null));
    assertEquals ("€\u00A01.50", PhloxFormat.amount (new BigDecimal ("1.5000"), "EUR", Locale.ENGLISH, null));
    assertEquals ("¥\u00A01,000.5", PhloxFormat.amount (new BigDecimal ("1000.5"), "JPY", Locale.ENGLISH, null));
    assertEquals ("19.125%", PhloxFormat.percent (new BigDecimal ("19.125"), Locale.ENGLISH, aOptions));
    assertEquals ("19.00%", PhloxFormat.percent (new BigDecimal ("19"), Locale.ENGLISH, aOptions));
    assertEquals ("€\u00A01.2345",
                  PhloxFormat.amount (new BigDecimal ("1.2345"),
                                      "EUR",
                                      Locale.ENGLISH,
                                      new PhloxFormatOptions ().setAmountMinFractionDigits (Integer.valueOf (2))));
  }
}
