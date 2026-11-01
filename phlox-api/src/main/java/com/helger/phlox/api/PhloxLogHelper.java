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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.concurrent.Immutable;

/**
 * Helper for messages that contain values of an invoice. The invoice is untrusted input, so such a
 * value must not be able to forge a log line or to fill the log.
 *
 * @author Philip Helger
 */
@Immutable
public final class PhloxLogHelper
{
  /** The default maximum number of characters that {@link #sanitise(String)} keeps. */
  public static final int DEFAULT_MAX_LENGTH = 200;

  private static final char REPLACEMENT = '_';
  private static final String TRUNCATION_MARK = "...";

  private PhloxLogHelper ()
  {}

  private static boolean _isUnsafe (final int nCodePoint)
  {
    if (Character.isISOControl (nCodePoint))
      return true;

    final int nType = Character.getType (nCodePoint);
    // Line and paragraph separators, and format characters such as the bidi controls
    return nType == Character.LINE_SEPARATOR ||
           nType == Character.PARAGRAPH_SEPARATOR ||
           nType == Character.FORMAT ||
           nType == Character.SURROGATE;
  }

  /**
   * Make a value safe for a message with the default maximum length of {@value #DEFAULT_MAX_LENGTH}
   * characters.
   *
   * @param sValue
   *        The value. May be {@code null}.
   * @return See {@link #sanitise(String, int)}. Never {@code null}.
   */
  @NonNull
  public static String sanitise (@Nullable final String sValue)
  {
    return sanitise (sValue, DEFAULT_MAX_LENGTH);
  }

  /**
   * Make a value safe for a message: newlines, other control characters and invisible format
   * characters are replaced by {@code _}, and the length is capped.
   *
   * @param sValue
   *        The value. May be {@code null}.
   * @param nMaxLength
   *        The maximum number of characters to keep. A longer value is cut and ends with
   *        {@code ...}.
   * @return The safe value; the text {@code null} for a {@code null} value. Never {@code null}.
   */
  @NonNull
  public static String sanitise (@Nullable final String sValue, @Nonnegative final int nMaxLength)
  {
    if (sValue == null)
      return "null";

    final StringBuilder aSB = new StringBuilder (Math.min (sValue.length (), nMaxLength) + TRUNCATION_MARK.length ());
    int nIndex = 0;
    while (nIndex < sValue.length ())
    {
      final int nCodePoint = sValue.codePointAt (nIndex);
      final int nCharCount = Character.charCount (nCodePoint);
      if (aSB.length () + nCharCount > nMaxLength)
      {
        aSB.append (TRUNCATION_MARK);
        break;
      }
      if (_isUnsafe (nCodePoint))
        aSB.append (REPLACEMENT);
      else
        aSB.appendCodePoint (nCodePoint);
      nIndex += nCharCount;
    }
    return aSB.toString ();
  }
}
