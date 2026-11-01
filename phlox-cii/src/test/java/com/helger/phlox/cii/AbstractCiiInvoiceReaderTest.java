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
package com.helger.phlox.cii;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;

import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;

/**
 * Test class for class {@link AbstractCiiInvoiceReader}.
 *
 * @author Philip Helger
 */
public final class AbstractCiiInvoiceReaderTest
{
  @Test
  public void testDateFormats ()
  {
    final ErrorList aErrors = new ErrorList ();
    assertEquals (LocalDate.of (2026, 3, 1), AbstractCiiInvoiceReader.parseFormattedDate ("20260301", "102", aErrors));
    assertEquals (LocalDate.of (2026, 3, 1), AbstractCiiInvoiceReader.parseFormattedDate ("20260301", null, aErrors));
    assertTrue (aErrors.isEmpty ());
  }

  @Test
  public void testWeekDateDoesNotDependOnTheDefaultLocale ()
  {
    // Monday of ISO week 1 of 2026
    final ErrorList aErrors = new ErrorList ();
    assertEquals (LocalDate.of (2025, 12, 29), AbstractCiiInvoiceReader.parseFormattedDate ("26011", "103", aErrors));
    assertTrue (aErrors.isEmpty ());
  }
}
