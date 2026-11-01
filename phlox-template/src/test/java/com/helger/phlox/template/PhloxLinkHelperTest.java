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
package com.helger.phlox.template;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link PhloxLinkHelper}.
 *
 * @author Philip Helger
 */
public final class PhloxLinkHelperTest
{
  @Test
  public void testIsSafeLink ()
  {
    assertTrue (PhloxLinkHelper.isSafeLink ("https://example.org"));
    assertTrue (PhloxLinkHelper.isSafeLink ("HTTP://example.org"));
    assertTrue (PhloxLinkHelper.isSafeLink ("mailto:a@example.org"));
    assertTrue (PhloxLinkHelper.isSafeLink ("relative/path"));

    assertFalse (PhloxLinkHelper.isSafeLink ("javascript:alert(1)"));
    assertFalse (PhloxLinkHelper.isSafeLink ("JavaScript:alert(1)"));
    assertFalse (PhloxLinkHelper.isSafeLink ("data:text/html,x"));
    assertFalse (PhloxLinkHelper.isSafeLink (null));
  }
}
