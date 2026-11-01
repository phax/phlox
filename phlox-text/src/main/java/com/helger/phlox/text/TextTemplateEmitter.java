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

import java.util.Locale;
import java.util.regex.Pattern;

import org.commonmark.ext.gfm.strikethrough.Strikethrough;
import org.commonmark.ext.gfm.tables.TableBlock;
import org.commonmark.ext.gfm.tables.TableBody;
import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableHead;
import org.commonmark.ext.gfm.tables.TableRow;
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
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.phlox.api.watermark.PhloxProgramWatermark;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.template.PhloxTemplatePostParser;
import com.helger.phlox.template.ast.AbstractPhloxAstVisitor;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxPageBreakNode;
import com.helger.phlox.template.ast.PhloxResolvedImage;
import com.helger.phlox.template.ast.PhloxTemplateAst;

/**
 * Turns the {@link PhloxTemplateAst} of a rendered template into plain text. Inline styling
 * (strong, emphasis, strikethrough) is stripped, headings of level 1 and 2 are underlined, tables
 * are laid out in aligned columns, groups and rows are stacked, and paragraphs are wrapped at
 * {@link TextRenderContext#getLineWidth()}. An image is replaced by its alternative text in
 * brackets. The user watermark is the first line and the program watermark the last line of the
 * output. Block classes set with {@code {{class:name}}} have no effect.
 * <p>
 * The output may go to a terminal, so all control characters except the line feed and the tab, and
 * the bidirectional embedding, override and isolate controls are removed from every text. They
 * could move the cursor, recolour the terminal or show an IBAN in a different order than it has.
 * The left-to-right and right-to-left marks stay: they are needed for mixed scripts.
 *
 * @author Philip Helger
 */
public final class TextTemplateEmitter extends AbstractPhloxAstVisitor <TextBlockSink, StringBuilder>
{
  /** The width of a rule if the line width is unlimited. */
  private static final int UNLIMITED_RULE_WIDTH = 80;
  /** The space between two table columns. */
  private static final String COLUMN_GAP = "  ";

  private static final Pattern BR_TAG = Pattern.compile ("<br\\s*/?>", Pattern.CASE_INSENSITIVE);

  /** One cell of a table: its lines and its alignment. */
  private static final class TextTableCell
  {
    private final ICommonsList <String> m_aLines;
    private final TableCell.Alignment m_eAlignment;

    TextTableCell (@NonNull final ICommonsList <String> aLines, final TableCell.@Nullable Alignment eAlignment)
    {
      m_aLines = aLines;
      m_eAlignment = eAlignment;
    }
  }

  private final TextRenderContext m_aCtx;

  /**
   * Create a new emitter.
   *
   * @param aCtx
   *        The render context that provides the line width and the watermarks. Never
   *        {@code null}.
   */
  public TextTemplateEmitter (@NonNull final TextRenderContext aCtx)
  {
    m_aCtx = aCtx;
  }

  private static boolean _isForbiddenCharacter (final char cChar)
  {
    // C0 except tab and line feed, DEL, C1
    if (cChar < 0x20)
      return cChar != '\t' && cChar != '\n';
    if (cChar >= 0x7f && cChar <= 0x9f)
      return true;
    // Bidi embedding and override (U+202A to U+202E), bidi isolate (U+2066 to U+2069)
    return (cChar >= 0x202a && cChar <= 0x202e) || (cChar >= 0x2066 && cChar <= 0x2069);
  }

  /**
   * Remove the characters that must not reach the output.
   *
   * @see #_isForbiddenCharacter(char)
   */
  @NonNull
  private static String _clean (@NonNull final String sText)
  {
    final int nLength = sText.length ();
    int nIndex = 0;
    while (nIndex < nLength && !_isForbiddenCharacter (sText.charAt (nIndex)))
      nIndex++;
    if (nIndex == nLength)
      return sText;

    final StringBuilder aSB = new StringBuilder (nLength);
    aSB.append (sText, 0, nIndex);
    for (; nIndex < nLength; ++nIndex)
    {
      final char cChar = sText.charAt (nIndex);
      if (!_isForbiddenCharacter (cChar))
        aSB.append (cChar);
    }
    return aSB.toString ();
  }

  private static int _length (@NonNull final String s)
  {
    return s.codePointCount (0, s.length ());
  }

  @NonNull
  private static String _pad (@NonNull final String s,
                              final int nWidth,
                              final TableCell.@Nullable Alignment eAlignment)
  {
    final int nMissing = nWidth - _length (s);
    if (nMissing <= 0)
      return s;
    if (eAlignment == TableCell.Alignment.RIGHT)
      return StringHelper.getRepeated (' ', nMissing) + s;
    if (eAlignment == TableCell.Alignment.CENTER)
      return StringHelper.getRepeated (' ', nMissing / 2) + s + StringHelper.getRepeated (' ', nMissing - nMissing / 2);
    return s + StringHelper.getRepeated (' ', nMissing);
  }

  /**
   * Split the collected inline text into lines. Empty lines at the start and at the end are
   * removed.
   */
  @NonNull
  private static ICommonsList <String> _toLines (@NonNull final CharSequence aText)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final String sLine : StringHelper.getExploded ('\n', aText.toString ()))
      ret.add (sLine.strip ());
    while (ret.isNotEmpty () && ret.getFirstOrNull ().isEmpty ())
      ret.removeFirstOrNull ();
    while (ret.isNotEmpty () && ret.getLastOrNull ().isEmpty ())
      ret.removeLastOrNull ();
    return ret;
  }

  /**
   * Wrap a line at the given width. Words are only separated at spaces; a word that is longer than
   * the width stays on a line of its own.
   */
  @NonNull
  private static ICommonsList <String> _wrap (@NonNull final String sLine, final int nWidth)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    if (nWidth <= 0 || _length (sLine) <= nWidth)
    {
      ret.add (sLine);
      return ret;
    }
    final StringBuilder aCurrent = new StringBuilder ();
    for (final String sWord : StringHelper.getExploded (' ', sLine))
    {
      if (sWord.isEmpty ())
        continue;
      if (aCurrent.length () > 0 && _length (aCurrent.toString ()) + 1 + _length (sWord) > nWidth)
      {
        ret.add (aCurrent.toString ());
        aCurrent.setLength (0);
      }
      if (aCurrent.length () > 0)
        aCurrent.append (' ');
      aCurrent.append (sWord);
    }
    if (aCurrent.length () > 0)
      ret.add (aCurrent.toString ());
    return ret;
  }

  @NonNull
  private static String _getRule (@NonNull final TextBlockSink aSink)
  {
    return StringHelper.getRepeated ('-', aSink.getWidth () > 0 ? aSink.getWidth () : UNLIMITED_RULE_WIDTH);
  }

  private static boolean _isSafeLink (@Nullable final String sDestination)
  {
    if (sDestination == null)
      return false;
    final int nColon = sDestination.indexOf (':');
    if (nColon < 0)
      return true;
    final String sScheme = sDestination.substring (0, nColon).toLowerCase (Locale.ROOT);
    return sScheme.equals ("http") || sScheme.equals ("https") || sScheme.equals ("mailto");
  }

  /**
   * @return The alternative text of the image in brackets, or <code>null</code> if the image has
   *         no alternative text.
   */
  @Nullable
  private String _getImageText (@NonNull final PhloxTemplateAst aAst, @NonNull final Image aImage)
  {
    final PhloxResolvedImage aResolved = aAst.getResolvedImage (aImage);
    // Nothing to show for an attachment or QR code that could not be resolved, as in HTML
    if (aResolved == null &&
        (aImage.getDestination ().startsWith ("attachment:") ||
         aImage.getDestination ().startsWith (PhloxTemplatePostParser.QRCODE_URL_PREFIX)))
      return null;
    String sAlt = aResolved == null || aResolved.getAltText () == null ? null : _clean (aResolved.getAltText ());
    if (StringHelper.isEmpty (sAlt))
    {
      final StringBuilder aAlt = new StringBuilder ();
      visitInlineChildren (aAst, aImage, aAlt);
      sAlt = aAlt.toString ().strip ();
    }
    return sAlt.isEmpty () ? null : "[" + sAlt + "]";
  }

  /**
   * Visit the inline children of a block and wrap the lines at the width of the sink.
   */
  @NonNull
  private ICommonsList <String> _getWrappedLines (@NonNull final PhloxTemplateAst aAst,
                                                  @NonNull final Node aBlock,
                                                  final int nWidth)
  {
    final StringBuilder aText = new StringBuilder ();
    visitInlineChildren (aAst, aBlock, aText);
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final String sLine : _toLines (aText))
      ret.addAll (_wrap (sLine, nWidth));
    return ret;
  }

  /**
   * Prefix the lines of the blocks of a nested sink: the first line gets the marker, all other
   * lines get an indentation of the same width.
   */
  private static void _addPrefixed (@NonNull final ICommonsList <String> aNestedLines,
                                    @NonNull final String sMarker,
                                    @NonNull final ICommonsList <String> aTarget)
  {
    final String sIndent = StringHelper.getRepeated (' ', _length (sMarker));
    boolean bFirst = true;
    for (final String sLine : aNestedLines)
    {
      aTarget.add (sLine.isEmpty () ? "" : (bFirst ? sMarker : sIndent) + sLine);
      bFirst = false;
    }
    if (bFirst)
      aTarget.add (sMarker);
  }

  private void _emitList (@NonNull final PhloxTemplateAst aAst,
                          @NonNull final Node aList,
                          final int nStartNumber,
                          final boolean bOrdered,
                          @NonNull final TextBlockSink aSink)
  {
    final ICommonsList <String> aLines = new CommonsArrayList <> ();
    int nNumber = nStartNumber;
    for (Node aItem = aList.getFirstChild (); aItem instanceof ListItem; aItem = aItem.getNext ())
    {
      final String sMarker = bOrdered ? Integer.toString (nNumber) + ". " : "- ";
      nNumber++;
      final TextBlockSink aItemSink = new TextBlockSink (aSink.getWidth () > 0 ? Math.max (1,
                                                                                           aSink.getWidth () -
                                                                                              _length (sMarker))
                                                                               : 0);
      visitBlockChildren (aAst, aItem, aItemSink);
      _addPrefixed (aItemSink.getAllLines (), sMarker, aLines);
    }
    aSink.addBlock (aLines);
  }

  private void _emitTable (@NonNull final PhloxTemplateAst aAst,
                           @NonNull final TableBlock aTable,
                           @NonNull final TextBlockSink aSink)
  {
    final ICommonsList <ICommonsList <TextTableCell>> aRows = new CommonsArrayList <> ();
    boolean bHasHead = false;
    for (Node aPart = aTable.getFirstChild (); aPart != null; aPart = aPart.getNext ())
    {
      final boolean bHead = aPart instanceof TableHead;
      if (!bHead && !(aPart instanceof TableBody))
        continue;
      if (bHead)
        bHasHead = true;

      for (Node aRowNode = aPart.getFirstChild (); aRowNode instanceof TableRow; aRowNode = aRowNode.getNext ())
      {
        final ICommonsList <TextTableCell> aRow = new CommonsArrayList <> ();
        for (Node aCellNode = aRowNode.getFirstChild (); aCellNode instanceof final TableCell aCell; aCellNode = aCellNode.getNext ())
        {
          final StringBuilder aText = new StringBuilder ();
          visitInlineChildren (aAst, aCell, aText);
          aRow.add (new TextTableCell (_toLines (aText), aCell.getAlignment ()));
        }
        aRows.add (aRow);
      }
    }
    if (aRows.isEmpty ())
      return;

    // The width of a column is the length of its longest line
    int nColumns = 0;
    for (final ICommonsList <TextTableCell> aRow : aRows)
      nColumns = Math.max (nColumns, aRow.size ());
    final int [] aWidths = new int [nColumns];
    for (final ICommonsList <TextTableCell> aRow : aRows)
      for (int nCol = 0; nCol < aRow.size (); ++nCol)
        for (final String sLine : aRow.get (nCol).m_aLines)
          aWidths[nCol] = Math.max (aWidths[nCol], _length (sLine));

    // A header row without any text (as in a table of key-value pairs) is not shown
    boolean bShowHead = false;
    if (bHasHead)
      for (final TextTableCell aCell : aRows.getFirstOrNull ())
        if (aCell.m_aLines.isNotEmpty ())
          bShowHead = true;

    final ICommonsList <String> aLines = new CommonsArrayList <> ();
    for (int nRow = bHasHead && !bShowHead ? 1 : 0; nRow < aRows.size (); ++nRow)
    {
      final ICommonsList <TextTableCell> aRow = aRows.get (nRow);
      int nHeight = 1;
      for (final TextTableCell aCell : aRow)
        nHeight = Math.max (nHeight, aCell.m_aLines.size ());
      for (int nLine = 0; nLine < nHeight; ++nLine)
      {
        final StringBuilder aLine = new StringBuilder ();
        for (int nCol = 0; nCol < nColumns; ++nCol)
        {
          // A column without any text takes no space
          if (aWidths[nCol] == 0)
            continue;
          final TextTableCell aCell = nCol < aRow.size () ? aRow.get (nCol) : null;
          final String sText = aCell != null && nLine < aCell.m_aLines.size () ? aCell.m_aLines.get (nLine) : "";
          if (aLine.length () > 0)
            aLine.append (COLUMN_GAP);
          aLine.append (_pad (sText, aWidths[nCol], aCell == null ? null : aCell.m_eAlignment));
        }
        aLines.add (aLine.toString ());
      }
      if (nRow == 0 && bShowHead)
      {
        final StringBuilder aRule = new StringBuilder ();
        for (final int nWidth : aWidths)
          if (nWidth > 0)
          {
            if (aRule.length () > 0)
              aRule.append (COLUMN_GAP);
            aRule.append (StringHelper.getRepeated ('-', nWidth));
          }
        aLines.add (aRule.toString ());
      }
    }
    aSink.addBlock (aLines);
  }

  @Override
  protected void visitText (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Text aText,
                            @NonNull final StringBuilder aTarget)
  {
    aTarget.append (_clean (aText.getLiteral ()));
  }

  @Override
  protected void visitSoftLineBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final SoftLineBreak aBreak,
                                     @NonNull final StringBuilder aTarget)
  {
    // A line break in the template is a line break in the output
    aTarget.append ('\n');
  }

  @Override
  protected void visitHardLineBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final HardLineBreak aBreak,
                                     @NonNull final StringBuilder aTarget)
  {
    aTarget.append ('\n');
  }

  @Override
  protected void visitStrongEmphasis (@NonNull final PhloxTemplateAst aAst,
                                      @NonNull final StrongEmphasis aStrong,
                                      @NonNull final StringBuilder aTarget)
  {
    visitInlineChildren (aAst, aStrong, aTarget);
  }

  @Override
  protected void visitEmphasis (@NonNull final PhloxTemplateAst aAst,
                                @NonNull final Emphasis aEmphasis,
                                @NonNull final StringBuilder aTarget)
  {
    visitInlineChildren (aAst, aEmphasis, aTarget);
  }

  @Override
  protected void visitStrikethrough (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final Strikethrough aStrikethrough,
                                     @NonNull final StringBuilder aTarget)
  {
    visitInlineChildren (aAst, aStrikethrough, aTarget);
  }

  @Override
  protected void visitCode (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Code aCode,
                            @NonNull final StringBuilder aTarget)
  {
    aTarget.append (_clean (aCode.getLiteral ()));
  }

  @Override
  protected void visitLink (@NonNull final PhloxTemplateAst aAst,
                            @NonNull final Link aLink,
                            @NonNull final StringBuilder aTarget)
  {
    final StringBuilder aText = new StringBuilder ();
    visitInlineChildren (aAst, aLink, aText);
    aTarget.append (aText);

    // The destination is shown after the text, unless the text already says it
    final String sDestination = aLink.getDestination ();
    if (_isSafeLink (sDestination))
    {
      final String sShown = _clean (StringHelper.trimStart (sDestination, "mailto:"));
      if (!sShown.equals (aText.toString ().strip ()))
        aTarget.append (" (").append (sShown).append (')');
    }
  }

  @Override
  protected void visitInlineImage (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final Image aImage,
                                   @NonNull final StringBuilder aTarget)
  {
    final String sText = _getImageText (aAst, aImage);
    if (sText != null)
      aTarget.append (sText);
  }

  @Override
  protected void visitHtmlInline (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final HtmlInline aHtml,
                                  @NonNull final StringBuilder aTarget)
  {
    // A <br> is a line break (the only way to break a line inside a table cell), all other raw
    // HTML is shown as text, as in the other formats
    if (BR_TAG.matcher (aHtml.getLiteral ()).matches ())
      aTarget.append ('\n');
    else
      aTarget.append (_clean (aHtml.getLiteral ()));
  }

  @Override
  protected void visitHeading (@NonNull final PhloxTemplateAst aAst,
                               @NonNull final Heading aHeading,
                               @NonNull final TextBlockSink aSink)
  {
    final StringBuilder aText = new StringBuilder ();
    visitInlineChildren (aAst, aHeading, aText);
    final String sText = String.join (" ", _toLines (aText));
    final ICommonsList <String> aLines = new CommonsArrayList <> (sText);
    if (aHeading.getLevel () == 1)
      aLines.add (StringHelper.getRepeated ('=', _length (sText)));
    else
      if (aHeading.getLevel () == 2)
        aLines.add (StringHelper.getRepeated ('-', _length (sText)));
    aSink.addBlock (aLines);
  }

  @Override
  protected void visitParagraph (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final Paragraph aParagraph,
                                 @NonNull final TextBlockSink aSink)
  {
    aSink.addBlock (_getWrappedLines (aAst, aParagraph, aSink.getWidth ()));
  }

  @Override
  protected void visitBulletList (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BulletList aList,
                                  @NonNull final TextBlockSink aSink)
  {
    _emitList (aAst, aList, 1, false, aSink);
  }

  @Override
  protected void visitOrderedList (@NonNull final PhloxTemplateAst aAst,
                                   @NonNull final OrderedList aList,
                                   @NonNull final TextBlockSink aSink)
  {
    final Integer aStart = aList.getMarkerStartNumber ();
    _emitList (aAst, aList, aStart == null ? 1 : aStart.intValue (), true, aSink);
  }

  @Override
  protected void visitBlockQuote (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final BlockQuote aQuote,
                                  @NonNull final TextBlockSink aSink)
  {
    // A block quote is indented
    final String sIndent = "  ";
    final TextBlockSink aQuoteSink = new TextBlockSink (aSink.getWidth () > 0 ? Math.max (1,
                                                                                          aSink.getWidth () -
                                                                                             sIndent.length ())
                                                                              : 0);
    visitBlockChildren (aAst, aQuote, aQuoteSink);
    final ICommonsList <String> aLines = new CommonsArrayList <> ();
    for (final String sLine : aQuoteSink.getAllLines ())
      aLines.add (sLine.isEmpty () ? "" : sIndent + sLine);
    aSink.addBlock (aLines);
  }

  @Override
  protected void visitFencedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                       @NonNull final FencedCodeBlock aBlock,
                                       @NonNull final TextBlockSink aSink)
  {
    aSink.addBlock (StringHelper.getExploded ('\n', StringHelper.trimEnd (_clean (aBlock.getLiteral ()), '\n')));
  }

  @Override
  protected void visitIndentedCodeBlock (@NonNull final PhloxTemplateAst aAst,
                                         @NonNull final IndentedCodeBlock aBlock,
                                         @NonNull final TextBlockSink aSink)
  {
    aSink.addBlock (StringHelper.getExploded ('\n', StringHelper.trimEnd (_clean (aBlock.getLiteral ()), '\n')));
  }

  @Override
  protected void visitThematicBreak (@NonNull final PhloxTemplateAst aAst,
                                     @NonNull final ThematicBreak aBreak,
                                     @NonNull final TextBlockSink aSink)
  {
    aSink.addLine (_getRule (aSink));
  }

  @Override
  protected void visitTable (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final TableBlock aTable,
                             @NonNull final TextBlockSink aSink)
  {
    _emitTable (aAst, aTable, aSink);
  }

  @Override
  protected void visitBlockImage (@NonNull final PhloxTemplateAst aAst,
                                  @NonNull final Image aImage,
                                  @NonNull final TextBlockSink aSink)
  {
    final String sText = _getImageText (aAst, aImage);
    if (sText != null)
      aSink.addLine (sText);
  }

  @Override
  protected void visitHtmlBlock (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final HtmlBlock aHtml,
                                 @NonNull final TextBlockSink aSink)
  {
    aSink.addBlock (StringHelper.getExploded ('\n', StringHelper.trimEnd (_clean (aHtml.getLiteral ()), '\n')));
  }

  @Override
  protected void visitGroup (@NonNull final PhloxTemplateAst aAst,
                             @NonNull final PhloxGroupNode aGroup,
                             @NonNull final TextBlockSink aSink)
  {
    // Groups and rows are stacked: plain text has no columns
    visitBlockChildren (aAst, aGroup, aSink);
  }

  @Override
  protected void visitPageBreak (@NonNull final PhloxTemplateAst aAst,
                                 @NonNull final PhloxPageBreakNode aPageBreak,
                                 @NonNull final TextBlockSink aSink)
  {
    // Plain text has no pages - a page break is shown as a rule
    aSink.addLine (_getRule (aSink));
  }

  /**
   * Convert the template AST to plain text.
   *
   * @param aAst
   *        The rendered template. Never {@code null}.
   * @return The plain text with <code>\n</code> as line separator and a line separator at the end.
   *         Never {@code null}.
   */
  @NonNull
  public String emit (@NonNull final PhloxTemplateAst aAst)
  {
    final TextBlockSink aSink = new TextBlockSink (m_aCtx.getLineWidth ());
    final WatermarkSpec aUW = m_aCtx.getUserWatermark ();
    if (aUW != null)
      aSink.addLine ("*** " + _clean (aUW.getText ()) + " ***");
    visitBlockChildren (aAst, aAst.getRoot (), aSink);
    final WatermarkSpec aPW = PhloxProgramWatermark.getInstance ();
    if (aPW != null)
      aSink.addLine (_clean (aPW.getText ()));
    return String.join ("\n", aSink.getAllLines ()) + "\n";
  }
}
