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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;

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
import com.helger.pdflayout.base.EPLPlaceholder;
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
import com.helger.pdflayout.richtext.run.PLRichTextRun;
import com.helger.pdflayout.richtext.run.PLRichTextRunBuilder;
import com.helger.pdflayout.spec.BorderStyleSpec;
import com.helger.pdflayout.spec.EHorzAlignment;
import com.helger.pdflayout.spec.FontSpec;
import com.helger.pdflayout.spec.PaddingSpec;
import com.helger.pdflayout.spec.PreloadFont;
import com.helger.pdflayout.spec.WidthSpec;
import com.helger.pdflayout.tagging.EPLStructureRole;
import com.helger.phlox.api.PhloxLogHelper;
import com.helger.phlox.api.PhloxRuntimeException;
import com.helger.phlox.template.PhloxTemplatePostParser;
import com.helger.phlox.template.ast.AbstractPhloxAstVisitor;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxPageBreakNode;
import com.helger.phlox.template.ast.PhloxPageMarginNode;
import com.helger.phlox.template.ast.PhloxPageNumberNode;
import com.helger.phlox.template.ast.PhloxSpanNode;
import com.helger.phlox.template.ast.PhloxResolvedImage;
import com.helger.phlox.template.ast.PhloxTemplateAst;

/**
 * Turns the {@link PhloxTemplateAst} of a rendered template into the content of a PDF page set.
 * <p>
 * ph-pdf-layout itself has one style per text element. The inline nodes of a paragraph, heading or
 * table cell are therefore folded into the markup of {@code ph-pdf-layout-richtext}
 * (<code>**bold**</code>, <code>*italic*</code>, <code>~~strikethrough~~</code>,
 * <code>`code`</code> for the monospace font, <code>{link[uri]}text{link}</code>), which
 * {@code PLRichText} wraps and draws. Inline images are skipped; inline raw HTML other than
 * {@code <br>} is shown as text, as in the other formats. A block class that was attached with
 * {@code {{class:name}}} is looked up with {@link PdfRenderContext#getBlockStyle(String)}. The text
 * colour, the font size and the bold of a group, row, table, list or block quote apply to the text
 * inside it, unless that has an own value; a heading and a table header row keep their font size.
 * <p>
 * Only a page break at the top level of the template, or in stacked groups below the top level, can
 * work, because ph-pdf-layout handles page breaks only as direct children of a page set. Such groups
 * are split at the page break into two groups of the same style each, at every level. A page break
 * in any other place, like a row, is ignored with a warning. A page break at the start or at the end
 * of the document never gets here, see {@link PhloxTemplatePostParser}.
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
  private static final float CELL_PADDING = 3f;
  private static final float MIN_COLUMN_WIDTH = 14f;
  private static final float LOGO_HEIGHT = 40f;
  private static final float MAX_IMAGE_WIDTH = 200f;
  // 96 CSS pixels are 72 points, so that height-px gives the same size as in HTML
  private static final float POINTS_PER_CSS_PIXEL = 0.75f;
  // Width of the column with the list marker: a bullet, or a number like "10."
  private static final float BULLET_WIDTH = 10f;
  private static final float NUMBER_WIDTH = 16f;
  // A block of that part of the content height of a page is not kept together: the height does not
  // include the padding and margin of the block
  private static final float MAX_KEEP_TOGETHER_RATIO = 0.9f;
  // 100 MB in the usual four bytes per pixel. Far above the logos and QR codes of an invoice
  private static final long MAX_IMAGE_PIXELS = 25_000_000L;

  private final PdfRenderContext m_aCtx;

  /** <code>true</code> while the text of a heading or table header is set in capitals. */
  private boolean m_bUppercase;

  /** The width in points that is available for a block at the current position. */
  private float m_fContentWidth;

  /** The highest block that is moved to the next page as a whole. */
  private final float m_fMaxKeepTogetherHeight;

  /** The combined style of the enclosing groups, if they have one. */
  private PdfBlockStyle m_aInheritedStyle;

  /** The consumer of the content of the page set: only there a page break works. */
  private Consumer <IPLRenderableObject <?>> m_aPageSink;

  /** The text colour at the current position of a block of text, as six hex digits. */
  private String m_sTextColor;

  /** The background at the current position of a block of text, as six hex digits, if any. */
  private String m_sBackgroundColor;

  /**
   * <code>true</code> while the page header or footer is built, where the page numbers are
   * replaced.
   */
  private boolean m_bInPageMargin;

  /** The page header of the template, if it has one and it is shown. */
  private IPLRenderableObject <?> m_aPageHeader;

  /** The page footer of the template, if it has one and it is shown. */
  private IPLRenderableObject <?> m_aPageFooter;

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
    m_fMaxKeepTogetherHeight = (aCtx.getPageSize ().getHeight () - aCtx.getMargins ().getYSum ()) *
                               MAX_KEEP_TOGETHER_RATIO;
  }

  /**
   * Apply a style and the inherited style to a font.
   *
   * @param aFont
   *        The font of the block.
   * @param aStyle
   *        The style of the block. May be {@code null}.
   * @param bInheritFontSize
   *        {@code false} for a block with a size of its own, which only the own style can change.
   */
  @NonNull
  private FontSpec _styled (@NonNull final FontSpec aFont,
                            @Nullable final PdfBlockStyle aStyle,
                            final boolean bInheritFontSize)
  {
    final PdfBlockStyle aEffective = aStyle == null ? m_aInheritedStyle
                                                    : m_aInheritedStyle == null ? aStyle
                                                                                : m_aInheritedStyle.getOverlaidWith (aStyle);
    FontSpec ret = aFont;
    if (aEffective != null)
    {
      if (aEffective.getTextColor () != null)
        ret = ret.getCloneWithDifferentColor (aEffective.getTextColor ());
      final PdfBlockStyle aSizeStyle = bInheritFontSize ? aEffective : aStyle;
      if (aSizeStyle != null && aSizeStyle.hasFontSize ())
        ret = ret.getCloneWithDifferentFontSize (aSizeStyle.getFontSize ().floatValue ());
      if (aEffective.isBold ())
        ret = ret.getCloneWithDifferentFont (m_aCtx.getFontFamily ().getBold ());
    }
    return ret;
  }

  /** Run an action with the text style of an enclosing block. */
  private void _withInheritedStyle (@Nullable final PdfBlockStyle aStyle, @NonNull final Runnable aAction)
  {
    final PdfBlockStyle aOldStyle = m_aInheritedStyle;
    if (aStyle != null)
      m_aInheritedStyle = aOldStyle == null ? aStyle : aOldStyle.getOverlaidWith (aStyle);
    try
    {
      aAction.run ();
    }
    finally
    {
      m_aInheritedStyle = aOldStyle;
    }
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
    PdfBlockStyle ret = null;
    for (final String sBlockClass : aAst.getBlockClasses (aNode))
    {
      final PdfBlockStyle aStyle = m_aCtx.getBlockStyle (sBlockClass);
      if (aStyle == null)
        LOGGER.debug ("No PDF block style registered for template class '" +
                      PhloxLogHelper.sanitise (sBlockClass) +
                      "' - skipping it");
      else
        ret = ret == null ? aStyle : ret.getOverlaidWith (aStyle);
    }
    return ret;
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
  private static String _getHexColor (@NonNull final PLColor aColor)
  {
    final int nRGB = aColor.getRed () << 16 | aColor.getGreen () << 8 | aColor.getBlue ();
    return StringHelper.getLeadingZero (Integer.toHexString (nRGB), 6);
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

  /**
   * The page numbers are replaced in each run of the page header and footer. There a marker that
   * changes nothing starts a new run after each dollar sign of a text, so that a value of the
   * invoice never becomes a page number.
   */
  @NonNull
  private String _getProtected (@NonNull final String sMarkup)
  {
    if (!m_bInPageMargin)
      return sMarkup;
    return StringReplace.replaceAll (sMarkup, "$", "${color:#" + m_sTextColor + "}");
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
                                                      final boolean bKeepTogether)
  {
    final ICommonsList <StringBuilder> aLines = new CommonsArrayList <> ();
    aLines.add (new StringBuilder ());
    m_sTextColor = _getHexColor (aBase.getColor ());
    m_sBackgroundColor = null;
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
    final ICommonsList <PLRichTextRun> aRuns = new PLRichTextRunBuilder (aFamily,
                                                                         aCodeFamily,
                                                                         aBase.getFontSize (),
                                                                         aBase.getColor ()).buildFromMarkup (String.join ("\\\n",
                                                                                                                          aNonBlank));
    final PLRichText aRichText = bKeepTogether ? new PdfKeepTogether.RichText (aRuns, m_fMaxKeepTogetherHeight)
                                               : new PLRichText (aRuns);
    aRichText.setHorzAlign (eAlign);
    aRichText.setReplacePlaceholder (m_bInPageMargin);
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
      aContent = _createInlineBlock (aAst, aHeading, _styled (aFont, aStyle, false), EHorzAlignment.LEFT, true);
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

  /**
   * Read an image. The size is checked before the pixels are decoded: a small file can unpack to a
   * huge image, and the image can come from the invoice.
   *
   * @return The image, or {@code null} if it has no supported format, is damaged or is too large.
   */
  @Nullable
  private static BufferedImage _readImage (@NonNull final PhloxResolvedImage aResolved)
  {
    try (final ImageInputStream aIIS = new MemoryCacheImageInputStream (new NonBlockingByteArrayInputStream (aResolved.getBinary ())))
    {
      final Iterator <ImageReader> aReaders = ImageIO.getImageReaders (aIIS);
      if (!aReaders.hasNext ())
        return null;
      final ImageReader aReader = aReaders.next ();
      try
      {
        aReader.setInput (aIIS);
        final long nPixels = (long) aReader.getWidth (0) * aReader.getHeight (0);
        if (nPixels > MAX_IMAGE_PIXELS)
        {
          LOGGER.warn ("An image of " + nPixels + " pixels exceeds the limit of " + MAX_IMAGE_PIXELS + " pixels");
          return null;
        }
        return aReader.read (0);
      }
      finally
      {
        aReader.dispose ();
      }
    }
    catch (final IOException | RuntimeException | OutOfMemoryError ex)
    {
      // Decoders fail with more than an IOException on damaged data
      LOGGER.debug ("Failed to read an image: " + PhloxLogHelper.sanitise (ex.getMessage ()));
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
      LOGGER.warn ("Template image '" +
                   PhloxLogHelper.sanitise (aImage.getDestination ()) +
                   "' could not be resolved - skipping it");
      return;
    }
    final BufferedImage aBufferedImage = _readImage (aResolved);
    if (aBufferedImage == null)
    {
      LOGGER.warn ("Template image '" +
                   PhloxLogHelper.sanitise (aImage.getDestination ()) +
                   "' is not a raster image of a usable size that PDF can embed - skipping it");
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
    if (fHeight > m_fMaxKeepTogetherHeight)
    {
      fHeight = m_fMaxKeepTogetherHeight;
      fWidth = fHeight * fRatio;
    }
    // Tagged PDF: a figure needs an alternate text
    String sAltText = aResolved.getAltText ();
    if (StringHelper.isEmpty (sAltText))
      sAltText = m_aCtx.label (LOGO_URL.equals (aImage.getDestination ()) ? "image.alt.logo" : "image.alt.default");
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
                                                                 _styled (m_aCtx.getBodyFont (), aStyle, true),
                                                                 EHorzAlignment.LEFT,
                                                                 false);
    if (aContent == null)
      return;
    aSink.accept (_decorate (new PLBox (aContent).setMarginBottom (PARAGRAPH_GAP), aStyle));
  }

  @NonNull
  private IPLRenderableObject <?> _createCell (@NonNull final PhloxTemplateAst aAst,
                                               @NonNull final TableCell aCell,
                                               @NonNull final FontSpec aFont,
                                               @NonNull final EHorzAlignment eAlign,
                                               final float fPadding)
  {
    final IPLRenderableObject <?> aContent = _createInlineBlock (aAst, aCell, aFont, eAlign, false);
    if (aContent == null)
      return new PLSpacerX (1f);
    // The box aligns its content inside the width of the cell
    return new PLBox (aContent).setPadding (new PaddingSpec (fPadding)).setHorzAlign (eAlign);
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

    // A very long word must not push the other columns below the padding of their cells
    final float fMinPercent = MIN_COLUMN_WIDTH * 100f / m_fContentWidth;
    fSum = 0;
    for (int i = 0; i < nColumns; ++i)
      fSum += Math.max (ret[i], fMinPercent);
    if (fSum > 100.001f)
      for (int i = 0; i < nColumns; ++i)
        ret[i] = Math.max (ret[i], fMinPercent) * 100f / fSum;
    return ret;
  }

  private void _addTableContent (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final TableBlock aTable,
                                 @Nullable final PdfBlockStyle aTableStyle,
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

    final float [] aColumnWidths = _columnWidths (aTable, nColumns);
    final PLTable aTbl = PLTable.createWithPercentage (aColumnWidths);
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
        final float fPadding = aStyle != null && aStyle.getPadding () > 0 ? aStyle.getPadding () : CELL_PADDING;
        final PdfKeepTogether.TableRow aRow = new PdfKeepTogether.TableRow (m_fMaxKeepTogetherHeight);
        int nColumn = 0;
        for (Node aCellNode = aRowNode.getFirstChild (); aCellNode instanceof final TableCell aCell; aCellNode = aCellNode.getNext ())
        {
          m_bUppercase = bHead;
          final PLTableCell aPLCell;
          try
          {
            aPLCell = new PLTableCell (_createCell (aAst,
                                                    aCell,
                                                    _styled (aBaseFont, aStyle, !bHead),
                                                    _align (aCell.getAlignment ()),
                                                    fPadding));
          }
          finally
          {
            m_bUppercase = false;
          }
          if (aStyle != null && aStyle.getBorderTopColor () != null)
            aPLCell.setBorderTop (new BorderStyleSpec (aStyle.getBorderTopColor (), aStyle.getBorderWidth ()));
          if (aStyle != null && aStyle.getBorderBottomColor () != null)
            aPLCell.setBorderBottom (new BorderStyleSpec (aStyle.getBorderBottomColor (), aStyle.getBorderWidth ()));
          else
            if (bHead)
              aPLCell.setBorderBottom (new BorderStyleSpec (m_aCtx.getPalette ().getInk (), 1f));
            else
              aPLCell.setBorderBottom (new BorderStyleSpec (m_aCtx.getPalette ().getRule (), 0.5f));
          if (aStyle != null && aStyle.getFillColor () != null)
            aPLCell.setFillColor (aStyle.getFillColor ());
          aRow.addCell (aPLCell, WidthSpec.perc (aColumnWidths[nColumn]));
          nColumn++;
        }
        // Rows may have fewer cells than the table has columns
        for (; nColumn < nColumns; ++nColumn)
          aRow.addCell (PLTableCell.createEmptyCell (), WidthSpec.perc (aColumnWidths[nColumn]));
        aTbl.addRow (aRow);
      }
    }
    aSink.accept (aTableStyle == null ? aTbl : _decorate (new PLBox (aTbl), aTableStyle));
  }

  private void _addTable (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final TableBlock aTable,
                          @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aTable);
    _withInheritedStyle (aStyle, () -> _addTableContent (aAst, aTable, aStyle, aSink));
  }

  /**
   * Add a code block. It uses the monospace font of PDF only with the standard fonts: a custom font
   * family must be used for everything, because the standard fonts lack most scripts and are not
   * embedded, which PDF/A does not allow.
   */
  private void _addCodeBlock (@NonNull final PhloxTemplateAst aAst,
                              @NonNull final Node aBlock,
                              @NonNull final String sLiteral,
                              @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aBlock);
    final PreloadFont aCodeFont = m_aCtx.getFontFamily () == PdfFontFamily.STANDARD ? PreloadFont.MONOSPACE
                                                                                    : m_aCtx.getFontFamily ().getRegular ();
    final FontSpec aFont = _styled (new FontSpec (aCodeFont, m_aCtx.getBodyFont ().getFontSize () - 1f), aStyle, true);
    final PLVBox aLines = new PLVBox ();
    // The line breaks of the CommonMark literal are normalised to LF
    for (final String sLine : StringHelper.getExploded ('\n', sLiteral.stripTrailing ()))
      aLines.addRow (new PLText (sLine.isEmpty () ? " " : sLine, aFont));
    aSink.accept (_decorate (new PLBox (aLines).setFillColor (PLColor.gray (0xf5))
                                               .setPadding (4)
                                               .setMarginBottom (PARAGRAPH_GAP),
                             aStyle));
  }

  /**
   * Create the creator of the list markers. A marker is rich text in the body font like the text of
   * the item, so that both have the same baseline.
   */
  @NonNull
  private IBulletPointCreator _createMarkerCreator (@NonNull final IntFunction <String> aMarkerText)
  {
    final FontSpec aFont = _styled (m_aCtx.getBodyFont (), null, true);
    final PLFontFamily aFamily = _fontFamily (aFont);
    return nIndex -> PLRichText.createFromMarkup (PdfTextSupport.escapeMarkup (aMarkerText.apply (nIndex)),
                                                  aFamily,
                                                  aFont.getFontSize (),
                                                  aFont.getColor ());
  }

  private void _addList (@NonNull final PhloxTemplateAst aAst,
                         @NonNull final Node aList,
                         @NonNull final IntFunction <String> aMarkerText,
                         final float fMarkerWidth,
                         @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aList);
    _withInheritedStyle (aStyle, () -> {
      final PLBulletPointList aBullets = new PLBulletPointList (WidthSpec.abs (fMarkerWidth),
                                                                _createMarkerCreator (aMarkerText));
      for (Node aItem = aList.getFirstChild (); aItem instanceof ListItem; aItem = aItem.getNext ())
      {
        final PLVBox aContent = new PLVBox ();
        visitBlockChildren (aAst, aItem, aContent::addRow);
        aBullets.addBulletPoint (aContent);
      }
      aSink.accept (_decorate (new PLBox (aBullets).setMarginBottom (PARAGRAPH_GAP), aStyle));
    });
  }

  /** Create the box around the blocks of a group or row. */
  @NonNull
  private static PLBox _createGroupBox (@NonNull final IPLRenderableObject <?> aContent,
                                        @Nullable final PdfBlockStyle aStyle)
  {
    // Tagged PDF: a group or row is a division
    return _decorate (new PLBox (aContent).setMarginBottom (PARAGRAPH_GAP).setStructureRole (EPLStructureRole.DIV),
                      aStyle);
  }

  @NonNull
  private PLVBox _createGroupContent (@Nullable final PdfBlockStyle aStyle)
  {
    return aStyle != null && aStyle.isKeepTogether () ? new PdfKeepTogether.VBox (m_fMaxKeepTogetherHeight)
                                                      : new PLVBox ();
  }

  /** Check for a page break directly in a stacked group or in the stacked groups below it. */
  private static boolean _hasPageBreakRecursive (@NonNull final PhloxGroupNode aGroup)
  {
    for (Node aChild = aGroup.getFirstChild (); aChild != null; aChild = aChild.getNext ())
      if (aChild instanceof PhloxPageBreakNode ||
          (aChild instanceof final PhloxGroupNode aChildGroup &&
           !aChildGroup.isRow () &&
           _hasPageBreakRecursive (aChildGroup)))
        return true;
    return false;
  }

  /**
   * Get the content of a stacked group, one part per page. A nested stacked group with a page break
   * continues in the next part of its parent, with a box of its own style in every part.
   */
  @NonNull
  private ICommonsList <PLVBox> _getStackedPartsRecursive (@NonNull final PhloxTemplateAst aAst,
                                                           @NonNull final PhloxGroupNode aGroup,
                                                           @Nullable final PdfBlockStyle aStyle)
  {
    final ICommonsList <PLVBox> ret = new CommonsArrayList <> ();
    ret.add (_createGroupContent (aStyle));
    for (Node aChild = aGroup.getFirstChild (); aChild != null; aChild = aChild.getNext ())
    {
      if (aChild instanceof PhloxPageBreakNode)
        ret.add (_createGroupContent (aStyle));
      else
        if (aChild instanceof final PhloxGroupNode aChildGroup &&
            !aChildGroup.isRow () &&
            _hasPageBreakRecursive (aChildGroup))
        {
          final PdfBlockStyle aChildStyle = _getStyle (aAst, aChildGroup);
          _withInheritedStyle (aChildStyle, () -> {
            boolean bFirstPart = true;
            for (final PLVBox aPart : _getStackedPartsRecursive (aAst, aChildGroup, aChildStyle))
            {
              if (!bFirstPart)
                ret.add (_createGroupContent (aStyle));
              bFirstPart = false;
              if (aPart.getRowCount () > 0)
                ret.getLastOrNull ().addRow (_createGroupBox (aPart, aChildStyle));
            }
          });
        }
        else
          visitBlock (aAst, aChild, ret.getLastOrNull ()::addRow);
    }
    return ret;
  }

  /**
   * Add the blocks of a group below each other. A page break in a group below the page set splits
   * the group, because a page break only works as a direct child of the page set.
   */
  private void _addStackedGroup (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final PhloxGroupNode aGroup,
                                 @Nullable final PdfBlockStyle aStyle,
                                 @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    if (aSink == m_aPageSink && _hasPageBreakRecursive (aGroup))
    {
      boolean bFirstPart = true;
      for (final PLVBox aPart : _getStackedPartsRecursive (aAst, aGroup, aStyle))
      {
        if (!bFirstPart)
          aSink.accept (new PLPageBreak (true));
        bFirstPart = false;
        if (aPart.getRowCount () > 0)
          aSink.accept (_createGroupBox (aPart, aStyle));
      }
    }
    else
    {
      final PLVBox aBlocks = _createGroupContent (aStyle);
      visitBlockChildren (aAst, aGroup, aBlocks::addRow);
      aSink.accept (_createGroupBox (aBlocks, aStyle));
    }
  }

  /**
   * Add the cells of a row side by side.
   */
  private void _addRowGroup (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final PhloxGroupNode aGroup,
                             @Nullable final PdfBlockStyle aStyle,
                             @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    // A row (parties, totals) is moved to the next page as a whole
    final PLHBox aRow = new PdfKeepTogether.HBox (m_fMaxKeepTogetherHeight);
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
      final PdfBlockStyle aCellStyle = aCell instanceof PhloxGroupNode ? _getStyle (aAst, aCell) : null;
      final float fCellPercent = aCellStyle != null && aCellStyle.getWidthPercent () != null ? aCellStyle.getWidthPercent ()
                                                                                                         .floatValue ()
                                                                                             : (100f - fFixedPercent) /
                                                                                               Math.max (1, nStarCells);
      m_fContentWidth = fRowWidth * fCellPercent / 100f - ROW_GAP;
      // A nested row decorates itself
      final PhloxGroupNode aCellGroup = aCell instanceof final PhloxGroupNode aGroupCell && !aGroupCell.isRow ()
                                                                                                                 ? aGroupCell
                                                                                                                 : null;
      if (aCellGroup != null)
        _withInheritedStyle (aCellStyle, () -> visitBlockChildren (aAst, aCellGroup, aCellContent::addRow));
      else
        visitBlock (aAst, aCell, aCellContent::addRow);
      m_fContentWidth = fRowWidth;
      final PLBox aCellBox = _decorate (new PLBox (aCellContent), aCellGroup != null ? aCellStyle : null);
      if (aCell.getNext () != null)
        aCellBox.setMarginRight (ROW_GAP);
      final Float aWidth = aCellStyle == null ? null : aCellStyle.getWidthPercent ();
      aRow.addColumn (aCellBox, aWidth == null ? WidthSpec.star () : WidthSpec.perc (aWidth.floatValue ()));
    }
    aSink.accept (_createGroupBox (aRow, aStyle));
  }

  /**
   * Add the blocks of a group below each other, or the cells of a row side by side.
   */
  private void _addGroup (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final PhloxGroupNode aGroup,
                          @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aGroup);
    _withInheritedStyle (aStyle, () -> {
      if (aGroup.isRow ())
        _addRowGroup (aAst, aGroup, aStyle, aSink);
      else
        _addStackedGroup (aAst, aGroup, aStyle, aSink);
    });
  }

  @Override
  protected void visitText (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Text aText,
                            @NonNull final List <StringBuilder> aLines)
  {
    final String sText = m_bUppercase ? aText.getLiteral ().toUpperCase (m_aCtx.getLocale ()) : aText.getLiteral ();
    _getLastLine (aLines).append (_getProtected (PdfTextSupport.escapeMarkup (PdfTextSupport.separateCurrencySymbol (sText))));
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
    _getLastLine (aLines).append ('`').append (_getProtected (PdfTextSupport.escapeMarkup (aCode.getLiteral ()))).append ('`');
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
  protected void visitSpan (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final PhloxSpanNode aSpan,
                            @NonNull final List <StringBuilder> aLines)
  {
    final String sOldTextColor = m_sTextColor;
    final String sOldBackgroundColor = m_sBackgroundColor;
    String sOpen = "";
    String sClose = "";
    if (aSpan.getBackgroundColor () != null)
    {
      // A background is no toggle that nests: the outer one ends here and starts again afterwards
      m_sBackgroundColor = aSpan.getBackgroundColor ();
      sOpen = (sOldBackgroundColor != null ? "{bg}" : "") + "{bg:#" + m_sBackgroundColor + "}";
      sClose = "{bg}" + (sOldBackgroundColor != null ? "{bg:#" + sOldBackgroundColor + "}" : "");
    }
    if (aSpan.getTextColor () != null)
    {
      // A colour stays until the next one
      m_sTextColor = aSpan.getTextColor ();
      sOpen += "{color:#" + m_sTextColor + "}";
      sClose = "{color:#" + sOldTextColor + "}" + sClose;
    }
    _appendWrappedRecursive (aAst, aSpan, aLines, sOpen, sClose);
    m_sTextColor = sOldTextColor;
    m_sBackgroundColor = sOldBackgroundColor;
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
    // Raw HTML of a template is never interpreted, except for a line break, which is the only way
    // to break a line inside a table cell. All else is shown as text, as in the other formats
    if (BR_TAG.matcher (aHtml.getLiteral ()).matches ())
      aLines.add (new StringBuilder ());
    else
      _getLastLine (aLines).append (_getProtected (PdfTextSupport.escapeMarkup (aHtml.getLiteral ())));
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
    _addList (aAst, aList, nIndex -> "\u2022", BULLET_WIDTH, aSink);
  }

  @Override
  protected void visitOrderedList (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final OrderedList aList,
                                   @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final int nStart = aList.getMarkerStartNumber () == null ? 1 : aList.getMarkerStartNumber ().intValue ();
    _addList (aAst, aList, nIndex -> (nStart + nIndex) + ".", NUMBER_WIDTH, aSink);
  }

  @Override
  protected void visitBlockQuote (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BlockQuote aQuote,
                                  @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    final PdfBlockStyle aStyle = _getStyle (aAst, aQuote);
    _withInheritedStyle (aStyle, () -> {
      final PLVBox aContent = new PLVBox ();
      visitBlockChildren (aAst, aQuote, aContent::addRow);
      final PLBox aBox = _decorate (new PLBox (aContent), aStyle);
      aSink.accept (aBox.setPaddingLeft (Math.max (6f, aBox.getPaddingLeft ()))
                        .setBorderLeft (new BorderStyleSpec (PLColor.GRAY, 2f))
                        .setMarginBottom (PARAGRAPH_GAP)
                        .setStructureRole (EPLStructureRole.BLOCK_QUOTE));
    });
  }

  @Override
  protected void visitFencedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                       @NonNull final FencedCodeBlock aBlock,
                                       @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addCodeBlock (aAst, aBlock, aBlock.getLiteral (), aSink);
  }

  @Override
  protected void visitIndentedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                         @NonNull final IndentedCodeBlock aBlock,
                                         @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    _addCodeBlock (aAst, aBlock, aBlock.getLiteral (), aSink);
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
    aSink.accept (new PLText (aHtml.getLiteral ().strip (), _styled (m_aCtx.getBodyFont (), _getStyle (aAst, aHtml), true)));
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
    if (aSink == m_aPageSink)
      aSink.accept (new PLPageBreak (true));
    else
      LOGGER.warn ("A page break inside a row, list or block quote is not possible in PDF - ignoring it");
  }

  @Override
  protected void visitPageMargin (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final PhloxPageMarginNode aPageMargin,
                                  @NonNull final Consumer <IPLRenderableObject <?>> aSink)
  {
    // The page header and footer are not part of the flow: the page set shows them on every page
    final boolean bHeader = aPageMargin.isHeader ();
    if (bHeader ? !m_aCtx.isShowPageHeader () : !m_aCtx.isShowPageFooter ())
      return;
    final PdfBlockStyle aStyle = _getStyle (aAst, aPageMargin);
    final PLVBox aContent = new PLVBox ();
    m_bInPageMargin = true;
    try
    {
      _withInheritedStyle (aStyle, () -> visitBlockChildren (aAst, aPageMargin, aContent::addRow));
    }
    finally
    {
      m_bInPageMargin = false;
    }
    if (aContent.getRowCount () > 0)
    {
      final PLBox aBox = new PLBox (aContent);
      if (bHeader)
      {
        // The page set draws the header from the top edge of the sheet: move it into the margin
        aBox.setMarginTop (m_aCtx.getMargins ().getTop () / 2).setMarginBottom (PARAGRAPH_GAP);
        m_aPageHeader = _decorate (aBox, aStyle);
      }
      else
      {
        aBox.setMarginTop (PARAGRAPH_GAP);
        m_aPageFooter = _decorate (aBox, aStyle);
      }
    }
  }

  @Override
  protected void visitPageNumber (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final PhloxPageNumberNode aPageNumber,
                                  @NonNull final List <StringBuilder> aLines)
  {
    if (m_bInPageMargin)
    {
      final EPLPlaceholder ePlaceholder = aPageNumber.isPageCount () ? EPLPlaceholder.TOTAL_PAGE_COUNT
                                                                     : EPLPlaceholder.TOTAL_PAGE_NUMBER;
      _getLastLine (aLines).append (PdfTextSupport.escapeMarkup (ePlaceholder.getVariable ()));
    }
  }

  /**
   * Convert the template AST to the content of a PDF page set.
   *
   * @param aAst
   *        The rendered template. Never {@code null}.
   * @return The page set with the size and the margins of the render context, and the page header
   *         and footer of the template if it has them and they are shown. Without
   *         {@link PdfRenderContext#isShowPageHeaderOnFirstPage()} the first page has no header (a
   *         different first page header that is <code>null</code>), and likewise for the footer.
   *         Never {@code null}.
   * @throws PhloxRuntimeException
   *         If the template has no content that can be shown in a PDF.
   */
  @NonNull
  public PLPageSet emit (@NonNull final PhloxTemplateAst aAst)
  {
    final PLPageSet aPS = new PLPageSet (m_aCtx.getPageSize ()).setMargin (m_aCtx.getMargins ());
    m_aPageSink = aPS::addElement;
    visitBlockChildren (aAst, aAst.getRoot (), m_aPageSink);
    if (m_aPageHeader != null)
    {
      aPS.setPageHeader (m_aPageHeader);
      aPS.setDifferentFirstPageHeader (!m_aCtx.isShowPageHeaderOnFirstPage ());
    }
    if (m_aPageFooter != null)
    {
      aPS.setPageFooter (m_aPageFooter);
      aPS.setDifferentFirstPageFooter (!m_aCtx.isShowPageFooterOnFirstPage ());
    }
    if (aPS.getElementCount () == 0)
      throw new PhloxRuntimeException ("The template produced no content for the PDF. The reason may be an image that could not be resolved or that PDF cannot show, like SVG.");
    return aPS;
  }
}
