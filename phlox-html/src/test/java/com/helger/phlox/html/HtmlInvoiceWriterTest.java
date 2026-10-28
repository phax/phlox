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
package com.helger.phlox.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/** End-to-end smoke test: parse UBL / CII → render HTML in EN and DE. */
public final class HtmlInvoiceWriterTest
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HtmlInvoiceWriterTest.class);

  private static void _renderAll (final IInvoiceReader <InputStream> aReader,
                                  final ICommonsList <ClassPathResource> aFiles,
                                  final String sLabel,
                                  final int nMinCount)
  {
    final HtmlInvoiceWriter aWriter = new HtmlInvoiceWriter ();
    int nCount = 0;
    for (final ClassPathResource aRes : aFiles)
    {
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
        assertNotNull ("Failed to parse " + aRes.getPath (), aModel);
        for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN, Locale.forLanguageTag ("es"), Locale.FRENCH, Locale.forLanguageTag("nl") })
        {
          final HtmlRenderContext aCtx = new HtmlRenderContext ().setLocale (aLocale)
                                                                 .setUserWatermark (WatermarkSpec.text ("DRAFT"));
          final String sHtml = aWriter.writeAsString (aModel, aCtx);
          assertNotNull (sHtml);
          assertTrue ("HTML output empty for " + aRes.getPath () + " / " + aLocale, sHtml.length () > 500);
          assertTrue ("Missing invoice number in HTML for " + aRes.getPath (), sHtml.contains (aModel.getNumber ()));

          final String sOutputPath = "generated/" +
                                     StringHelper.trimStart (aRes.getPath (), "external/") +
                                     "-" +
                                     aLocale.getLanguage () +
                                     ".html";
          SimpleFileIO.writeFile (new File (sOutputPath), sHtml, StandardCharsets.UTF_8);
        }
        nCount++;
      }
      catch (final IOException ex)
      {
        throw new RuntimeException ("Failed to open " + aRes.getPath (), ex);
      }
    }
    LOGGER.info ("Rendered " + nCount + " " + sLabel + " to HTML (×2 locales)");
    assertTrue ("Expected at least " + nMinCount + " documents rendered", nCount >= nMinCount);
  }

  private static String _renderEn (final IInvoiceReader <InputStream> aReader, final String sPath)
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH + sPath).getInputStream ())
    {
      final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
      assertNotNull ("Failed to parse " + sPath, aModel);
      return new HtmlInvoiceWriter ().writeAsString (aModel, new HtmlRenderContext ().setLocale (Locale.ENGLISH));
    }
    catch (final IOException ex)
    {
      throw new RuntimeException ("Failed to open " + sPath, ex);
    }
  }

  @Test
  public void testRenderUblToHtml ()
  {
    _renderAll (UblInvoiceReader.fromUbl21 (), MockPhloxTestFiles.getUbl21InvoiceFiles (), "UBL 2.1 invoices", 10);
  }

  @Test
  public void testRenderUbl25InvoicesToHtml ()
  {
    _renderAll (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getUbl25InvoiceFiles (), "UBL 2.5 invoices", 10);
  }

  @Test
  public void testRenderUbl25CreditNotesToHtml ()
  {
    _renderAll (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getUbl25CreditNoteFiles (), "UBL 2.5 credit notes", 5);
  }

  @Test
  public void testRenderCiiD25AToHtml ()
  {
    _renderAll (CiiInvoiceReader.fromD25A (), MockPhloxTestFiles.getCiiD25AFiles (), "CII D25A documents", 10);
  }

  @Test
  public void testRenderNew2026ContentToHtml ()
  {
    // BG-37 / BG-38 invoice line delivery information
    final String sUbl = _renderEn (UblInvoiceReader.fromUbl25 (), "2026/ubl/invoice/d25a-new-linedelivery-invoice-ubl.xml");
    assertTrue (sUbl.contains ("4035813333333"));
    assertTrue (sUbl.contains ("Innsbruck"));
    final String sCii = _renderEn (CiiInvoiceReader.fromD25A (), "2026/cii/d25a-new-linedelivery-invoice.xml");
    assertTrue (sCii.contains ("4035813333333"));
    assertTrue (sCii.contains ("Innsbruck"));

    // BG-33 / BG-35 / BG-36 payment terms with early discount and late penalty
    final String sTerms = _renderEn (UblInvoiceReader.fromUbl25 (), "2026/ubl/invoice/d25a-new-paymentterms-invoice-ubl.xml");
    assertTrue (sTerms.contains ("Net 30 days"));
    assertTrue (sTerms.contains ("2.64"));
    assertTrue (sTerms.contains ("12.5"));
  }

  @Test
  public void test2017HtmlDoesNotContain2026Sections ()
  {
    final String sHtml = _renderEn (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    assertEquals (-1, sHtml.indexOf ("payment-discount"));
    assertEquals (-1, sHtml.indexOf ("payment-penalty"));
  }
}
