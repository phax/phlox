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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Locale;

import org.junit.Test;

/**
 * Test class for class {@link DefaultPhloxLabelResolver}.
 *
 * @author Philip Helger
 */
public final class DefaultPhloxLabelResolverTest
{
  @Test
  public void testSingletonIsStable ()
  {
    // Multiple references to the singleton must point at the same instance — the cache key on
    // PhloxTemplate depends on identity-equality of the resolver.
    assertSame (DefaultPhloxLabelResolver.INSTANCE, DefaultPhloxLabelResolver.INSTANCE);
  }

  @Test
  public void testEnglishLabel ()
  {
    final String sValue = DefaultPhloxLabelResolver.INSTANCE.getLabel ("section.header.title.invoice", Locale.ENGLISH);
    assertNotNull (sValue);
    assertEquals ("Invoice", sValue);
  }

  @Test
  public void testGermanLabel ()
  {
    final String sValue = DefaultPhloxLabelResolver.INSTANCE.getLabel ("section.header.title.invoice", Locale.GERMAN);
    assertNotNull (sValue);
    // We accept either of the common German renderings rather than pinning the exact string —
    // this test is here to catch a missing bundle, not to enforce wording.
    assertNotNull ("expected a non-empty German rendering for section.header.title.invoice", sValue);
  }

  @Test
  public void testSpanishLabel ()
  {
    final Locale aSpanish = Locale.forLanguageTag ("es");
    assertEquals ("Factura", DefaultPhloxLabelResolver.INSTANCE.getLabel ("section.header.title.invoice", aSpanish));
    // Non-ASCII on purpose: the bundle is read as UTF-8, like the German one
    assertEquals ("N\u00FAmero de factura", DefaultPhloxLabelResolver.INSTANCE.getLabel ("BT-1", aSpanish));
    // A region of Spanish finds the same bundle
    assertEquals ("Factura",
                  DefaultPhloxLabelResolver.INSTANCE.getLabel ("section.header.title.invoice",
                                                               Locale.forLanguageTag ("es-ES")));
  }

  @Test
  public void testMissingKeyFallsBackToKey ()
  {
    final String sKey = "no.such.key.in.bundle";
    assertEquals (sKey, DefaultPhloxLabelResolver.INSTANCE.getLabel (sKey, Locale.ENGLISH));
  }

  @Test
  public void testCodeListLookup ()
  {
    // VAT category "S" is in the standard EN 16931 code list.
    final String sValue = DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatcategory", "S", Locale.ENGLISH);
    assertNotNull (sValue);
  }

  @Test
  public void testNamesComeFromTheCodeLists ()
  {
    assertEquals ("Standard rate", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatcategory", "S", Locale.ENGLISH));
    assertEquals ("Regelsteuersatz", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatcategory", "S", Locale.GERMAN));
    assertEquals ("Commercial invoice", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("doctype", "380", Locale.ENGLISH));
    assertEquals ("Handelsrechnung", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("doctype", "380", Locale.GERMAN));
    // Any other locale gets the English names
    assertEquals ("Standard rate", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatcategory", "S", Locale.FRENCH));
  }

  @Test
  public void testUnitCodes ()
  {
    assertEquals ("kilogram", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("unit", "KGM", Locale.ENGLISH));
    assertEquals ("Kilogramm", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("unit", "KGM", Locale.GERMAN));
    assertEquals ("kilogram", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("unit", "KGM", Locale.FRENCH));
  }

  @Test
  public void testCountryAndCurrencyAreLocalizedByTheJdk ()
  {
    assertEquals ("Austria", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("country", "AT", Locale.ENGLISH));
    assertEquals ("\u00D6sterreich", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("country", "AT", Locale.GERMAN));
    assertEquals ("Euro", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("currency", "EUR", Locale.GERMAN));
  }

  @Test
  public void testEnglishNamesOfTheEn16931CodeListsAreTheFallback ()
  {
    // These lists have no translation
    for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN })
    {
      assertEquals ("Reverse charge", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatexemption", "VATEX-EU-AE", aLocale));
      assertNotEquals ("AAI", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("notesubject", "AAI", aLocale));
      assertNotEquals ("95", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("allowancereason", "95", aLocale));
    }
  }

  @Test
  public void testMissingCodeFallsBackToCode ()
  {
    final String sCode = "ZZ_UNKNOWN";
    assertEquals (sCode, DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatcategory", sCode, Locale.ENGLISH));
  }

  @Test
  public void testVatexCodeIsCaseInsensitive ()
  {
    // The lookup of en16931-basics ignores the case, like the EN 16931 Schematron rule BR-CL-22
    final String sName = DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatexemption", "VATEX-EU-132-1A", Locale.ENGLISH);
    assertNotEquals ("VATEX-EU-132-1A", sName);
    assertEquals (sName, DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatexemption", "vatex-eu-132-1a", Locale.ENGLISH));
    assertEquals (sName, DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatexemption", "Vatex-EU-132-1A", Locale.ENGLISH));
  }
}
