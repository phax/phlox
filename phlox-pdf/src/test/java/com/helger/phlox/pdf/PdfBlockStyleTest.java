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
package com.helger.phlox.pdf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.helger.pdflayout.base.PLColor;

/**
 * Test class for class {@link PdfBlockStyle}.
 *
 * @author Philip Helger
 */
public final class PdfBlockStyleTest
{
  @Test
  public void testOverlay ()
  {
    final PdfBlockStyle aBase = PdfBlockStyle.builder ()
                                             .fillColor (PLColor.YELLOW)
                                             .textColor (PLColor.RED)
                                             .padding (3)
                                             .bold (true)
                                             .build ();
    final PdfBlockStyle aOther = PdfBlockStyle.builder ()
                                              .textColor (PLColor.BLUE)
                                              .fontSize (20)
                                              .keepTogether (true)
                                              .build ();
    final PdfBlockStyle aResult = aBase.getOverlaidWith (aOther);
    assertEquals (PLColor.YELLOW, aResult.getFillColor ());
    assertEquals (PLColor.BLUE, aResult.getTextColor ());
    assertEquals (3f, aResult.getPadding (), 0f);
    assertTrue (aResult.isBold ());
    assertEquals (Float.valueOf (20), aResult.getFontSize ());
    assertTrue (aResult.isKeepTogether ());
  }
}
