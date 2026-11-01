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

import org.junit.Test;

/**
 * Test class for class {@link PhloxLogHelper}.
 *
 * @author Philip Helger
 */
public final class PhloxLogHelperTest
{
  @Test
  public void testUnchanged ()
  {
    assertEquals ("null", PhloxLogHelper.sanitise (null));
    assertEquals ("", PhloxLogHelper.sanitise (""));
    assertEquals ("Rechnung 1 € ä", PhloxLogHelper.sanitise ("Rechnung 1 € ä"));
    // A supplementary character is no control character
    assertEquals ("😀", PhloxLogHelper.sanitise ("😀"));
  }

  @Test
  public void testControlCharacters ()
  {
    assertEquals ("a_b_c_d_e", PhloxLogHelper.sanitise ("a\nb\rc\td\u001Be"));
    // NEL, the line and paragraph separator, DEL
    assertEquals ("____", PhloxLogHelper.sanitise ("\u0085  \u007F"));
    // The bidi controls can reorder a line
    assertEquals ("__", PhloxLogHelper.sanitise ("‮⁦"));
    // A lone surrogate
    assertEquals ("a_", PhloxLogHelper.sanitise ("a\uD83D"));
  }

  @Test
  public void testLength ()
  {
    assertEquals ("x".repeat (PhloxLogHelper.DEFAULT_MAX_LENGTH),
                  PhloxLogHelper.sanitise ("x".repeat (PhloxLogHelper.DEFAULT_MAX_LENGTH)));
    assertEquals ("x".repeat (PhloxLogHelper.DEFAULT_MAX_LENGTH) + "...",
                  PhloxLogHelper.sanitise ("x".repeat (PhloxLogHelper.DEFAULT_MAX_LENGTH + 1)));
    assertEquals ("abc...", PhloxLogHelper.sanitise ("abcdef", 3));
    assertEquals ("...", PhloxLogHelper.sanitise ("abcdef", 0));
    // A surrogate pair is not cut in two
    assertEquals ("a...", PhloxLogHelper.sanitise ("a😀", 2));
  }
}
