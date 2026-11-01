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

import java.util.Locale;
import java.util.regex.Pattern;

import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.css.ECSSUnit;
import com.helger.css.property.CCSSProperties;
import com.helger.css.propertyvalue.CCSSValue;
import com.helger.html.hc.IHCNodeWithChildren;
import com.helger.html.hc.html.IHCElement;
import com.helger.html.hc.html.embedded.HCImg;
import com.helger.html.hc.html.grouping.HCBlockQuote;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCHR;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCP;
import com.helger.html.hc.html.grouping.HCPre;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.grouping.IHCList;
import com.helger.html.hc.html.root.HCHtml;
import com.helger.html.hc.html.sections.HCH1;
import com.helger.html.hc.html.sections.HCH2;
import com.helger.html.hc.html.sections.HCH3;
import com.helger.html.hc.html.sections.HCH4;
import com.helger.html.hc.html.sections.HCH5;
import com.helger.html.hc.html.sections.HCH6;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCBR;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.html.textlevel.HCS;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.phlox.api.PhloxLogHelper;
import com.helger.phlox.template.PhloxLinkHelper;
import com.helger.phlox.template.PhloxTemplatePostParser;
import com.helger.phlox.template.ast.AbstractPhloxAstVisitor;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxPageBreakNode;
import com.helger.phlox.template.ast.PhloxResolvedImage;
import com.helger.phlox.template.ast.PhloxTemplateAst;
import com.helger.url.SimpleURL;

/**
 * Turns the {@link PhloxTemplateAst} of a rendered template into an HTML document. The document
 * head, the watermarks and the page wrapper are the same as in {@link HtmlInvoiceWriter}; the
 * content is built by walking the CommonMark tree. A block class that was attached with
 * {@code {{class:name}}} is added to the emitted element, prefixed with
 * {@link HtmlRenderContext#getCssClassPrefix()}.
 *
 * @author Philip Helger
 */
public final class HtmlTemplateEmitter extends
                                       AbstractPhloxAstVisitor <IHCNodeWithChildren <?>, IHCNodeWithChildren <?>>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (HtmlTemplateEmitter.class);

  private static final String LOGO_URL = "attachment:logo";

  private static final Pattern BR_TAG = Pattern.compile ("<br\\s*/?>", Pattern.CASE_INSENSITIVE);

  private final HtmlRenderContext m_aCtx;

  /**
   * Create a new emitter.
   *
   * @param aCtx
   *        The render context that provides the CSS class prefix, the stylesheet options and the
   *        watermarks. Never {@code null}.
   */
  public HtmlTemplateEmitter (@NonNull final HtmlRenderContext aCtx)
  {
    m_aCtx = aCtx;
  }

  private void _addClass (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final Node aNode,
                          @NonNull final IHCElement <?> aElement)
  {
    for (final String sBlockClass : aAst.getBlockClasses (aNode))
      aElement.addClass (HtmlDocumentSupport.cls (m_aCtx.getCssClassPrefix () + sBlockClass));
  }

  @Nullable
  private HCImg _createImage (@NonNull final PhloxTemplateAst aAst, @NonNull final Image aImage)
  {
    final PhloxResolvedImage aResolved = aAst.getResolvedImage (aImage);
    final String sSrc;
    if (aResolved != null)
      sSrc = PhloxTemplatePostParser.toDataUri (aResolved);
    else
    {
      if (LOGO_URL.equals (aImage.getDestination ()) ||
          aImage.getDestination ().startsWith ("attachment:") ||
          aImage.getDestination ().startsWith (PhloxTemplatePostParser.QRCODE_URL_PREFIX))
      {
        // Nothing to show for an attachment or QR code that could not be resolved
        LOGGER.warn ("Template image '" +
                     PhloxLogHelper.sanitise (aImage.getDestination ()) +
                     "' could not be resolved - skipping it");
        return null;
      }
      sSrc = aImage.getDestination ();
    }

    final HCImg aImg = new HCImg ().setSrc (new SimpleURL (sSrc));
    if (aResolved != null && aResolved.getAltText () != null)
      aImg.setAlt (aResolved.getAltText ());
    if (LOGO_URL.equals (aImage.getDestination ()))
      aImg.addClass (HtmlDocumentSupport.cls (m_aCtx.getCssClassPrefix () + "logo"));

    final int nHeightPx = PhloxTemplatePostParser.getImageHeightPx (aImage);
    if (nHeightPx > 0)
    {
      // The explicit height wins over the limits of the logo class
      aImg.addStyle (CCSSProperties.HEIGHT.newValue (ECSSUnit.px (nHeightPx)));
      aImg.addStyle (CCSSProperties.MAX_HEIGHT.newValue (CCSSValue.NONE));
      aImg.addStyle (CCSSProperties.MAX_WIDTH.newValue (CCSSValue.NONE));
    }
    return aImg;
  }

  private void _emitHeading (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final Heading aHeading,
                             @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final String sPrefix = m_aCtx.getCssClassPrefix ();
    final IHCElement <?> aElement;
    final IHCNodeWithChildren <?> aContent;
    switch (aHeading.getLevel ())
    {
      case 1:
      {
        final HCH1 aH = new HCH1 ().addClass (HtmlDocumentSupport.cls (sPrefix + "title"));
        aElement = aH;
        aContent = aH;
        break;
      }
      case 2:
      {
        final HCH2 aH = new HCH2 ().addClass (HtmlDocumentSupport.cls (sPrefix + "section-title"));
        aElement = aH;
        aContent = aH;
        break;
      }
      case 3:
      {
        final HCH3 aH = new HCH3 ().addClass (HtmlDocumentSupport.cls (sPrefix + "subsection-title"));
        aElement = aH;
        aContent = aH;
        break;
      }
      case 4:
      {
        final HCH4 aH = new HCH4 ();
        aElement = aH;
        aContent = aH;
        break;
      }
      case 5:
      {
        final HCH5 aH = new HCH5 ();
        aElement = aH;
        aContent = aH;
        break;
      }
      default:
      {
        final HCH6 aH = new HCH6 ();
        aElement = aH;
        aContent = aH;
        break;
      }
    }
    _addClass (aAst, aHeading, aElement);
    visitInlineChildren (aAst, aHeading, aContent);
    aTarget.addChild (aElement);
  }

  private void _emitTable (@NonNull final PhloxTemplateAst aAst,
                           @NonNull final TableBlock aTable,
                           @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCTable aTbl = new HCTable ().addClass (HtmlDocumentSupport.cls (m_aCtx.getCssClassPrefix () + "table"));
    _addClass (aAst, aTable, aTbl);
    for (Node aPart = aTable.getFirstChild (); aPart != null; aPart = aPart.getNext ())
    {
      final boolean bHead = aPart instanceof TableHead;
      if (!bHead && !(aPart instanceof TableBody))
        continue;

      for (Node aRowNode = aPart.getFirstChild (); aRowNode instanceof TableRow; aRowNode = aRowNode.getNext ())
      {
        final HCRow aRow = bHead ? aTbl.getHead ().addRow () : aTbl.getBody ().addRow ();
        _addClass (aAst, aRowNode, aRow);
        for (Node aCellNode = aRowNode.getFirstChild (); aCellNode instanceof final TableCell aCell; aCellNode = aCellNode.getNext ())
        {
          final IHCCell <?> aHtmlCell = aCell.isHeader () ? aRow.addTH () : aRow.addTD ();
          if (aCell.getAlignment () != null)
            aHtmlCell.addStyle (CCSSProperties.TEXT_ALIGN.newValue (aCell.getAlignment ()
                                                                         .name ()
                                                                         .toLowerCase (Locale.ROOT)));
          visitInlineChildren (aAst, aCell, aHtmlCell);
        }
      }
    }
    aTarget.addChild (aTbl);
  }

  private void _emitList (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final Node aList,
                          @NonNull final IHCList <?, ?> aHtmlList,
                          @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _addClass (aAst, aList, aHtmlList);
    for (Node aItem = aList.getFirstChild (); aItem instanceof ListItem; aItem = aItem.getNext ())
      visitBlockChildren (aAst, aItem, aHtmlList.addItem ());
    aTarget.addChild (aHtmlList);
  }

  private void _emitImage (@NonNull final PhloxTemplateAst aAst,
                           @NonNull final Image aImage,
                           @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCImg aImg = _createImage (aAst, aImage);
    if (aImg != null)
      aTarget.addChild (aImg);
  }

  private void _emitCodeBlock (@NonNull final PhloxTemplateAst aAst,
                               @NonNull final Node aBlock,
                               @NonNull final String sLiteral,
                               @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCPre aPre = new HCPre ();
    _addClass (aAst, aBlock, aPre);
    aTarget.addChild (aPre.addChild (new HCCode ().addChild (sLiteral)));
  }

  @Override
  protected void visitText (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Text aText,
                            @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    aTarget.addChild (new HCTextNode (aText.getLiteral ()));
  }

  @Override
  protected void visitSoftLineBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final SoftLineBreak aBreak,
                                     @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    // A line break in the template is a line break in the output
    aTarget.addChild (new HCBR ());
  }

  @Override
  protected void visitHardLineBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final HardLineBreak aBreak,
                                     @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    // A line break in the template is a line break in the output
    aTarget.addChild (new HCBR ());
  }

  @Override
  protected void visitStrongEmphasis (@NonNull final PhloxTemplateAst aAst,
                                      @NonNull final StrongEmphasis aStrong,
                                      @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCStrong aElement = new HCStrong ();
    visitInlineChildren (aAst, aStrong, aElement);
    aTarget.addChild (aElement);
  }

  @Override
  protected void visitEmphasis (@NonNull final PhloxTemplateAst aAst,
                                @NonNull final Emphasis aEmphasis,
                                @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCEM aElement = new HCEM ();
    visitInlineChildren (aAst, aEmphasis, aElement);
    aTarget.addChild (aElement);
  }

  @Override
  protected void visitStrikethrough (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final Strikethrough aStrikethrough,
                                     @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCS aElement = new HCS ();
    visitInlineChildren (aAst, aStrikethrough, aElement);
    aTarget.addChild (aElement);
  }

  @Override
  protected void visitCode (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Code aCode,
                            @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    aTarget.addChild (new HCCode ().addChild (aCode.getLiteral ()));
  }

  @Override
  protected void visitLink (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Link aLink,
                            @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    if (PhloxLinkHelper.isSafeLink (aLink.getDestination ()))
    {
      final HCA aA = new HCA (new SimpleURL (aLink.getDestination ()));
      visitInlineChildren (aAst, aLink, aA);
      aTarget.addChild (aA);
    }
    else
      visitInlineChildren (aAst, aLink, aTarget);
  }

  @Override
  protected void visitInlineImage (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final Image aImage,
                                   @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitImage (aAst, aImage, aTarget);
  }

  @Override
  protected void visitHtmlInline (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final HtmlInline aHtml,
                                  @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    // Raw HTML of a template is never passed through, except for a line break, which is the only
    // way to break a line inside a table cell
    if (BR_TAG.matcher (aHtml.getLiteral ()).matches ())
      aTarget.addChild (new HCBR ());
    else
      aTarget.addChild (aHtml.getLiteral ());
  }

  @Override
  protected void visitHeading (@NonNull final PhloxTemplateAst aAst,
                               @NonNull final Heading aHeading,
                               @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitHeading (aAst, aHeading, aTarget);
  }

  @Override
  protected void visitParagraph (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final Paragraph aParagraph,
                                 @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCP aP = new HCP ();
    _addClass (aAst, aParagraph, aP);
    visitInlineChildren (aAst, aParagraph, aP);
    aTarget.addChild (aP);
  }

  @Override
  protected void visitBulletList (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BulletList aList,
                                  @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitList (aAst, aList, new HCUL (), aTarget);
  }

  @Override
  protected void visitOrderedList (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final OrderedList aList,
                                   @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCOL aOL = new HCOL ();
    if (aList.getMarkerStartNumber () != null && aList.getMarkerStartNumber ().intValue () != 1)
      aOL.setStart (aList.getMarkerStartNumber ());
    _emitList (aAst, aList, aOL, aTarget);
  }

  @Override
  protected void visitBlockQuote (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BlockQuote aQuote,
                                  @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCBlockQuote aElement = new HCBlockQuote ();
    _addClass (aAst, aQuote, aElement);
    visitBlockChildren (aAst, aQuote, aElement);
    aTarget.addChild (aElement);
  }

  @Override
  protected void visitFencedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                       @NonNull final FencedCodeBlock aBlock,
                                       @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitCodeBlock (aAst, aBlock, aBlock.getLiteral (), aTarget);
  }

  @Override
  protected void visitIndentedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                         @NonNull final IndentedCodeBlock aBlock,
                                         @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitCodeBlock (aAst, aBlock, aBlock.getLiteral (), aTarget);
  }

  @Override
  protected void visitThematicBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final ThematicBreak aBreak,
                                     @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCHR aHR = new HCHR ();
    _addClass (aAst, aBreak, aHR);
    aTarget.addChild (aHR);
  }

  @Override
  protected void visitTable (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final TableBlock aTable,
                             @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitTable (aAst, aTable, aTarget);
  }

  @Override
  protected void visitBlockImage (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final Image aImage,
                                  @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    _emitImage (aAst, aImage, aTarget);
  }

  @Override
  protected void visitHtmlBlock (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final HtmlBlock aHtml,
                                 @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    aTarget.addChild (aHtml.getLiteral ());
  }

  @Override
  protected void visitGroup (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final PhloxGroupNode aGroup,
                             @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    final HCDiv aDiv = new HCDiv ();
    if (aGroup.isRow ())
      aDiv.addClass (HtmlDocumentSupport.cls (m_aCtx.getCssClassPrefix () + "row"));
    _addClass (aAst, aGroup, aDiv);
    visitBlockChildren (aAst, aGroup, aDiv);
    aTarget.addChild (aDiv);
  }

  @Override
  protected void visitPageBreak (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final PhloxPageBreakNode aPageBreak,
                                 @NonNull final IHCNodeWithChildren <?> aTarget)
  {
    aTarget.addChild (new HCDiv ().addStyle (CCSSProperties.PAGE_BREAK_AFTER.newValue ("always")));
  }

  /**
   * Convert the template AST to an HTML document.
   *
   * @param aAst
   *        The rendered template. Never {@code null}.
   * @return The populated {@link HCHtml}. Never {@code null}.
   */
  @NonNull
  public HCHtml emit (@NonNull final PhloxTemplateAst aAst)
  {
    final HCHtml aHtml = new HCHtml ();
    final HCDiv aPage = HtmlDocumentSupport.createPage (aHtml, aAst.getInvoice (), m_aCtx);
    visitBlockChildren (aAst, aAst.getRoot (), aPage);
    return aHtml;
  }
}
