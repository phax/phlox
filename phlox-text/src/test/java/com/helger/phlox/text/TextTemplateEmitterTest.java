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
package com.helger.phlox.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.phlox.api.watermark.PhloxProgramWatermark;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.cii.CiiInvoiceReader;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.testfiles.MockPhloxTestFiles;
import com.helger.phlox.ubl.UblInvoiceReader;

/**
 * Test class for class {@link TextTemplateEmitter}.
 *
 * @author Philip Helger
 */
public final class TextTemplateEmitterTest
{
  private static final String EXAMPLE1 = "2017/ubl/invoice/ubl-tc434-example1.xml";

  @NonNull
  private static PhloxInvoice _read (@NonNull final IInvoiceReader <InputStream> aReader,
                                     @NonNull final ClassPathResource aRes)
  {
    try (final InputStream aIS = aRes.getInputStream ())
    {
      final PhloxInvoice aModel = aReader.read (aIS, new ErrorList ());
      assertNotNull ("Failed to parse " + aRes.getPath (), aModel);
      return aModel;
    }
    catch (final IOException ex)
    {
      throw new IllegalStateException ("Failed to open " + aRes.getPath (), ex);
    }
  }

  @NonNull
  private static PhloxInvoice _example1 ()
  {
    return _read (UblInvoiceReader.fromUbl21 (), new ClassPathResource (MockPhloxTestFiles.BASE_PATH + EXAMPLE1));
  }

  /**
   * Render a template and remove the program watermark, which is always the last line.
   */
  @NonNull
  private static String _render (@NonNull final String sTemplate, @NonNull final TextRenderContext aCtx)
  {
    final String sText = new TextInvoiceWriter ().writeAsString (_example1 (),
                                                                 aCtx.setTemplate (PhloxTemplate.fromString (sTemplate)));
    final String sProgram = "\n\n" + PhloxProgramWatermark.getInstance ().getText () + "\n";
    assertTrue (sText, sText.endsWith (sProgram));
    return sText.substring (0, sText.length () - sProgram.length ());
  }

  @NonNull
  private static String _render (@NonNull final String sTemplate)
  {
    return _render (sTemplate, new TextRenderContext ());
  }

  @Test
  public void testInlineStylingIsStripped ()
  {
    assertEquals ("Bold em strike code 12115118", _render ("**Bold** *em* ~~strike~~ `code` {{BT-1}}"));
  }

  @Test
  public void testHeadings ()
  {
    assertEquals ("Title\n=====\n\nSection\n-------\n\nSub", _render ("# Title\n\n## Section\n\n### Sub"));
  }

  @Test
  public void testLineBreaks ()
  {
    // A line break in the template is a line break in the output
    assertEquals ("a\nb\nc", _render ("a\nb\\\nc"));
  }

  @Test
  public void testTable ()
  {
    assertEquals ("A     Amount  C\n----  ------  -\nx       1.00  y\nlong   10.00",
                  _render ("| A | Amount | C |\n|---|---:|:---:|\n| x | 1.00 | y |\n| long | 10.00 | |"));
  }

  @Test
  public void testTableWithoutHeaderText ()
  {
    // The empty header row of a table of key-value pairs is not shown
    assertEquals ("Key    Value\nOther  1", _render ("| | |\n|---|---|\n| Key | Value |\n| Other | 1 |"));
  }

  @Test
  public void testTableCellWithLineBreak ()
  {
    assertEquals ("A  B\n-  -\nx  z\ny", _render ("| A | B |\n|---|---|\n| x<br>y | z |"));
  }

  @Test
  public void testLists ()
  {
    assertEquals ("- one\n- two\n\n3. three\n4. four", _render ("- one\n- two\n\n3. three\n4. four"));
  }

  @Test
  public void testBlockQuote ()
  {
    assertEquals ("before\n\n  quoted\n  text", _render ("before\n\n> quoted\n> text"));
  }

  @Test
  public void testLinks ()
  {
    assertEquals ("site (https://example.org), a@b.example, mail (a@b.example), bad",
                  _render ("[site](https://example.org), [a@b.example](mailto:a@b.example), [mail](mailto:a@b.example), [bad](javascript:x)"));
  }

  @Test
  public void testRules ()
  {
    final String sRule = StringHelper.getRepeated ('-', TextRenderContext.DEFAULT_LINE_WIDTH);
    assertEquals ("a\n\n" + sRule + "\n\nb\n\n" + sRule + "\n\nc", _render ("a\n\n---\n\nb\n\n{{pagebreak}}\n\nc"));
    assertEquals ("a\n\n" + StringHelper.getRepeated ('-', 10) + "\n\nb",
                  _render ("a\n\n---\n\nb", new TextRenderContext ().setLineWidth (10)));
  }

  @Test
  public void testWrapping ()
  {
    final String sTemplate = "one two three four five six seven eight nine ten";
    assertEquals ("one two three four\nfive six seven eight\nnine ten",
                  _render (sTemplate, new TextRenderContext ().setLineWidth (20)));
    assertEquals ("- one two three\n  four five six\n  seven eight nine\n  ten",
                  _render ("- " + sTemplate, new TextRenderContext ().setLineWidth (18)));
    // A word longer than the line width stays on a line of its own
    assertEquals ("one\nthreethreethree\ntwo", _render ("one threethreethree two", new TextRenderContext ().setLineWidth (5)));
    // No wrapping
    assertEquals (sTemplate, _render (sTemplate, new TextRenderContext ().setLineWidth (0)));
  }

  @Test
  public void testGroupsAreStacked ()
  {
    assertEquals ("left\n\nright", _render ("{{row:r}}\n\n{{group:a}}\n\nleft\n\n{{end}}\n\n{{group:b}}\n\nright\n\n{{end}}\n\n{{end}}"));
  }

  @Test
  public void testImages () throws IOException
  {
    // A logo that is not set is left out
    assertEquals ("a", _render ("a\n\n![Logo](attachment:logo)"));

    final TextRenderContext aCtx = new TextRenderContext ();
    try (final InputStream aIS = MockPhloxTestFiles.getLogo ().getInputStream ())
    {
      aCtx.setLogo (new PhloxAttachment ().setBinary (aIS.readAllBytes ())
                                          .setMimeCode ("image/png")
                                          .setFilename ("phoss.png"));
    }
    assertEquals ("a\n\n[Logo]\n\nb [Logo] c", _render ("a\n\n![Logo](attachment:logo)\n\nb ![Logo](attachment:logo) c", aCtx));
  }

  @Test
  public void testWatermarks ()
  {
    final String sText = new TextInvoiceWriter ().writeAsString (_example1 (),
                                                                 new TextRenderContext ().setUserWatermark (WatermarkSpec.text ("DRAFT")));
    assertTrue (sText.startsWith ("*** DRAFT ***\n\n"));
    assertTrue (sText.endsWith ("\n\n" + PhloxProgramWatermark.getInstance ().getText () + "\n"));
  }

  @Test
  public void testDefaultTemplate ()
  {
    final String sText = new TextInvoiceWriter ().writeAsString (_example1 (),
                                                                 new TextRenderContext ().setLocale (Locale.GERMAN));
    assertTrue (sText.startsWith ("Rechnung\n\n12115118\n========\n"));
    assertFalse (sText.contains ("**"));
  }

  private static void _renderAll (@NonNull final IInvoiceReader <InputStream> aReader,
                                  @NonNull final ICommonsList <ClassPathResource> aFiles)
  {
    assertFalse (aFiles.isEmpty ());
    for (final ClassPathResource aRes : aFiles)
    {
      final PhloxInvoice aModel = _read (aReader, aRes);
      for (final Locale aLocale : new Locale [] { Locale.ENGLISH, Locale.GERMAN })
      {
        final String sText = new TextInvoiceWriter ().writeAsString (aModel, new TextRenderContext ().setLocale (aLocale));
        assertTrue (aRes.getPath (), sText.length () > 200);
        assertFalse (aRes.getPath (), sText.contains ("<br"));
      }
    }
  }

  @Test
  public void testRenderAllExamples ()
  {
    _renderAll (UblInvoiceReader.fromUbl21 (), MockPhloxTestFiles.getAllUbl21Files ());
    _renderAll (UblInvoiceReader.fromUbl25 (), MockPhloxTestFiles.getAllUbl25Files ());
    _renderAll (CiiInvoiceReader.fromD16B (), MockPhloxTestFiles.getCiiD16BFiles ());
    _renderAll (CiiInvoiceReader.fromD25A (), MockPhloxTestFiles.getCiiD25AFiles ());
  }

  @Test
  public void testQrCode ()
  {
    assertEquals ("[Code]", _render ("![Code](qrcode:abc \"height-px:80\")"));
    assertEquals ("Before", _render ("Before\n\n![Gone](qrcode:)"));
  }

  @Test
  public void testControlCharactersAreRemoved ()
  {
    final String sBad = "\u0007\u0008\u001B[31m\u007F\u0085\u009B\u202A\u202B\u202C\u202D\u202E\u2066\u2067\u2068\u2069";
    final String sClean = "[31m";
    assertEquals ("a" + sClean + "b", _render ("a" + sBad + "b"));
    assertEquals ("a" + sClean + "b", _render ("`a" + sBad + "b`"));
    assertEquals ("a" + sClean + "b", _render ("```\na" + sBad + "b\n```"));
    assertEquals ("a" + sClean + "b", _render ("    a" + sBad + "b"));
    assertEquals ("x <s a='" + sClean + "'> y", _render ("x <s a='" + sBad + "'> y"));
    assertEquals ("<div>" + sClean + "</div>", _render ("<div>" + sBad + "</div>"));
    assertEquals ("t (https://a.example/" + sClean + ")", _render ("[t](<https://a.example/" + sBad + ">)"));
    // The width of a column does not count them
    assertEquals ("A   B\n--  -\nab  c", _render ("| A | B |\n|---|---|\n| a" + sBad.substring (0, 2) + "b | c |"));
    assertEquals ("*** " + sClean + " ***\n\nx",
                  _render ("x", new TextRenderContext ().setUserWatermark (WatermarkSpec.text (sBad))));
  }

  @Test
  public void testLineFeedTabAndMarksStay ()
  {
    assertEquals ("a\tb", _render ("```\na\tb\n```"));
    assertEquals ("a\nb", _render ("```\na\nb\n```"));
    assertEquals ("a\u200Eb\u200Fc", _render ("a\u200Eb\u200Fc"));
    assertEquals ("\u05D0\u05D1 \u00E4", _render ("\u05D0\u05D1 \u00E4"));
  }
}
