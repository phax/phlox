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

import java.util.Locale;

import org.jspecify.annotations.Nullable;

/**
 * Decides which link targets of a template reach the output, because a target can contain values of
 * the invoice.
 *
 * @author Philip Helger
 */
public final class PhloxLinkHelper
{
  private PhloxLinkHelper ()
  {}

  /**
   * Check if a link target is safe to be followed by the reader of the output.
   *
   * @param sDestination
   *        The link target. May be {@code null}.
   * @return {@code true} for a relative target and for the schemes <code>http</code>,
   *         <code>https</code> and <code>mailto</code>.
   */
  public static boolean isSafeLink (@Nullable final String sDestination)
  {
    if (sDestination == null)
      return false;
    final int nColon = sDestination.indexOf (':');
    if (nColon < 0)
      return true;
    final String sScheme = sDestination.substring (0, nColon).toLowerCase (Locale.ROOT);
    return sScheme.equals ("http") || sScheme.equals ("https") || sScheme.equals ("mailto");
  }
}
