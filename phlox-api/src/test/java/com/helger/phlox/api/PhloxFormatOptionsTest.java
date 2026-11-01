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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.time.format.FormatStyle;

import org.junit.Test;

/**
 * Test class for class {@link PhloxFormatOptions}.
 *
 * @author Philip Helger
 */
public final class PhloxFormatOptionsTest
{
  @Test
  public void testDefaults ()
  {
    final PhloxFormatOptions aOpts = PhloxFormatOptions.defaults ();
    assertNotNull (aOpts);
    assertEquals (PhloxFormatOptions.DEFAULT_DATE_STYLE, aOpts.getDateStyle ());
    assertNull (aOpts.getAmountFractionDigits ());
    assertEquals (PhloxFormatOptions.DEFAULT_PERCENT_FRACTION_DIGITS, aOpts.getPercentFractionDigits ());
    assertEquals (PhloxFormatOptions.DEFAULT_NEGATIVE_STYLE, aOpts.getNegativeStyle ());
  }

  @Test
  public void testClone ()
  {
    final PhloxFormatOptions aOpts = new PhloxFormatOptions ().setDateStyle (FormatStyle.LONG)
                                                              .setAmountFractionDigits (Integer.valueOf (3))
                                                              .setPercentFractionDigits (4)
                                                              .setNegativeStyle (ENegativeStyle.PARENTHESES);
    final PhloxFormatOptions aClone = aOpts.getClone ();
    assertNotSame (aOpts, aClone);
    assertEquals (FormatStyle.LONG, aClone.getDateStyle ());
    assertEquals (Integer.valueOf (3), aClone.getAmountFractionDigits ());
    assertEquals (4, aClone.getPercentFractionDigits ());
    assertEquals (ENegativeStyle.PARENTHESES, aClone.getNegativeStyle ());

    aClone.setPercentFractionDigits (1);
    assertEquals (4, aOpts.getPercentFractionDigits ());
  }

  @Test
  public void testFluentChainingReturnsSameInstance ()
  {
    final PhloxFormatOptions aOpts = new PhloxFormatOptions ();
    assertSame (aOpts, aOpts.setDateStyle (FormatStyle.LONG));
    assertSame (aOpts, aOpts.setAmountFractionDigits (Integer.valueOf (3)));
    assertSame (aOpts, aOpts.setPercentFractionDigits (4));
    assertSame (aOpts, aOpts.setNegativeStyle (ENegativeStyle.PARENTHESES));
  }

  @Test
  public void testSettersStickValues ()
  {
    final PhloxFormatOptions aOpts = new PhloxFormatOptions ().setDateStyle (FormatStyle.LONG)
                                                              .setAmountFractionDigits (Integer.valueOf (3))
                                                              .setPercentFractionDigits (4)
                                                              .setNegativeStyle (ENegativeStyle.PARENTHESES);
    assertEquals (FormatStyle.LONG, aOpts.getDateStyle ());
    assertEquals (Integer.valueOf (3), aOpts.getAmountFractionDigits ());
    assertEquals (4, aOpts.getPercentFractionDigits ());
    assertEquals (ENegativeStyle.PARENTHESES, aOpts.getNegativeStyle ());
  }
}
