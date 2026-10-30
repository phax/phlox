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
package com.helger.phlox.template.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.collection.commons.ICommonsList;

/**
 * Test class for class {@link PhloxTemplateCoverage}.
 *
 * @author Philip Helger
 */
public final class PhloxTemplateCoverageTest
{
  private static boolean _isMissing (@NonNull final String sSource, @NonNull final String sBT)
  {
    return PhloxTemplateCoverage.getMissingBTs (sSource).contains (sBT);
  }

  @Test
  public void testAllBTs ()
  {
    final ICommonsList <String> aAll = PhloxModelRegistry.getAllBTs ();
    assertEquals ("BT-1", aAll.get (0));
    assertTrue (aAll.indexOf ("BT-9") < aAll.indexOf ("BT-10"));
    assertTrue (aAll.indexOf ("BT-32") < aAll.indexOf ("BT-32-1"));
    assertTrue (aAll.indexOf ("BT-32-1") < aAll.indexOf ("BT-33"));
    // Neither groups nor derived names
    assertFalse (aAll.contains ("BG-1"));
    assertFalse (aAll.contains ("BT-149-if-not-one"));
    assertEquals (aAll, PhloxTemplateCoverage.getMissingBTs (""));
  }

  @Test
  public void testOnlyPrintedValuesCount ()
  {
    assertFalse (_isMissing ("{{BT-1}}", "BT-1"));
    assertFalse (_isMissing ("{{ BG-22.BT-115 | amount }}", "BT-115"));
    assertFalse (_isMissing ("{{#BG-3}}{{BT-25}}{{/BG-3}}", "BT-25"));
    assertFalse (_isMissing ("{{..BT-126}}", "BT-126"));
    assertTrue (_isMissing ("{{label:BT-1}}", "BT-1"));
    assertTrue (_isMissing ("{{?BT-1}}x{{/BT-1}}", "BT-1"));
    assertTrue (_isMissing ("{{! a comment with BT-1 }}", "BT-1"));
  }

  @Test
  public void testDerivedNames ()
  {
    assertFalse (_isMissing ("{{BG-29.BT-149-if-not-one}}", "BT-149"));
    assertFalse (_isMissing ("{{BT-125.filename}}", "BT-125"));
    assertFalse (_isMissing ("{{BT-111 | amount:BT-6}}", "BT-6"));
  }

  @Test
  public void testAddress ()
  {
    final String sInPath = "{{BG-4.BG-5.address}}";
    assertFalse (_isMissing (sInPath, "BT-35"));
    assertFalse (_isMissing (sInPath, "BT-162"));
    // Neither the country nor another address
    assertTrue (_isMissing (sInPath, "BT-40"));
    assertTrue (_isMissing (sInPath, "BT-50"));

    final String sInSection = "{{#BG-7.BG-8}}{{?BT-55}}x{{/BT-55}}{{address}}{{/BG-7.BG-8}}";
    assertFalse (_isMissing (sInSection, "BT-50"));
    assertTrue (_isMissing (sInSection, "BT-35"));

    // The section is closed again
    assertTrue (_isMissing ("{{#BG-7.BG-8}}{{/BG-7.BG-8}}{{address}}", "BT-50"));
  }
}
