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

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.theme.PhloxTheme;
import com.helger.phlox.theme.PhloxThemeApplier;
import com.helger.phlox.theme.PhloxThemeException;
import com.helger.phlox.theme.PhloxThemeObject;

/**
 * Plain-text specific render context. Adds the line width on top of the shared
 * {@link AbstractRenderContext} knobs.
 *
 * @author Philip Helger
 */
public class TextRenderContext extends AbstractRenderContext <TextRenderContext>
{
  /** Default line width in characters, at which paragraphs are wrapped. */
  public static final int DEFAULT_LINE_WIDTH = 80;

  private int m_nLineWidth = DEFAULT_LINE_WIDTH;

  /** Create a new context with the default plain-text settings. */
  public TextRenderContext ()
  {}

  /**
   * Create a new context pre-populated with the shared settings of another context.
   *
   * @param aSource
   *        The context to copy the shared settings from. Never {@code null}.
   * @return A new context. Never {@code null}.
   */
  @NonNull
  public static TextRenderContext copyFrom (@NonNull final AbstractRenderContext <?> aSource)
  {
    return new TextRenderContext ().copyBaseFieldsFrom (aSource);
  }

  /**
   * Get the line width in characters, at which paragraphs are wrapped. Tables are never wrapped.
   *
   * @return The line width. 0 means that paragraphs are not wrapped.
   */
  public int getLineWidth ()
  {
    return m_nLineWidth;
  }

  /**
   * Set the line width in characters, at which paragraphs are wrapped. Tables are never wrapped.
   *
   * @param nLineWidth
   *        The new value. Must be &ge; 0. 0 means that paragraphs are not wrapped.
   * @return this for chaining.
   */
  @NonNull
  public TextRenderContext setLineWidth (final int nLineWidth)
  {
    ValueEnforcer.isGE0 (nLineWidth, "LineWidth");
    m_nLineWidth = nLineWidth;
    return this;
  }

  /**
   * Apply a theme. The <code>common</code> part (language, formats, template, watermark, ...) and
   * the key <code>lineWidth</code> of the <code>text</code> part apply to the plain-text output;
   * colours, fonts and the <code>html</code> and <code>pdf</code> parts are ignored.
   *
   * @param aTheme
   *        The theme. May not be {@code null}.
   * @return this for chaining.
   * @throws PhloxThemeException
   *         If a value of the theme is invalid.
   */
  @NonNull
  public TextRenderContext applyTheme (@NonNull final PhloxTheme aTheme)
  {
    ValueEnforcer.notNull (aTheme, "Theme");
    PhloxThemeApplier.applyCommon (this, aTheme);

    final PhloxThemeObject aText = aTheme.getText ();
    aText.checkKeys ("lineWidth");
    final Integer aLineWidth = aText.getInt ("lineWidth");
    if (aLineWidth != null)
    {
      if (aLineWidth.intValue () < 0)
        throw new PhloxThemeException ("Theme error at '" +
                                       aText.getPath () +
                                       ".lineWidth': the line width must not be negative");
      setLineWidth (aLineWidth.intValue ());
    }
    return this;
  }
}
