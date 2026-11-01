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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test class for class {@link PhloxSupportingDocument}.
 *
 * @author Philip Helger
 */
public final class PhloxSupportingDocumentTest
{
  @Test
  public void testIsEmpty ()
  {
    assertTrue (new PhloxSupportingDocument ().isEmpty ());
    assertFalse (new PhloxSupportingDocument ().setReference ("a").isEmpty ());
    assertFalse (new PhloxSupportingDocument ().setDescription ("a").isEmpty ());
    assertFalse (new PhloxSupportingDocument ().setExternalLocation ("a").isEmpty ());
    assertFalse (new PhloxSupportingDocument ().setAttachment (new PhloxAttachment ()).isEmpty ());
  }
}
