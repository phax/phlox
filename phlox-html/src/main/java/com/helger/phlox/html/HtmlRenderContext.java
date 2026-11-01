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

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.collection.commons.CommonsLinkedHashMap;
import com.helger.collection.commons.ICommonsOrderedMap;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.theme.PhloxTheme;
import com.helger.phlox.theme.PhloxThemeException;
import com.helger.phlox.theme.PhloxThemeApplier;
import com.helger.phlox.theme.PhloxThemeObject;

/**
 * HTML-specific render context. Adds CSS prefix and stylesheet options on top of the shared
 * {@link AbstractRenderContext} knobs.
 *
 * @author Philip Helger
 */
public class HtmlRenderContext extends AbstractRenderContext <HtmlRenderContext>
{
  /** Default prefix of all CSS classes emitted by the writer. */
  public static final String DEFAULT_CSS_CLASS_PREFIX = "phlox-";
  /** Default for embedding the stylesheet into the document. */
  public static final boolean DEFAULT_EMBEDDED_STYLESHEET = true;
  /** Default for emitting the HTML doctype. */
  public static final boolean DEFAULT_EMIT_DOCTYPE = true;

  private static final Pattern CSS_VARIABLE_NAME = Pattern.compile ("--[A-Za-z0-9_-]+");

  private String m_sCssClassPrefix = DEFAULT_CSS_CLASS_PREFIX;
  private boolean m_bEmbeddedStylesheet = DEFAULT_EMBEDDED_STYLESHEET;
  private String m_sCustomStylesheet;
  private boolean m_bEmitDoctype = DEFAULT_EMIT_DOCTYPE;
  private HtmlFontFamily m_aFontFamily;
  private final ICommonsOrderedMap <String, String> m_aCssVariables = new CommonsLinkedHashMap <> ();

  /** Create a new context with the default HTML settings. */
  public HtmlRenderContext ()
  {}

  /**
   * Create a new context pre-populated with the shared settings of another context.
   *
   * @param aSource
   *        The context to copy the shared settings from. Never {@code null}.
   * @return A new context. Never {@code null}.
   */
  @NonNull
  public static HtmlRenderContext copyFrom (@NonNull final AbstractRenderContext <?> aSource)
  {
    return new HtmlRenderContext ().copyBaseFieldsFrom (aSource);
  }

  /**
   * Get the CSS class prefix.
   *
   * @return The CSS class prefix.
   */
  @NonNull
  public String getCssClassPrefix ()
  {
    return m_sCssClassPrefix;
  }

  /**
   * Set the CSS class prefix.
   *
   * @param sCssClassPrefix
   *        The new value. Never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public HtmlRenderContext setCssClassPrefix (@NonNull final String sCssClassPrefix)
  {
    m_sCssClassPrefix = sCssClassPrefix;
    return this;
  }

  /**
   * Check if the default stylesheet is embedded into the document.
   *
   * @return {@code true} if the default stylesheet is embedded into the document.
   */
  public boolean isEmbeddedStylesheet ()
  {
    return m_bEmbeddedStylesheet;
  }

  /**
   * Set whether the default stylesheet is embedded into the document. The CSS variables, the fonts
   * and the custom stylesheet do not depend on it: they are written in any case.
   *
   * @param bEmbeddedStylesheet
   *        {@code false} to drop the default stylesheet.
   * @return this for chaining.
   */
  @NonNull
  public HtmlRenderContext setEmbeddedStylesheet (final boolean bEmbeddedStylesheet)
  {
    m_bEmbeddedStylesheet = bEmbeddedStylesheet;
    return this;
  }

  /**
   * Get the custom stylesheet.
   *
   * @return The custom stylesheet. May be {@code null}.
   */
  @Nullable
  public String getCustomStylesheet ()
  {
    return m_sCustomStylesheet;
  }

  /**
   * Set the custom stylesheet.
   *
   * @param sCustomStylesheet
   *        The new value. May be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public HtmlRenderContext setCustomStylesheet (@Nullable final String sCustomStylesheet)
  {
    m_sCustomStylesheet = sCustomStylesheet;
    return this;
  }

  /**
   * Get the embedded fonts.
   *
   * @return The fonts or {@code null} if the fonts of the default stylesheet are used.
   */
  @Nullable
  public HtmlFontFamily getFontFamily ()
  {
    return m_aFontFamily;
  }

  /**
   * Set the fonts that are embedded into the document. They are written independent of
   * {@link #isEmbeddedStylesheet()}.
   *
   * @param aFontFamily
   *        The new value. May be {@code null} for the fonts of the default stylesheet.
   * @return this for chaining.
   */
  @NonNull
  public HtmlRenderContext setFontFamily (@Nullable final HtmlFontFamily aFontFamily)
  {
    m_aFontFamily = aFontFamily;
    return this;
  }

  /**
   * Check if the HTML doctype is emitted.
   *
   * @return {@code true} if the HTML doctype is emitted.
   */
  public boolean isEmitDoctype ()
  {
    return m_bEmitDoctype;
  }

  /**
   * Set whether the HTML doctype is written. It has an effect on
   * {@link HtmlInvoiceWriter#writeAsString(com.helger.phlox.api.model.PhloxInvoice, HtmlRenderContext)}
   * and {@link HtmlInvoiceWriter#writeTo(com.helger.phlox.api.model.PhloxInvoice, HtmlRenderContext, java.io.OutputStream)}.
   *
   * @param bEmitDoctype
   *        {@code false} to omit the doctype.
   * @return this for chaining.
   */
  @NonNull
  public HtmlRenderContext setEmitDoctype (final boolean bEmitDoctype)
  {
    m_bEmitDoctype = bEmitDoctype;
    return this;
  }

  /**
   * Get the CSS custom properties that are written into the page after the default stylesheet.
   *
   * @return The properties with the name including <code>--</code>. Never {@code null}.
   */
  @NonNull
  public ICommonsOrderedMap <String, String> getCssVariables ()
  {
    return new CommonsLinkedHashMap <> (m_aCssVariables);
  }

  /**
   * Set a CSS custom property that is written into the page after the default stylesheet, like
   * <code>--phlox-accent</code>. The stylesheet of the default layout takes its colours from such
   * properties.
   *
   * @param sName
   *        The name including the leading <code>--</code>. May not be {@code null}.
   * @param sValue
   *        The value. May not be {@code null} and must not contain <code>;</code>, <code>{</code>,
   *        <code>}</code> or <code>&lt;</code>.
   * @return this for chaining.
   * @throws IllegalArgumentException
   *         If the name or the value is invalid.
   */
  @NonNull
  public HtmlRenderContext putCssVariable (@NonNull final String sName, @NonNull final String sValue)
  {
    ValueEnforcer.isTrue (CSS_VARIABLE_NAME.matcher (sName).matches (), "Invalid name of a CSS variable");
    ValueEnforcer.isTrue (sValue.indexOf (';') < 0 &&
                          sValue.indexOf ('{') < 0 &&
                          sValue.indexOf ('}') < 0 &&
                          sValue.indexOf ('<') < 0,
                          "Invalid value of a CSS variable");
    m_aCssVariables.put (sName, sValue);
    return this;
  }

  @NonNull
  private static HtmlFontFamily _getFontFamily (@NonNull final PhloxTheme aTheme, @NonNull final PhloxThemeObject aFonts)
  {
    aFonts.checkKeys ("regular", "bold", "italic", "boldItalic");
    final String sRegular = aFonts.getString ("regular");
    if (sRegular == null)
      throw new PhloxThemeException ("Theme error at '" + aFonts.getPath () + ".regular': the regular font is required");
    final String sBold = aFonts.getString ("bold");
    final String sItalic = aFonts.getString ("italic");
    final String sBoldItalic = aFonts.getString ("boldItalic");
    try
    {
      return new HtmlFontFamily (sRegular,
                                 aTheme.readBinary (sRegular),
                                 sBold,
                                 sBold == null ? null : aTheme.readBinary (sBold),
                                 sItalic,
                                 sItalic == null ? null : aTheme.readBinary (sItalic),
                                 sBoldItalic,
                                 sBoldItalic == null ? null : aTheme.readBinary (sBoldItalic));
    }
    catch (final IllegalArgumentException | PhloxThemeException ex)
    {
      throw new PhloxThemeException ("Theme error at '" + aFonts.getPath () + "': " + ex.getMessage (), ex);
    }
  }

  /**
   * Apply a theme: its <code>common</code> part, its variables (as CSS custom properties) and the
   * <code>html</code> part with the keys <code>cssClassPrefix</code>, <code>embeddedStylesheet</code>,
   * <code>emitDoctype</code>, <code>customStylesheet</code> (the CSS itself) and
   * <code>customStylesheetFile</code> (the path of a CSS file) and <code>fonts</code> (the font files
   * <code>regular</code>, <code>bold</code>, <code>italic</code> and <code>boldItalic</code>, of
   * which only <code>regular</code> is required). If both stylesheets are given, the
   * file comes first. Only the values that are in the theme are changed.
   *
   * @param aTheme
   *        The theme. May not be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public HtmlRenderContext applyTheme (@NonNull final PhloxTheme aTheme)
  {
    ValueEnforcer.notNull (aTheme, "Theme");
    PhloxThemeApplier.applyCommon (this, aTheme);

    aTheme.getVariables ().forEach (this::putCssVariable);

    final PhloxThemeObject aHtml = aTheme.getHtml ();
    aHtml.checkKeys ("cssClassPrefix", "embeddedStylesheet", "emitDoctype", "customStylesheet", "customStylesheetFile", "fonts");
    if (aHtml.getString ("cssClassPrefix") != null)
      setCssClassPrefix (aHtml.getString ("cssClassPrefix"));
    if (aHtml.getBoolean ("embeddedStylesheet") != null)
      setEmbeddedStylesheet (aHtml.getBoolean ("embeddedStylesheet").booleanValue ());
    if (aHtml.getBoolean ("emitDoctype") != null)
      setEmitDoctype (aHtml.getBoolean ("emitDoctype").booleanValue ());

    final PhloxThemeObject aFonts = aHtml.getObject ("fonts");
    if (aFonts != null)
      setFontFamily (_getFontFamily (aTheme, aFonts));

    final String sFile = aHtml.getString ("customStylesheetFile");
    final String sInline = aHtml.getString ("customStylesheet");
    if (sFile != null || sInline != null)
    {
      final StringBuilder aCss = new StringBuilder ();
      if (sFile != null)
        aCss.append (new String (aTheme.readBinary (sFile), StandardCharsets.UTF_8));
      if (sInline != null)
      {
        if (aCss.length () > 0)
          aCss.append ('\n');
        aCss.append (sInline);
      }
      setCustomStylesheet (aCss.toString ());
    }
    return this;
  }
}
