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
package com.helger.phlox.api.model;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Test class for class {@link PhloxAttachment}.
 *
 * @author Philip Helger
 */
public final class PhloxAttachmentTest
{
  @Test
  public void testGetImageMimeCodeOrNull ()
  {
    assertEquals ("image/png", PhloxAttachment.getImageMimeCodeOrNull ("a.PNG"));
    assertEquals ("image/jpeg", PhloxAttachment.getImageMimeCodeOrNull ("a/b.jpg"));
    assertEquals ("image/jpeg", PhloxAttachment.getImageMimeCodeOrNull ("a.jpeg"));
    assertEquals ("image/gif", PhloxAttachment.getImageMimeCodeOrNull ("a.gif"));
    assertEquals ("image/svg+xml", PhloxAttachment.getImageMimeCodeOrNull ("a.svg"));
    assertNull (PhloxAttachment.getImageMimeCodeOrNull ("application.properties"));
  }

  @Test
  public void testCreateLogo ()
  {
    final byte [] aBinary = { 1, 2 };
    final PhloxAttachment aLogo = PhloxAttachment.createLogo (aBinary, "logo.gif");
    assertArrayEquals (aBinary, aLogo.getBinary ());
    assertEquals ("image/gif", aLogo.getMimeCode ());
    assertEquals ("logo.gif", aLogo.getFilename ());

    // The type of a file with another name is a guess
    assertEquals ("image/png", PhloxAttachment.createLogo (aBinary, "logo").getMimeCode ());
  }
}
