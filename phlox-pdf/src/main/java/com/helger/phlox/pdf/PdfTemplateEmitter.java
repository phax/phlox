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
package com.helger.phlox.pdf;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

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

import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringReplace;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.pdflayout.base.AbstractPLRenderableObject;
import com.helger.pdflayout.base.IPLRenderableObject;
import com.helger.pdflayout.base.PLColor;
import com.helger.pdflayout.base.PLPageSet;
import com.helger.pdflayout.element.box.PLBox;
import com.helger.pdflayout.element.hbox.PLHBox;
import com.helger.pdflayout.element.image.EPLImageType;
import com.helger.pdflayout.element.image.PLImage;
import com.helger.pdflayout.element.link.AbstractPLExternalLink;
import com.helger.pdflayout.element.list.IBulletPointCreator;
import com.helger.pdflayout.element.list.PLBulletPointList;
import com.helger.pdflayout.element.special.PLPageBreak;
import com.helger.pdflayout.element.special.PLSpacerX;
import com.helger.pdflayout.element.special.PLSpacerY;
import com.helger.pdflayout.element.table.PLTable;
import com.helger.pdflayout.element.table.PLTableCell;
import com.helger.pdflayout.element.text.PLText;
import com.helger.pdflayout.element.vbox.PLVBox;
import com.helger.pdflayout.richtext.element.PLRichText;
import com.helger.pdflayout.richtext.run.PLFontFamily;
import com.helger.pdflayout.richtext.run.PLRichTextRunBuilder;
import com.helger.pdflayout.spec.BorderStyleSpec;
import com.helger.pdflayout.spec.EHorzAlignment;
import com.helger.pdflayout.spec.FontSpec;
import com.helger.pdflayout.spec.PaddingSpec;
import com.helger.pdflayout.spec.PreloadFont;
import com.helger.pdflayout.spec.WidthSpec;
import com.helger.pdflayout.tagging.EPLStructureRole;
import com.helger.phlox.template.PhloxTemplatePostParser;
import com.helger.phlox.template.ast.AbstractPhloxAstVisitor;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxPageBreakNode;
import com.helger.phlox.template.ast.PhloxResolvedImage;
import com.helger.phlox.template.ast.PhloxTemplateAst;

/**
 * Turns the {@link PhloxTemplateAst} of a rendered template into the content of a PDF page set.
 * <p>
 * ph-pdf-layout itself has one style per text element. The inline nodes of a paragraph, heading or
 * table cell are therefore folded into the markup of {@code ph-pdf-layout-richtext}
 * (<code>**bold**</code>, <code>*italic*</code>, <code>~~strikethrough~~</code>,
 * <code>`code`</code> for the monospace font, <code>{link[uri]}text{link}</code>), which
 * {@code PLRichText} wraps and draws. Inline images are skipped. A block class that was attached
 * with {@code {{class:name}}} is looked up with {@link PdfRenderContext#getBlockStyle(String)}.
 *
 * @author Philip Helger
 */
public final class PdfTemplateEmitter extends
                                      AbstractPhloxAstVisitor <Consumer <IPLRenderableObject <?>>, List <StringBuilder>>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PdfTemplateEmitter.class);

  private static final String LOGO_URL = "attachment:logo";
  private static final Pattern BR_TAG = Pattern.compile ("<br\\s*/?>", Pattern.CASE_INSENSITIVE);
  private static final float PARAGRAPH_GAP = 4f;
  private static final float ROW_GAP = 14f;
  private static final float LOGO_HEIGHT = 40f;
  private static final float MAX_IMAGE_WIDTH = 200f;
  // 96 CSS pixels are 72 points, so that height-px gives the same size as in HTML
  private static final float POINTS_PER_CSS_PIXEL = 0.75f;
  // Width of the column with the list marker: a bullet, or a number like "10."
  private static final float BULLET_WIDTH = 10f;
  private static final float NUMBER_WIDTH = 16f;

  private final PdfRenderContext m_aCtx;

  /** <code>true</code> while the text of a heading or table header is set in capitals. */
  private boolean m_bUppercase;

  /** The width in points that is available for a block at the current position. */
  private float m_fContentWidth;

  /** The text colour of the enclosing group, if it has one. */
  private PLColor m_aInheritedTextColor;

  /**
   * Create a new emitter.
   *
   * @param aCtx
   *        The render context that provides the page size, the margins, the fonts and the block
   *        styles. Never {@code null}.
   */
  public PdfTemplateEmitter (@NonNull final PdfRenderContext aCtx)
  {
    m_aCtx = aCtx;
    m_fContentWidth = aCtx.getPageSize ().getWidth () - aCtx.getMargins ().getXSum ();
  }

  @NonNull
  private FontSpec _styled (@NonNull final FontSpec aFont, @Nullable final PdfBlockStyle aStyle)
  {
    FontSpec ret = aFont;
    final PLColor aColor = aStyle != null && aStyle.getTextColor () != null ? aStyle.getTextColor ()
                                                                            : m_aInheritedTextColor;
    if (aColor != null)
      ret = ret.getCloneWithDifferentColor (aColor);
    if (aStyle != null)
    {
      if (aStyle.hasFontSize ())
        ret = ret.getCloneWithDifferentFontSize (aStyle.getFontSize ().floatValue ());
      if (aStyle.isBold ())
        ret = ret.getCloneWithDifferentFont (m_aCtx.getFontFamily ().getBold ());
    }
    return ret;
  }

  /** Apply the background, the padding and the lines of a block style to a box. */
  @NonNull
  private static PLBox _decorate (@NonNull final PLBox aBox, @Nullable final PdfBlockStyle aStyle)
  {
    if (aStyle == null)
      return aBox;
    if (aStyle.getFillColor () != null)
      aBox.setFillColor (aStyle.getFillColor ());
    if (aStyle.getPadding () > 0)
      aBox.setPadding (aStyle.getPadding ());
    else
      if (aStyle.getFillColor () != null)
        aBox.setPadding (2);
    if (aStyle.getBorderTopColor () != null)
      aBox.setBorderTop (new BorderStyleSpec (aStyle.getBorderTopColor (), aStyle.getBorderWidth ()));
    if (aStyle.getBorderBottomColor () != null)
      aBox.setBorderBottom (new BorderStyleSpec (aStyle.getBorderBottomColor (), aStyle.getBorderWidth ()));
    return aBox;
  }

  @Nullable
  private PdfBlockStyle _getStyle (@NonNull final PhloxTemplateAst aAst, @NonNull final Node aNode)
  {
    final String sBlockClass = aAst.getBlockClass (aNode);
    if (sBlockClass == null)
      return null;
    final PdfBlockStyle aStyle = m_aCtx.getBlockStyle (sBlockClass);
    if (aStyle == null)
      LOGGER.debug ("No PDF block style registered for template class '" + sBlockClass + "' - rendering it unstyled");
    return aStyle;
  }

  /**
   * PDF only allows links to a few URI schemes; other targets, like attachments, stay plain text.
   */
  private static boolean _isSupportedLink (@Nullable final String sDestination)
  {
    if (sDestination == null)
      return false;
    final int nColon = sDestination.indexOf (':');
    return nColon > 0 &&
           AbstractPLExternalLink.getAllowedURISchemes ()
                                 .contains (sDestination.substring (0, nColon).toLowerCase (Locale.ROOT));
  }

  @NonNull
  private static String _encodeLinkDestination (@NonNull final String sDestination)
  {
    String ret = StringReplace.replaceAll (sDestination, "[", "%5B");
    ret = StringReplace.replaceAll (ret, "]", "%5D");
    ret = StringReplace.replaceAll (ret, "{", "%7B");
    return StringReplace.replaceAll (ret, "}", "%7D");
  }

  @NonNull
  private static StringBuilder _getLastLine (@NonNull final List <StringBuilder> aLines)
  {
    return aLines.get (aLines.size () - 1);
  }

  /**
   * Append the inline children of a node as ph-pdf-layout markup between an opening and a closing
   * marker. The children may start new lines, so the closing marker goes to the last line.
   */
  private void _appendWrappedRecursive (@NonNull final PhloxTemplateAst aAst,
                                        @NonNull final Node aNode,
                                        @NonNull final List <StringBuilder> aLines,
                                        @NonNull final String sOpen,
                                        @NonNull final String sClose)
  {
    _getLastLine (aLines).append (sOpen);
    visitInlineChildren (aAst, aNode, aLines);
    _getLastLine (aLines).append (sClose);
  }

  @NonNull
  private PLFontFamily _fontFamily (@NonNull final FontSpec aBase)
  {
    final PdfFontFamily aFamily = m_aCtx.getFontFamily ();
    if (aBase.getPreloadFont ().equals (aFamily.getBold ()))
      return new PLFontFamily (aFamily.getBold (),
                               aFamily.getBold (),
                               aFamily.getBoldItalic (),
                               aFamily.getBoldItalic ());
    return aFamily.getAsPLFontFamily ();
  }

  /**
   * Create the content of a block of text (paragraph, heading, table cell) from its inline nodes.
   */
  @Nullable
  private IPLRenderableObject <?> _createInlineBlock (@NonNull final PhloxTemplateAst aAst,
                                                      @NonNull final Node aParent,
                                                      @NonNull final FontSpec aBase,
                                                      @NonNull final EHorzAlignment eAlign,
                                                      final boolean bVertSplittable)
  {
    final ICommonsList <StringBuilder> aLines = new CommonsArrayList <> ();
    aLines.add (new StringBuilder ());
    visitInlineChildren (aAst, aParent, aLines);

    final ICommonsList <String> aNonBlank = new CommonsArrayList <> ();
    for (final StringBuilder aLine : aLines)
    {
      final String sLine = aLine.toString ().strip ();
      if (!sLine.isEmpty ())
        aNonBlank.add (sLine);
    }
    if (aNonBlank.isEmpty ())
      return null;

    // Code uses the standard monospace font only with the standard fonts. A custom font family
    // must be used for everything: the standard fonts lack most scripts and are not embedded
    // (PDF/A)
    final PLFontFamily aFamily = _fontFamily (aBase);
    final PLFontFamily aCodeFamily = m_aCtx.getFontFamily () == PdfFontFamily.STANDARD ? PLFontFamily.monospace ()
                                                                                       : aFamily;
    // A backslash in front of the line ending is a hard line break in the markup
    final PLRichText aRichText = new PLRichText (new PLRichTextRunBuilder (aFamily,
                                                                           aCodeFamily,
                                                                           aBase.getFontSize (),
                                                                           aBase.getColor ()).buildFromMarkup (String.join ("\\\n",
                                                                                                                            aNonBlank)));
    aRichText.setHorzAlign (eAlign);
    aRichText.setVertSplittable (bVertSplittable);
    return aRichText;
  }

  @NonNull
  private static EPLStructureRole _getHeadingRole (final int nLevel)
  {
    return switch (nLevel)
    {
      case 1 -> EPLStructureRole.H1;
      case 2 -> EPLStructureRole.H2;
      case 3 -> EPLStructureRole.H3;
      case 4 -> EPLStructureRole.H4;
      case 5 -> EPLStructureRole.H5;
      default -> EPLStructureRole.H6;
    };
  }

  private void _addHeading (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Heading aHeading,
                            @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aHeading);
    final FontSpec aBody = m_aCtx.getBodyFont ();
    final FontSpec aFont;
    final PLBox aBox;
    aFont = switch (aHeading.getLevel ())
    {
      case 1 -> new FontSpec (m_aCtx.getFontFamily ().getBold (), 18f);
      case 2 -> m_aCtx.getHeaderFont ();
      default -> aBody.getCloneWithDifferentFont (m_aCtx.getFontFamily ().getBold ())
                      .getCloneWithDifferentFontSize (aBody.getFontSize () + (aHeading.getLevel () == 3 ? 1f : 0f));
    };
    m_bUppercase = aHeading.getLevel () == 2;
    final IPLRenderableObject <?> aContent;
    try
    {
      aContent = _createInlineBlock (aAst, aHeading, _styled (aFont, aStyle), EHorzAlignment.LEFT, false);
    }
    finally
    {
      m_bUppercase = false;
    }
    if (aContent == null)
      return;
    // Tagged PDF: the text of a heading is a heading of the same level
    if (aContent instanceof final AbstractPLRenderableObject <?> aRealContent)
      aRealContent.setStructureRole (_getHeadingRole (aHeading.getLevel ()));
    aBox = new PLBox (aContent);
    if (aHeading.getLevel () == 1)
      aBox.setMarginBottom (6);
    else
      if (aHeading.getLevel () == 2)
        aBox.setMarginTop (8).setMarginBottom (3);
      else
        aBox.setMarginTop (3).setMarginBottom (2);
    aSink.accept (_decorate (aBox, aStyle));
  }

  @Nullable
  private static BufferedImage _readImage (@NonNull final PhloxResolvedImage aResolved)
  {
    try
    {
      return ImageIO.read (new NonBlockingByteArrayInputStream (aResolved.getBinary ()));
    }
    catch (final IOException ex)
    {
      return null;
    }
  }

  private void _addImage (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final Image aImage,
                          @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PhloxResolvedImage aResolved = aAst.getResolvedImage (aImage);
    if (aResolved == null)
    {
      LOGGER.warn ("Template image '" + aImage.getDestination () + "' could not be resolved - skipping it");
      return;
    }
    final BufferedImage aBufferedImage = _readImage (aResolved);
    if (aBufferedImage == null)
    {
      LOGGER.warn ("Template image '" +
                   aImage.getDestination () +
                   "' is not a raster image PDF can embed - skipping it");
      return;
    }
    final float fRatio = (float) aBufferedImage.getWidth () / aBufferedImage.getHeight ();
    float fHeight = LOGO_URL.equals (aImage.getDestination ()) ? LOGO_HEIGHT : aBufferedImage.getHeight ();
    final int nHeightPx = PhloxTemplatePostParser.getImageHeightPx (aImage);
    if (nHeightPx > 0)
      fHeight = nHeightPx * POINTS_PER_CSS_PIXEL;
    float fWidth = fHeight * fRatio;
    // An explicit height is only limited by the page
    final float fMaxWidth = nHeightPx > 0 ? m_fContentWidth : MAX_IMAGE_WIDTH;
    if (fWidth > fMaxWidth)
    {
      fWidth = fMaxWidth;
      fHeight = fWidth / fRatio;
    }
    // Tagged PDF: a figure needs an alternate text
    String sAltText = aResolved.getAltText ();
    if (StringHelper.isEmpty (sAltText))
      sAltText = LOGO_URL.equals (aImage.getDestination ()) ? "Logo" : "Image";
    final PLImage aPLImage = new PLImage (aBufferedImage, fWidth, fHeight).setMarginBottom (PARAGRAPH_GAP)
                                                                          .setAlternateText (sAltText);
    // The default JPEG compression blurs the modules of a QR code
    if (aImage.getDestination ().startsWith (PhloxTemplatePostParser.QRCODE_URL_PREFIX))
      aPLImage.setImageType (EPLImageType.LOSSLESS);
    aSink.accept (aPLImage);
  }

  private void _addParagraph (@NonNull final PhloxTemplateAst aAst,
                              @NonNull final Paragraph aParagraph,
                              @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    // A paragraph that only holds an image is a block level image
    if (aParagraph.getFirstChild () instanceof final Image aImage && aImage.getNext () == null)
    {
      _addImage (aAst, aImage, aSink);
      return;
    }

    final PdfBlockStyle aStyle = _getStyle (aAst, aParagraph);
    final IPLRenderableObject <?> aContent = _createInlineBlock (aAst,
                                                                 aParagraph,
                                                                 _styled (m_aCtx.getBodyFont (), aStyle),
                                                                 EHorzAlignment.LEFT,
                                                                 true);
    if (aContent == null)
      return;
    aSink.accept (_decorate (new PLBox (aContent).setMarginBottom (PARAGRAPH_GAP), aStyle));
  }

  @NonNull
  private IPLRenderableObject <?> _createCell (@NonNull final PhloxTemplateAst aAst,
                                               @NonNull final TableCell aCell,
                                               @NonNull final FontSpec aFont,
                                               @NonNull final EHorzAlignment eAlign)
  {
    final IPLRenderableObject <?> aContent = _createInlineBlock (aAst, aCell, aFont, eAlign, true);
    if (aContent == null)
      return new PLSpacerX (1f);
    // The box aligns its content inside the width of the cell
    return new PLBox (aContent).setPadding (new PaddingSpec (3)).setHorzAlign (eAlign);
  }

  @NonNull
  private static EHorzAlignment _align (final TableCell.@Nullable Alignment eAlignment)
  {
    if (eAlignment == TableCell.Alignment.RIGHT)
      return EHorzAlignment.RIGHT;
    if (eAlignment == TableCell.Alignment.CENTER)
      return EHorzAlignment.CENTER;
    return EHorzAlignment.LEFT;
  }

  private static int _textLength (@NonNull final Node aNode)
  {
    int nLen = 0;
    for (Node aChild = aNode.getFirstChild (); aChild != null; aChild = aChild.getNext ())
      nLen += aChild instanceof final Text aText ? aText.getLiteral ().length () : _textLength (aChild);
    return nLen;
  }

  private static void _collectWordLengths (@NonNull final Node aNode, @NonNull final int [] aLongestWord)
  {
    for (Node aChild = aNode.getFirstChild (); aChild != null; aChild = aChild.getNext ())
    {
      if (aChild instanceof final Text aText)
      {
        for (final String sWord : StringHelper.getExploded (' ', aText.getLiteral ()))
          aLongestWord[0] = Math.max (aLongestWord[0], sWord.length ());
      }
      else
        _collectWordLengths (aChild, aLongestWord);
    }
  }

  /**
   * The columns get the width of their longest word first, so that amounts do not wrap. The rest of
   * the width is shared in proportion to the longest content of the columns, within sensible
   * limits.
   */
  @NonNull
  private float [] _columnWidths (@NonNull final TableBlock aTable, final int nColumns)
  {
    final float [] aWeights = new float [nColumns];
    final int [] aLongestWords = new int [nColumns];
    final float fHeaderScale = m_aCtx.getTableHeaderFont ().getFontSize () *
                               1.1f /
                               m_aCtx.getBodyFont ().getFontSize ();
    for (int i = 0; i < nColumns; ++i)
      aWeights[i] = 9f;
    for (Node aPart = aTable.getFirstChild (); aPart != null; aPart = aPart.getNext ())
      for (Node aRow = aPart.getFirstChild (); aRow != null; aRow = aRow.getNext ())
      {
        int nCol = 0;
        for (Node aCell = aRow.getFirstChild (); aCell != null && nCol < nColumns; aCell = aCell.getNext ())
        {
          aWeights[nCol] = Math.max (aWeights[nCol], Math.min (_textLength (aCell), 40));
          final int [] aWord = new int [1];
          _collectWordLengths (aCell, aWord);
          // The header is set in a smaller bold font; the word is as long as it would be in the
          // font of the body
          final int nWordLength = aPart instanceof TableHead ? Math.round (aWord[0] * fHeaderScale) : aWord[0];
          aLongestWords[nCol] = Math.max (aLongestWords[nCol], nWordLength);
          nCol++;
        }
      }

    // Width in points that the longest word needs, plus the padding of the cell
    final float fCharWidth = m_aCtx.getBodyFont ().getFontSize () * 0.56f;
    final float [] aMin = new float [nColumns];
    float fMinSum = 0;
    float fWeightSum = 0;
    for (int i = 0; i < nColumns; ++i)
    {
      aMin[i] = aLongestWords[i] * fCharWidth + 8f;
      fMinSum += aMin[i];
      fWeightSum += aWeights[i];
    }
    final float fRest = Math.max (0f, m_fContentWidth - fMinSum);
    final float [] ret = new float [nColumns];
    float fSum = 0;
    for (int i = 0; i < nColumns; ++i)
    {
      ret[i] = aMin[i] + fRest * aWeights[i] / fWeightSum;
      fSum += ret[i];
    }
    for (int i = 0; i < nColumns; ++i)
      ret[i] = ret[i] * 100f / fSum;
    return ret;
  }

  private void _addTable (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final TableBlock aTable,
                          @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final Node aHead = aTable.getFirstChild ();
    final Node aFirstRow = aHead == null ? null : aHead.getFirstChild ();
    if (aFirstRow == null)
      return;
    int nColumns = 0;
    for (Node aCell = aFirstRow.getFirstChild (); aCell != null; aCell = aCell.getNext ())
      nColumns++;
    if (nColumns == 0)
      return;

    final PLTable aTbl = PLTable.createWithPercentage (_columnWidths (aTable, nColumns));
    // A header row without any text (as in a table of key-value pairs) is not shown
    final boolean bShowHeader = _textLength (aFirstRow) > 0;
    if (bShowHeader)
    {
      // The header row is repeated on every page
      aTbl.setHeaderRowCount (1);
    }
    for (Node aPart = aTable.getFirstChild (); aPart != null; aPart = aPart.getNext ())
    {
      final boolean bHead = aPart instanceof TableHead;
      if (!bHead && !(aPart instanceof TableBody))
        continue;
      if (bHead && !bShowHeader)
        continue;
      for (Node aRowNode = aPart.getFirstChild (); aRowNode instanceof TableRow; aRowNode = aRowNode.getNext ())
      {
        final PdfBlockStyle aStyle = _getStyle (aAst, aRowNode);
        final FontSpec aBaseFont = bHead ? m_aCtx.getTableHeaderFont () : m_aCtx.getBodyFont ();
        final ICommonsList <PLTableCell> aCells = new CommonsArrayList <> ();
        for (Node aCellNode = aRowNode.getFirstChild (); aCellNode instanceof final TableCell aCell; aCellNode = aCellNode.getNext ())
        {
          m_bUppercase = bHead;
          final PLTableCell aPLCell;
          try
          {
            aPLCell = new PLTableCell (_createCell (aAst,
                                                    aCell,
                                                    _styled (aBaseFont, aStyle),
                                                    _align (aCell.getAlignment ())));
          }
          finally
          {
            m_bUppercase = false;
          }
          if (bHead)
            aPLCell.setBorderBottom (new BorderStyleSpec (m_aCtx.getPalette ().getInk (), 1f));
          else
            aPLCell.setBorderBottom (new BorderStyleSpec (m_aCtx.getPalette ().getRule (), 0.5f));
          if (aStyle != null && aStyle.getFillColor () != null)
            aPLCell.setFillColor (aStyle.getFillColor ());
          aCells.add (aPLCell);
        }
        // Rows may have fewer cells than the table has columns
        while (aCells.size () < nColumns)
          aCells.add (PLTableCell.createEmptyCell ());
        aTbl.addAndReturnRow (aCells).setVertSplittable (false);
      }
    }
    aSink.accept (aTbl);
  }

  private void _addCodeBlock (@NonNull final String sLiteral, @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final FontSpec aFont = new FontSpec (PreloadFont.MONOSPACE, m_aCtx.getBodyFont ().getFontSize () - 1f);
    final PLVBox aLines = new PLVBox ();
    // The line breaks of the CommonMark literal are normalised to LF
    for (final String sLine : StringHelper.getExploded ('\n', sLiteral.stripTrailing ()))
      aLines.addRow (new PLText (sLine.isEmpty () ? " " : sLine, aFont));
    aSink.accept (new PLBox (aLines).setFillColor (PLColor.gray (0xf5)).setPadding (4).setMarginBottom (PARAGRAPH_GAP));
  }

  /**
   * Create the creator of the list markers. A marker is rich text in the body font like the text of
   * the item, so that both have the same baseline.
   */
  @NonNull
  private IBulletPointCreator _createMarkerCreator (@NonNull final IntFunction <String> aMarkerText)
  {
    final FontSpec aFont = _styled (m_aCtx.getBodyFont (), null);
    final PLFontFamily aFamily = m_aCtx.getFontFamily ().getAsPLFontFamily ();
    return nIndex -> PLRichText.createFromMarkup (PdfTextSupport.escapeMarkup (aMarkerText.apply (nIndex)),
                                                  aFamily,
                                                  aFont.getFontSize (),
                                                  aFont.getColor ());
  }

  private void _addList (@NonNull final PhloxTemplateAst aAst,
                         @NonNull final Node aList,
                         @NonNull final IBulletPointCreator aCreator,
                         final float fMarkerWidth,
                         @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PLBulletPointList aBullets = new PLBulletPointList (WidthSpec.abs (fMarkerWidth), aCreator);
    for (Node aItem = aList.getFirstChild (); aItem instanceof ListItem; aItem = aItem.getNext ())
    {
      final PLVBox aContent = new PLVBox ();
      visitBlockChildren (aAst, aItem, aContent::addRow);
      aBullets.addBulletPoint (aContent);
    }
    aSink.accept (new PLBox (aBullets).setMarginBottom (PARAGRAPH_GAP));
  }

  /**
   * Add the blocks of a group below each other, or the cells of a row side by side.
   */
  private void _addGroup (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final PhloxGroupNode aGroup,
                          @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aGroup);
    final PLColor aOldColor = m_aInheritedTextColor;
    if (aStyle != null && aStyle.getTextColor () != null)
      m_aInheritedTextColor = aStyle.getTextColor ();
    try
    {
      final IPLRenderableObject <?> aContent;
      if (aGroup.isRow ())
      {
        // A row (parties, totals) is moved to the next page as a whole
        final PLHBox aRow = new PLHBox ().setVertSplittable (false);
        // Work out the width of the cells, so that the tables in them can plan their columns
        final float fRowWidth = m_fContentWidth;
        float fFixedPercent = 0;
        int nStarCells = 0;
        for (Node aCell = aGroup.getFirstChild (); aCell != null; aCell = aCell.getNext ())
        {
          final PdfBlockStyle aCellStyle = aCell instanceof PhloxGroupNode ? _getStyle (aAst, aCell) : null;
          if (aCellStyle != null && aCellStyle.getWidthPercent () != null)
            fFixedPercent += aCellStyle.getWidthPercent ().floatValue ();
          else
            nStarCells++;
        }
        for (Node aCell = aGroup.getFirstChild (); aCell != null; aCell = aCell.getNext ())
        {
          final PLVBox aCellContent = new PLVBox ();
          final PdfBlockStyle aWidthStyle = aCell instanceof PhloxGroupNode ? _getStyle (aAst, aCell) : null;
          final float fCellPercent = aWidthStyle != null && aWidthStyle.getWidthPercent () != null ? aWidthStyle
                                                                                                                .getWidthPercent ()
                                                                                                                .floatValue ()
                                                                                                   : (100f -
                                                                                                      fFixedPercent) /
                                                                                                     Math.max (1,
                                                                                                               nStarCells);
          m_fContentWidth = fRowWidth * fCellPercent / 100f - ROW_GAP;
          if (aCell instanceof final PhloxGroupNode aCellGroup && !aCellGroup.isRow ())
            visitBlockChildren (aAst, aCellGroup, aCellContent::addRow);
          else
            visitBlock (aAst, aCell, aCellContent::addRow);
          m_fContentWidth = fRowWidth;
          final PdfBlockStyle aCellStyle = aCell instanceof PhloxGroupNode ? _getStyle (aAst, aCell) : null;
          final PLBox aCellBox = _decorate (new PLBox (aCellContent), aCellStyle);
          if (aCell.getNext () != null)
            aCellBox.setMarginRight (ROW_GAP);
          final Float aWidth = aCellStyle == null ? null : aCellStyle.getWidthPercent ();
          aRow.addColumn (aCellBox, aWidth == null ? WidthSpec.star () : WidthSpec.perc (aWidth.floatValue ()));
        }
        aContent = aRow;
      }
      else
      {
        final PLVBox aBlocks = new PLVBox ();
        if (aStyle != null && aStyle.isKeepTogether ())
          aBlocks.setVertSplittable (false);
        visitBlockChildren (aAst, aGroup, aBlocks::addRow);
        aContent = aBlocks;
      }
      // Tagged PDF: a group or row is a division
      aSink.accept (_decorate (new PLBox (aContent).setMarginBottom (PARAGRAPH_GAP)
                                                   .setStructureRole (EPLStructureRole.DIV),
                               aGroup.isRow () ? null : aStyle));
    }
    finally
    {
      m_aInheritedTextColor = aOldColor;
    }
  }

  @Override
  protected void visitText (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Text aText,
                            @NonNull final List <StringBuilder> aLines)
  {
    final String sText = m_bUppercase ? aText.getLiteral ().toUpperCase (m_aCtx.getLocale ()) : aText.getLiteral ();
    _getLastLine (aLines).append (PdfTextSupport.escapeMarkup (PdfTextSupport.separateCurrencySymbol (sText)));
  }

  @Override
  protected void visitSoftLineBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final SoftLineBreak aBreak,
                                     @NonNull final List <StringBuilder> aLines)
  {
    aLines.add (new StringBuilder ());
  }

  @Override
  protected void visitHardLineBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final HardLineBreak aBreak,
                                     @NonNull final List <StringBuilder> aLines)
  {
    aLines.add (new StringBuilder ());
  }

  @Override
  protected void visitStrongEmphasis (@NonNull final PhloxTemplateAst aAst,
                                      @NonNull final StrongEmphasis aStrong,
                                      @NonNull final List <StringBuilder> aLines)
  {
    _appendWrappedRecursive (aAst, aStrong, aLines, "**", "**");
  }

  @Override
  protected void visitEmphasis (@NonNull final PhloxTemplateAst aAst,
                                @NonNull final Emphasis aEmphasis,
                                @NonNull final List <StringBuilder> aLines)
  {
    _appendWrappedRecursive (aAst, aEmphasis, aLines, "*", "*");
  }

  @Override
  protected void visitStrikethrough (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final Strikethrough aStrikethrough,
                                     @NonNull final List <StringBuilder> aLines)
  {
    _appendWrappedRecursive (aAst, aStrikethrough, aLines, "~~", "~~");
  }

  @Override
  protected void visitCode (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Code aCode,
                            @NonNull final List <StringBuilder> aLines)
  {
    _getLastLine (aLines).append ('`').append (PdfTextSupport.escapeMarkup (aCode.getLiteral ())).append ('`');
  }

  @Override
  protected void visitLink (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Link aLink,
                            @NonNull final List <StringBuilder> aLines)
  {
    if (_isSupportedLink (aLink.getDestination ()))
      _appendWrappedRecursive (aAst,
                               aLink,
                               aLines,
                               "{link[" +
                                       PdfTextSupport.escapeMarkup (_encodeLinkDestination (aLink.getDestination ())) +
                                       "]}",
                               "{link}");
    else
    {
      // Links to unsupported targets: keep the text
      visitInlineChildren (aAst, aLink, aLines);
    }
  }

  @Override
  protected void visitInlineImage (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final Image aImage,
                                   @NonNull final List <StringBuilder> aLines)
  {
    // Inline images are not supported - only block level images are
  }

  @Override
  protected void visitHtmlInline (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final HtmlInline aHtml,
                                  @NonNull final List <StringBuilder> aLines)
  {
    if (BR_TAG.matcher (aHtml.getLiteral ()).matches ())
      aLines.add (new StringBuilder ());
  }

  @Override
  protected void visitHeading (@NonNull final PhloxTemplateAst aAst,
                               @NonNull final Heading aHeading,
                               @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addHeading (aAst, aHeading, aSink);
  }

  @Override
  protected void visitParagraph (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final Paragraph aParagraph,
                                 @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addParagraph (aAst, aParagraph, aSink);
  }

  @Override
  protected void visitBulletList (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BulletList aList,
                                  @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addList (aAst, aList, _createMarkerCreator (nIndex -> "\u2022"), BULLET_WIDTH, aSink);
  }

  @Override
  protected void visitOrderedList (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final OrderedList aList,
                                   @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final int nStart = aList.getMarkerStartNumber () == null ? 1 : aList.getMarkerStartNumber ().intValue ();
    _addList (aAst, aList, _createMarkerCreator (nIndex -> (nStart + nIndex) + "."), NUMBER_WIDTH, aSink);
  }

  @Override
  protected void visitBlockQuote (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BlockQuote aQuote,
                                  @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PLVBox aContent = new PLVBox ();
    visitBlockChildren (aAst, aQuote, aContent::addRow);
    aSink.accept (new PLBox (aContent).setBorderLeft (new BorderStyleSpec (PLColor.GRAY, 2f))
                                      .setPaddingLeft (6)
                                      .setMarginBottom (PARAGRAPH_GAP)
                                      .setStructureRole (EPLStructureRole.BLOCK_QUOTE));
  }

  @Override
  protected void visitFencedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                       @NonNull final FencedCodeBlock aBlock,
                                       @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addCodeBlock (aBlock.getLiteral (), aSink);
  }

  @Override
  protected void visitIndentedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                         @NonNull final IndentedCodeBlock aBlock,
                                         @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addCodeBlock (aBlock.getLiteral (), aSink);
  }

  @Override
  protected void visitThematicBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final ThematicBreak aBreak,
                                     @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aRuleStyle = _getStyle (aAst, aBreak);
    final boolean bStyled = aRuleStyle != null && aRuleStyle.getBorderTopColor () != null;
    aSink.accept (new PLBox (new PLSpacerY (1f)).setBorderTop (new BorderStyleSpec (bStyled ? aRuleStyle.getBorderTopColor ()
                                                                                            : PLColor.GRAY,
                                                                                    bStyled ? aRuleStyle.getBorderWidth ()
                                                                                            : 0.75f)).setMarginY (4));
  }

  @Override
  protected void visitTable (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final TableBlock aTable,
                             @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addTable (aAst, aTable, aSink);
  }

  @Override
  protected void visitBlockImage (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final Image aImage,
                                  @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addImage (aAst, aImage, aSink);
  }

  @Override
  protected void visitHtmlBlock (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final HtmlBlock aHtml,
                                 @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    aSink.accept (new PLText (aHtml.getLiteral ().strip (), m_aCtx.getBodyFont ()));
  }

  @Override
  protected void visitGroup (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final PhloxGroupNode aGroup,
                             @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addGroup (aAst, aGroup, aSink);
  }

  @Override
  protected void visitPageBreak (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final PhloxPageBreakNode aPageBreak,
                                 @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    aSink.accept (new PLPageBreak (true));
  }

  /**
   * Convert the template AST to the content of a PDF page set.
   *
   * @param aAst
   *        The rendered template. Never {@code null}.
   * @return The page set with the size and the margins of the render context. Never {@code null}.
   */
  @NonNull
  public PLPageSet emit (@NonNull final PhloxTemplateAst aAst)
  {
    final PLPageSet aPS = new PLPageSet (m_aCtx.getPageSize ()).setMargin (m_aCtx.getMargins ());
    visitBlockChildren (aAst, aAst.getRoot (), aPS::addElement);
    return aPS;
  }
}
