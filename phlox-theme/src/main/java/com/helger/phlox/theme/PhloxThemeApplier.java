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
package com.helger.phlox.theme;

import java.time.format.FormatStyle;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringReplace;
import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.ENegativeStyle;
import com.helger.phlox.api.PhloxFormatOptions;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.watermark.EWatermarkPosition;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.template.PhloxTemplate;

/**
 * Applies the <code>common</code> part of a {@link PhloxTheme} to a render context. The keys of
 * <code>common</code> are:
 * <ul>
 * <li><code>locale</code> - the output language as language tag, like <code>de</code> or
 * <code>de-AT</code></li>
 * <li><code>dateStyle</code> - <code>short</code>, <code>medium</code>, <code>long</code> or
 * <code>full</code></li>
 * <li><code>amountMinFractionDigits</code>, <code>percentMinFractionDigits</code> - the minimum number of decimals,
 * <code>negativeStyle</code> - how negative numbers are shown</li>
 * <li><code>annotateBtIDs</code> - append the BT/BG identifier to every label (<code>true</code> or
 * <code>false</code>)</li>
 * <li><code>template</code> - <code>default</code>, <code>classpath:path</code> or the path of a
 * <code>.ptpl</code> file</li>
 * <li><code>logo</code> - the path of a PNG, JPEG or GIF file, or <code>classpath:path</code></li>
 * <li><code>watermark</code> - an object with the keys <code>text</code> (required),
 * <code>opacity</code>, <code>rotation</code>, <code>color</code>, <code>position</code>
 * (<code>center</code>, <code>top</code>, <code>bottom</code> or <code>footer-right</code>),
 * <code>fontSize</code> and <code>repeatPerPage</code></li>
 * </ul>
 * The label resolver cannot be set in a theme, because it is code.
 *
 * @author Philip Helger
 */
@Immutable
public final class PhloxThemeApplier
{
  private static final String TEMPLATE_DEFAULT = "default";
  private static final String TEMPLATE_CLASSPATH_PREFIX = "classpath:";

  private PhloxThemeApplier ()
  {}

  @NonNull
  private static WatermarkSpec _createWatermark (@NonNull final PhloxThemeObject aObject)
  {
    aObject.checkKeys ("text", "opacity", "rotation", "color", "position", "fontSize", "repeatPerPage");
    final String sText = aObject.getString ("text");
    if (sText == null || sText.isEmpty ())
      throw new PhloxThemeException ("Theme error at '" + aObject.getPath () + ".text': the text of the watermark is required");
    final WatermarkSpec ret = WatermarkSpec.text (sText);
    if (aObject.getFloat ("opacity") != null)
      ret.setOpacity (aObject.getFloat ("opacity").floatValue ());
    if (aObject.getFloat ("rotation") != null)
      ret.setRotation (aObject.getFloat ("rotation").floatValue ());
    if (aObject.getColor ("color") != null)
      ret.setColor (aObject.getColor ("color"));
    if (aObject.getEnum ("position", EWatermarkPosition.class) != null)
      ret.setPosition (aObject.getEnum ("position", EWatermarkPosition.class));
    if (aObject.getFloat ("fontSize") != null)
      ret.setFontSizePt (aObject.getFloat ("fontSize").floatValue ());
    if (aObject.getBoolean ("repeatPerPage") != null)
      ret.setRepeatPerPage (aObject.getBoolean ("repeatPerPage").booleanValue ());
    return ret;
  }

  /**
   * Apply the <code>common</code> part of the theme. Only the values that are in the theme are
   * changed.
   *
   * @param aCtx
   *        The render context to change. May not be <code>null</code>.
   * @param aTheme
   *        The theme. May not be <code>null</code>.
   * @throws PhloxThemeException
   *         If a value is invalid.
   */
  public static void applyCommon (@NonNull final AbstractRenderContext <?> aCtx, @NonNull final PhloxTheme aTheme)
  {
    ValueEnforcer.notNull (aCtx, "Ctx");
    ValueEnforcer.notNull (aTheme, "Theme");

    final PhloxThemeObject aCommon = aTheme.getCommon ();
    aCommon.checkKeys ("locale",
                       "dateStyle",
                       "amountMinFractionDigits",
                       "percentMinFractionDigits",
                       "negativeStyle",
                       "annotateBtIDs",
                       "template",
                       "logo",
                       "watermark");

    final String sLocale = aCommon.getString ("locale");
    if (sLocale != null)
      aCtx.setLocale (Locale.forLanguageTag (StringReplace.replaceAll (sLocale, '_', '-')));

    final PhloxFormatOptions aOptions = aCtx.getFormatOptions ();
    final FormatStyle eDateStyle = aCommon.getEnum ("dateStyle", FormatStyle.class);
    if (eDateStyle != null)
      aOptions.setDateStyle (eDateStyle);
    final Integer aAmountDigits = aCommon.getInt ("amountMinFractionDigits");
    if (aAmountDigits != null)
      aOptions.setAmountMinFractionDigits (aAmountDigits);
    final Integer aPercentDigits = aCommon.getInt ("percentMinFractionDigits");
    if (aPercentDigits != null)
      aOptions.setPercentMinFractionDigits (aPercentDigits.intValue ());
    final ENegativeStyle eNegativeStyle = aCommon.getEnum ("negativeStyle", ENegativeStyle.class);
    if (eNegativeStyle != null)
      aOptions.setNegativeStyle (eNegativeStyle);
    aCtx.setFormatOptions (aOptions);

    final Boolean aAnnotate = aCommon.getBoolean ("annotateBtIDs");
    if (aAnnotate != null)
      aCtx.setAnnotateBtIDs (aAnnotate.booleanValue ());

    final String sTemplate = aCommon.getString ("template");
    if (sTemplate != null)
    {
      if (TEMPLATE_DEFAULT.equals (sTemplate))
        aCtx.setTemplate (PhloxTemplate.getDefault ());
      else
        if (sTemplate.startsWith (TEMPLATE_CLASSPATH_PREFIX))
          aCtx.setTemplate (PhloxTemplate.fromClasspath (sTemplate.substring (TEMPLATE_CLASSPATH_PREFIX.length ())));
        else
          aCtx.setTemplate (PhloxTemplate.fromFile (aTheme.resolveFile (sTemplate)));
    }

    final String sLogo = aCommon.getString ("logo");
    if (sLogo != null)
      aCtx.setLogo (PhloxAttachment.createLogo (aTheme.readBinary (sLogo), sLogo));

    final PhloxThemeObject aWatermark = aCommon.getObject ("watermark");
    if (aWatermark != null)
      aCtx.setUserWatermark (_createWatermark (aWatermark));
  }
}
