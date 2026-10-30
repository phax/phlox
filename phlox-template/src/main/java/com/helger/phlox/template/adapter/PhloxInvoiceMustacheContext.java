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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.FormatStyle;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.NotThreadSafe;
import com.helger.annotation.style.VisibleForTesting;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringReplace;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.iterator.ArrayIterator;
import com.helger.datetime.format.EDTFormatterMode;
import com.helger.datetime.format.PDTFormatter;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.i18n.IPhloxLabelResolver;
import com.helger.phlox.api.i18n.PhloxFormat;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.template.PhloxTemplateException.EStage;
import com.helger.phlox.template.PhloxTemplateException;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;

/**
 * The JMustache adapter — preprocesses {@code .ptpl} source so JMustache understands the phlox
 * extensions ({@code {{?…}}}, {@code {{label:…}}}, the pipe-modifier syntax), exposes a
 * {@link com.samskivert.mustache.Mustache.Collector} that resolves BT/BG paths through
 * {@link PhloxModelRegistry}, applies format modifiers, emits {@code [[PHLOX:CLASS=…]]} /
 * {@code [[PHLOX:PAGEBREAK]]} sentinels for the post-pass, and routes {@code {{label:…}}} through
 * the active {@code IPhloxLabelResolver}.
 * <p>
 * Per-render execution state (the {@link PhloxRenderEnv}) flows through a {@link ThreadLocal} so
 * the compiled JMustache {@code Template} stays sharable across threads.
 *
 * @author Philip Helger
 */
@NotThreadSafe
public final class PhloxInvoiceMustacheContext
{
  /** Opening literal of the {@code {{class:…}}} sentinel; the post-pass scans for this prefix. */
  public static final String SENTINEL_CLASS_PREFIX = "[[PHLOX:CLASS=";
  /**
   * Closing literal of the {@code {{class:…}}} sentinel, paired with
   * {@link #SENTINEL_CLASS_PREFIX}.
   */
  public static final String SENTINEL_CLASS_SUFFIX = "]]";
  /**
   * Literal emitted for the {@code {{pagebreak}}} directive; the post-pass converts it into a
   * {@code PhloxPageBreakNode}.
   */
  public static final String SENTINEL_PAGEBREAK = "[[PHLOX:PAGEBREAK]]";

  /** The sentinel prefix of <code>{{group:name}}</code>. */
  public static final String SENTINEL_GROUP_PREFIX = "[[PHLOX:GROUP=";

  /** The sentinel prefix of <code>{{row:name}}</code>. */
  public static final String SENTINEL_ROW_PREFIX = "[[PHLOX:ROW=";

  /** The sentinel of <code>{{end}}</code>, which closes the innermost group or row. */
  public static final String SENTINEL_END = "[[PHLOX:END]]";

  private static final Logger LOGGER = LoggerFactory.getLogger (PhloxInvoiceMustacheContext.class);

  // Replace dots in presence-check paths so JMustache does not split them.
  private static final char DOT_PLACEHOLDER = '§';
  private static final String PRESENCE_PREFIX = "¤PRESENT¤";

  private static final ThreadLocal <PhloxRenderEnv> ENV = new ThreadLocal <> ();

  private static final Mustache.Collector COLLECTOR = new PhloxCollector ();

  // Required because the default template's multi-line comments contain literal Mustache
  // tags ({{label:...}}, {{?BT-X}}) as documentation, and standard Mustache comment parsing
  // ends at the FIRST {{!…}} inner `}}` — which would leave the rest of the comment text
  // exposed to the directive scanner. We follow the convention used in default.ptpl:
  // - inline comments: {{! …text… }} (all on one line)
  // - block comments: {{!\n …text…\n}} (closing `}}` on its own line, optionally indented)
  private static final Pattern BLOCK_COMMENT = Pattern.compile ("\\{\\{!.*?\\n[ \\t]*\\}\\}", Pattern.DOTALL);
  private static final Pattern INLINE_COMMENT = Pattern.compile ("\\{\\{!.*?\\}\\}");

  // The KEY may itself contain dots ({{label:section.header.title.invoice}}) — we don't care,
  // because the substitution happens before Mustache compile so JMustache never sees a
  // multi-segment label path.
  private static final Pattern LABEL_PATTERN = Pattern.compile ("\\{\\{\\s*label:([^}\\s|]+)\\s*\\}\\}");

  private static final String PARENT_PREFIX = "..";

  private static final class PhloxCollector implements Mustache.Collector
  {
    private final Mustache.VariableFetcher m_aVarFetcher = (aCtx, sName) -> {
      // "..X" addresses the enclosing context: if the current context cannot answer X, let
      // JMustache continue with the parent context, which is asked with the same name
      if (sName.startsWith (PARENT_PREFIX))
      {
        final Object aParentResolved = resolveName (aCtx, sName.substring (PARENT_PREFIX.length ()));
        return aParentResolved == null ? Template.NO_FETCHER_FOUND : aParentResolved;
      }
      final Object aResolved = resolveName (aCtx, sName);
      // JMustache requires NO_FETCHER_FOUND when the variable is not known so it can fall
      // through to defaultValue("") rather than treating null as a section iteration value.
      // We return the raw resolved value (which may itself be null) and let JMustache's
      // configured nullValue handle interpolation; for sections, null is treated as falsy.
      return aResolved;
    };

    public Iterator <?> toIterator (final Object aValue)
    {
      if (aValue instanceof final Iterable aIterable)
        return aIterable.iterator ();
      if (aValue instanceof final Iterator aIter)
        return aIter;
      if (aValue != null && aValue.getClass ().isArray ())
        return new ArrayIterator <> ((Object []) aValue);
      return null;
    }

    public Mustache.VariableFetcher createFetcher (final Object aCtx, final String sName)
    {
      return m_aVarFetcher;
    }

    public <K, V> Map <K, V> createFetcherCache ()
    {
      return new ConcurrentHashMap <> ();
    }
  }

  private PhloxInvoiceMustacheContext ()
  {}

  @NonNull
  static String stripComments (@NonNull final String sSource)
  {
    final String sNoBlock = BLOCK_COMMENT.matcher (sSource).replaceAll ("");
    return INLINE_COMMENT.matcher (sNoBlock).replaceAll ("");
  }

  @NonNull
  private static String _bakeLabels (@NonNull final String sSource,
                                     @NonNull final Locale aLocale,
                                     @NonNull final IPhloxLabelResolver aResolver)
  {
    final Matcher aMatcher = LABEL_PATTERN.matcher (sSource);
    final StringBuilder aSB = new StringBuilder (sSource.length ());
    while (aMatcher.find ())
    {
      final String sResolved = aResolver.getLabel (aMatcher.group (1), aLocale);
      aMatcher.appendReplacement (aSB, Matcher.quoteReplacement (sResolved));
    }
    aMatcher.appendTail (aSB);
    return aSB.toString ();
  }

  // Hand-rolled scanner — skips {{!comment}} regions entirely so the {{?BT-X}} examples
  // that appear inside the default template's comment blocks are NOT rewritten.
  @NonNull
  private static String _encodePresenceChecks (@NonNull final String sSource)
  {
    final StringBuilder aSB = new StringBuilder (sSource.length ());
    final Deque <String> aStack = new ArrayDeque <> ();
    // The names as written in the template, to report unbalanced tags
    final Deque <String> aWrittenNames = new ArrayDeque <> ();
    final int nLen = sSource.length ();
    int i = 0;
    while (i < nLen)
    {
      if (i + 1 < nLen && sSource.charAt (i) == '{' && sSource.charAt (i + 1) == '{')
      {
        // Inside a {{…}} directive — find the closing }}.
        final int nEnd = sSource.indexOf ("}}", i + 2);
        if (nEnd < 0)
        {
          aSB.append (sSource, i, nLen);
          break;
        }

        final String sInside = sSource.substring (i + 2, nEnd).trim ();

        // Comments should already have been stripped by stripComments before we get here;
        // if any survive (e.g. a one-line `{{! foo }}`), emit verbatim and let JMustache
        // discard them.
        if (sInside.startsWith ("!"))
        {
          LOGGER.warn ("Found a comment even though it should have been stripped before!");
          aSB.append (sSource, i, nEnd + 2);
          i = nEnd + 2;
          continue;
        }

        // Directives we care about: ?, #, ^, /
        if (sInside.length () > 0)
        {
          final char cOp = sInside.charAt (0);
          if (cOp == '?' || cOp == '#' || cOp == '^' || cOp == '/')
          {
            // Pipe in a directive isn't a thing here, but defensive trim on whitespace.
            final String sName = sInside.substring (1).trim ();
            switch (cOp)
            {
              case '?' ->
              {
                final String sEncoded = PRESENCE_PREFIX + StringReplace.replaceAll (sName, '.', DOT_PLACEHOLDER);
                aStack.push (sEncoded);
                aWrittenNames.push ("?" + sName);
                aSB.append ("{{#").append (sEncoded).append ("}}");
              }
              case '#', '^' ->
              {
                aStack.push (sName);
                aWrittenNames.push (cOp + sName);
                aSB.append ("{{").append (cOp).append (sName).append ("}}");
              }
              case '/' ->
              {
                if (aStack.isEmpty ())
                  LOGGER.warn ("Template tag '{{/" + sName + "}}' has no matching opening tag");
                else
                {
                  final String sWritten = aWrittenNames.pop ();
                  if (!sWritten.substring (1).equals (sName))
                    LOGGER.warn ("Template tag '{{" + sWritten + "}}' is closed by '{{/" + sName + "}}'");
                }
                final String sOpenName = aStack.isEmpty () ? sName : aStack.pop ();
                aSB.append ("{{/").append (sOpenName).append ("}}");
              }
              default -> aSB.append (sSource, i, nEnd + 2);
            }
            i = nEnd + 2;
            continue;
          }
        }

        // Plain interpolation or unknown directive — copy verbatim.
        aSB.append (sSource, i, nEnd + 2);
        i = nEnd + 2;
      }
      else
      {
        aSB.append (sSource.charAt (i));
        i++;
      }
    }
    for (final String sOpen : aWrittenNames)
      LOGGER.warn ("Template tag '{{" + sOpen + "}}' is never closed");
    return aSB.toString ();
  }

  @Nullable
  private static Object _walkDottedPath (@NonNull final Object aCtx, @NonNull final String sHead)
  {
    if (sHead.indexOf ('.') < 0)
      return PhloxModelRegistry.resolve (aCtx, sHead);
    Object aCurrent = aCtx;
    for (final String sSegment : StringHelper.getExploded ('.', sHead))
    {
      if (aCurrent == null)
        return null;
      aCurrent = PhloxModelRegistry.resolve (aCurrent, sSegment);
    }
    return aCurrent;
  }

  private static boolean _isTruthy (@Nullable final Object aValue)
  {
    if (aValue == null)
      return false;
    if (aValue instanceof final Boolean bValue)
      return bValue.booleanValue ();
    if (aValue instanceof final String sValue)
      return !sValue.isEmpty ();
    if (aValue instanceof final Collection <?> aCollection)
      return !aCollection.isEmpty ();
    return true;
  }

  /**
   * Neutralise Markdown in invoice data. The substituted template is parsed as CommonMark, so data
   * like <code>1. Main Street</code> or <code>a | b</code> would otherwise change the document
   * structure. Line breaks become the template's explicit break tag.
   */
  @NonNull
  private static String _escapeMarkdown (@NonNull final String sValue)
  {
    final StringBuilder aSB = new StringBuilder (sValue.length () + 8);
    final int nLen = sValue.length ();
    boolean bLineStart = true;
    boolean bDigitsFromLineStart = false;
    for (int i = 0; i < nLen; ++i)
    {
      final char c = sValue.charAt (i);
      final boolean bAtLineStart = bLineStart;
      final boolean bDigitsBefore = bDigitsFromLineStart;
      bLineStart = false;
      bDigitsFromLineStart = Character.isDigit (c) && (bAtLineStart || bDigitsBefore);
      switch (c)
      {
        case '\r':
          if (i + 1 < nLen && sValue.charAt (i + 1) == '\n')
            ++i;
          aSB.append ("<br>");
          bLineStart = true;
          break;
        case '\n':
          aSB.append ("<br>");
          bLineStart = true;
          break;
        case '\\':
        case '`':
        case '*':
        case '_':
        case '[':
        case ']':
        case '<':
        case '>':
        case '|':
        case '~':
        case '&':
          aSB.append ('\\').append (c);
          break;
        case '#':
        case '+':
        case '-':
          // Block markers only matter at the start of a line
          if (bAtLineStart)
            aSB.append ('\\');
          aSB.append (c);
          break;
        case '.':
        case ')':
          // "1." and "1)" at a line start open an ordered list
          if (bDigitsBefore && (i + 1 == nLen || sValue.charAt (i + 1) == ' '))
            aSB.append ('\\');
          aSB.append (c);
          break;
        default:
          aSB.append (c);
          break;
      }
    }
    return aSB.toString ();
  }

  @NonNull
  private static String _renderLeaf (@NonNull final Object aValue, @NonNull final PhloxRenderEnv aEnv)
  {
    if (aValue instanceof final String sValue)
      return _escapeMarkdown (sValue);
    if (aValue instanceof final PhloxIdentifier aID)
    {
      final String sScheme = aID.getSchemeID ();
      final String sVal = aID.getValue ();
      if (sVal == null || sVal.isEmpty ())
        return "";
      return _escapeMarkdown (sScheme == null || sScheme.isEmpty () ? sVal : sScheme + ":" + sVal);
    }
    if (aValue instanceof final LocalDate aDate)
      return PhloxFormat.date (aDate, aEnv.getLocale (), aEnv.getFormatOptions ());
    if (aValue instanceof final BigDecimal aDecimal)
      return aDecimal.toPlainString ();
    return _escapeMarkdown (aValue.toString ());
  }

  @NonNull
  private static FormatStyle _parseDateStyle (@Nullable final String sArg)
  {
    if (sArg == null)
      return FormatStyle.MEDIUM;
    return switch (sArg)
    {
      case "short" -> FormatStyle.SHORT;
      case "medium" -> FormatStyle.MEDIUM;
      case "long" -> FormatStyle.LONG;
      case "full" -> FormatStyle.FULL;
      default -> FormatStyle.MEDIUM;
    };
  }

  @NonNull
  private static String _titleCase (@NonNull final String sIn, @NonNull final PhloxRenderEnv aEnv)
  {
    // Lightweight ASCII title case — first char of each whitespace-separated word.
    final StringBuilder aSB = new StringBuilder (sIn.length ());
    boolean bAtStart = true;
    for (int i = 0; i < sIn.length (); i++)
    {
      final char c = sIn.charAt (i);
      if (Character.isWhitespace (c))
      {
        aSB.append (c);
        bAtStart = true;
      }
      else
      {
        aSB.append (bAtStart ? Character.toString (c).toUpperCase (aEnv.getLocale ()).charAt (0)
                             : Character.toString (c).toLowerCase (aEnv.getLocale ()).charAt (0));
        bAtStart = false;
      }
    }
    return aSB.toString ();
  }

  @Nullable
  private static String _resolveCurrency (@NonNull final String sBt, @NonNull final PhloxRenderEnv aEnv)
  {
    return switch (sBt)
    {
      case "BT-5" -> aEnv.getInvoice ().getCurrency ();
      case "BT-6" -> aEnv.getInvoice ().getVatAccountingCurrency ();
      default -> null;
    };
  }

  @Nullable
  private static String _applyOneModifier (@Nullable final Object aValue,
                                           @NonNull final String sName,
                                           @Nullable final String sArg,
                                           @NonNull final PhloxRenderEnv aEnv)
  {
    if (aValue == null)
      return "";

    switch (sName)
    {
      case "raw":
        return aValue.toString ();
      case "amount":
      {
        if (!(aValue instanceof final BigDecimal aAmount))
          return aValue.toString ();
        final String sCurrencyBt = sArg != null && !sArg.isEmpty () ? sArg : "BT-5";
        final String sCurrency = _resolveCurrency (sCurrencyBt, aEnv);
        return PhloxFormat.amount (aAmount, sCurrency, aEnv.getLocale (), aEnv.getFormatOptions ());
      }
      case "percent":
        if (!(aValue instanceof final BigDecimal aPercentage))
          return aValue.toString ();
        return PhloxFormat.percent (aPercentage, aEnv.getLocale (), aEnv.getFormatOptions ());
      case "date":
      {
        if (!(aValue instanceof final LocalDate aDate))
          return aValue.toString ();
        final FormatStyle eStyle = _parseDateStyle (sArg);
        return PDTFormatter.getFormatterDate (eStyle, aEnv.getLocale (), EDTFormatterMode.PRINT).format (aDate);
      }
      case "code":
      {
        if (sArg == null)
          return aValue.toString ();
        return aEnv.getRenderContext ().codeLabel (sArg, aValue.toString ());
      }
      case "upper":
        return aValue.toString ().toUpperCase (aEnv.getLocale ());
      case "lower":
        return aValue.toString ().toLowerCase (aEnv.getLocale ());
      case "title":
        return _titleCase (aValue.toString (), aEnv);
      default:
        throw new PhloxTemplateException (EStage.SUBSTITUTE, sName, "Unknown format modifier '" + sName + "'");
    }
  }

  @NonNull
  private static String _applyModifiers (@NonNull final Object aValueIn,
                                         @NonNull final List <String> aModifiers,
                                         @NonNull final PhloxRenderEnv aEnv)
  {
    Object aValue = aValueIn;
    boolean bRaw = false;
    for (final String sModifier : aModifiers)
    {
      final int nColon = sModifier.indexOf (':');
      final String sName = nColon < 0 ? sModifier : sModifier.substring (0, nColon);
      final String sArg = nColon < 0 ? null : sModifier.substring (nColon + 1);
      bRaw |= "raw".equals (sName);
      aValue = _applyOneModifier (aValue, sName, sArg, aEnv);
    }
    if (aValue == null)
      return "";
    return bRaw ? aValue.toString () : _escapeMarkdown (aValue.toString ());
  }

  private static boolean _referencesPresent (@NonNull final PhloxInvoice aInv)
  {
    if (StringHelper.isNotEmpty (aInv.getProjectReference ()) ||
        StringHelper.isNotEmpty (aInv.getContractReference ()) ||
        StringHelper.isNotEmpty (aInv.getPurchaseOrderReference ()) ||
        StringHelper.isNotEmpty (aInv.getSalesOrderReference ()) ||
        StringHelper.isNotEmpty (aInv.getReceivingAdviceReference ()) ||
        StringHelper.isNotEmpty (aInv.getDespatchAdviceReference ()) ||
        StringHelper.isNotEmpty (aInv.getTenderOrLotReference ()) ||
        StringHelper.isNotEmpty (aInv.getDeliveryNoteReference ()) ||
        StringHelper.isNotEmpty (aInv.getBuyerAccountingReference ()))
      return true;
    final PhloxIdentifier aObj = aInv.getInvoicedObjectIdentifier ();
    if (aObj != null && StringHelper.isNotEmpty (aObj.getValue ()))
      return true;
    return !aInv.precedingInvoiceRefs ().isEmpty ();
  }

  private static boolean _lineAllowancesPresent (@NonNull final PhloxInvoice aInv)
  {
    for (final PhloxInvoiceLine aLine : aInv.lines ())
      if (!aLine.allowances ().isEmpty ())
        return true;
    return false;
  }

  private static boolean _lineChargesPresent (@NonNull final PhloxInvoice aInv)
  {
    for (final PhloxInvoiceLine aLine : aInv.lines ())
      if (!aLine.charges ().isEmpty ())
        return true;
    return false;
  }

  private static boolean _lineExtrasPresent (@NonNull final PhloxInvoice aInv)
  {
    for (final PhloxInvoiceLine aLine : aInv.lines ())
    {
      if (StringHelper.isNotEmpty (aLine.getNote ()) ||
          StringHelper.isNotEmpty (aLine.getPurchaseOrderLineRef ()) ||
          StringHelper.isNotEmpty (aLine.getAccountingCost ()) ||
          aLine.getPeriod () != null)
        return true;
      final PhloxIdentifier aObj = aLine.getObjectIdentifier ();
      if (aObj != null && StringHelper.isNotEmpty (aObj.getValue ()))
        return true;
      if (StringHelper.isNotEmpty (aLine.getPurchaseOrderReference ()) ||
          StringHelper.isNotEmpty (aLine.getSalesOrderReference ()) ||
          StringHelper.isNotEmpty (aLine.getSalesOrderLineReference ()) ||
          StringHelper.isNotEmpty (aLine.getDespatchAdviceReference ()) ||
          StringHelper.isNotEmpty (aLine.getDespatchAdviceLineReference ()) ||
          StringHelper.isNotEmpty (aLine.getReceivingAdviceReference ()) ||
          StringHelper.isNotEmpty (aLine.getReceivingAdviceLineReference ()) ||
          StringHelper.isNotEmpty (aLine.getDeliveryNoteReference ()) ||
          StringHelper.isNotEmpty (aLine.getDeliveryNoteLineReference ()) ||
          aLine.getDelivery () != null ||
          !aLine.precedingInvoiceRefs ().isEmpty ())
        return true;
      if (aLine.getItem () != null && StringHelper.isNotEmpty (aLine.getItem ().getGoodsServicesCode ()))
        return true;
      if (aLine.getLineVat () != null &&
          (StringHelper.isNotEmpty (aLine.getLineVat ().getExemptionReasonText ()) ||
           StringHelper.isNotEmpty (aLine.getLineVat ().getExemptionReasonCode ())))
        return true;
    }
    return false;
  }

  @Nullable
  private static Object _walkPath (@NonNull final Object aCtx,
                                   @NonNull final PhloxRenderEnv aEnv,
                                   @NonNull final String sPath)
  {
    Object aCurrent = aCtx;
    for (final String sSegment : StringHelper.getExploded ('.', sPath))
    {
      if (aCurrent == null)
        return null;
      // Allow the synthesized booleans at any level so {{?logo}} still works.
      if ("logo".equals (sSegment))
        return Boolean.valueOf (aEnv.getRenderContext ().getLogo () != null);
      if ("is_credit_note".equals (sSegment))
        return Boolean.valueOf (aEnv.getInvoice ().getKind ().isCreditNote ());
      if ("is_invoice".equals (sSegment))
        return Boolean.valueOf (!aEnv.getInvoice ().getKind ().isCreditNote ());
      if ("references_present".equals (sSegment))
        return Boolean.valueOf (_referencesPresent (aEnv.getInvoice ()));
      if ("line_extras_present".equals (sSegment))
        return Boolean.valueOf (_lineExtrasPresent (aEnv.getInvoice ()));
      if ("line_allowances_present".equals (sSegment))
        return Boolean.valueOf (_lineAllowancesPresent (aEnv.getInvoice ()));
      if ("line_charges_present".equals (sSegment))
        return Boolean.valueOf (_lineChargesPresent (aEnv.getInvoice ()));
      aCurrent = PhloxModelRegistry.resolve (aCurrent, sSegment);
    }
    return aCurrent;
  }

  @NonNull
  private static ICommonsList <String> _splitOnPipe (@NonNull final String sIn)
  {
    final ICommonsList <String> aOut = new CommonsArrayList <> ();
    for (final String sPart : StringHelper.getExploded ('|', sIn))
      aOut.add (sPart.trim ());
    return aOut;
  }

  /**
   * Get the singleton {@link com.samskivert.mustache.Mustache.Collector} used by every compiled.
   *
   * @return The singleton {@link com.samskivert.mustache.Mustache.Collector} used by every compiled
   *         template. Backed by a thread-local execution environment set by
   *         {@link #withEnv(PhloxInvoice, AbstractRenderContext, Runnable)}.
   */
  public static Mustache.@NonNull Collector getCollector ()
  {
    return COLLECTOR;
  }

  /**
   * Run {@code aJob} with a per-render {@link PhloxRenderEnv} bound to the current thread. The env
   * is cleared after the job finishes (including on exception) so leftover state cannot contaminate
   * the next render on the same thread.
   *
   * @param aInvoice
   *        Source invoice the runtime fetcher resolves against.
   * @param aCtx
   *        Render context the fetcher reads locale / format options / synthesized booleans from.
   * @param aJob
   *        Render closure — typically a call to {@code Template.execute(...)}.
   */
  public static void withEnv (@NonNull final PhloxInvoice aInvoice,
                              @NonNull final AbstractRenderContext <?> aCtx,
                              @NonNull final Runnable aJob)
  {
    final PhloxRenderEnv aPrev = ENV.get ();
    ENV.set (new PhloxRenderEnv (aInvoice, aCtx));
    try
    {
      aJob.run ();
    }
    finally
    {
      if (aPrev == null)
        ENV.remove ();
      else
        ENV.set (aPrev);
    }
  }

  /**
   * Stage 0 of the pipeline — locale-bake the raw {@code .ptpl} source. Pure string transformation:
   * <ul>
   * <li>Strips every {@code {{! comment }}} block (inline and multi-line). Done up front because
   * standard Mustache comment parsing closes at the first inner {@code }}}, and our default
   * template's documentation comments contain literal {@code {{…}}} examples that would otherwise
   * leak into the runtime stream.</li>
   * <li>Substitutes every {@code {{label:KEY}}} via {@code aResolver.getLabel(KEY, aLocale)}. After
   * this stage no {@code {{label:…}}} placeholder remains; stage 1 (Mustache execute) sees only
   * invoice-dependent constructs.</li>
   * <li>Rewrites {@code {{?NAME}}…{{/NAME}}} as {@code {{#?ENCODED}}…{{/?ENCODED}}} — encoding dots
   * inside the path so JMustache does not split them into separate segments. The encoding is undone
   * by the runtime fetcher when it evaluates the presence check.</li>
   * </ul>
   * Output: a {@link String} that is itself a {@code .ptpl}-shaped document with placeholders,
   * loops and conditionals intact but no labels or comments. This string is the input to stage 0a
   * (JMustache compile).
   *
   * @param sSource
   *        Raw template source.
   * @param aLocale
   *        Locale to use for label lookups.
   * @param aResolver
   *        Label resolver to use for label lookups.
   * @return Locale-baked source ready to hand to JMustache.
   */
  @NonNull
  public static String preprocess (@NonNull final String sSource,
                                   @NonNull final Locale aLocale,
                                   @NonNull final IPhloxLabelResolver aResolver)
  {
    final String sNoComments = stripComments (sSource);
    final String sLabelBaked = _bakeLabels (sNoComments, aLocale, aResolver);
    return _encodePresenceChecks (sLabelBaked);
  }

  /**
   * Master name-resolution entry point. Receives the current Mustache context and the raw variable
   * name from the substituted source (post-preprocessing).
   */
  @Nullable
  static Object resolveName (@NonNull final Object aCtx, @NonNull final String sRawName)
  {
    final PhloxRenderEnv aEnv = ENV.get ();
    if (aEnv == null)
      throw new PhloxTemplateException (EStage.SUBSTITUTE,
                                        sRawName,
                                        "No PhloxRenderEnv bound on this thread — wrap execute() in withEnv(...)");

    // 1. Presence check (encoded by preprocessor)
    if (sRawName.startsWith (PRESENCE_PREFIX))
    {
      final String sPath = StringReplace.replaceAll (sRawName.substring (PRESENCE_PREFIX.length ()),
                                                     DOT_PLACEHOLDER,
                                                     '.');
      return Boolean.valueOf (_isTruthy (_walkPath (aCtx, aEnv, sPath)));
    }

    // 2. Modifier pipeline — split on pipes
    final List <String> aParts = _splitOnPipe (sRawName);
    final String sHead = aParts.get (0);
    final List <String> aModifiers = aParts.size () > 1 ? aParts.subList (1, aParts.size ()) : List.of ();

    // 3. Special heads. Labels are pre-substituted in stage 0 — if we ever see one at runtime,
    // something bypassed the localize pipeline, so fail fast.
    if (sHead.startsWith ("label:"))
      throw new PhloxTemplateException (EStage.SUBSTITUTE,
                                        sHead,
                                        "raw {{label:…}} reached the runtime fetcher — the template was not run through stage 0 (localize). Use PhloxTemplate.localize(...) / renderAst(...)");
    if (sHead.startsWith ("class:"))
      return SENTINEL_CLASS_PREFIX + sHead.substring ("class:".length ()) + SENTINEL_CLASS_SUFFIX;
    if (sHead.startsWith ("group:"))
      return SENTINEL_GROUP_PREFIX + sHead.substring ("group:".length ()) + SENTINEL_CLASS_SUFFIX;
    if (sHead.startsWith ("row:"))
      return SENTINEL_ROW_PREFIX + sHead.substring ("row:".length ()) + SENTINEL_CLASS_SUFFIX;
    if ("end".equals (sHead))
      return SENTINEL_END;
    if ("pagebreak".equals (sHead))
      return SENTINEL_PAGEBREAK;

    // 4. Synthesized booleans (only meaningful at the invoice root, but cheap to evaluate
    // anywhere — JMustache walks parent contexts before reaching us, so being permissive
    // here is harmless).
    if ("logo".equals (sHead))
      return Boolean.valueOf (aEnv.getRenderContext ().getLogo () != null);
    if ("is_credit_note".equals (sHead))
      return Boolean.valueOf (aEnv.getInvoice ().getKind ().isCreditNote ());
    if ("is_invoice".equals (sHead))
      return Boolean.valueOf (!aEnv.getInvoice ().getKind ().isCreditNote ());
    if ("references_present".equals (sHead))
      return Boolean.valueOf (_referencesPresent (aEnv.getInvoice ()));
    if ("line_extras_present".equals (sHead))
      return Boolean.valueOf (_lineExtrasPresent (aEnv.getInvoice ()));
    if ("line_allowances_present".equals (sHead))
      return Boolean.valueOf (_lineAllowancesPresent (aEnv.getInvoice ()));
    if ("line_charges_present".equals (sHead))
      return Boolean.valueOf (_lineChargesPresent (aEnv.getInvoice ()));

    // 5. Plain BT/BG resolution. The head may be dotted (e.g. `BG-22.BT-115`) when JMustache
    // skipped its own dot-split — that happens whenever the placeholder also carries a pipe
    // modifier ({@code {{BG-22.BT-115 | amount}}}), because the embedded space stops
    // JMustache treating the name as a path. Walk it ourselves.
    final Object aValue = _walkDottedPath (aCtx, sHead);
    if (aValue == null)
      return null;
    // Lists (BG-3, BG-25, …) must reach JMustache untouched so that {{#BG-x}} sections iterate
    if (aValue instanceof Iterable <?>)
      return aValue;
    if (aModifiers.isEmpty ())
      return _renderLeaf (aValue, aEnv);
    return _applyModifiers (aValue, aModifiers, aEnv);
  }

  /** Visible for tests — exposes the in-effect env on the current thread. */
  @Nullable
  @VisibleForTesting
  static PhloxRenderEnv currentEnv ()
  {
    return ENV.get ();
  }
}
