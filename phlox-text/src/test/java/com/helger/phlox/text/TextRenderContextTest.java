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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Locale;

import org.junit.Test;

import com.helger.phlox.theme.PhloxThemeException;
import com.helger.phlox.theme.PhloxThemeReader;

/**
 * Test class for class {@link TextRenderContext}.
 *
 * @author Philip Helger
 */
public final class TextRenderContextTest
{
  @Test
  public void testLineWidth ()
  {
    final TextRenderContext aCtx = new TextRenderContext ();
    assertEquals (TextRenderContext.DEFAULT_LINE_WIDTH, aCtx.getLineWidth ());
    aCtx.setLineWidth (0);
    assertEquals (0, aCtx.getLineWidth ());
    try
    {
      aCtx.setLineWidth (-1);
      fail ();
    }
    catch (final IllegalArgumentException ex)
    {
      // expected
    }
  }

  @Test
  public void testCopyFrom ()
  {
    final TextRenderContext aSource = new TextRenderContext ().setLocale (Locale.GERMAN).setLineWidth (40);
    final TextRenderContext aCopy = TextRenderContext.copyFrom (aSource);
    assertEquals (Locale.GERMAN, aCopy.getLocale ());
    // The line width is not a shared setting
    assertEquals (TextRenderContext.DEFAULT_LINE_WIDTH, aCopy.getLineWidth ());
  }

  @Test
  public void testApplyTheme ()
  {
    assertEquals (40, new TextRenderContext ().applyTheme (PhloxThemeReader.readFromString ("{\"text\":{\"lineWidth\":40}}", null)).getLineWidth ());
    // 0 means no wrapping
    assertEquals (0, new TextRenderContext ().applyTheme (PhloxThemeReader.readFromString ("{\"text\":{\"lineWidth\":0}}", null)).getLineWidth ());
    // Only what is in the theme changes
    assertEquals (33, new TextRenderContext ().setLineWidth (33).applyTheme (PhloxThemeReader.readFromString ("{}", null)).getLineWidth ());

    for (final String sTheme : new String [] { "{\"text\":{\"lineWidth\":-1}}", "{\"text\":{\"lineWdth\":10}}" })
      try
      {
        new TextRenderContext ().applyTheme (PhloxThemeReader.readFromString (sTheme, null));
        fail ();
      }
      catch (final PhloxThemeException ex)
      {
        // expected
      }
  }
}
