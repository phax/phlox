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

import com.helger.base.string.StringHelper;

/**
 * Test class for class {@link PdfTextSupport}.
 *
 * @author Philip Helger
 */
public final class PdfTextSupportTest
{
  // The cases of 200000 characters took minutes with the former regular expressions
  private static final int LONG_RUN = 200_000;
  private static final long LONG_RUN_TIMEOUT_MS = 5_000;

  @Test
  public void testSeparateCurrencySymbol ()
  {
    assertEquals ("\u20AC\u00A010.00", PdfTextSupport.separateCurrencySymbol ("\u20AC10.00"));
    assertEquals ("$$\u00A01", PdfTextSupport.separateCurrencySymbol ("$$1"));
    assertEquals ("\u20AC 10", PdfTextSupport.separateCurrencySymbol ("\u20AC 10"));
    assertEquals ("$", PdfTextSupport.separateCurrencySymbol ("$"));
    assertEquals ("", PdfTextSupport.separateCurrencySymbol (""));
    // Letters outside the BMP stay intact
    assertEquals ("\uD83D\uDE00\u20AC\u00A01", PdfTextSupport.separateCurrencySymbol ("\uD83D\uDE00\u20AC1"));
  }

  @Test (timeout = LONG_RUN_TIMEOUT_MS)
  public void testSeparateCurrencySymbolIsLinear ()
  {
    final String sRun = StringHelper.getRepeated ('$', LONG_RUN);
    assertEquals (sRun, PdfTextSupport.separateCurrencySymbol (sRun));
  }

  @Test
  public void testEscapeMarkup ()
  {
    assertEquals ("\\{a\\}", PdfTextSupport.escapeMarkup ("{a}"));
    assertEquals ("\\\\\\{", PdfTextSupport.escapeMarkup ("\\{"));
    assertEquals ("\\*\\_\\`\\~~ ~ a", PdfTextSupport.escapeMarkup ("*_`~~ ~ a"));
  }

  @Test
  public void testGetSafeFileName ()
  {
    assertEquals ("invoice.pdf", PdfTextSupport.getSafeFileName ("invoice.pdf", "x"));
    assertEquals ("evil_x_.exe", PdfTextSupport.getSafeFileName ("../../evil<x>.exe", "x"));
    assertEquals ("evil.exe", PdfTextSupport.getSafeFileName ("C:\\temp\\evil.exe", "x"));
    assertEquals ("hidden", PdfTextSupport.getSafeFileName ("..hidden", "x"));
    assertEquals ("ab.txt", PdfTextSupport.getSafeFileName ("a\u0000b\u202E.txt", "x"));
    assertEquals ("x", PdfTextSupport.getSafeFileName (null, "x"));
    assertEquals ("x", PdfTextSupport.getSafeFileName ("", "x"));
    assertEquals ("x", PdfTextSupport.getSafeFileName ("../..", "x"));
    assertEquals ("x", PdfTextSupport.getSafeFileName ("a/", "x"));
    // The extension stays
    assertEquals (255, PdfTextSupport.getSafeFileName (StringHelper.getRepeated ('a', 300) + ".pdf", "x").length ());
    assertTrue (PdfTextSupport.getSafeFileName (StringHelper.getRepeated ('a', 300) + ".pdf", "x").endsWith (".pdf"));
  }

  @Test
  public void testGetSafeMimeType ()
  {
    assertEquals ("application/pdf", PdfTextSupport.getSafeMimeType ("application/pdf"));
    assertEquals ("image/svg+xml", PdfTextSupport.getSafeMimeType ("image/svg+xml"));
    assertEquals ("text/plain", PdfTextSupport.getSafeMimeType (" text/plain; charset=UTF-8 "));
    for (final String sInvalid : new String [] { null, "", "pdf", "/pdf", "text/", "a/b/c", "text/<x>", "text/pl ain" })
      assertEquals (PdfTextSupport.MIME_TYPE_DEFAULT, PdfTextSupport.getSafeMimeType (sInvalid));
  }
}
