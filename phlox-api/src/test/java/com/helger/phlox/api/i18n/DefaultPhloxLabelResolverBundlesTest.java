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
import static org.junit.Assert.assertTrue;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.junit.Test;

/**
 * Test the contents of the resource bundles behind {@link DefaultPhloxLabelResolver}: the English,
 * the German and the Spanish label bundle must contain the same BG/BT keys, and the business terms
 * and groups introduced by EN 16931:2026 must be present in all of them.
 *
 * @author Philip Helger
 */
public final class DefaultPhloxLabelResolverBundlesTest
{
  private static final Pattern BG_BT_KEY = Pattern.compile ("(?:BG|BT)-\\d+(?:-\\d+)?");

  /** There is no constant for Spanish in {@link Locale} */
  private static final Locale SPANISH = Locale.forLanguageTag ("es");

  private static final String [] NEW_2026_IDS = { "BG-33",
                                                   "BG-34",
                                                   "BG-35",
                                                   "BG-36",
                                                   "BG-37",
                                                   "BG-38",
                                                   "BG-39",
                                                   "BT-10-1",
                                                   "BT-32-1",
                                                   "BT-166",
                                                   "BT-167",
                                                   "BT-167-1",
                                                   "BT-167-2",
                                                   "BT-170",
                                                   "BT-171",
                                                   "BT-172",
                                                   "BT-173",
                                                   "BT-174",
                                                   "BT-175",
                                                   "BT-176",
                                                   "BT-177",
                                                   "BT-177-1",
                                                   "BT-179",
                                                   "BT-180",
                                                   "BT-181",
                                                   "BT-182",
                                                   "BT-183",
                                                   "BT-184",
                                                   "BT-185",
                                                   "BT-186",
                                                   "BT-186-1",
                                                   "BT-187",
                                                   "BT-188",
                                                   "BT-189",
                                                   "BT-190",
                                                   "BT-191",
                                                   "BT-192",
                                                   "BT-193",
                                                   "BT-193-1",
                                                   "BT-194",
                                                   "BT-195",
                                                   "BT-196",
                                                   "BT-197",
                                                   "BT-198",
                                                   "BT-199",
                                                   "BT-200",
                                                   "BT-201",
                                                   "BT-202",
                                                   "BT-203",
                                                   "BT-204",
                                                   "BT-205",
                                                   "BT-206",
                                                   "BT-207",
                                                   "BT-208",
                                                   "BT-209",
                                                   "BT-210",
                                                   "BT-211",
                                                   "BT-212",
                                                   "BT-213",
                                                   "BT-214",
                                                   "BT-215",
                                                   "BT-216",
                                                   "BT-217",
                                                   "BT-218",
                                                   "BT-219",
                                                   "BT-220" };

  private static Set <String> _getIDs (final Locale aLocale)
  {
    final Set <String> ret = new TreeSet <> ();
    final ResourceBundle aBundle = ResourceBundle.getBundle (DefaultPhloxLabelResolver.BUNDLE_LABELS, aLocale);
    for (final String sKey : aBundle.keySet ())
      if (BG_BT_KEY.matcher (sKey).matches ())
        ret.add (sKey);
    return ret;
  }

  @Test
  public void testEnglishAndGermanHaveTheSameIDs ()
  {
    final Set <String> aEnglish = _getIDs (Locale.ENGLISH);
    final Set <String> aGerman = _getIDs (Locale.GERMAN);
    assertFalse (aEnglish.isEmpty ());
    assertEquals (aEnglish, aGerman);
  }

  @Test
  public void testEnglishAndSpanishHaveTheSameIDs ()
  {
    final Set <String> aEnglish = _getIDs (Locale.ENGLISH);
    final Set <String> aSpanish = _getIDs (SPANISH);
    assertFalse (aEnglish.isEmpty ());
    assertEquals (aEnglish, aSpanish);
  }

  @Test
  public void testNew2026IDsArePresentInAllLanguages ()
  {
    final Set <String> aEnglish = _getIDs (Locale.ENGLISH);
    final Set <String> aGerman = _getIDs (Locale.GERMAN);
    final Set <String> aSpanish = _getIDs (SPANISH);
    for (final String sID : NEW_2026_IDS)
    {
      assertTrue ("Missing English label for " + sID, aEnglish.contains (sID));
      assertTrue ("Missing German label for " + sID, aGerman.contains (sID));
      assertTrue ("Missing Spanish label for " + sID, aSpanish.contains (sID));
    }
  }

  @Test
  public void testNoEmptyLabels ()
  {
    for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN, SPANISH })
    {
      final ResourceBundle aBundle = ResourceBundle.getBundle (DefaultPhloxLabelResolver.BUNDLE_LABELS, aLocale);
      for (final String sID : _getIDs (aLocale))
        assertFalse ("Empty label for " + sID + " in " + aLocale, aBundle.getString (sID).isBlank ());
    }
  }

  @Test
  public void testDottedCodeListNameIsAccepted ()
  {
    // Writers ask for the logical list name "vat.category", the registered name is "vatcategory"
    assertEquals (DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vatcategory", "S", Locale.ENGLISH),
                  DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vat.category", "S", Locale.ENGLISH));
    assertEquals ("Standard rate", DefaultPhloxLabelResolver.INSTANCE.getCodeLabel ("vat.category", "S", Locale.ENGLISH));
  }
}
