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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Base64;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.commonmark.ext.gfm.tables.TableCell;
import org.commonmark.ext.gfm.tables.TableRow;
import org.commonmark.node.AbstractVisitor;
import org.commonmark.node.Code;
import org.commonmark.node.Image;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.base.string.StringReplace;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.io.resource.ClassPathResource;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxSupportingDocument;
import com.helger.phlox.template.PhloxTemplateException.EStage;
import com.helger.phlox.template.adapter.PhloxInvoiceMustacheContext;
import com.helger.phlox.template.ast.PhloxGroupNode;
import com.helger.phlox.template.ast.PhloxPageBreakNode;
import com.helger.phlox.template.ast.PhloxResolvedImage;

/**
 * Walks the commonmark-java AST after parsing and performs the phlox-specific transforms described
 * on the wiki page Template-Processing, section "Stage 2a":
 * <ol>
 * <li>Builds a {@link PhloxGroupNode} from the sentinels of {@code {{group:name}}},
 * {@code {{row:name}}} and {@code {{end}}}.</li>
 * <li>Extracts the sentinels of {@code {{class:name}}} from {@link Text} nodes and attaches the
 * class to the next sibling block via a {@code Map<Node, String>}.</li>
 * <li>Replaces the sentinels of {@code {{pagebreak}}} with {@link PhloxPageBreakNode}.</li>
 * <li>Drops all other sentinels with a warning.</li>
 * <li>Resolves {@link Image} {@code destination} URLs ({@code data:}, {@code attachment:logo},
 * {@code attachment:BG-24[i]}, {@code classpath:/…}, {@code qrcode:…}) into
 * {@link PhloxResolvedImage} entries.
 * Rejects {@code http:}, {@code https:}, {@code file:} schemes with
 * {@link PhloxTemplateException}.</li>
 * </ol>
 * A sentinel only comes from a template, see {@link PhloxInvoiceMustacheContext#SENTINEL_MARKER}.
 *
 * @author Philip Helger
 */
public final class PhloxTemplatePostParser
{
  /** The prefix of an image URL that shows the percent-encoded text after it as a QR code. */
  public static final String QRCODE_URL_PREFIX = "qrcode:";

  /** The height of a QR code without an explicit {@code height-px}. */
  public static final int DEFAULT_QRCODE_HEIGHT_PX = 120;

  /**
   * The maximum nesting of {@code {{group:name}}} and {@code {{row:name}}}. The layout effort of the
   * PDF writer grows exponentially with the depth.
   */
  public static final int MAX_GROUP_DEPTH = 16;

  private static final Logger LOGGER = LoggerFactory.getLogger (PhloxTemplatePostParser.class);

  private static final String MARKER = PhloxInvoiceMustacheContext.SENTINEL_CLASS_SUFFIX;
  private static final Pattern CLASS_SENTINEL = Pattern.compile (PhloxInvoiceMustacheContext.SENTINEL_CLASS_PREFIX +
                                                                 "([^" +
                                                                 MARKER +
                                                                 "]+)" +
                                                                 MARKER);
  private static final Pattern GROUP_SENTINEL = Pattern.compile (MARKER +
                                                                 "PHLOX:(GROUP|ROW)=([^" +
                                                                 MARKER +
                                                                 "]+)" +
                                                                 MARKER);
  private static final Pattern ANY_SENTINEL = Pattern.compile (MARKER + "PHLOX:[^" + MARKER + "]*" + MARKER);
  private static final String END_SENTINEL = PhloxInvoiceMustacheContext.SENTINEL_END;
  private static final String PAGEBREAK_SENTINEL = PhloxInvoiceMustacheContext.SENTINEL_PAGEBREAK;
  private static final String DATA_URL_PREFIX = "data:";
  private static final int MAX_DATA_URL_DISPLAY_LENGTH = 40;
  private static final Pattern ATTACHMENT_BG24_PATTERN = Pattern.compile ("attachment:BG-24\\[(\\d+)\\]");
  // The image title holds the options of an image, e.g. ![alt](url "height-px:120")
  private static final Pattern HEIGHT_PX_TITLE = Pattern.compile ("height-px:([1-9][0-9]{0,4})");

  private final PhloxInvoice m_aInvoice;
  private final AbstractRenderContext <?> m_aContext;
  private final ICommonsMap <Node, String> m_aBlockClassMap = new CommonsHashMap <> ();
  private final ICommonsMap <Image, PhloxResolvedImage> m_aImageMap = new CommonsHashMap <> ();

  /**
   * Removes the sentinels that are left after the layout was built: a layout tag next to text or
   * below the top level, and a class in a heading or a table cell. They have no effect.
   */
  private static final class StraySentinelRemover extends AbstractVisitor
  {
    @NonNull
    private static String _getCleaned (@NonNull final String sText)
    {
      if (sText.indexOf (PhloxInvoiceMustacheContext.SENTINEL_MARKER) < 0)
        return sText;
      LOGGER.warn ("A template tag has no effect here: a group, row or end tag needs a top-level paragraph of its own, and a class tag a line in front of the block");
      return ANY_SENTINEL.matcher (sText).replaceAll ("");
    }

    @Override
    public void visit (final Text aText)
    {
      final String sCleaned = _getCleaned (aText.getLiteral ());
      if (!sCleaned.isEmpty ())
        aText.setLiteral (sCleaned);
      else
      {
        final Node aParent = aText.getParent ();
        aText.unlink ();
        if (aParent instanceof Paragraph && aParent.getFirstChild () == null)
          aParent.unlink ();
      }
    }

    @Override
    public void visit (final Code aCode)
    {
      aCode.setLiteral (_getCleaned (aCode.getLiteral ()));
    }
  }

  private final class ImageResolver extends AbstractVisitor
  {
    @Override
    public void visit (final Image aImage)
    {
      final String sDest = aImage.getDestination ();
      if (sDest == null || sDest.isEmpty ())
      {
        super.visit (aImage);
        return;
      }
      // Fail on a bad option at parse time, like on a bad URL
      getImageHeightPx (aImage);
      final PhloxResolvedImage aResolved = _resolveImage (sDest, aImage);
      if (aResolved != null)
        m_aImageMap.put (aImage, aResolved);
      super.visit (aImage);
    }
  }

  /**
   * Construct a post-parser bound to the given invoice and render context. Both are needed
   * for image-URL resolution (the invoice supplies BG-24 supporting docs, the render context
   * supplies the optional logo).
   *
   * @param aInvoice
   *        Source invoice.
   * @param aContext
   *        Render context.
   */
  public PhloxTemplatePostParser (@NonNull final PhloxInvoice aInvoice,
                                  @NonNull final AbstractRenderContext <?> aContext)
  {
    m_aInvoice = aInvoice;
    m_aContext = aContext;
  }

  private void _attachClassesTo (@NonNull final Node aTarget, @NonNull final List <String> aClasses)
  {
    if (aClasses.isEmpty ())
      return;
    final String sExisting = m_aBlockClassMap.get (aTarget);
    final String sJoined = String.join (" ", aClasses);
    m_aBlockClassMap.put (aTarget, sExisting == null ? sJoined : sExisting + " " + sJoined);
  }

  private void _processParagraph (@NonNull final Paragraph aParagraph)
  {
    final ICommonsList <String> aClassesFound = new CommonsArrayList <> ();
    boolean bHasPageBreak = false;
    boolean bHasNonSentinelContent = false;

    Node aInline = aParagraph.getFirstChild ();
    while (aInline != null)
    {
      final Node aNextInline = aInline.getNext ();
      if (aInline instanceof final Text aText)
      {
        final String sOriginal = aText.getLiteral ();
        final StringBuilder aRemaining = new StringBuilder ();
        final Matcher aClassMatcher = CLASS_SENTINEL.matcher (sOriginal);
        int nLastEnd = 0;
        while (aClassMatcher.find ())
        {
          aRemaining.append (sOriginal, nLastEnd, aClassMatcher.start ());
          aClassesFound.add (aClassMatcher.group (1));
          nLastEnd = aClassMatcher.end ();
        }
        aRemaining.append (sOriginal, nLastEnd, sOriginal.length ());

        String sCleaned = aRemaining.toString ();
        // Strip any PAGEBREAK sentinels too.
        if (sCleaned.contains (PAGEBREAK_SENTINEL))
        {
          bHasPageBreak = true;
          sCleaned = StringReplace.replaceAll (sCleaned, PAGEBREAK_SENTINEL, "");
        }

        if (sCleaned.isEmpty ())
          aText.unlink ();
        else
        {
          aText.setLiteral (sCleaned);
          if (!sCleaned.isBlank ())
            bHasNonSentinelContent = true;
        }
      }
      else
      {
        bHasNonSentinelContent = true;
      }
      aInline = aNextInline;
    }

    // Decide what to do with the paragraph now.
    if (bHasPageBreak && !bHasNonSentinelContent)
    {
      final PhloxPageBreakNode aBreak = new PhloxPageBreakNode ();
      aParagraph.insertBefore (aBreak);
      aParagraph.unlink ();
      _attachClassesTo (aBreak, aClassesFound);
      return;
    }

    if (!bHasNonSentinelContent && aParagraph.getFirstChild () == null)
    {
      // Paragraph is now empty; attach the collected classes to the next sibling block, if any.
      final Node aNextSibling = aParagraph.getNext ();
      aParagraph.unlink ();
      if (aNextSibling != null)
        _attachClassesTo (aNextSibling, aClassesFound);
      return;
    }

    // Paragraph still has content — keep it, attach classes to it directly.
    _attachClassesTo (aParagraph, aClassesFound);
  }

  /**
   * A line {@code {{class:name}}} inside a table is parsed by the GFM extension as a row whose first
   * cell holds the sentinel. Such a row carries the class of the row that follows it.
   *
   * @param aRow
   *        The table row to check. Never {@code null}.
   * @return {@code true} if the row only consists of class sentinels and was removed.
   */
  private boolean _processClassRow (@NonNull final TableRow aRow)
  {
    final ICommonsList <String> aClassesFound = new CommonsArrayList <> ();
    boolean bFirstCell = true;
    for (Node aCell = aRow.getFirstChild (); aCell instanceof TableCell; aCell = aCell.getNext ())
    {
      if (bFirstCell)
      {
        bFirstCell = false;
        for (Node aInline = aCell.getFirstChild (); aInline != null; aInline = aInline.getNext ())
        {
          if (!(aInline instanceof final Text aText))
            return false;
          final Matcher aClassMatcher = CLASS_SENTINEL.matcher (aText.getLiteral ());
          int nLastEnd = 0;
          while (aClassMatcher.find ())
          {
            if (!aText.getLiteral ().substring (nLastEnd, aClassMatcher.start ()).isBlank ())
              return false;
            aClassesFound.add (aClassMatcher.group (1));
            nLastEnd = aClassMatcher.end ();
          }
          if (!aText.getLiteral ().substring (nLastEnd).isBlank ())
            return false;
        }
      }
      else
        if (aCell.getFirstChild () != null)
          return false;
    }
    if (aClassesFound.isEmpty ())
      return false;

    final Node aNextRow = aRow.getNext ();
    if (aNextRow != null)
      _attachClassesTo (aNextRow, aClassesFound);
    aRow.unlink ();
    return true;
  }

  /**
   * @return The lines of a paragraph that only consists of text and line breaks, or
   *         <code>null</code>.
   */
  @Nullable
  private static List <String> _getPlainLines (@NonNull final Node aNode)
  {
    if (!(aNode instanceof Paragraph))
      return null;
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    StringBuilder aSB = new StringBuilder ();
    for (Node aChild = aNode.getFirstChild (); aChild != null; aChild = aChild.getNext ())
    {
      if (aChild instanceof final Text aText)
        aSB.append (aText.getLiteral ());
      else
        if (aChild instanceof SoftLineBreak)
        {
          ret.add (aSB.toString ().strip ());
          aSB = new StringBuilder ();
        }
        else
          return null;
    }
    ret.add (aSB.toString ().strip ());
    return ret;
  }

  private static boolean _isLayoutSentinel (@NonNull final String sLine)
  {
    return END_SENTINEL.equals (sLine) || GROUP_SENTINEL.matcher (sLine).matches ();
  }

  /**
   * @return The layout sentinels of a paragraph that only consists of group, row and end
   *         sentinels, or <code>null</code> for any other node.
   */
  @Nullable
  private static List <String> _getLayoutSentinels (@NonNull final Node aNode)
  {
    final List <String> aLines = _getPlainLines (aNode);
    if (aLines == null)
      return null;
    for (final String sLine : aLines)
      if (!_isLayoutSentinel (sLine))
        return null;
    return aLines;
  }

  /**
   * Turn the paragraphs holding group, row or end sentinels into {@link PhloxGroupNode}s that
   * contain the blocks up to the matching end. Only the top level of the document is handled.
   *
   * @param aRoot
   *        The document root. Never <code>null</code>.
   */
  private void _buildGroups (@NonNull final Node aRoot)
  {
    final ICommonsList <Node> aChildren = new CommonsArrayList <> ();
    boolean bAnyMarker = false;
    for (Node aChild = aRoot.getFirstChild (); aChild != null; aChild = aChild.getNext ())
    {
      aChildren.add (aChild);
      if (_getLayoutSentinels (aChild) != null)
        bAnyMarker = true;
    }
    if (!bAnyMarker)
      return;

    for (final Node aChild : aChildren)
      aChild.unlink ();

    final Deque <Node> aStack = new ArrayDeque <> ();
    Node aCurrent = aRoot;
    for (final Node aChild : aChildren)
    {
      final List <String> aSentinels = _getLayoutSentinels (aChild);
      if (aSentinels == null)
      {
        aCurrent.appendChild (aChild);
        continue;
      }
      for (final String sSentinel : aSentinels)
      {
        final Matcher aMatcher = GROUP_SENTINEL.matcher (sSentinel);
        if (aMatcher.matches ())
        {
          if (aStack.size () >= MAX_GROUP_DEPTH)
            throw new PhloxTemplateException (EStage.PARSE,
                                              aMatcher.group (2),
                                              "groups and rows are nested deeper than " + MAX_GROUP_DEPTH + " levels");
          final PhloxGroupNode aGroup = new PhloxGroupNode ("ROW".equals (aMatcher.group (1)));
          m_aBlockClassMap.put (aGroup, aMatcher.group (2).strip ());
          aCurrent.appendChild (aGroup);
          aStack.push (aCurrent);
          aCurrent = aGroup;
        }
        else
          if (aStack.isEmpty ())
            LOGGER.warn ("Template tag '{{end}}' has no matching '{{group:...}}' or '{{row:...}}'");
          else
            aCurrent = aStack.pop ();
      }
    }
    if (!aStack.isEmpty ())
      LOGGER.warn ("Template has " + aStack.size () + " '{{group:...}}' or '{{row:...}}' without '{{end}}'");
  }

  private void _processSentinels (@NonNull final Node aRoot)
  {
    // Two-pass scan: collect candidate paragraphs first to avoid mutating during iteration.
    Node aChild = aRoot.getFirstChild ();
    while (aChild != null)
    {
      final Node aNext = aChild.getNext ();
      if (aChild instanceof final Paragraph aParagraph)
        _processParagraph (aParagraph);
      else
        if (aChild instanceof final TableRow aRow && _processClassRow (aRow))
        {
          // Removed - nothing to recurse into
        }
        else
          _processSentinels (aChild); // recurse into block containers (BlockQuote, ListItem, etc.)
      aChild = aNext;
    }
  }

  @Nullable
  private static String _firstTextOf (@NonNull final Image aImage)
  {
    Node aChild = aImage.getFirstChild ();
    while (aChild != null)
    {
      if (aChild instanceof final Text aText)
        return aText.getLiteral ();
      aChild = aChild.getNext ();
    }
    return null;
  }

  @Nullable
  private static String _guessMime (@NonNull final String sPath)
  {
    final String sLower = sPath.toLowerCase (Locale.ROOT);
    if (sLower.endsWith (".png"))
      return "image/png";
    if (sLower.endsWith (".jpg") || sLower.endsWith (".jpeg"))
      return "image/jpeg";
    if (sLower.endsWith (".gif"))
      return "image/gif";
    if (sLower.endsWith (".svg"))
      return "image/svg+xml";
    return null;
  }

  /** @return The start of a <code>data:</code> URL, which may have any size, for a message. */
  @NonNull
  private static String _getShortened (@NonNull final String sDest)
  {
    return sDest.length () <= MAX_DATA_URL_DISPLAY_LENGTH ? sDest
                                                          : sDest.substring (0, MAX_DATA_URL_DISPLAY_LENGTH) + "...";
  }

  /**
   * Decode the text of a {@code qrcode:} URL or a {@code data:} URL. Only {@code %XX} is special, so
   * unlike with <code>URLDecoder</code> a plus sign stays a plus sign.
   */
  private static byte @NonNull [] _percentDecode (@NonNull final String sEncoded, @NonNull final String sDest)
  {
    final byte [] aBytes = sEncoded.getBytes (StandardCharsets.UTF_8);
    try (final NonBlockingByteArrayOutputStream aOut = new NonBlockingByteArrayOutputStream (aBytes.length))
    {
      for (int i = 0; i < aBytes.length; ++i)
      {
        if (aBytes[i] != '%')
        {
          aOut.write (aBytes[i]);
          continue;
        }
        final int nHigh = i + 2 < aBytes.length ? Character.digit (aBytes[i + 1], 16) : -1;
        final int nLow = i + 2 < aBytes.length ? Character.digit (aBytes[i + 2], 16) : -1;
        if (nHigh < 0 || nLow < 0)
          throw new PhloxTemplateException (EStage.PARSE, sDest, "invalid percent encoding in the image URL");
        aOut.write (nHigh * 16 + nLow);
        i += 2;
      }
      return aOut.toByteArray ();
    }
  }

  /**
   * @return The image of the QR code or <code>null</code> if the invoice data gives none. This is
   *         not an error of the template, so the image is left out.
   */
  @Nullable
  private static PhloxResolvedImage _resolveQrCode (@NonNull final String sDest, @Nullable final String sAlt)
  {
    final String sText = new String (_percentDecode (sDest.substring (QRCODE_URL_PREFIX.length ()), sDest),
                                     StandardCharsets.UTF_8);
    if (sText.isEmpty ())
      return null;
    final byte [] aPng = PhloxQrCodeGenerator.createPng (sText);
    if (aPng == null)
    {
      LOGGER.warn ("The text of a QR code is too long with " + sText.length () + " characters - skipping it");
      return null;
    }
    return new PhloxResolvedImage (aPng, PhloxQrCodeGenerator.MIME_TYPE, null, sAlt);
  }

  /**
   * Decode a {@code data:} URL, so that the writers do not have to tell it from the other schemes.
   */
  @NonNull
  private static PhloxResolvedImage _resolveDataUrl (@NonNull final String sDest, @Nullable final String sAlt)
  {
    final int nComma = sDest.indexOf (',');
    if (nComma < 0)
      throw new PhloxTemplateException (EStage.PARSE, _getShortened (sDest), "data: URL without a comma");
    final String sMeta = sDest.substring (DATA_URL_PREFIX.length (), nComma);
    final int nSemicolon = sMeta.indexOf (';');
    final String sMime = nSemicolon < 0 ? sMeta : sMeta.substring (0, nSemicolon);
    if (!sMime.toLowerCase (Locale.ROOT).startsWith ("image/"))
      throw new PhloxTemplateException (EStage.PARSE, _getShortened (sDest), "data: URL is not an image");

    final String sData = sDest.substring (nComma + 1);
    final byte [] aBinary;
    if (sMeta.toLowerCase (Locale.ROOT).endsWith (";base64"))
      try
      {
        aBinary = Base64.getDecoder ().decode (sData);
      }
      catch (final IllegalArgumentException ex)
      {
        throw new PhloxTemplateException (EStage.PARSE, _getShortened (sDest), "data: URL has invalid Base64", ex);
      }
    else
      aBinary = _percentDecode (sData, _getShortened (sDest));
    if (aBinary.length == 0)
      throw new PhloxTemplateException (EStage.PARSE, _getShortened (sDest), "data: URL has no content");
    return new PhloxResolvedImage (aBinary, sMime, null, sAlt);
  }

  @Nullable
  private PhloxResolvedImage _resolveImage (@NonNull final String sDest, @NonNull final Image aImage)
  {
    final String sAlt = _firstTextOf (aImage);
    if (sDest.startsWith (DATA_URL_PREFIX))
      return _resolveDataUrl (sDest, sAlt);
    if (sDest.equals ("attachment:logo"))
    {
      final PhloxAttachment aLogo = m_aContext.getLogo ();
      if (aLogo == null || aLogo.getBinary () == null)
      {
        LOGGER.warn ("Template references attachment:logo but RenderContext.getLogo() is null");
        return null;
      }
      return new PhloxResolvedImage (aLogo.getBinary (), aLogo.getMimeCode (), aLogo.getFilename (), sAlt);
    }
    final Matcher aBg24Matcher = ATTACHMENT_BG24_PATTERN.matcher (sDest);
    if (aBg24Matcher.matches ())
    {
      final int nIndex = Integer.parseInt (aBg24Matcher.group (1));
      final ICommonsList <PhloxSupportingDocument> aDocs = m_aInvoice.supportingDocuments ();
      if (nIndex < 0 || nIndex >= aDocs.size ())
        throw new PhloxTemplateException (EStage.PARSE,
                                          sDest,
                                          "attachment:BG-24[" +
                                                 nIndex +
                                                 "] is out of range (have " +
                                                 aDocs.size () +
                                                 " supporting documents)");
      final PhloxAttachment aAtt = aDocs.get (nIndex).getAttachment ();
      if (aAtt == null || aAtt.getBinary () == null)
        throw new PhloxTemplateException (EStage.PARSE,
                                          sDest,
                                          "attachment:BG-24[" + nIndex + "] has no inline binary");
      return new PhloxResolvedImage (aAtt.getBinary (), aAtt.getMimeCode (), aAtt.getFilename (), sAlt);
    }
    if (sDest.startsWith ("classpath:"))
    {
      final String sPath = sDest.substring ("classpath:".length ());
      // A custom template may build the URL from invoice data, so it must not reach other resources
      final String sMime = _guessMime (sPath);
      if (sMime == null)
        throw new PhloxTemplateException (EStage.PARSE, sDest, "classpath resource is no png, jpg, jpeg, gif or svg image");
      final ClassPathResource aRes = new ClassPathResource (sPath);
      if (!aRes.exists ())
        throw new PhloxTemplateException (EStage.PARSE, sDest, "classpath resource does not exist: " + sPath);
      try (InputStream aIS = aRes.getInputStream ())
      {
        if (aIS == null)
          throw new PhloxTemplateException (EStage.PARSE, sDest, "classpath resource has no stream");
        return new PhloxResolvedImage (aIS.readAllBytes (), sMime, sPath, sAlt);
      }
      catch (final IOException ex)
      {
        throw new PhloxTemplateException (EStage.PARSE, sDest, "failed to read classpath resource", ex);
      }
    }
    if (sDest.startsWith (QRCODE_URL_PREFIX))
      return _resolveQrCode (sDest, sAlt);
    if (sDest.startsWith ("http:") || sDest.startsWith ("https:") || sDest.startsWith ("file:"))
      throw new PhloxTemplateException (EStage.PARSE,
                                        sDest,
                                        "image URL scheme rejected — writers do not fetch external resources");
    throw new PhloxTemplateException (EStage.PARSE,
                                      sDest,
                                      "unsupported image URL scheme; expected one of: data:, attachment:, classpath:, qrcode:");
  }

  private static boolean _hasContentRecursive (@NonNull final Node aParent)
  {
    for (Node aChild = aParent.getFirstChild (); aChild != null; aChild = aChild.getNext ())
    {
      if (aChild instanceof PhloxGroupNode)
      {
        if (_hasContentRecursive (aChild))
          return true;
      }
      else
        if (!(aChild instanceof PhloxPageBreakNode))
          return true;
    }
    return false;
  }

  /**
   * Run all post-passes against the document root.
   *
   * @param aRoot
   *        The document root. Never {@code null}.
   * @throws PhloxTemplateException
   *         when the document has no content, a group is nested too deeply or an image cannot be
   *         resolved.
   */
  public void process (@NonNull final Node aRoot)
  {
    _buildGroups (aRoot);
    _processSentinels (aRoot);
    aRoot.accept (new StraySentinelRemover ());
    if (!_hasContentRecursive (aRoot))
      throw new PhloxTemplateException (EStage.PARSE, null, "the template produced no content for this invoice");
    aRoot.accept (new ImageResolver ());
  }

  /**
   * Get the sidecar map of block → CSS classes assigned by {@code {{class:…}}} sentinels.
   *
   * @return Sidecar map of block → CSS classes, separated by a space.
   */
  @NonNull
  public Map <Node, String> getBlockClassMap ()
  {
    return m_aBlockClassMap;
  }

  /**
   * Get the sidecar map of {@link Image} node → resolved binary payload.
   *
   * @return Sidecar map of {@link Image} node → resolved binary payload.
   */
  @NonNull
  public Map <Image, PhloxResolvedImage> getImageMap ()
  {
    return m_aImageMap;
  }

  /**
   * Get the height that the template requests for an image, in CSS pixels. A QR code without a
   * request has {@link #DEFAULT_QRCODE_HEIGHT_PX}.
   *
   * @param aImage
   *        The image node. Never {@code null}.
   * @return The height in pixels, or 0 to use the default size of the writer.
   * @throws PhloxTemplateException
   *         when the image has a title other than <code>height-px:N</code>.
   */
  public static int getImageHeightPx (@NonNull final Image aImage)
  {
    final String sTitle = aImage.getTitle ();
    if (sTitle == null || sTitle.isEmpty ())
    {
      final String sDest = aImage.getDestination ();
      return sDest != null && sDest.startsWith (QRCODE_URL_PREFIX) ? DEFAULT_QRCODE_HEIGHT_PX : 0;
    }
    final Matcher aMatcher = HEIGHT_PX_TITLE.matcher (sTitle.trim ());
    if (!aMatcher.matches ())
      throw new PhloxTemplateException (EStage.PARSE,
                                        aImage.getDestination (),
                                        "invalid image option '" + sTitle + "'; expected height-px:N with N > 0");
    return Integer.parseInt (aMatcher.group (1));
  }

  /**
   * Encode the given bytes as a {@code data:} URI. Static helper used by emitters that need to
   * inline a resolved image into an HTML {@code src} attribute.
   *
   * @param aImage
   *        The resolved image. Never {@code null}.
   * @return The {@code data:} URI. Never {@code null}.
   */
  @NonNull
  public static String toDataUri (@NonNull final PhloxResolvedImage aImage)
  {
    final String sMime = aImage.getMimeCode () == null ? "application/octet-stream" : aImage.getMimeCode ();
    return "data:" + sMime + ";base64," + Base64.getEncoder ().encodeToString (aImage.getBinary ());
  }
}
