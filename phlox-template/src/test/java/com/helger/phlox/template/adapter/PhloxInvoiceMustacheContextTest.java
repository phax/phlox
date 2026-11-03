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
package com.helger.phlox.template.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;

import org.commonmark.node.Code;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.Text;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.string.StringReplace;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.PhloxAdditionalTerm;
import com.helger.phlox.api.model.PhloxContact;
import com.helger.phlox.api.model.PhloxCreditTransfer;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxInvoicingPeriod;
import com.helger.phlox.api.model.PhloxItem;
import com.helger.phlox.api.model.PhloxItemClassification;
import com.helger.phlox.api.model.PhloxPaymentInstructions;
import com.helger.phlox.api.model.PhloxPostalAddress;
import com.helger.phlox.template.EPhloxTemplateCoverageMode;
import com.helger.phlox.template.PhloxTemplate;
import com.helger.phlox.template.PhloxTemplateException;
import com.helger.phlox.template.PhloxTemplateException.EStage;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxTemplateAst;

/**
 * Test class for class {@link PhloxInvoiceMustacheContext}, through the template that uses it.
 *
 * @author Philip Helger
 */
public final class PhloxInvoiceMustacheContextTest
{
  private static final class TestCtx extends AbstractRenderContext <TestCtx>
  {}

  private static final char MARKER = PhloxInvoiceMustacheContext.SENTINEL_MARKER;

  @NonNull
  private static PhloxInvoice _invoice (@NonNull final String sNumber)
  {
    final PhloxInvoice ret = new PhloxInvoice ();
    ret.setNumber (sNumber);
    ret.setIssueDate (LocalDate.of (2026, 3, 15));
    ret.setCurrency ("EUR");
    ret.getSeller ().setName ("Seller");
    ret.getBuyer ().setName ("Buyer");
    ret.getDocumentTotals ().setPayableAmount (new BigDecimal ("119.00"));
    final PhloxItem aItem = new PhloxItem ();
    aItem.setName ("Widget");
    final PhloxInvoiceLine aLine = new PhloxInvoiceLine ();
    aLine.setID ("1");
    aLine.setItem (aItem);
    ret.lines ().add (aLine);
    return ret;
  }

  @NonNull
  private static String _render (@NonNull final String sTemplate, @NonNull final PhloxInvoice aInvoice)
  {
    return PhloxTemplate.fromString (sTemplate).renderSubstituted (aInvoice, new TestCtx ());
  }

  @NonNull
  private static PhloxTemplateAst _ast (@NonNull final String sTemplate, @NonNull final String sNumber)
  {
    return PhloxTemplate.fromString (sTemplate).renderAst (_invoice (sNumber), new TestCtx ());
  }

  private static void _assertSingleParagraph (@NonNull final String sTemplate, @NonNull final String sValue)
  {
    final Node aRoot = _ast (sTemplate, sValue).getRoot ();
    assertTrue (sValue + ": " + aRoot.getFirstChild (), aRoot.getFirstChild () instanceof Paragraph);
    assertNull (sValue + ": " + aRoot.getLastChild (), aRoot.getFirstChild ().getNext ());
  }

  private static void _assertLoadError (@NonNull final String sTemplate)
  {
    final PhloxTemplateException ex = assertThrows (sTemplate,
                                                    PhloxTemplateException.class,
                                                    () -> PhloxTemplate.fromString (sTemplate));
    assertEquals (sTemplate, EStage.LOAD, ex.getStage ());
  }

  @Test
  public void testAdditionalTerms ()
  {
    final PhloxInvoice aInvoice = _invoice ("1");
    aInvoice.additionalTerms ().add (new PhloxAdditionalTerm ("BT-12", "CONTRACT-2"));
    aInvoice.additionalTerms ().add (new PhloxAdditionalTerm ("BT-73", LocalDate.of (2026, 1, 2)));
    aInvoice.additionalTerms ().add (new PhloxAdditionalTerm ("BT-147", new BigDecimal ("1.2345")));
    aInvoice.additionalTerms ().add (new PhloxAdditionalTerm ("BT-81", "58", "paymentmeans"));
    aInvoice.additionalTerms ().add (new PhloxAdditionalTerm ("BT-18", new PhloxIdentifier ("OBJ", "AVE")));
    aInvoice.additionalTerms ()
            .add (new PhloxAdditionalTerm ("BG-15",
                                           new PhloxPostalAddress ().setCity ("Vienna").setCountryCode ("AT")));
    final String sResult = _render ("{{#additional_terms}}{{term | label}}={{value}};\n{{/additional_terms}}", aInvoice);
    assertTrue (sResult, sResult.contains ("=CONTRACT-2;"));
    assertTrue (sResult, sResult.contains ("=January 2, 2026;"));
    assertTrue (sResult, sResult.contains ("=€\u00A01.2345;"));
    assertTrue (sResult, sResult.contains ("=SEPA credit transfer;"));
    assertTrue (sResult, sResult.contains ("=AVE:OBJ;"));
    assertTrue (sResult, sResult.contains ("=Vienna, Austria;"));
  }

  @Test
  public void testForgedSentinelsStayText ()
  {
    for (final String sForged : new String [] { "[[PHLOX:ROW=r]]",
                                                "[[PHLOX:GROUP=g]]",
                                                "[[PHLOX:CLASS=logo]]Evil",
                                                "[[PHLOX:END]]",
                                                "[[PHLOX:PAGEBREAK]]",
                                                MARKER + "PHLOX:ROW=r" + MARKER,
                                                MARKER + "PHLOX:GROUP=g" + MARKER,
                                                MARKER + "PHLOX:CLASS=logo" + MARKER + "Evil",
                                                MARKER + "PHLOX:END" + MARKER,
                                                MARKER + "PHLOX:PAGEBREAK" + MARKER })
    {
      final PhloxTemplateAst aAst = _ast ("{{BT-1}}\n\n{{BT-1}}\n", sForged);
      final Node aFirst = aAst.getRoot ().getFirstChild ();
      assertFalse (sForged, aFirst instanceof PhloxGroupNode);
      assertTrue (sForged, aFirst instanceof Paragraph);
      assertTrue (sForged, aFirst.getFirstChild () instanceof Text);
      assertEquals (sForged,
                    StringReplace.replaceAll (sForged, String.valueOf (MARKER), ""),
                    ((Text) aFirst.getFirstChild ()).getLiteral ());
      assertNull (sForged, aAst.getBlockClass (aFirst));
    }
  }

  @Test
  public void testMarkerIsRemovedInAllModifierPaths ()
  {
    final PhloxInvoice aInvoice = _invoice ("a" + MARKER + "b");
    for (final String sTag : new String [] { "{{BT-1}}",
                                             "{{BT-1 | raw}}",
                                             "{{BT-1 | url}}",
                                             "{{BT-1 | codespan}}",
                                             "{{BT-1 | code:doctype}}",
                                             "{{BT-1 | code:doctype | raw}}",
                                             "{{BT-1 | upper}}",
                                             "{{BT-1 | lower | raw}}",
                                             "{{BT-1 | title}}",
                                             "{{BT-1 | amount}}",
                                             "{{BT-1 | percent}}",
                                             "{{BT-1 | date}}" })
    {
      final String sOut = _render (sTag, aInvoice);
      assertEquals (sTag, -1, sOut.indexOf (MARKER));
      assertFalse (sTag, sOut.contains ("EF%B7%90"));
    }
  }

  @Test
  public void testMarkerIsRemovedBeforeEscaping ()
  {
    // Otherwise the character would hide the block marker from the escaping
    assertEquals ("\\# x", _render ("{{BT-1}}", _invoice (MARKER + "# x")));
    assertEquals ("\\- x", _render ("{{BT-1}}", _invoice ("  " + MARKER + "- x")));
  }

  @Test
  public void testBlockInjection ()
  {
    for (final String sValue : new String [] { "   # Total due 0.00 EUR",
                                               "\t# Total",
                                               "    indented code",
                                               "\t\tindented code",
                                               "\n\n    code",
                                               "  - item",
                                               "   + item",
                                               "   1. item",
                                               "  2) item",
                                               "1.\titem",
                                               "   ---",
                                               "- - -",
                                               " * * *",
                                               " ___",
                                               "   ```",
                                               "   ~~~",
                                               "   > quote",
                                               "   | a |",
                                               "   <div>",
                                               "   [a]: http://evil" })
    {
      _assertSingleParagraph ("{{BT-1}}", sValue);
      _assertSingleParagraph ("line\n{{BT-1}}\n", sValue);
    }
    // The line below a line of text would turn that line into a heading or a table
    for (final String sValue : new String [] { "===", "   ===", "---", "   ---", ":---:", "   :--" })
      _assertSingleParagraph ("line\n{{BT-1}}\n", sValue);
    _assertSingleParagraph ("| a |\n{{BT-1}}\n", ":--");
    _assertSingleParagraph ("| a |\n{{BT-1}}\n", "---");
  }

  @Test
  public void testLineBreaksStayInTheValue ()
  {
    assertEquals ("a<br>  \\# b", _render ("{{BT-1}}", _invoice ("a\n  # b")));
    assertEquals ("a<br>b", _render ("{{BT-1}}", _invoice ("\n\n  a\r\nb")));
  }

  @Test
  public void testLinkDestination ()
  {
    final String sURL = "http://example.org/a)b(c_d*e";
    final Node aLink = _ast ("[x]({{BT-1}})", sURL).getRoot ().getFirstChild ().getFirstChild ();
    assertTrue (String.valueOf (aLink), aLink instanceof Link);
    assertEquals (sURL, ((Link) aLink).getDestination ());

    // The value cannot end the destination and go on with Markdown of its own
    final Node aEvil = _ast ("[x]({{BT-1}})", "y)**z").getRoot ().getFirstChild ().getFirstChild ();
    assertEquals ("y)**z", ((Link) aEvil).getDestination ());
    assertNull (aEvil.getNext ());
  }

  @Test
  public void testCodeSpan ()
  {
    for (final String sValue : new String [] { "DE75_5121", "a`b", "`a", "a`", "```", "a``b`c", "a\\b", "x*y*" })
    {
      final Node aCode = _ast ("{{BT-1 | codespan}}", sValue).getRoot ().getFirstChild ().getFirstChild ();
      assertTrue (sValue, aCode instanceof Code);
      assertEquals (sValue, ((Code) aCode).getLiteral ());
      assertNull (sValue, aCode.getNext ());
    }
    final Node aMultiLine = _ast ("{{BT-1 | codespan}}", "a\n\nb").getRoot ().getFirstChild ();
    assertEquals ("a  b", ((Code) aMultiLine.getFirstChild ()).getLiteral ());
    assertEquals ("", _render ("{{BT-10 | codespan}}", _invoice ("1")));
  }

  @Test
  public void testDefaultTemplateCodeSpans ()
  {
    final PhloxInvoice aInvoice = _invoice ("1");
    final PhloxPaymentInstructions aPayment = new PhloxPaymentInstructions ();
    aPayment.creditTransfers ().add (new PhloxCreditTransfer ().setAccountIdentifier ("DE_75`12"));
    aInvoice.setPaymentInstructions (aPayment);
    final String sOut = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, new TestCtx ());
    assertTrue (sOut, sOut.contains ("``DE_75`12``"));
  }

  @Test
  public void testParentOfEmptyValue ()
  {
    final PhloxInvoice aInvoice = _invoice ("INV");
    // BT-10 has no value
    assertEquals ("[]", _render ("[{{..BT-10}}]", aInvoice));
    assertEquals ("[]", _render ("[{{#BG-25}}{{..BT-10}}{{/BG-25}}]", aInvoice));
    assertEquals ("[]", _render ("[{{#BG-25}}{{..BG-14.BT-73}}{{/BG-25}}]", aInvoice));
    assertEquals ("[INV]", _render ("[{{#BG-25}}{{..BT-1}}{{/BG-25}}]", aInvoice));
  }

  @Test
  public void testSectionOnSingleObject ()
  {
    final PhloxInvoice aInvoice = _invoice ("INV");
    assertEquals ("119.00", _render ("{{#BG-22}}{{BT-115}}{{/BG-22}}", aInvoice));
    assertEquals ("Seller/INV", _render ("{{#BG-4}}{{BT-27}}/{{..BT-1}}{{/BG-4}}", aInvoice));
    assertEquals ("", _render ("{{#BG-22}}{{BT-1}}{{/BG-22}}", aInvoice));

    // A nested path
    aInvoice.getSeller ().setContact (new PhloxContact ().setTelephone ("123"));
    assertEquals ("123", _render ("{{#BG-4.BG-6}}{{BT-42}}{{/BG-4.BG-6}}", aInvoice));
    // No object, and an object without any content
    assertEquals ("", _render ("{{#BG-14}}x{{/BG-14}}", aInvoice));
    assertEquals ("y", _render ("{{^BG-14}}y{{/BG-14}}", aInvoice));
    aInvoice.setInvoicingPeriod (new PhloxInvoicingPeriod ());
    assertEquals ("", _render ("{{#BG-14}}x{{/BG-14}}", aInvoice));
    assertEquals ("y", _render ("{{^BG-14}}y{{/BG-14}}", aInvoice));
    assertEquals ("", _render ("{{?BG-14}}x{{/BG-14}}", aInvoice));
    aInvoice.getInvoicingPeriod ().setStartDate (LocalDate.of (2026, 1, 1));
    assertEquals ("x", _render ("{{#BG-14}}x{{/BG-14}}", aInvoice));
    assertEquals ("x", _render ("{{?BG-14}}x{{/BG-14}}", aInvoice));

    // The loop names of JMustache are not touched
    assertEquals ("[1]", _render ("{{#BG-25}}{{#-first}}[{{-index}}]{{/-first}}{{/BG-25}}", aInvoice));
  }

  @Test
  public void testUnknownNames ()
  {
    for (final String sTemplate : new String [] { "{{FOO}}",
                                                  "{{ FOO | raw }}",
                                                  "{{?FOO}}x{{/FOO}}",
                                                  "{{#FOO}}x{{/FOO}}",
                                                  "{{^FOO}}x{{/FOO}}",
                                                  "{{BT-1.FOO}}",
                                                  "{{BG-22.FOO}}",
                                                  "{{..FOO}}",
                                                  "{{{FOO}}}",
                                                  "{{&FOO}}",
                                                  "{{BT-99999}}",
                                                  "{{.}}",
                                                  "{{this}}",
                                                  "{{BT-1 | nope}}",
                                                  "{{BT-10 | nope}}",
                                                  "{{#BG-25}}{{BT-126 | amount | nope:x}}{{/BG-25}}",
                                                  "{{> partial}}",
                                                  "{{>partial}}",
                                                  "{{=<% %>=}}" })
      _assertLoadError (sTemplate);

    // Known names without a value stay empty, wherever they are used
    final PhloxInvoice aInvoice = _invoice ("INV");
    assertEquals ("[]", _render ("[{{BT-10}}]", aInvoice));
    assertEquals ("[]", _render ("[{{BT-115}}]", aInvoice));
    assertEquals ("[]", _render ("[{{BG-14.BT-73 | date}}]", aInvoice));
    assertEquals ("[]", _render ("[{{BT-10 | upper | raw}}]", aInvoice));
    // Comments are not looked at
    assertEquals ("x", _render ("{{!\n  a {{FOO}} in a comment\n}}x", aInvoice));
  }

  @Test
  public void testLoadFailuresHaveStageLoad ()
  {
    assertEquals (EStage.LOAD,
                  assertThrows (PhloxTemplateException.class,
                                () -> PhloxTemplate.fromClasspath ("no/such/template.ptpl")).getStage ());
    assertEquals (EStage.LOAD,
                  assertThrows (PhloxTemplateException.class,
                                () -> PhloxTemplate.fromFile (new File ("no/such/template.ptpl"))).getStage ());
    // Unbalanced tags are found when the template is compiled
    final PhloxTemplate aBroken = PhloxTemplate.fromString ("{{#BG-25}}x");
    assertEquals (EStage.LOAD,
                  assertThrows (PhloxTemplateException.class, () -> aBroken.localize (new TestCtx ())).getStage ());
  }

  @Test
  public void testAnnotateBtIDs ()
  {
    final PhloxTemplate aTemplate = PhloxTemplate.fromString ("{{label:BT-1}}, {{label:BG-4}}, {{label:section.notes.title}}");
    final PhloxInvoice aInvoice = _invoice ("INV");
    final String sPlain = aTemplate.renderSubstituted (aInvoice, new TestCtx ());
    assertEquals ("Invoice number, Seller, Notes", sPlain);
    // The same template, so this must not be served from the cache
    final PhloxTemplateAst aAst = aTemplate.renderAst (aInvoice, new TestCtx ().setAnnotateBtIDs (true));
    assertEquals ("Invoice number [BT-1], Seller [BG-4], Notes",
                  ((Text) aAst.getRoot ().getFirstChild ().getFirstChild ()).getLiteral ());
    assertEquals (sPlain, aTemplate.renderSubstituted (aInvoice, new TestCtx ()));
  }

  @Test
  public void testDefaultTemplateContactWithoutName ()
  {
    final PhloxInvoice aInvoice = _invoice ("INV");
    aInvoice.getSeller ().setContact (new PhloxContact ().setTelephone ("+43 1").setEmail ("a@example.org"));
    aInvoice.getBuyer ().setContact (new PhloxContact ().setEmail ("b@example.org"));
    final String sOut = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, new TestCtx ());
    assertTrue (sOut, sOut.contains ("\n\\+43 1 · [a@example.org](mailto:a@example.org)\n"));
    assertTrue (sOut, sOut.contains ("\n[b@example.org](mailto:b@example.org)\n"));

    aInvoice.getSeller ().setContact (new PhloxContact ().setName ("Jo").setEmail ("a@example.org"));
    aInvoice.getBuyer ()
            .setContact (new PhloxContact ().setName ("Al").setTelephone ("+43 2").setEmail ("b@example.org"));
    final String sAll = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, new TestCtx ());
    assertTrue (sAll, sAll.contains ("\nJo · [a@example.org](mailto:a@example.org)\n"));
    assertTrue (sAll, sAll.contains ("\nAl · \\+43 2 · [b@example.org](mailto:b@example.org)\n"));
  }

  @Test
  public void testDefaultTemplateClassificationVersion ()
  {
    final PhloxInvoice aInvoice = _invoice ("INV");
    final PhloxItem aItem = aInvoice.lines ().get (0).getItem ();
    aItem.classifications ()
         .add (new PhloxItemClassification ().setCode ("123").setListID ("STI").setListVersionID ("2.1"));
    aItem.classifications ().add (new PhloxItemClassification ().setCode ("456").setListID ("ZZZ"));
    final String sOut = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, new TestCtx ());
    assertTrue (sOut, sOut.contains ("STI:123, ZZZ:456 (Item classification identifier scheme version identifier: STI:2.1)"));
  }

  @Test
  public void testDefaultTemplateVatAccountingCurrencyAndVatPointDateCode ()
  {
    final PhloxInvoice aInvoice = _invoice ("INV");
    aInvoice.setVatAccountingCurrency ("SEK");
    aInvoice.setVatPointDateCode ("35");
    final String sOut = PhloxTemplate.getDefault ().renderSubstituted (aInvoice, new TestCtx ());
    assertTrue (sOut, sOut.contains ("| SEK |"));
    assertTrue (sOut, sOut.contains ("| Delivery date"));
    assertFalse (sOut, sOut.contains ("| 35 |"));
  }

  @Test
  public void testDefaultTemplateIsComplete ()
  {
    assertEquals ("[]", PhloxTemplate.getDefault ().getMissingBTs ().toString ());
    assertTrue (PhloxModelRegistry.getAllBTs ().contains ("BT-158-2"));
    // BT-6 is printed on its own, not only as the currency of BT-111
    assertTrue (PhloxTemplateCoverage.getMissingBTs ("{{BT-111 | amount}}").contains ("BT-6"));
    PhloxTemplate.getDefault ().checkCoverage (EPhloxTemplateCoverageMode.REJECT);
  }
}
