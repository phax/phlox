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
package com.helger.phlox.api;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.trait.IGenericImplTrait;
import com.helger.phlox.api.i18n.DefaultPhloxLabelResolver;
import com.helger.phlox.api.i18n.IPhloxLabelResolver;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.template.IPhloxTemplate;
import com.helger.phlox.api.watermark.WatermarkSpec;

/**
 * Base render context — syntax-neutral, output-format-neutral configuration shared by every
 * writer. Output-specific subclasses ({@code HtmlRenderContext}, {@code PdfRenderContext})
 * add knobs for their medium.
 * <p>
 * The {@code IMPLTYPE} parameter ensures fluent setters return the concrete subtype so
 * call chains stay typed:
 * <pre>
 * new HtmlRenderContext ()
 *     .setLocale (Locale.GERMAN)         // returns HtmlRenderContext
 *     .setEmbeddedStylesheet (false);    // HTML-only setter still in chain
 * </pre>
 *
 * @author Philip Helger
 * @param <IMPLTYPE>
 *        The concrete subclass implementing this context.
 */
public abstract class AbstractRenderContext <IMPLTYPE extends AbstractRenderContext <IMPLTYPE>> implements
                                            IGenericImplTrait <IMPLTYPE>
{
  /** Default for appending the BT/BG identifier to each label. */
  public static final boolean DEFAULT_ANNOTATE_BT_IDS = false;

  private Locale m_aLocale = Locale.ENGLISH;
  private PhloxFormatOptions m_aFormatOptions = PhloxFormatOptions.defaults ();
  private PhloxAttachment m_aLogo;
  private IPhloxLabelResolver m_aLabelResolver = DefaultPhloxLabelResolver.INSTANCE;
  private boolean m_bAnnotateBtIDs = DEFAULT_ANNOTATE_BT_IDS;
  private WatermarkSpec m_aUserWatermark;
  private IPhloxTemplate m_aTemplate;

  /** Subclass-only constructor. */
  protected AbstractRenderContext ()
  {}

  /**
   * Get the target locale for labels and locale-driven formatting.
   *
   * @return The target locale for labels and locale-driven formatting. Never {@code null}.
   */
  @NonNull
  public Locale getLocale ()
  {
    return m_aLocale;
  }

  /**
   * Set the target locale.
   *
   * @param aLocale
   *        Locale; never {@code null}.
   * @return this for chaining (typed as the concrete subclass).
   */
  @NonNull
  public IMPLTYPE setLocale (@NonNull final Locale aLocale)
  {
    m_aLocale = aLocale;
    return thisAsT ();
  }

  /**
   * Get the per-call format options.
   *
   * @return The per-call format options. Never {@code null}.
   */
  @NonNull
  public PhloxFormatOptions getFormatOptions ()
  {
    return m_aFormatOptions;
  }

  /**
   * Set the format options.
   *
   * @param aFormatOptions
   *        Format options; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public IMPLTYPE setFormatOptions (@NonNull final PhloxFormatOptions aFormatOptions)
  {
    m_aFormatOptions = aFormatOptions;
    return thisAsT ();
  }

  /**
   * Get the optional logo to render in the page header.
   *
   * @return The optional logo to render in the page header. May be {@code null}.
   */
  @Nullable
  public PhloxAttachment getLogo ()
  {
    return m_aLogo;
  }

  /**
   * Set the optional logo to render in the page header.
   *
   * @param aLogo
   *        Logo image; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public IMPLTYPE setLogo (@Nullable final PhloxAttachment aLogo)
  {
    m_aLogo = aLogo;
    return thisAsT ();
  }

  /**
   * Get the label resolver in use.
   *
   * @return The label resolver in use. Never {@code null}.
   */
  @NonNull
  public IPhloxLabelResolver getLabelResolver ()
  {
    return m_aLabelResolver;
  }

  /**
   * Replace the label resolver.
   *
   * @param aLabelResolver
   *        Resolver; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public IMPLTYPE setLabelResolver (@NonNull final IPhloxLabelResolver aLabelResolver)
  {
    m_aLabelResolver = aLabelResolver;
    return thisAsT ();
  }

  /**
   * Get the {@code true} when labels should be suffixed with their BT/BG ID (debug aid).
   *
   * @return {@code true} when labels should be suffixed with their BT/BG ID (debug aid).
   */
  public boolean isAnnotateBtIDs ()
  {
    return m_bAnnotateBtIDs;
  }

  /**
   * Set whether labels should be suffixed with their BT/BG ID (debug aid).
   *
   * @param bAnnotateBtIDs
   *        {@code true} to annotate.
   * @return this for chaining.
   */
  @NonNull
  public IMPLTYPE setAnnotateBtIDs (final boolean bAnnotateBtIDs)
  {
    m_bAnnotateBtIDs = bAnnotateBtIDs;
    return thisAsT ();
  }

  /**
   * Get the optional user watermark, e.g. "DRAFT".
   *
   * @return The optional user watermark (e.g. "DRAFT"). May be {@code null}.
   */
  @Nullable
  public WatermarkSpec getUserWatermark ()
  {
    return m_aUserWatermark;
  }

  /**
   * Set the optional user watermark drawn on every output page.
   *
   * @param aUserWatermark
   *        Watermark spec; may be {@code null} for no user watermark.
   * @return this for chaining.
   */
  @NonNull
  public IMPLTYPE setUserWatermark (@Nullable final WatermarkSpec aUserWatermark)
  {
    m_aUserWatermark = aUserWatermark;
    return thisAsT ();
  }

  /**
   * Get the optional template that drives the writers when set.
   *
   * @return The optional template that drives the writers when set. When {@code null} (the
   *         default), the writers use the default template ({@code PhloxTemplate.getDefault ()}).
   */
  @Nullable
  public IPhloxTemplate getTemplate ()
  {
    return m_aTemplate;
  }

  /**
   * Set the optional {@link IPhloxTemplate} that drives layout. When {@code null}, the writers
   * use the default template ({@code PhloxTemplate.getDefault ()}).
   *
   * @param aTemplate
   *        Compiled template; may be {@code null} to clear.
   * @return this for chaining.
   */
  @NonNull
  public IMPLTYPE setTemplate (@Nullable final IPhloxTemplate aTemplate)
  {
    m_aTemplate = aTemplate;
    return thisAsT ();
  }

  /**
   * Copy all base fields from another context into this one. Used by subclass static factories
   * (e.g. {@code PdfRenderContext.copyFrom(...)}) to share settings across formats. The format
   * options are copied by value, so changing them here does not change the source.
   *
   * @param aSource
   *        The source context to copy from.
   * @return {@code this} for chaining.
   */
  @NonNull
  public IMPLTYPE copyBaseFieldsFrom (@NonNull final AbstractRenderContext <?> aSource)
  {
    m_aLocale = aSource.m_aLocale;
    m_aFormatOptions = aSource.m_aFormatOptions.getClone ();
    m_aLogo = aSource.m_aLogo;
    m_aLabelResolver = aSource.m_aLabelResolver;
    m_bAnnotateBtIDs = aSource.m_bAnnotateBtIDs;
    m_aUserWatermark = aSource.m_aUserWatermark;
    m_aTemplate = aSource.m_aTemplate;
    return thisAsT ();
  }

  /**
   * Convenience: look up a label using the configured resolver and locale.
   *
   * @param sKey
   *        The label key.
   * @return The resolved label, with the BT/BG identifier appended in brackets when
   *         {@link #isAnnotateBtIDs()} is set.
   */
  @NonNull
  public String label (@NonNull final String sKey)
  {
    final String sValue = m_aLabelResolver.getLabel (sKey, m_aLocale);
    if (m_bAnnotateBtIDs && (sKey.startsWith ("BT-") || sKey.startsWith ("BG-")))
      return sValue + " [" + sKey + "]";
    return sValue;
  }

  /**
   * Convenience: look up a code-list label using the configured resolver and locale.
   *
   * @param sListName
   *        The name of the code list, e.g. {@code vatcategory}. Never {@code null}.
   * @param sCode
   *        The code to look up. Never {@code null}.
   * @return The label of the code or the code itself if there is no label. Never {@code null}.
   */
  @NonNull
  public String codeLabel (@NonNull final String sListName, @NonNull final String sCode)
  {
    return m_aLabelResolver.getCodeLabel (sListName, sCode, m_aLocale);
  }
}
