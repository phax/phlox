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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.testfiles.MockPhloxTestFiles;

/**
 * Smoke + shape tests for {@link CiiInvoiceReader} reading CII D16B (EN 16931:2017). The corpus is sliced into tiers (core,
 * XRechnung 1.2.2 / 2.0.0 / 3.0.2, technical, issues) so that a failure tells us which
 * upstream subset broke. Each well-formed file must parse without fatal errors and must yield
 * an invoice that carries the EN 16931 mandatory fields. The single intentionally-malformed
 * fixture is checked separately.
 */
public final class CiiInvoiceReaderTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CiiInvoiceReaderTest.class);

  private static void _assertMandatoryFields (final PhloxInvoice aModel,
                                              @NonNull final ClassPathResource aRes,
                                              @NonNull final ErrorList aErrors)
  {
    final String sPath = aRes.getPath ();
    assertNotNull ("Reader returned null for " + sPath + " — errors: " + aErrors, aModel);

    // Header-level mandatory BTs.
    assertNotNull ("BT-1 (invoice number) missing in " + sPath, aModel.getNumber ());
    assertNotNull ("BT-2 (issue date) missing in " + sPath, aModel.getIssueDate ());
    assertNotNull ("BT-5 (currency) missing in " + sPath, aModel.getCurrency ());

    // BG-4 seller — must exist with a name.
    assertNotNull ("BG-4 (seller) missing in " + sPath, aModel.getSeller ());
    assertNotNull ("BT-27 (seller name) missing in " + sPath, aModel.getSeller ().getName ());

    // BG-7 buyer — must exist with a name.
    assertNotNull ("BG-7 (buyer) missing in " + sPath, aModel.getBuyer ());
    assertNotNull ("BT-44 (buyer name) missing in " + sPath, aModel.getBuyer ().getName ());

    // BG-22 totals — mandatory.
    assertNotNull ("BG-22 (document totals) missing in " + sPath, aModel.getDocumentTotals ());

    // BG-25 lines — at least one mandatory per EN 16931.
    assertFalse ("BG-25 (invoice lines) empty in " + sPath, aModel.lines ().isEmpty ());
  }

  /**
   * Iterates the supplied tier, asserting that every file parses without fatal errors and that
   * the resulting model carries every EN 16931 mandatory field — BT-1, BT-2, BT-5, BG-4 with
   * a seller name, BG-7 with a buyer name, BG-22 totals, and at least one line in BG-25.
   *
   * @param aTier
   *        the slice to iterate.
   * @param sTierName
   *        human-readable label used in assertion messages and the final log line.
   * @param nExpectedCount
   *        number of files the tier is expected to contain; defended against accidental
   *        accessor drift (e.g. a file added or removed without updating
   *        {@code MockPhloxTestFiles}).
   */
  private static void _readAllAndAssertWellFormed (@NonNull final ICommonsList <ClassPathResource> aTier,
                                                   @NonNull final String sTierName,
                                                   final int nExpectedCount)
  {
    assertTrue ("Tier '" +
                sTierName +
                "' returned " +
                aTier.size () +
                " files; expected at least " +
                nExpectedCount,
                aTier.size () >= nExpectedCount);

    final CiiInvoiceReader aReader = CiiInvoiceReader.fromD16B ();
    int nCount = 0;
    for (final ClassPathResource aRes : aTier)
    {
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final ErrorList aErrors = new ErrorList ();
        final PhloxInvoice aModel = aReader.read (aIS, aErrors);
        _assertMandatoryFields (aModel, aRes, aErrors);
        assertEquals ("Edition of " + aRes.getPath (), EEN16931Edition.EN2017, aModel.getEdition ());
        nCount++;
      }
      catch (final IOException ex)
      {
        throw new RuntimeException ("Failed to open " + aRes.getPath (), ex);
      }
    }
    LOGGER.info ("Tier '" + sTierName + "' — parsed " + nCount + " CII D16B files");
  }

  @Test
  public void testReadCoreFiles ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BCoreFiles (), "core", 15);
  }

  @Test
  public void testReadXRechnung122Files ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BXRechnung122Files (), "xrechnung/1.2.2", 21);
  }

  @Test
  public void testReadXRechnung200Files ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BXRechnung200Files (), "xrechnung/2.0.0", 19);
  }

  @Test
  public void testReadXRechnung302Files ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BXRechnung302Files (), "xrechnung/3.0.2", 33);
  }

  @Test
  public void testReadTechnicalFiles ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BTechnicalFiles (), "xrechnung/technical", 6);
  }

  @Test
  public void testReadIssueFiles ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BIssueFiles (), "issues", 10);
  }

  @Test
  public void testReadZugferdFiles ()
  {
    _readAllAndAssertWellFormed (MockPhloxTestFiles.getCiiD16BZugferdFiles (), "zugferd/2.5.2", 3);
  }

  @Test
  public void testReadEntireCorpus ()
  {
    // Sanity check: the union accessor returns every well-formed file across all tiers.
    final ICommonsList <ClassPathResource> aAll = MockPhloxTestFiles.getCiiD16BFiles ();
    assertTrue ("Expected at least 100 well-formed CII files; got " + aAll.size (), aAll.size () >= 100);
    _readAllAndAssertWellFormed (aAll, "all", aAll.size ());
  }
}
