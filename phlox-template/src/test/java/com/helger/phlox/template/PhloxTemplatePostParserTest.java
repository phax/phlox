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
package com.helger.phlox.template;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Locale;

import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.helger.base.wrapper.Wrapper;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.template.PhloxTemplateException.EStage;
import com.helger.phlox.template.adapter.PhloxInvoiceMustacheContext;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxPageBreakNode;
import com.helger.phlox.template.ast.PhloxResolvedImage;
import com.helger.phlox.template.ast.PhloxSpanNode;
import com.helger.phlox.template.ast.PhloxTemplateAst;

/**
 * Test class for class {@link PhloxTemplatePostParser}.
 *
 * @author Philip Helger
 */
public final class PhloxTemplatePostParserTest
{
  private static final class TestCtx extends AbstractRenderContext <TestCtx>
  {}

  @NonNull
  private static PhloxInvoice _invoice ()
  {
    final PhloxInvoice ret = new PhloxInvoice ();
    ret.setNumber ("INV");
    return ret;
  }

  @NonNull
  private static PhloxTemplateAst _ast (@NonNull final String sTemplate)
  {
    return PhloxTemplate.fromString (sTemplate).renderAst (_invoice (), new TestCtx ());
  }

  private static int _countPageBreaks (@NonNull final Node aParent)
  {
    int ret = 0;
    for (Node aChild = aParent.getFirstChild (); aChild != null; aChild = aChild.getNext ())
      if (aChild instanceof PhloxPageBreakNode)
        ret++;
      else
        if (aChild instanceof PhloxGroupNode)
          ret += _countPageBreaks (aChild);
    return ret;
  }

  private static int _countPageBreaks (@NonNull final String sTemplate)
  {
    return _countPageBreaks (_ast (sTemplate).getRoot ());
  }

  @NonNull
  private static PhloxSpanNode _firstSpan (@NonNull final String sTemplate)
  {
    Node aNode = _ast (sTemplate).getRoot ().getFirstChild ().getFirstChild ();
    while (aNode != null && !(aNode instanceof PhloxSpanNode))
      aNode = aNode.getNext ();
    assertNotNull (aNode);
    return (PhloxSpanNode) aNode;
  }

  @Test
  public void testColorSpans ()
  {
    final PhloxSpanNode aColor = _firstSpan ("a {{color:#cc0000}}b **c**{{endcolor}} d");
    assertEquals ("cc0000", aColor.getTextColor ());
    assertNull (aColor.getBackgroundColor ());
    assertEquals ("b ", ((Text) aColor.getFirstChild ()).getLiteral ());
    assertTrue (aColor.getLastChild () instanceof StrongEmphasis);
    assertEquals (" d", ((Text) aColor.getNext ()).getLiteral ());

    final PhloxSpanNode aOuter = _firstSpan ("{{bg:#ffee00}}a{{color:#cc0000}}b{{endcolor}}c{{endbg}}");
    assertEquals ("ffee00", aOuter.getBackgroundColor ());
    assertNull (aOuter.getTextColor ());
    assertEquals ("cc0000", ((PhloxSpanNode) aOuter.getFirstChild ().getNext ()).getTextColor ());
    assertEquals ("c", ((Text) aOuter.getLastChild ()).getLiteral ());

    // A span without an end ends with its block and keeps the content
    assertEquals ("a", ((Text) _firstSpan ("{{color:#cc0000}}a\n\nb").getFirstChild ()).getLiteral ());

    // An end without a start and an image text have no effect, and leave no sentinel behind
    final Node aParagraph = _ast ("a{{endcolor}}b").getRoot ().getFirstChild ();
    assertEquals ("a", ((Text) aParagraph.getFirstChild ()).getLiteral ());
    assertEquals ("b", ((Text) aParagraph.getLastChild ()).getLiteral ());
    final Wrapper <String> aAlt = new Wrapper <> ("");
    _ast ("![{{color:#cc0000}}x{{endcolor}}](qrcode:x)").getRoot ().accept (new AbstractVisitor ()
    {
      @Override
      public void visit (final Text aText)
      {
        aAlt.set (aAlt.get () + aText.getLiteral ());
      }
    });
    assertEquals ("x", aAlt.get ());
  }

  @Test
  public void testColorMustBeValid ()
  {
    for (final String sTag : new String [] { "{{color:red}}", "{{color:#ccc}}", "{{bg:#gg0000}}", "{{bg:}}" })
      assertEquals (sTag,
                    EStage.LOAD,
                    assertThrows (sTag, PhloxTemplateException.class, () -> PhloxTemplate.fromString (sTag)).getStage ());
  }

  @Test
  public void testInvoiceValueCannotOpenASpan ()
  {
    final PhloxInvoice aInvoice = _invoice ();
    aInvoice.setNumber (PhloxInvoiceMustacheContext.SENTINEL_COLOR_PREFIX + "cc0000" + PhloxInvoiceMustacheContext.SENTINEL_CLASS_SUFFIX + "x");
    final Node aParagraph = PhloxTemplate.fromString ("{{BT-1}}").renderAst (aInvoice, new TestCtx ()).getRoot ().getFirstChild ();
    assertTrue (aParagraph.getFirstChild () instanceof Text);
    assertNull (aParagraph.getFirstChild ().getNext ());
  }

  @Test
  public void testPageBreaksAtTheEdgesAreRemoved ()
  {
    final String sBreak = "{{pagebreak}}\n\n";
    assertEquals (1, _countPageBreaks ("a\n\n" + sBreak + "b\n"));
    assertEquals (2, _countPageBreaks ("a\n\n" + sBreak + sBreak + "b\n"));

    assertEquals (0, _countPageBreaks (sBreak + "a\n"));
    assertEquals (0, _countPageBreaks ("a\n\n" + sBreak));
    assertEquals (0, _countPageBreaks (sBreak + sBreak + "a\n\n" + sBreak + sBreak));

    // In a group at the edge, and a group that has nothing else is gone
    assertEquals (0, _countPageBreaks ("{{group:g}}\n\n" + sBreak + "a\n\n" + sBreak + "{{end}}\n"));
    assertEquals (0, _countPageBreaks ("a\n\n{{group:g}}\n\n" + sBreak + "{{end}}\n\n" + sBreak));
    assertEquals (1, _countPageBreaks ("{{group:g}}\n\na\n\n" + sBreak + "b\n\n{{end}}\n"));

    // Nothing else in the template is still an error
    final PhloxTemplateException ex = assertThrows (PhloxTemplateException.class, () -> _ast (sBreak));
    assertEquals (EStage.PARSE, ex.getStage ());
  }

  @NonNull
  private static Image _findImage (@NonNull final PhloxTemplateAst aAst)
  {
    final Wrapper <Image> aFound = new Wrapper <> ();
    aAst.getRoot ().accept (new AbstractVisitor ()
    {
      @Override
      public void visit (final Image aImage)
      {
        aFound.set (aImage);
      }
    });
    assertNotNull (aFound.get ());
    return aFound.get ();
  }

  @NonNull
  private static String _collectText (@NonNull final PhloxTemplateAst aAst)
  {
    final StringBuilder aSB = new StringBuilder ();
    aAst.getRoot ().accept (new AbstractVisitor ()
    {
      @Override
      public void visit (final Text aText)
      {
        aSB.append (aText.getLiteral ());
      }
    });
    return aSB.toString ();
  }

  private static void _assertParseError (@NonNull final String sTemplate)
  {
    final PhloxTemplateException ex = assertThrows (sTemplate, PhloxTemplateException.class, () -> _ast (sTemplate));
    assertEquals (sTemplate, EStage.PARSE, ex.getStage ());
  }

  @Test
  public void testGroupDepthCap ()
  {
    final int nMax = PhloxTemplatePostParser.MAX_GROUP_DEPTH;
    final PhloxTemplateAst aAst = _ast ("{{group:a}}\n\n".repeat (nMax) + "x\n");
    Node aNode = aAst.getRoot ();
    for (int i = 0; i < nMax; ++i)
    {
      aNode = aNode.getFirstChild ();
      assertTrue (aNode instanceof PhloxGroupNode);
    }
    _assertParseError ("{{group:a}}\n\n".repeat (nMax + 1) + "x\n");
    _assertParseError ("{{row:a}}\n\n".repeat (nMax + 1) + "x\n");
    // Closed groups do not count
    assertNotNull (_ast ("{{group:a}}\n\n{{end}}\n\n".repeat (nMax * 2) + "x\n"));
  }

  @Test
  public void testLayoutTagNextToText ()
  {
    for (final String sTemplate : new String [] { "{{group:a}} text\n\nmore\n",
                                                  "text {{end}}\n",
                                                  "- {{row:a}}\n\nmore\n",
                                                  "> {{group:a}}\n\nmore\n",
                                                  "# {{class:a}}Title\n\nmore\n",
                                                  "`{{group:a}}`\n\nmore\n" })
    {
      final String sText = _collectText (_ast (sTemplate));
      assertEquals (sTemplate, -1, sText.indexOf (PhloxInvoiceMustacheContext.SENTINEL_MARKER));
      assertFalse (sTemplate, sText.contains ("PHLOX:"));
    }
    // A paragraph with nothing but a dropped tag is gone
    final Node aRoot = _ast ("{{group:a}}{{class:b}}\n\nmore\n").getRoot ();
    assertTrue (aRoot.getFirstChild () instanceof Paragraph);
    assertTrue (aRoot.getFirstChild ().getFirstChild () instanceof Text);
    assertEquals ("more", ((Text) aRoot.getFirstChild ().getFirstChild ()).getLiteral ());
  }

  @Test
  public void testNoContent ()
  {
    for (final String sTemplate : new String [] { "",
                                                  "\n\n",
                                                  "{{! only a comment }}",
                                                  "{{class:a}}",
                                                  "{{group:a}}\n\n{{end}}\n",
                                                  "{{row:a}}\n\n{{group:b}}\n\n{{end}}\n\n{{end}}\n",
                                                  "{{pagebreak}}",
                                                  "{{?BT-10}}x{{/BT-10}}" })
      _assertParseError (sTemplate);
  }

  @Test
  public void testMultipleClasses ()
  {
    final PhloxTemplateAst aSpace = _ast ("{{class:a b}}\n\ntext\n");
    assertEquals ("a b", aSpace.getBlockClass (aSpace.getRoot ().getFirstChild ()));
    assertEquals ("[a, b]", aSpace.getBlockClasses (aSpace.getRoot ().getFirstChild ()).toString ());

    final PhloxTemplateAst aTwoTags = _ast ("{{class:a}}\n{{class:b}}\n\ntext\n");
    assertEquals ("[a, b]", aTwoTags.getBlockClasses (aTwoTags.getRoot ().getFirstChild ()).toString ());

    final PhloxTemplateAst aGroup = _ast ("{{group:a b}}\n\ntext\n\n{{end}}\n");
    assertEquals ("[a, b]", aGroup.getBlockClasses (aGroup.getRoot ().getFirstChild ()).toString ());
    assertTrue (aGroup.getBlockClasses (aGroup.getRoot ().getFirstChild ().getFirstChild ()).isEmpty ());
  }

  @Test
  public void testClasspathImages ()
  {
    final PhloxTemplateAst aAst = _ast ("![a](classpath:logo/phoss.png)");
    final PhloxResolvedImage aImage = aAst.getResolvedImage (_findImage (aAst));
    assertEquals ("image/png", aImage.getMimeCode ());

    _assertParseError ("![a](classpath:phlox-labels_en.properties)");
    _assertParseError ("![a](classpath:templates/default.ptpl)");
    _assertParseError ("![a](classpath:logo/phoss.png.txt)");
    _assertParseError ("![a](classpath:logo/does-not-exist.png)");
  }

  @Test
  public void testDataImages ()
  {
    final byte [] aBytes = { 1, 2, 3, (byte) 0xff };
    final String sBase64 = Base64.getEncoder ().encodeToString (aBytes);
    final PhloxTemplateAst aAst = _ast ("![Stamp](data:image/png;base64," + sBase64 + ")");
    final PhloxResolvedImage aImage = aAst.getResolvedImage (_findImage (aAst));
    assertNotNull (aImage);
    assertArrayEquals (aBytes, aImage.getBinary ());
    assertEquals ("image/png", aImage.getMimeCode ());
    assertEquals ("Stamp", aImage.getAltText ());

    // Parameters and percent encoding
    final PhloxTemplateAst aText = _ast ("![a](data:image/svg+xml;charset=utf-8,%3Csvg%2F%3E)");
    final PhloxResolvedImage aSvg = aText.getResolvedImage (_findImage (aText));
    assertEquals ("image/svg+xml", aSvg.getMimeCode ());
    assertEquals ("<svg/>", new String (aSvg.getBinary (), StandardCharsets.UTF_8));

    _assertParseError ("![a](data:image/png;base64,%%%)");
    _assertParseError ("![a](data:text/html;base64,PGI+)");
    _assertParseError ("![a](data:image/png;base64)");
    _assertParseError ("![a](data:image/png;base64,)");
    _assertParseError ("![a](data:,x)");
  }

  @Test
  public void testLogoAlternativeTextIsLocalised ()
  {
    final byte [] aBytes = { 1 };
    final TestCtx aCtx = new TestCtx ().setLogo (new PhloxAttachment ().setBinary (aBytes).setMimeCode ("image/png"))
                                       .setLocale (Locale.forLanguageTag ("es"));
    final PhloxTemplateAst aAst = PhloxTemplate.getDefault ().renderAst (_invoice (), aCtx);
    assertEquals ("Logotipo", aAst.getResolvedImage (_findImage (aAst)).getAltText ());
  }
}
