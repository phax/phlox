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

import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Text adjustments for the template driven PDF output ({@link PdfTemplateEmitter} and the footer and
 * the embedded files of {@link PdfInvoiceWriter}). All of them run on untrusted text, so they are
 * linear scans and not regular expressions that could backtrack.
 *
 * @author Philip Helger
 */
final class PdfTextSupport
{
  /** The MIME type of a file that has none or an invalid one. */
  static final String MIME_TYPE_DEFAULT = "application/octet-stream";

  /** The syntax of RFC 6838, without parameters. */
  private static final Pattern MIME_TYPE = Pattern.compile ("[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]{0,126}/[A-Za-z0-9][A-Za-z0-9!#$&^_.+-]{0,126}");

  private static final int MAX_FILE_NAME_LENGTH = 255;

  private PdfTextSupport ()
  {}

  /**
   * Insert a non-breaking space between a currency symbol and the number that follows it, as in
   * {@code €10.00}. The glyph of the symbol touches the first digit in the PDF fonts, and the symbol
   * and the number are never split over two lines.
   *
   * @param sText
   *        The text to adjust. Never {@code null}.
   * @return The adjusted text. Never {@code null}.
   */
  @NonNull
  static String separateCurrencySymbol (@NonNull final String sText)
  {
    final StringBuilder aSB = new StringBuilder (sText.length () + 4);
    boolean bAfterCurrencySymbol = false;
    int nIndex = 0;
    while (nIndex < sText.length ())
    {
      final int nCodePoint = sText.codePointAt (nIndex);
      if (bAfterCurrencySymbol && Character.getType (nCodePoint) == Character.DECIMAL_DIGIT_NUMBER)
        aSB.append ('\u00A0');
      aSB.appendCodePoint (nCodePoint);
      bAfterCurrencySymbol = Character.getType (nCodePoint) == Character.CURRENCY_SYMBOL;
      nIndex += Character.charCount (nCodePoint);
    }
    return aSB.toString ();
  }

  /**
   * Reduce every run of spaces to a single space. The lines of a block are stripped and joined with
   * an explicit hard line break, so that spaces never have the meaning "hard line break" that the
   * markup of ph-pdf-layout-richtext gives to two spaces before a line ending. Without this the
   * regular expression for that break takes quadratic time on a long run of spaces.
   *
   * @param sText
   *        The text to adjust. Never {@code null}.
   * @return The adjusted text. Never {@code null}.
   */
  @NonNull
  static String collapseSpaces (@NonNull final String sText)
  {
    if (sText.indexOf ("  ") < 0)
      return sText;

    final StringBuilder aSB = new StringBuilder (sText.length ());
    char cPrev = 0;
    for (int i = 0; i < sText.length (); ++i)
    {
      final char cChar = sText.charAt (i);
      if (cChar != ' ' || cPrev != ' ')
        aSB.append (cChar);
      cPrev = cChar;
    }
    return aSB.toString ();
  }

  /**
   * Reduce a file name from an invoice to a base name that is safe in any file system, because the
   * reader of the PDF may extract the file under this name.
   *
   * @param sFileName
   *        The file name of the invoice. May be {@code null}.
   * @param sFallback
   *        The name to use if nothing of the file name is left. Never {@code null}.
   * @return The file name without folders, control characters, reserved characters and leading
   *         dots. Never {@code null}.
   */
  @NonNull
  static String getSafeFileName (@Nullable final String sFileName, @NonNull final String sFallback)
  {
    if (sFileName == null)
      return sFallback;

    final int nStart = Math.max (sFileName.lastIndexOf ('/'), sFileName.lastIndexOf ('\\')) + 1;
    final StringBuilder aSB = new StringBuilder (sFileName.length () - nStart);
    for (int i = nStart; i < sFileName.length (); ++i)
    {
      final char cChar = sFileName.charAt (i);
      if (Character.isISOControl (cChar) || Character.getType (cChar) == Character.FORMAT)
        continue;
      aSB.append (":*?\"<>|".indexOf (cChar) >= 0 ? '_' : cChar);
    }
    int nFirst = 0;
    while (nFirst < aSB.length () && (aSB.charAt (nFirst) == '.' || aSB.charAt (nFirst) == ' '))
      nFirst++;
    final String ret = aSB.substring (nFirst).stripTrailing ();
    if (ret.isEmpty ())
      return sFallback;
    // The end has the extension
    return ret.substring (Math.max (0, ret.length () - MAX_FILE_NAME_LENGTH));
  }

  /**
   * Check the syntax of a MIME type from an invoice.
   *
   * @param sMimeType
   *        The MIME type of the invoice. May be {@code null}.
   * @return The MIME type without parameters, or {@link #MIME_TYPE_DEFAULT} if it is missing or
   *         invalid. Never {@code null}.
   */
  @NonNull
  static String getSafeMimeType (@Nullable final String sMimeType)
  {
    if (sMimeType == null)
      return MIME_TYPE_DEFAULT;

    final int nParameters = sMimeType.indexOf (';');
    final String ret = (nParameters < 0 ? sMimeType : sMimeType.substring (0, nParameters)).strip ();
    return MIME_TYPE.matcher (ret).matches () ? ret : MIME_TYPE_DEFAULT;
  }

  /**
   * Escape the characters that the markup of ph-pdf-layout-richtext treats as a style or link
   * directive. A curly brace cannot be escaped, so it is replaced by a round one.
   *
   * @param sText
   *        The text to escape. Never {@code null}.
   * @return The escaped text. Never {@code null}.
   */
  @NonNull
  static String escapeMarkup (@NonNull final String sText)
  {
    final StringBuilder aSb = new StringBuilder (sText.length () + 8);
    final char [] aChars = sText.toCharArray ();
    for (int i = 0; i < aChars.length; ++i)
    {
      final char c = aChars[i];
      // Two tildes in a row are the strike-through marker
      // The markup parser does not honour a backslash in front of "{", so invoice data could
      // inject {link}, {color} or {bg} directives. Curly braces are shown as round ones instead.
      if (c == '{')
        aSb.append ('(');
      else
        if (c == '}')
          aSb.append (')');
        else
        {
          if (c == '\\' ||
              c == '*' ||
              c == '_' ||
              c == '`' ||
              (c == '~' && i + 1 < aChars.length && aChars[i + 1] == '~'))
            aSb.append ('\\');
          aSb.append (c);
        }
    }
    return aSb.toString ();
  }
}
