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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.pdflayout.base.PLColor;

/**
 * The look of a template block, group or table row that was marked with {@code {{class:name}}},
 * {@code {{group:name}}} or {@code {{row:name}}}. Register it under that name with
 * {@link PdfRenderContext#putBlockStyle(String, PdfBlockStyle)}. The style is immutable; create it
 * with a {@link Builder}, either from scratch ({@link #builder()}) or from an existing style
 * ({@link #builder(PdfBlockStyle)}, {@link #toBuilder()}).
 *
 * @author Philip Helger
 */
public final class PdfBlockStyle
{
  /** The default width of the top and bottom line in points. */
  public static final float DEFAULT_BORDER_WIDTH = 0.75f;

  /**
   * Builder for {@link PdfBlockStyle}.
   *
   * @author Philip Helger
   */
  public static final class Builder
  {
    private PLColor m_aFillColor;
    private PLColor m_aTextColor;
    private float m_fPadding;
    private PLColor m_aBorderTopColor;
    private PLColor m_aBorderBottomColor;
    private float m_fBorderWidth = DEFAULT_BORDER_WIDTH;
    private Float m_aFontSize;
    private boolean m_bBold;
    private Float m_aWidthPercent;
    private boolean m_bKeepTogether;

    private Builder ()
    {}

    /**
     * Set the background colour.
     *
     * @param aColor
     *        The colour. May be {@code null} for no background.
     * @return this for chaining
     */
    @NonNull
    public Builder fillColor (@Nullable final PLColor aColor)
    {
      m_aFillColor = aColor;
      return this;
    }

    /**
     * Set the text colour.
     *
     * @param aColor
     *        The colour. May be {@code null} to keep the colour of the font.
     * @return this for chaining
     */
    @NonNull
    public Builder textColor (@Nullable final PLColor aColor)
    {
      m_aTextColor = aColor;
      return this;
    }

    /**
     * Set the space between the border of a block and its content.
     *
     * @param fPadding
     *        The padding in points. Must be &ge; 0.
     * @return this for chaining
     */
    @NonNull
    public Builder padding (final float fPadding)
    {
      m_fPadding = fPadding;
      return this;
    }

    /**
     * Set the line above the block.
     *
     * @param aColor
     *        The colour. May be {@code null} for no line.
     * @return this for chaining
     */
    @NonNull
    public Builder borderTopColor (@Nullable final PLColor aColor)
    {
      m_aBorderTopColor = aColor;
      return this;
    }

    /**
     * Set the line below the block.
     *
     * @param aColor
     *        The colour. May be {@code null} for no line.
     * @return this for chaining
     */
    @NonNull
    public Builder borderBottomColor (@Nullable final PLColor aColor)
    {
      m_aBorderBottomColor = aColor;
      return this;
    }

    /**
     * Set the width of the top and bottom line.
     *
     * @param fBorderWidth
     *        The line width in points.
     * @return this for chaining
     */
    @NonNull
    public Builder borderWidth (final float fBorderWidth)
    {
      m_fBorderWidth = fBorderWidth;
      return this;
    }

    /**
     * Set the font size that replaces the one of the block.
     *
     * @param fFontSize
     *        The font size in points.
     * @return this for chaining
     */
    @NonNull
    public Builder fontSize (final float fFontSize)
    {
      return fontSize (Float.valueOf (fFontSize));
    }

    /**
     * Set the font size that replaces the one of the block.
     *
     * @param aFontSize
     *        The font size in points. May be {@code null} to keep the size of the font.
     * @return this for chaining
     */
    @NonNull
    public Builder fontSize (@Nullable final Float aFontSize)
    {
      m_aFontSize = aFontSize;
      return this;
    }

    /**
     * Set whether the text is bold.
     *
     * @param bBold
     *        {@code true} for bold text.
     * @return this for chaining
     */
    @NonNull
    public Builder bold (final boolean bBold)
    {
      m_bBold = bBold;
      return this;
    }

    /**
     * Set the width of a cell in a row.
     *
     * @param fWidthPercent
     *        The width in percent of the row.
     * @return this for chaining
     */
    @NonNull
    public Builder widthPercent (final float fWidthPercent)
    {
      return widthPercent (Float.valueOf (fWidthPercent));
    }

    /**
     * Set the width of a cell in a row.
     *
     * @param aWidthPercent
     *        The width in percent of the row. May be {@code null} if the cell shares the remaining
     *        width with the other cells without a width.
     * @return this for chaining
     */
    @NonNull
    public Builder widthPercent (@Nullable final Float aWidthPercent)
    {
      m_aWidthPercent = aWidthPercent;
      return this;
    }

    /**
     * Set whether the block is moved to the next page as a whole instead of being split across two
     * pages. A block that is higher than a page cannot be kept together.
     *
     * @param bKeepTogether
     *        <code>true</code> to never split the block.
     * @return this for chaining
     */
    @NonNull
    public Builder keepTogether (final boolean bKeepTogether)
    {
      m_bKeepTogether = bKeepTogether;
      return this;
    }

    /**
     * @return The new style. Never {@code null}.
     */
    @NonNull
    public PdfBlockStyle build ()
    {
      return new PdfBlockStyle (this);
    }
  }

  private final PLColor m_aFillColor;
  private final PLColor m_aTextColor;
  private final float m_fPadding;
  private final PLColor m_aBorderTopColor;
  private final PLColor m_aBorderBottomColor;
  private final float m_fBorderWidth;
  private final Float m_aFontSize;
  private final boolean m_bBold;
  private final Float m_aWidthPercent;
  private final boolean m_bKeepTogether;

  private PdfBlockStyle (@NonNull final Builder aBuilder)
  {
    m_aFillColor = aBuilder.m_aFillColor;
    m_aTextColor = aBuilder.m_aTextColor;
    m_fPadding = aBuilder.m_fPadding;
    m_aBorderTopColor = aBuilder.m_aBorderTopColor;
    m_aBorderBottomColor = aBuilder.m_aBorderBottomColor;
    m_fBorderWidth = aBuilder.m_fBorderWidth;
    m_aFontSize = aBuilder.m_aFontSize;
    m_bBold = aBuilder.m_bBold;
    m_aWidthPercent = aBuilder.m_aWidthPercent;
    m_bKeepTogether = aBuilder.m_bKeepTogether;
  }

  /**
   * Get the background colour.
   *
   * @return The background colour or {@code null} if there is none.
   */
  @Nullable
  public PLColor getFillColor ()
  {
    return m_aFillColor;
  }

  /**
   * Get the text colour.
   *
   * @return The text colour or {@code null} to keep the colour of the font.
   */
  @Nullable
  public PLColor getTextColor ()
  {
    return m_aTextColor;
  }

  /**
   * Get the space between the border of a block and its content.
   *
   * @return The padding in points. Always &ge; 0.
   */
  public float getPadding ()
  {
    return m_fPadding;
  }

  /**
   * Get the colour of the line above a block.
   *
   * @return The colour or {@code null} if there is no line.
   */
  @Nullable
  public PLColor getBorderTopColor ()
  {
    return m_aBorderTopColor;
  }

  /**
   * Get the colour of the line below a block.
   *
   * @return The colour or {@code null} if there is no line.
   */
  @Nullable
  public PLColor getBorderBottomColor ()
  {
    return m_aBorderBottomColor;
  }

  /**
   * Get the width of the top and bottom line.
   *
   * @return The line width in points.
   */
  public float getBorderWidth ()
  {
    return m_fBorderWidth;
  }

  /**
   * Get the font size that replaces the one of the block.
   *
   * @return The font size in points or {@code null} to keep the size of the font.
   */
  @Nullable
  public Float getFontSize ()
  {
    return m_aFontSize;
  }

  public boolean hasFontSize ()
  {
    return m_aFontSize != null;
  }

  /**
   * Check if the text is set in bold.
   *
   * @return {@code true} if the text of the block is bold.
   */
  public boolean isBold ()
  {
    return m_bBold;
  }

  /**
   * Get the width of a cell in a row.
   *
   * @return The width in percent of the row or {@code null} if the cell shares the remaining width
   *         with the other cells without a width.
   */
  @Nullable
  public Float getWidthPercent ()
  {
    return m_aWidthPercent;
  }

  /**
   * Create an empty builder.
   *
   * @return A new builder. Never {@code null}.
   */
  @NonNull
  public static Builder builder ()
  {
    return new Builder ();
  }

  /**
   * Create a builder that is initialized with the values of an existing style.
   *
   * @param aSource
   *        The style to start from. Never {@code null}.
   * @return A new builder. Never {@code null}.
   */
  @NonNull
  public static Builder builder (@NonNull final PdfBlockStyle aSource)
  {
    return new Builder ().fillColor (aSource.m_aFillColor)
                         .textColor (aSource.m_aTextColor)
                         .padding (aSource.m_fPadding)
                         .borderTopColor (aSource.m_aBorderTopColor)
                         .borderBottomColor (aSource.m_aBorderBottomColor)
                         .borderWidth (aSource.m_fBorderWidth)
                         .fontSize (aSource.m_aFontSize)
                         .bold (aSource.m_bBold)
                         .widthPercent (aSource.m_aWidthPercent)
                         .keepTogether (aSource.m_bKeepTogether);
  }

  /**
   * Create a new style.
   *
   * @param aFillColor
   *        The background colour. May be {@code null} for no background.
   * @param aTextColor
   *        The text colour. May be {@code null} to keep the colour of the font.
   */
  public PdfBlockStyle (@Nullable final PLColor aFillColor, @Nullable final PLColor aTextColor)
  {
    this (builder ().fillColor (aFillColor).textColor (aTextColor));
  }

  /**
   * Create a builder that is initialized with the values of this style.
   *
   * @return A new builder. Never {@code null}.
   */
  @NonNull
  public Builder toBuilder ()
  {
    return builder (this);
  }

  /**
   * Combine this style with the style of a further class of the same block. The other style wins
   * for all that it sets, and all else stays as it is here.
   *
   * @param aOther
   *        The style to apply on top of this one. Never {@code null}.
   * @return A new style. Never {@code null}.
   */
  @NonNull
  public PdfBlockStyle getOverlaidWith (@NonNull final PdfBlockStyle aOther)
  {
    final Builder ret = toBuilder ();
    if (aOther.m_aFillColor != null)
      ret.fillColor (aOther.m_aFillColor);
    if (aOther.m_aTextColor != null)
      ret.textColor (aOther.m_aTextColor);
    if (aOther.m_fPadding > 0)
      ret.padding (aOther.m_fPadding);
    if (aOther.m_aBorderTopColor != null)
      ret.borderTopColor (aOther.m_aBorderTopColor);
    if (aOther.m_aBorderBottomColor != null)
      ret.borderBottomColor (aOther.m_aBorderBottomColor);
    if (Float.compare (aOther.m_fBorderWidth, DEFAULT_BORDER_WIDTH) != 0)
      ret.borderWidth (aOther.m_fBorderWidth);
    if (aOther.m_aFontSize != null)
      ret.fontSize (aOther.m_aFontSize);
    if (aOther.m_bBold)
      ret.bold (true);
    if (aOther.m_aWidthPercent != null)
      ret.widthPercent (aOther.m_aWidthPercent);
    if (aOther.m_bKeepTogether)
      ret.keepTogether (true);
    return ret.build ();
  }

  /**
   * Create a style with a background colour only.
   *
   * @param aFillColor
   *        The background colour. Never {@code null}.
   * @return The new style. Never {@code null}.
   */
  @NonNull
  public static PdfBlockStyle fill (@NonNull final PLColor aFillColor)
  {
    return builder ().fillColor (aFillColor).build ();
  }

  /**
   * Create a style with a text colour only.
   *
   * @param aTextColor
   *        The text colour. Never {@code null}.
   * @return The new style. Never {@code null}.
   */
  @NonNull
  public static PdfBlockStyle text (@NonNull final PLColor aTextColor)
  {
    return builder ().textColor (aTextColor).build ();
  }

  /**
   * Check if the block is moved to the next page as a whole instead of being split across two
   * pages.
   *
   * @return <code>true</code> if the block is never split.
   */
  public boolean isKeepTogether ()
  {
    return m_bKeepTogether;
  }
}
