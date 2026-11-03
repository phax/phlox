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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.time.format.FormatStyle;
import java.util.Locale;

import org.junit.Test;

import com.helger.phlox.api.AbstractRenderContext;
import com.helger.phlox.api.ENegativeStyle;
import com.helger.phlox.api.watermark.EWatermarkPosition;
import com.helger.phlox.api.watermark.WatermarkSpec;
import com.helger.phlox.template.PhloxTemplate;

/**
 * Test class for class {@link PhloxThemeApplier}.
 *
 * @author Philip Helger
 */
public final class PhloxThemeApplierTest
{
  /** Concrete subclass purely so the abstract base can be instantiated in this test. */
  private static final class TestCtx extends AbstractRenderContext <TestCtx>
  {}

  @Test
  public void testApplyCommon ()
  {
    final PhloxTheme aTheme = PhloxThemeReader.readFromString ("{\"variables\":{\"--c\":\"#336699\"},\"common\":{"
                                                               + "\"locale\":\"de_AT\",\"dateStyle\":\"long\","
                                                               + "\"amountMinFractionDigits\":3,\"percentMinFractionDigits\":1,"
                                                               + "\"negativeStyle\":\"minus-sign\",\"annotateBtIDs\":true,\"showPageFooter\":false,\"showPageHeader\":false,"
                                                               + "\"showPageHeaderOnFirstPage\":true,\"showPageFooterOnFirstPage\":false,"
                                                               + "\"template\":\"default\","
                                                               + "\"watermark\":{\"text\":\"DRAFT\",\"opacity\":0.5,\"rotation\":-30,"
                                                               + "\"color\":\"var(--c)\",\"position\":\"footer-right\",\"fontSize\":20,\"repeatPerPage\":false}}}",
                                                               null);
    final TestCtx aCtx = new TestCtx ();
    PhloxThemeApplier.applyCommon (aCtx, aTheme);

    assertEquals (Locale.forLanguageTag ("de-AT"), aCtx.getLocale ());
    assertEquals (FormatStyle.LONG, aCtx.getFormatOptions ().getDateStyle ());
    assertEquals (Integer.valueOf (3), aCtx.getFormatOptions ().getAmountMinFractionDigits ());
    assertEquals (1, aCtx.getFormatOptions ().getPercentMinFractionDigits ());
    assertEquals (ENegativeStyle.MINUS_SIGN, aCtx.getFormatOptions ().getNegativeStyle ());
    assertTrue (aCtx.isAnnotateBtIDs ());
    assertFalse (aCtx.isShowPageFooter ());
    assertFalse (aCtx.isShowPageHeader ());
    assertTrue (aCtx.isShowPageHeaderOnFirstPage ());
    assertFalse (aCtx.isShowPageFooterOnFirstPage ());
    assertSame (PhloxTemplate.getDefault (), aCtx.getTemplate ());

    final WatermarkSpec aWM = aCtx.getUserWatermark ();
    assertNotNull (aWM);
    assertEquals ("DRAFT", aWM.getText ());
    assertEquals (0.5f, aWM.getOpacity (), 0.0001f);
    assertEquals (-30f, aWM.getRotation (), 0.0001f);
    assertEquals (new Color (0x33, 0x66, 0x99), aWM.getColor ());
    assertEquals (EWatermarkPosition.FOOTER_RIGHT, aWM.getPosition ());
    assertEquals (20f, aWM.getFontSizePt (), 0.0001f);
  }

  @Test
  public void testOnlyWhatIsInTheThemeChanges ()
  {
    final TestCtx aCtx = new TestCtx ().setLocale (Locale.FRENCH);
    PhloxThemeApplier.applyCommon (aCtx, PhloxThemeReader.readFromString ("{}", null));
    assertEquals (Locale.FRENCH, aCtx.getLocale ());
    assertNull (aCtx.getUserWatermark ());
    assertNull (aCtx.getLogo ());
    assertNull (aCtx.getTemplate ());
    assertFalse (aCtx.isAnnotateBtIDs ());
  }

  @Test
  public void testLogoFromTheClasspath ()
  {
    final TestCtx aCtx = new TestCtx ();
    PhloxThemeApplier.applyCommon (aCtx,
                                   PhloxThemeReader.readFromString ("{\"common\":{\"logo\":\"classpath:sample-theme.json\"}}", null));
    assertNotNull (aCtx.getLogo ());
    assertTrue (aCtx.getLogo ().getBinary ().length > 10);
    assertEquals ("image/png", aCtx.getLogo ().getMimeCode ());
  }

  @Test
  public void testErrors ()
  {
    final String [] aInvalid = { "{\"common\":{\"locale\":\"de\",\"colour\":1}}",
                                 "{\"common\":{\"watermark\":{\"opacity\":0.5}}}",
                                 "{\"common\":{\"watermark\":{\"text\":\"x\",\"position\":\"left\"}}}",
                                 "{\"common\":{\"dateStyle\":\"tiny\"}}",
                                 "{\"common\":{\"logo\":\"classpath:does-not-exist.png\"}}" };
    for (final String sJson : aInvalid)
      try
      {
        PhloxThemeApplier.applyCommon (new TestCtx (), PhloxThemeReader.readFromString (sJson, null));
        fail ("Expected an error for " + sJson);
      }
      catch (final PhloxThemeException ex)
      {
        assertNotNull (ex.getMessage ());
      }
  }
}
