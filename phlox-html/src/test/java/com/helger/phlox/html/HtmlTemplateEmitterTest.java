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
import java.util.regex.Pattern;

import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.file.SimpleFileIO;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Test class for class {@link HtmlTemplateEmitter}.
 *
 * @author Philip Helger
 */
public final class HtmlTemplateEmitterTest
{
  private static PhloxInvoice _read (final IInvoiceReader <InputStream> aReader, final String sPath)
  {
    try (final InputStream aIS = new ClassPathResource (MockPhloxTestFiles.BASE_PATH + sPath).getInputStream ())
    {
      final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
      assertNotNull ("Failed to parse " + sPath, aModel);
      return aModel;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + sPath, ex);
    }
  }

  private static String _render (final PhloxInvoice aModel, final PhloxTemplate aTemplate, final Locale aLocale)
  {
    return new HtmlInvoiceWriter ().writeAsString (aModel,
                                                   new HtmlRenderContext ().setLocale (aLocale)
                                                                           .setTemplate (aTemplate));
  }

  private static void _renderAll (final IInvoiceReader <InputStream> aReader,
                                  final ICommonsList <ClassPathResource> aFiles,
                                  final int nMinCount)
  {
    int nCount = 0;
    for (final ClassPathResource aRes : aFiles)
    {
      try (final InputStream aIS = aRes.getInputStream ())
      {
        final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
        assertNotNull ("Failed to parse " + aRes.getPath (), aModel);
        for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN })
        {
          final String sHtml = _render (aModel, PhloxTemplate.getDefault (), aLocale);
          assertTrue ("HTML output too short for " + aRes.getPath (), sHtml.length () > 500);
          assertTrue ("Missing invoice number for " + aRes.getPath (), sHtml.contains (aModel.getNumber ()));
          assertTrue ("Missing table for " + aRes.getPath (), sHtml.contains ("<table"));
          assertFalse ("Unresolved sentinel in " + aRes.getPath (), sHtml.contains ("[[PHLOX:"));
          assertFalse ("Unresolved placeholder in " + aRes.getPath (), sHtml.contains ("{{"));

          final String sOutputPath = "generated/template/" +
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
        throw new IllegalStateException ("Failed to open " + aRes.getPath (), ex);
      }
    }
    assertTrue ("Expected at least " + nMinCount + " documents rendered", nCount >= nMinCount);
  }

  @Test
  public void testDefaultTemplateUbl21 ()
  {
    _renderAll (UblInvoiceReader.fromUbl21 (), MockPhloxTestFiles.getUbl21InvoiceFiles (), 10);
  }

  @Test
  public void testDefaultTemplateUbl25 ()
  {
    _renderAll (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getAllUbl25Files (), 10);
  }

  @Test
  public void testDefaultTemplateCiiD16B ()
  {
    _renderAll (CiiInvoiceReader.fromD16B (), MockPhloxTestFiles.getCiiD16BFiles (), 100);
  }

  @Test
  public void testDefaultTemplateCiiD25A ()
  {
    _renderAll (CiiInvoiceReader.fromD25A (), MockPhloxTestFiles.getCiiD25AFiles (), 10);
  }

  @Test
  public void testMarkdownNodes ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sSource = "# {{BT-1}}\n\n" +
                           "## Section\n\n" +
                           "### Sub\n\n" +
                           "{{class:hint}}\n" +
                           "Text **bold** *em* ~~del~~ `code` [link](https://example.org/x)\nsecond line<br>third line\n\n" +
                           "- one\n- two\n\n" +
                           "3. three\n4. four\n\n" +
                           "> quote\n\n" +
                           "---\n\n" +
                           "| A | B | C |\n|:--|:-:|--:|\n| 1 | 2 | 3 |\n\n" +
                           "{{pagebreak}}\n\n" +
                           "    indented code\n";
    final String sHtml = _render (aModel, PhloxTemplate.fromString (sSource), Locale.ENGLISH);
    assertTrue (sHtml.contains ("<h1 class=\"phlox-title\">" + aModel.getNumber () + "</h1>"));
    assertTrue (sHtml.contains ("<h2 class=\"phlox-section-title\">Section</h2>"));
    assertTrue (sHtml.contains ("<h3 class=\"phlox-subsection-title\">Sub</h3>"));
    assertTrue (sHtml.contains ("<p class=\"phlox-hint\">"));
    assertTrue (sHtml.contains ("<strong>bold</strong>"));
    assertTrue (sHtml.contains ("<em>em</em>"));
    assertTrue (sHtml.contains ("<s>del</s>"));
    assertTrue (sHtml.contains ("<code>code</code>"));
    assertTrue (sHtml.contains ("href=\"https://example.org/x\""));
    assertTrue (sHtml.contains ("second line<br />third line"));
    assertTrue (sHtml.contains ("<ul>"));
    assertTrue (sHtml.contains ("<ol"));
    assertTrue (sHtml.contains ("<blockquote>"));
    assertTrue (sHtml.contains ("<hr"));
    assertTrue (sHtml.contains ("text-align:center"));
    assertTrue (sHtml.contains ("text-align:right"));
    assertTrue (sHtml.contains ("page-break-after:always"));
    assertTrue (sHtml.contains ("<pre><code>indented code"));
  }

  @Test
  public void testRowClassAndLineBreakInTable ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sHtml = _render (aModel, PhloxTemplate.getDefault (), Locale.ENGLISH);
    // {{group:due}} wraps the amount due, {{row:parties}} lays out the parties side by side
    assertTrue (sHtml.contains ("<div class=\"phlox-due\">"));
    assertTrue (sHtml.contains ("<div class=\"phlox-row phlox-parties\">"));
    // {{class:name}} in front of a table row
    final String sRowHtml = _render (aModel,
                                     PhloxTemplate.fromString ("| A | B |\n|---|---|\n{{class:totals-due}}\n| 1 | 2 |\n"),
                                     Locale.ENGLISH);
    assertTrue (sRowHtml, sRowHtml.contains ("<tr class=\"phlox-totals-due\">"));
    // Item name and seller item identifier are separated by a <br> in the lines table
    assertTrue (Pattern.compile ("</strong>\\s*<br />").matcher (sHtml).find ());
    assertFalse (sHtml.contains ("</strong>\\"));
  }

  @Test
  public void testProgramWatermarkIsLinkToProject ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sTemplate = _render (aModel, PhloxTemplate.getDefault (), Locale.ENGLISH);
    final String sBuiltIn = new HtmlInvoiceWriter ().writeAsString (aModel,
                                                                    new HtmlRenderContext ().setLocale (Locale.ENGLISH));
    for (final String sHtml : new String [] { sTemplate, sBuiltIn })
    {
      assertTrue (sHtml.contains ("href=\"https://github.com/phax/phlox\""));
      assertTrue (sHtml.contains ("target=\"phlox\""));
      assertFalse (sHtml.contains ("pointer-events:none;z-index:9999;}.phlox-watermark-program"));
    }
  }

  @Test
  public void testEmptyAddressHasNoStrayCharacters ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl25 (), "2026/ubl/invoice/d25a-edge-zero-bt111-invoice-ubl.xml");
    final String sHtml = _render (aModel, PhloxTemplate.getDefault (), Locale.ENGLISH);
    assertFalse (sHtml.contains ("<p>\\</p>"));
    assertFalse (sHtml.contains ("\\"));
  }

  @Test
  public void testRawHtmlIsEscaped ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sHtml = _render (aModel, PhloxTemplate.fromString ("<script>alert(1)</script>\n"), Locale.ENGLISH);
    assertFalse (sHtml.contains ("<script>"));
    assertTrue (sHtml.contains ("&lt;script&gt;"));
  }

  @Test
  public void testQrCode ()
  {
    final PhloxInvoice aModel = _read (UblInvoiceReader.fromUbl21 (), "2017/ubl/invoice/ubl-tc434-example1.xml");
    final String sHtml = _render (aModel,
                                  PhloxTemplate.fromString ("![EPC](qrcode:one \"height-px:80\")\n\n![Link](qrcode:{{BT-1 | url}})\n\n![Gone](qrcode:)"),
                                  Locale.ENGLISH);
    final String sPngSource = "src=\"data:image/png;base64,";
    int nImages = 0;
    for (int i = sHtml.indexOf (sPngSource); i >= 0; i = sHtml.indexOf (sPngSource, i + 1))
      nImages++;
    assertEquals (sHtml, 2, nImages);
    assertTrue (sHtml, sHtml.contains ("alt=\"EPC\""));
    assertTrue (sHtml, sHtml.contains ("height:80px"));
    // The default height of the second one
    assertTrue (sHtml, sHtml.contains ("height:120px"));
    assertFalse (sHtml, sHtml.contains ("Gone"));
    assertFalse (sHtml, sHtml.contains ("qrcode:"));
  }
}
