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
package com.helger.phlox.api.watermark;

import java.awt.Color;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;

/**
 * Syntax-neutral watermark description. Consumed identically by HTML and PDF writers; each writer
 * interprets it for its medium.
 *
 * @author Philip Helger
 */
public class WatermarkSpec
{
  /** Default opacity of the watermark. */
  public static final float DEFAULT_OPACITY = 0.10f;
  /** Default rotation of the watermark in degrees. */
  public static final float DEFAULT_ROTATION = -45f;
  /** Default font size of the watermark in points. */
  public static final float DEFAULT_FONT_SIZE_PT = 72f;
  /** Default for repeating the watermark on every page. */
  public static final boolean DEFAULT_REPEAT_PER_PAGE = true;

  private final String m_sText;
  private float m_fOpacity = DEFAULT_OPACITY;
  private float m_fRotation = DEFAULT_ROTATION;
  private Color m_aColor = Color.GRAY;
  private EWatermarkPosition m_ePosition = EWatermarkPosition.CENTER;
  private float m_fFontSizePt = DEFAULT_FONT_SIZE_PT;
  private boolean m_bRepeatPerPage = DEFAULT_REPEAT_PER_PAGE;

  /**
   * Subclass-only constructor.
   *
   * @param sText
   *        The watermark text; never {@code null}.
   */
  protected WatermarkSpec (@NonNull final String sText)
  {
    ValueEnforcer.notNull (sText, "Text");
    m_sText = sText;
  }

  /**
   * Create a new spec with the given text and all other settings at their defaults.
   *
   * @param sText
   *        Watermark text; never {@code null}.
   * @return The new spec.
   */
  @NonNull
  public static WatermarkSpec text (@NonNull final String sText)
  {
    return new WatermarkSpec (sText);
  }

  /**
   * Get the watermark text.
   *
   * @return The watermark text. Never {@code null}.
   */
  @NonNull
  public String getText ()
  {
    return m_sText;
  }

  /**
   * Get the opacity in the {@code [0..1]} range (default {@value #DEFAULT_OPACITY}).
   *
   * @return The opacity in the {@code [0..1]} range (default {@value #DEFAULT_OPACITY}).
   */
  public float getOpacity ()
  {
    return m_fOpacity;
  }

  /**
   * Set the watermark opacity.
   *
   * @param fOpacity
   *        Opacity in the {@code [0..1]} range. Values outside the range are clamped at use.
   * @return this for chaining.
   */
  @NonNull
  public WatermarkSpec setOpacity (final float fOpacity)
  {
    m_fOpacity = fOpacity;
    return this;
  }

  /**
   * Get the rotation in degrees.
   *
   * @return Rotation in degrees. Negative values rotate clockwise.
   */
  public float getRotation ()
  {
    return m_fRotation;
  }

  /**
   * Set the watermark rotation in degrees.
   *
   * @param fRotation
   *        Rotation in degrees (negative = clockwise).
   * @return this for chaining.
   */
  @NonNull
  public WatermarkSpec setRotation (final float fRotation)
  {
    m_fRotation = fRotation;
    return this;
  }

  /**
   * Get the watermark colour.
   *
   * @return The watermark colour. Never {@code null}.
   */
  @NonNull
  public Color getColor ()
  {
    return m_aColor;
  }

  /**
   * Set the watermark colour.
   *
   * @param aColor
   *        Colour; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public WatermarkSpec setColor (@NonNull final Color aColor)
  {
    m_aColor = aColor;
    return this;
  }

  /**
   * Get the watermark position.
   *
   * @return The watermark position. Never {@code null}.
   */
  @NonNull
  public EWatermarkPosition getPosition ()
  {
    return m_ePosition;
  }

  /**
   * Set the watermark position.
   *
   * @param ePosition
   *        Position; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public WatermarkSpec setPosition (@NonNull final EWatermarkPosition ePosition)
  {
    m_ePosition = ePosition;
    return this;
  }

  /**
   * Get the font size in PDF points (default {@value #DEFAULT_FONT_SIZE_PT}).
   *
   * @return The font size in PDF points (default {@value #DEFAULT_FONT_SIZE_PT}).
   */
  public float getFontSizePt ()
  {
    return m_fFontSizePt;
  }

  /**
   * Set the font size in PDF points.
   *
   * @param fFontSizePt
   *        Font size in points.
   * @return this for chaining.
   */
  @NonNull
  public WatermarkSpec setFontSizePt (final float fFontSizePt)
  {
    m_fFontSizePt = fFontSizePt;
    return this;
  }

  /**
   * Get the {@code true} when the watermark should repeat on every page.
   *
   * @return {@code true} when the watermark should repeat on every page.
   */
  public boolean isRepeatPerPage ()
  {
    return m_bRepeatPerPage;
  }

  /**
   * Set whether the watermark should repeat on every page. Otherwise the PDF writer draws it on the
   * first page only, and the HTML writer positions it <code>absolute</code> instead of
   * <code>fixed</code>, so that a browser prints it on the first page only. The plain-text writer
   * shows it once in any case.
   *
   * @param bRepeatPerPage
   *        {@code true} to repeat.
   * @return this for chaining.
   */
  @NonNull
  public WatermarkSpec setRepeatPerPage (final boolean bRepeatPerPage)
  {
    m_bRepeatPerPage = bRepeatPerPage;
    return this;
  }

  /**
   * Get the opaque RGBA string for HTML.
   *
   * @return An opaque RGBA string for HTML (e.g. {@code "rgba(128,128,128,0.10)"}) combining the
   *         configured colour and opacity.
   */
  @NonNull
  public String getCssRgba ()
  {
    return "rgba(" +
           m_aColor.getRed () +
           "," +
           m_aColor.getGreen () +
           "," +
           m_aColor.getBlue () +
           "," +
           m_fOpacity +
           ")";
  }

  /**
   * Helper that returns the given spec, or {@code null} if the spec is {@code null}. Used in
   * inheritance setters to keep things compact.
   *
   * @param aSpec
   *        The spec, may be {@code null}.
   * @return The same spec, or {@code null}.
   */
  @Nullable
  public static WatermarkSpec orNull (@Nullable final WatermarkSpec aSpec)
  {
    return aSpec;
  }
}
