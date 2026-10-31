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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.junit.Test;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Test that renders the synthetic invoices of {@link MockPhloxTestFiles} (many lines, Cyrillic and
 * Greek, a currency without decimals) and an invoice with a logo to HTML. The files are written to
 * <code>generated/synthetic</code>.
 *
 * @author Philip Helger
 */
public final class HtmlSyntheticInvoiceTest
{
  private static PhloxInvoice _read (final String sPath)
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH + sPath).getInputStream ())
    {
      final PhloxInvoice aModel = UblInvoiceReader.fromUbl21 ().read (aIS, new ErrorList ());
      assertNotNull ("Failed to parse " + sPath, aModel);
      return aModel;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sPath, ex);
    }
  }

  private static String _render (final PhloxInvoice aModel, final HtmlRenderContext aCtx, final String sName)
  {
    final String ret = new HtmlInvoiceWriter ().writeAsString (aModel, aCtx);
    SimpleFileIO.writeFile (new File ("generated/synthetic/" + sName + ".html"), ret, StandardCharsets.UTF_8);
    return ret;
  }

  private static int _count (final String sText, final String sToFind)
  {
    int nCount = 0;
    for (int nIndex = sText.indexOf (sToFind); nIndex >= 0; nIndex = sText.indexOf (sToFind, nIndex + sToFind.length ()))
      nCount++;
    return nCount;
  }

  @Test
  public void testLongInvoice ()
  {
    final PhloxInvoice aModel = _read (MockPhloxTestFiles.SYNTHETIC_LONG_INVOICE);
    for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN })
    {
      final String sHtml = _render (aModel, new HtmlRenderContext ().setLocale (aLocale), "long-" + aLocale.getLanguage ());
      // One article identifier in each of the lines
      assertEquals (120, _count (sHtml, "ART-0"));
      assertTrue (sHtml.contains ("Hex bolt type A1"));
      assertTrue (sHtml.contains ("Relay type P5"));
    }
  }

  @Test
  public void testCyrillicAndGreek ()
  {
    final PhloxInvoice aModel = _read (MockPhloxTestFiles.SYNTHETIC_CYRILLIC_GREEK_INVOICE);
    final String sHtml = _render (aModel, new HtmlRenderContext ().setLocale (Locale.GERMAN), "cyrillic-greek-de");
    assertTrue (sHtml.contains ("Фотон Електроникс ООД"));
    assertTrue (sHtml.contains ("Ελληνική Βιομηχανία Αισθητήρων Α.Ε."));
    assertTrue (sHtml.contains ("Αισθητήρας θερμοκρασίας"));
    // The browser is told the encoding, in the first 1024 bytes
    assertTrue (sHtml.indexOf ("<meta charset=\"UTF-8\"") > 0);
    assertTrue (sHtml.indexOf ("<meta charset=\"UTF-8\"") < 1024);
  }

  @Test
  public void testYenHasNoDecimals ()
  {
    final PhloxInvoice aModel = _read (MockPhloxTestFiles.SYNTHETIC_JPY_INVOICE);
    final String sEn = _render (aModel, new HtmlRenderContext ().setLocale (Locale.ENGLISH), "jpy-en");
    assertTrue (sEn.contains ("1,250,000"));
    assertTrue (sEn.contains ("2,245,608"));
    assertFalse (sEn.contains ("1,250,000.00"));

    final String sDe = _render (aModel, new HtmlRenderContext ().setLocale (Locale.GERMAN), "jpy-de");
    assertTrue (sDe.contains ("1.250.000"));
    assertTrue (sDe.contains ("2.245.608"));
    assertFalse (sDe.contains ("1.250.000,00"));
  }

  @Test
  public void testLogo () throws IOException
  {
    final PhloxInvoice aModel = _read ("2017/ubl/invoice/ubl-tc434-example1.xml");
    final PhloxAttachment aLogo;
    try (final InputStream aIS = MockPhloxTestFiles.getLogo ().getInputStream ())
    {
      aLogo = new PhloxAttachment ().setBinary (aIS.readAllBytes ()).setMimeCode ("image/png").setFilename ("phoss.png");
    }
    assertFalse (_render (aModel, new HtmlRenderContext (), "logo-without").contains ("class=\"phlox-logo\""));
    final String sWith = _render (aModel, new HtmlRenderContext ().setLogo (aLogo), "logo-with");
    assertTrue (sWith.contains ("class=\"phlox-logo\""));
    assertTrue (sWith.contains ("src=\"data:image/png;base64,iVBORw0KGgo"));
  }
}
