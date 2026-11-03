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
package com.helger.phlox.template.ast;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.parser.Parser;
import org.junit.Test;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.PhloxInvoice;

/**
 * Test class for class {@link AbstractPhloxAstVisitor}.
 *
 * @author Philip Helger
 */
public final class AbstractPhloxAstVisitorTest
{
  /** Names of all handlers that a visitor has to provide - one per node type. */
  private static final Set <String> ALL_HANDLERS = new TreeSet <> (List.of ("visitText",
                                                                           "visitSoftLineBreak",
                                                                           "visitHardLineBreak",
                                                                           "visitStrongEmphasis",
                                                                           "visitEmphasis",
                                                                           "visitStrikethrough",
                                                                           "visitCode",
                                                                           "visitLink",
                                                                           "visitInlineImage",
                                                                           "visitHtmlInline",
                                                                           "visitHeading",
                                                                           "visitParagraph",
                                                                           "visitBulletList",
                                                                           "visitOrderedList",
                                                                           "visitBlockQuote",
                                                                           "visitFencedCodeBlock",
                                                                           "visitIndentedCodeBlock",
                                                                           "visitThematicBreak",
                                                                           "visitTable",
                                                                           "visitBlockImage",
                                                                           "visitHtmlBlock",
                                                                           "visitGroup",
                                                                           "visitPageBreak"));

  private static final String MARKDOWN = """
                                         # Title

                                         Text *em* **strong** ~~del~~ `code` [link](https://example.org) ![img](x.png) <b>html</b>
                                         soft
                                         hard\\
                                         break

                                         - bullet

                                         1. ordered

                                         > quote

                                         ```
                                         fenced
                                         ```

                                             indented

                                         ---

                                         | a | b |
                                         |---|---|
                                         | c | d |

                                         <div>x</div>
                                         """;

  /** Concrete subclass purely so the abstract base can be instantiated in this test. */
  private static final class TestCtx extends AbstractRenderContext <TestCtx>
  {}

  /**
   * A visitor that records the name of every handler that is called, and visits the children of
   * the nodes that have some. The context is the list of recorded names.
   */
  private static final class RecordingVisitor extends AbstractPhloxAstVisitor <ICommonsList <String>, ICommonsList <String>>
  {
    private void _visitListItems (final PhloxTemplateAst aAst,
                                           final Node aList,
                                           final ICommonsList <String> aCtx)
    {
      for (Node aItem = aList.getFirstChild (); aItem != null; aItem = aItem.getNext ())
        visitBlockChildren (aAst, aItem, aCtx);
    }

    @Override
    protected void visitText (final PhloxTemplateAst aAst, final Text aText, final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitText");
    }

    @Override
    protected void visitSoftLineBreak (final PhloxTemplateAst aAst,
                                       final SoftLineBreak aBreak,
                                       final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitSoftLineBreak");
    }

    @Override
    protected void visitHardLineBreak (final PhloxTemplateAst aAst,
                                       final HardLineBreak aBreak,
                                       final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitHardLineBreak");
    }

    @Override
    protected void visitStrongEmphasis (final PhloxTemplateAst aAst,
                                        final StrongEmphasis aStrong,
                                        final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitStrongEmphasis");
      visitInlineChildren (aAst, aStrong, aCtx);
    }

    @Override
    protected void visitSpan (final PhloxTemplateAst aAst, final PhloxSpanNode aSpan, final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitSpan");
      visitInlineChildren (aAst, aSpan, aCtx);
    }

    @Override
    protected void visitEmphasis (final PhloxTemplateAst aAst,
                                  final Emphasis aEmphasis,
                                  final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitEmphasis");
      visitInlineChildren (aAst, aEmphasis, aCtx);
    }

    @Override
    protected void visitStrikethrough (final PhloxTemplateAst aAst,
                                       final Strikethrough aStrikethrough,
                                       final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitStrikethrough");
      visitInlineChildren (aAst, aStrikethrough, aCtx);
    }

    @Override
    protected void visitCode (final PhloxTemplateAst aAst, final Code aCode, final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitCode");
    }

    @Override
    protected void visitLink (final PhloxTemplateAst aAst, final Link aLink, final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitLink");
      visitInlineChildren (aAst, aLink, aCtx);
    }

    @Override
    protected void visitInlineImage (final PhloxTemplateAst aAst,
                                     final Image aImage,
                                     final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitInlineImage");
    }

    @Override
    protected void visitHtmlInline (final PhloxTemplateAst aAst,
                                    final HtmlInline aHtml,
                                    final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitHtmlInline");
    }

    @Override
    protected void visitHeading (final PhloxTemplateAst aAst,
                                 final Heading aHeading,
                                 final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitHeading");
      visitInlineChildren (aAst, aHeading, aCtx);
    }

    @Override
    protected void visitParagraph (final PhloxTemplateAst aAst,
                                   final Paragraph aParagraph,
                                   final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitParagraph");
      visitInlineChildren (aAst, aParagraph, aCtx);
    }

    @Override
    protected void visitBulletList (final PhloxTemplateAst aAst,
                                    final BulletList aList,
                                    final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitBulletList");
      _visitListItems (aAst, aList, aCtx);
    }

    @Override
    protected void visitOrderedList (final PhloxTemplateAst aAst,
                                     final OrderedList aList,
                                     final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitOrderedList");
      _visitListItems (aAst, aList, aCtx);
    }

    @Override
    protected void visitBlockQuote (final PhloxTemplateAst aAst,
                                    final BlockQuote aQuote,
                                    final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitBlockQuote");
      visitBlockChildren (aAst, aQuote, aCtx);
    }

    @Override
    protected void visitFencedCodeBlock (final PhloxTemplateAst aAst,
                                         final FencedCodeBlock aBlock,
                                         final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitFencedCodeBlock");
    }

    @Override
    protected void visitIndentedCodeBlock (final PhloxTemplateAst aAst,
                                           final IndentedCodeBlock aBlock,
                                           final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitIndentedCodeBlock");
    }

    @Override
    protected void visitThematicBreak (final PhloxTemplateAst aAst,
                                       final ThematicBreak aBreak,
                                       final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitThematicBreak");
    }

    @Override
    protected void visitTable (final PhloxTemplateAst aAst, final TableBlock aTable, final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitTable");
    }

    @Override
    protected void visitBlockImage (final PhloxTemplateAst aAst,
                                    final Image aImage,
                                    final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitBlockImage");
    }

    @Override
    protected void visitHtmlBlock (final PhloxTemplateAst aAst,
                                   final HtmlBlock aHtml,
                                   final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitHtmlBlock");
    }

    @Override
    protected void visitGroup (final PhloxTemplateAst aAst,
                               final PhloxGroupNode aGroup,
                               final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitGroup");
      visitBlockChildren (aAst, aGroup, aCtx);
    }

    @Override
    protected void visitPageBreak (final PhloxTemplateAst aAst,
                                   final PhloxPageBreakNode aPageBreak,
                                   final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitPageBreak");
    }

    @Override
    protected void visitUnknownInline (final PhloxTemplateAst aAst,
                                       final Node aNode,
                                       final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitUnknownInline");
      super.visitUnknownInline (aAst, aNode, aCtx);
    }

    @Override
    protected void visitUnknownBlock (final PhloxTemplateAst aAst,
                                      final Node aNode,
                                      final ICommonsList <String> aCtx)
    {
      aCtx.add ("visitUnknownBlock");
      super.visitUnknownBlock (aAst, aNode, aCtx);
    }

    /**
     * Visit the children of the root of the AST as block nodes.
     *
     * @param aRoot
     *        The root node. May not be <code>null</code>.
     * @return The names of the handlers that were called, in call order.
     */
    ICommonsList <String> visitRoot (final Node aRoot)
    {
      final PhloxTemplateAst aAst = new PhloxTemplateAst (aRoot,
                                                          new HashMap <> (),
                                                          new HashMap <> (),
                                                          new PhloxInvoice (),
                                                          new TestCtx ());
      final ICommonsList <String> ret = new CommonsArrayList <> ();
      visitBlockChildren (aAst, aRoot, ret);
      return ret;
    }
  }

  private static Document _parse (final String sMarkdown)
  {
    final List <Extension> aExtensions = List.of (TablesExtension.create (), StrikethroughExtension.create ());
    return (Document) Parser.builder ().extensions (aExtensions).build ().parse (sMarkdown);
  }

  @Test
  public void testEveryNodeTypeReachesItsHandler ()
  {
    final Document aDoc = _parse (MARKDOWN);
    final PhloxGroupNode aGroup = new PhloxGroupNode (true);
    aGroup.appendChild (new Paragraph ());
    aDoc.appendChild (aGroup);
    aDoc.appendChild (new PhloxPageBreakNode ());
    // An image only is a block level image when it is not part of a paragraph
    aDoc.appendChild (new Image ("x.png", null));

    final ICommonsList <String> aCalls = new RecordingVisitor ().visitRoot (aDoc);

    final Set <String> aExpected = new TreeSet <> (ALL_HANDLERS);
    final Set <String> aActual = new TreeSet <> (aCalls);
    assertEquals (aExpected, aActual);
  }

  @Test
  public void testDocumentOrderAndNesting ()
  {
    final ICommonsList <String> aCalls = new RecordingVisitor ().visitRoot (_parse ("# T\n\ntext *em* end\n\n---\n"));
    assertEquals (List.of ("visitHeading",
                           "visitText",
                           "visitParagraph",
                           "visitText",
                           "visitEmphasis",
                           "visitText",
                           "visitText",
                           "visitThematicBreak"),
                  aCalls);
  }

  @Test
  public void testInlineAndBlockImageAreDistinct ()
  {
    final ICommonsList <String> aInline = new RecordingVisitor ().visitRoot (_parse ("![img](x.png)"));
    assertEquals (List.of ("visitParagraph", "visitInlineImage"), aInline);

    final Document aDoc = new Document ();
    aDoc.appendChild (new Image ("x.png", null));
    assertEquals (List.of ("visitBlockImage"), new RecordingVisitor ().visitRoot (aDoc));
  }

  @Test
  public void testSoftAndHardLineBreakAreDistinct ()
  {
    final ICommonsList <String> aCalls = new RecordingVisitor ().visitRoot (_parse ("a\nb\\\nc"));
    assertTrue (aCalls.contains ("visitSoftLineBreak"));
    assertTrue (aCalls.contains ("visitHardLineBreak"));
  }

  @Test
  public void testUnknownBlockKeepsChildren ()
  {
    // A link reference definition has no handler
    final ICommonsList <String> aRef = new RecordingVisitor ().visitRoot (_parse ("[ref]: http://example.org\n"));
    assertEquals (List.of ("visitUnknownBlock"), aRef);

    // The content of an unknown container is not lost
    final Document aDoc = new Document ();
    final CustomBlock aUnknown = new CustomBlock ()
    {};
    aUnknown.appendChild (new Paragraph ());
    aDoc.appendChild (aUnknown);
    assertEquals (List.of ("visitUnknownBlock", "visitParagraph"), new RecordingVisitor ().visitRoot (aDoc));
  }

  @Test
  public void testUnknownInlineKeepsChildren ()
  {
    final Document aDoc = new Document ();
    final Paragraph aPara = new Paragraph ();
    final CustomNode aUnknown = new CustomNode ()
    {};
    aUnknown.appendChild (new Text ("kept"));
    aPara.appendChild (aUnknown);
    aDoc.appendChild (aPara);
    assertEquals (List.of ("visitParagraph", "visitUnknownInline", "visitText"),
                  new RecordingVisitor ().visitRoot (aDoc));
  }
}
