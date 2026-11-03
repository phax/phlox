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

import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
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
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class of all writers that walk the {@link PhloxTemplateAst}. It owns the one and only
 * mapping from a commonmark / Phlox node type to a handler method, so that a writer cannot silently
 * miss a node type: every node type has an abstract <code>visitXxx</code> method, and a node type
 * that is added here fails to compile in every writer until the writer handles it.
 * <p>
 * The nodes are split into two groups, because writers treat them differently. Block nodes
 * (headings, paragraphs, lists, tables, groups, ...) are dispatched with
 * {@link #visitBlock(PhloxTemplateAst, Node, Object)} and receive a context of type
 * <code>BLOCKCTXTYPE</code>; inline nodes (text, emphasis, links, ...) are dispatched with
 * {@link #visitInline(PhloxTemplateAst, Node, Object)} and receive a context of type
 * <code>INLINECTXTYPE</code>. The context is whatever the writer passes down while it walks the
 * tree, e.g. the parent element it adds its output to. A writer that does not need two kinds of
 * context uses the same type for both.
 * <p>
 * The children of a node are not visited automatically. A handler that wants its children visited
 * calls {@link #visitBlockChildren(PhloxTemplateAst, Node, Object)} or
 * {@link #visitInlineChildren(PhloxTemplateAst, Node, Object)} itself, after it has set up the
 * context for them. The structural children of a list (list items) and of a table (head, body,
 * rows, cells) are not dispatched at all; the handlers of the list and the table walk them.
 *
 * @author Philip Helger
 * @param <BLOCKCTXTYPE>
 *        The type of the context that is passed to the handlers of block nodes.
 * @param <INLINECTXTYPE>
 *        The type of the context that is passed to the handlers of inline nodes.
 */
public abstract class AbstractPhloxAstVisitor <BLOCKCTXTYPE, INLINECTXTYPE>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPhloxAstVisitor.class);

  /**
   * Handle a text.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aText
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitText (@NonNull PhloxTemplateAst aAst,
                                     @NonNull Text aText,
                                     @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle a soft line break (a line ending inside a paragraph).
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aBreak
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitSoftLineBreak (@NonNull PhloxTemplateAst aAst,
                                              @NonNull SoftLineBreak aBreak,
                                              @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle a hard line break.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aBreak
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitHardLineBreak (@NonNull PhloxTemplateAst aAst,
                                              @NonNull HardLineBreak aBreak,
                                              @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle strong emphasis.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aStrong
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitStrongEmphasis (@NonNull PhloxTemplateAst aAst,
                                               @NonNull StrongEmphasis aStrong,
                                               @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle emphasis.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aEmphasis
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitEmphasis (@NonNull PhloxTemplateAst aAst,
                                         @NonNull Emphasis aEmphasis,
                                         @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle strikethrough.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aStrikethrough
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitStrikethrough (@NonNull PhloxTemplateAst aAst,
                                              @NonNull Strikethrough aStrikethrough,
                                              @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle inline code.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aCode
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitCode (@NonNull PhloxTemplateAst aAst,
                                     @NonNull Code aCode,
                                     @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle a link.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aLink
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitLink (@NonNull PhloxTemplateAst aAst,
                                     @NonNull Link aLink,
                                     @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle an image that is part of the text of a block.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aImage
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitInlineImage (@NonNull PhloxTemplateAst aAst,
                                            @NonNull Image aImage,
                                            @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle raw HTML inside the text of a block.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aHtml
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitHtmlInline (@NonNull PhloxTemplateAst aAst,
                                           @NonNull HtmlInline aHtml,
                                           @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle a heading.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aHeading
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitHeading (@NonNull PhloxTemplateAst aAst,
                                        @NonNull Heading aHeading,
                                        @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a paragraph.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aParagraph
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitParagraph (@NonNull PhloxTemplateAst aAst,
                                          @NonNull Paragraph aParagraph,
                                          @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a bullet list. The handler walks the list items itself.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aList
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitBulletList (@NonNull PhloxTemplateAst aAst,
                                           @NonNull BulletList aList,
                                           @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle an ordered list. The handler walks the list items itself.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aList
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitOrderedList (@NonNull PhloxTemplateAst aAst,
                                            @NonNull OrderedList aList,
                                            @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a block quote.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aQuote
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitBlockQuote (@NonNull PhloxTemplateAst aAst,
                                           @NonNull BlockQuote aQuote,
                                           @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a fenced code block.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aBlock
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitFencedCodeBlock (@NonNull PhloxTemplateAst aAst,
                                                @NonNull FencedCodeBlock aBlock,
                                                @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle an indented code block.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aBlock
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitIndentedCodeBlock (@NonNull PhloxTemplateAst aAst,
                                                  @NonNull IndentedCodeBlock aBlock,
                                                  @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a thematic break (horizontal rule).
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aBreak
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitThematicBreak (@NonNull PhloxTemplateAst aAst,
                                              @NonNull ThematicBreak aBreak,
                                              @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a table. The handler walks the table head, body, rows and cells itself.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aTable
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitTable (@NonNull PhloxTemplateAst aAst,
                                      @NonNull TableBlock aTable,
                                      @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle an image that stands on its own, outside of the text of a block.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aImage
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitBlockImage (@NonNull PhloxTemplateAst aAst,
                                           @NonNull Image aImage,
                                           @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a block of raw HTML.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aHtml
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitHtmlBlock (@NonNull PhloxTemplateAst aAst,
                                          @NonNull HtmlBlock aHtml,
                                          @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a <code>{{group:}}</code> or <code>{{row:}}</code> block of the template.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aGroup
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitGroup (@NonNull PhloxTemplateAst aAst,
                                      @NonNull PhloxGroupNode aGroup,
                                      @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle a text colour or a background colour of the template. The implementation visits the
   * children with {@link #visitInlineChildren}.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aSpan
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitSpan (@NonNull PhloxTemplateAst aAst,
                                     @NonNull PhloxSpanNode aSpan,
                                     @NonNull INLINECTXTYPE aCtx);

  /**
   * Handle a <code>{{pagebreak}}</code> of the template.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aPageBreak
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected abstract void visitPageBreak (@NonNull PhloxTemplateAst aAst,
                                          @NonNull PhloxPageBreakNode aPageBreak,
                                          @NonNull BLOCKCTXTYPE aCtx);

  /**
   * Handle an inline node of a type that has no <code>visitXxx</code> method, e.g. because a
   * commonmark extension was added to the parser. The default implementation logs a warning and
   * visits the children, so that their content is kept.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aNode
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected void visitUnknownInline (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final Node aNode,
                                     @NonNull final INLINECTXTYPE aCtx)
  {
    LOGGER.warn ("Unsupported template node " + aNode.getClass ().getSimpleName ());
    visitInlineChildren (aAst, aNode, aCtx);
  }

  /**
   * Handle a block node of a type that has no <code>visitXxx</code> method, e.g. a link reference
   * definition or a node of a commonmark extension that was added to the parser. The default
   * implementation logs a warning and visits the children, so that their content is kept.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aNode
   *        The node to handle. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected void visitUnknownBlock (@NonNull final PhloxTemplateAst aAst,
                                    @NonNull final Node aNode,
                                    @NonNull final BLOCKCTXTYPE aCtx)
  {
    LOGGER.warn ("Unsupported template node " + aNode.getClass ().getSimpleName ());
    visitBlockChildren (aAst, aNode, aCtx);
  }

  /**
   * Hand an inline node to the <code>visitXxx</code> method of its type.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aNode
   *        The node to dispatch. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected final void visitInline (@NonNull final PhloxTemplateAst aAst,
                                    @NonNull final Node aNode,
                                    @NonNull final INLINECTXTYPE aCtx)
  {
    if (aNode instanceof final Text aText)
      visitText (aAst, aText, aCtx);
    else
      if (aNode instanceof final SoftLineBreak aBreak)
        visitSoftLineBreak (aAst, aBreak, aCtx);
      else
        if (aNode instanceof final HardLineBreak aBreak)
          visitHardLineBreak (aAst, aBreak, aCtx);
        else
          if (aNode instanceof final StrongEmphasis aStrong)
            visitStrongEmphasis (aAst, aStrong, aCtx);
          else
            if (aNode instanceof final Emphasis aEmphasis)
              visitEmphasis (aAst, aEmphasis, aCtx);
            else
              if (aNode instanceof final Strikethrough aStrikethrough)
                visitStrikethrough (aAst, aStrikethrough, aCtx);
              else
                if (aNode instanceof final Code aCode)
                  visitCode (aAst, aCode, aCtx);
                else
                  if (aNode instanceof final Link aLink)
                    visitLink (aAst, aLink, aCtx);
                  else
                    if (aNode instanceof final Image aImage)
                      visitInlineImage (aAst, aImage, aCtx);
                    else
                      if (aNode instanceof final HtmlInline aHtml)
                        visitHtmlInline (aAst, aHtml, aCtx);
                      else
                        if (aNode instanceof final PhloxSpanNode aSpan)
                          visitSpan (aAst, aSpan, aCtx);
                        else
                          visitUnknownInline (aAst, aNode, aCtx);
  }

  /**
   * Visit all children of a node as inline nodes, in document order.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aParent
   *        The node whose children are visited. Never <code>null</code>.
   * @param aCtx
   *        The context that is passed to every child. Never <code>null</code>.
   */
  protected final void visitInlineChildren (@NonNull final PhloxTemplateAst aAst,
                                            @NonNull final Node aParent,
                                            @NonNull final INLINECTXTYPE aCtx)
  {
    for (Node aChild = aParent.getFirstChild (); aChild != null; aChild = aChild.getNext ())
      visitInline (aAst, aChild, aCtx);
  }

  /**
   * Hand a block node to the <code>visitXxx</code> method of its type.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aNode
   *        The node to dispatch. Never <code>null</code>.
   * @param aCtx
   *        The context. Never <code>null</code>.
   */
  protected final void visitBlock (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final Node aNode,
                                   @NonNull final BLOCKCTXTYPE aCtx)
  {
    if (aNode instanceof final Heading aHeading)
      visitHeading (aAst, aHeading, aCtx);
    else
      if (aNode instanceof final Paragraph aParagraph)
        visitParagraph (aAst, aParagraph, aCtx);
      else
        if (aNode instanceof final BulletList aList)
          visitBulletList (aAst, aList, aCtx);
        else
          if (aNode instanceof final OrderedList aList)
            visitOrderedList (aAst, aList, aCtx);
          else
            if (aNode instanceof final BlockQuote aQuote)
              visitBlockQuote (aAst, aQuote, aCtx);
            else
              if (aNode instanceof final FencedCodeBlock aBlock)
                visitFencedCodeBlock (aAst, aBlock, aCtx);
              else
                if (aNode instanceof final IndentedCodeBlock aBlock)
                  visitIndentedCodeBlock (aAst, aBlock, aCtx);
                else
                  if (aNode instanceof final ThematicBreak aBreak)
                    visitThematicBreak (aAst, aBreak, aCtx);
                  else
                    if (aNode instanceof final TableBlock aTable)
                      visitTable (aAst, aTable, aCtx);
                    else
                      if (aNode instanceof final Image aImage)
                        visitBlockImage (aAst, aImage, aCtx);
                      else
                        if (aNode instanceof final HtmlBlock aHtml)
                          visitHtmlBlock (aAst, aHtml, aCtx);
                        else
                          if (aNode instanceof final PhloxGroupNode aGroup)
                            visitGroup (aAst, aGroup, aCtx);
                          else
                            if (aNode instanceof final PhloxPageBreakNode aPageBreak)
                              visitPageBreak (aAst, aPageBreak, aCtx);
                            else
                              visitUnknownBlock (aAst, aNode, aCtx);
  }

  /**
   * Visit all children of a node as block nodes, in document order.
   *
   * @param aAst
   *        The template AST. Never <code>null</code>.
   * @param aParent
   *        The node whose children are visited, e.g. the root of the AST. Never <code>null</code>.
   * @param aCtx
   *        The context that is passed to every child. Never <code>null</code>.
   */
  protected final void visitBlockChildren (@NonNull final PhloxTemplateAst aAst,
                                           @NonNull final Node aParent,
                                           @NonNull final BLOCKCTXTYPE aCtx)
  {
    for (Node aChild = aParent.getFirstChild (); aChild != null; aChild = aChild.getNext ())
      visitBlock (aAst, aChild, aCtx);
  }
}
