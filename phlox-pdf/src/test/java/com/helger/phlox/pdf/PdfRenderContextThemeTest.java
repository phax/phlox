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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.helger.pdflayout.base.PLColor;
import com.helger.pdflayout.pdfa.EPdfALevel;
import com.helger.pdflayout.spec.PreloadFont;
import com.helger.phlox.theme.PhloxThemeException;
import com.helger.phlox.theme.PhloxThemeReader;

/**
 * Test class for {@link PdfRenderContext#applyTheme(com.helger.phlox.theme.PhloxTheme)}.
 *
 * @author Philip Helger
 */
public final class PdfRenderContextThemeTest
{
  private static PdfRenderContext _apply (final String sJson)
  {
    return new PdfRenderContext ().applyTheme (PhloxThemeReader.readFromString (sJson, null));
  }

  @Test
  public void testArchiveOptions ()
  {
    final PdfRenderContext aDefault = new PdfRenderContext ();
    final PdfRenderContext aEmpty = _apply ("{\"pdf\":{}}");
    assertEquals (aDefault.getPdfALevel (), aEmpty.getPdfALevel ());
    assertEquals (Boolean.valueOf (aDefault.isPdfUA ()), Boolean.valueOf (aEmpty.isPdfUA ()));
    assertEquals (Boolean.valueOf (aDefault.isEmbedAttachments ()), Boolean.valueOf (aEmpty.isEmbedAttachments ()));
    assertEquals (aDefault.getDocumentCreationDateTime (), aEmpty.getDocumentCreationDateTime ());

    final PdfRenderContext aCtx = _apply ("{\"pdf\":{\"pdfALevel\":\"3A\",\"pdfUA\":true,\"embedAttachments\":true,"
                                          + "\"documentCreationDateTime\":\"2026-11-01T10:15:30+01:00\"}}");
    assertEquals (EPdfALevel.PDF_A_3A, aCtx.getPdfALevel ());
    assertEquals (Boolean.TRUE, Boolean.valueOf (aCtx.isPdfUA ()));
    assertEquals (Boolean.TRUE, Boolean.valueOf (aCtx.isEmbedAttachments ()));
    assertEquals (ZonedDateTime.parse ("2026-11-01T10:15:30+01:00"), aCtx.getDocumentCreationDateTime ());
  }

  @Test
  public void testArchiveOptionsInvalid ()
  {
    for (final String sJson : new String [] { "{\"pdf\":{\"pdfALevel\":\"4x\"}}",
                                              "{\"pdf\":{\"documentCreationDateTime\":\"yesterday\"}}" })
      try
      {
        _apply (sJson);
        fail (sJson);
      }
      catch (final PhloxThemeException ex)
      {
        // Expected
        assertNotNull (ex.getMessage ());
      }
  }

  @Test
  public void testPageAndFonts ()
  {
    final PdfRenderContext aCtx = _apply ("{\"pdf\":{\"pageSize\":\"letter\",\"landscape\":true,"
                                          + "\"margins\":{\"top\":\"10mm\",\"left\":\"1in\"},"
                                          + "\"bodyFont\":{\"size\":9,\"bold\":true,\"color\":\"#102030\"}}}");
    // Letter is 612 x 792 points, landscape swaps them
    assertEquals (792f, aCtx.getPageSize ().getWidth (), 0.01f);
    assertEquals (612f, aCtx.getPageSize ().getHeight (), 0.01f);
    assertEquals (10 * 72 / 25.4f, aCtx.getMargins ().getTop (), 0.01f);
    assertEquals (72f, aCtx.getMargins ().getLeft (), 0.01f);
    // The sides that are not named keep their value
    assertEquals (PdfRenderContext.DEFAULT_MARGINS.getRight (), aCtx.getMargins ().getRight (), 0.01f);
    assertEquals (9f, aCtx.getBodyFont ().getFontSize (), 0.01f);
    assertEquals (new PLColor (0x10, 0x20, 0x30), aCtx.getBodyFont ().getColor ());
    assertEquals (PreloadFont.REGULAR_BOLD, aCtx.getBodyFont ().getPreloadFont ());
  }

  @Test
  public void testPaletteDrivesTheDefaultStyles ()
  {
    final PdfRenderContext aCtx = _apply ("{\"variables\":{\"--brand\":\"#8a1538\",\"--phlox-accent\":\"var(--brand)\"}}");
    final PLColor aBrand = new PLColor (0x8a, 0x15, 0x38);
    assertEquals (aBrand, aCtx.getPalette ().getAccent ());
    assertEquals (aBrand, aCtx.getBlockStyle ("due").getFillColor ());
    assertEquals (aBrand, aCtx.getBlockStyle ("rule").getBorderTopColor ());
    assertEquals (aBrand, aCtx.getBlockStyle ("kind").getTextColor ());
    // The other colours stay as they are
    assertEquals (PdfPalette.DEFAULT.getWash (), aCtx.getPalette ().getWash ());
  }

  @Test
  public void testBlockStyles ()
  {
    final PdfRenderContext aCtx = _apply ("{\"variables\":{\"--x\":\"#00ff00\"},\"pdf\":{\"blockStyles\":{"
                                          + "\"due\":{\"padding\":\"4mm\"},"
                                          + "\"mine\":{\"extends\":\"due\",\"fill\":\"var(--x)\",\"bold\":false,"
                                          + "\"borderBottom\":{\"color\":\"#000000\",\"width\":2}},"
                                          + "\"plain\":{\"reset\":true,\"fontSize\":12}}}}");
    // A style of the same name changes only what is named
    final PdfBlockStyle aDue = aCtx.getBlockStyle ("due");
    assertEquals (4 * 72 / 25.4f, aDue.getPadding (), 0.01f);
    assertEquals (PdfPalette.DEFAULT.getAccent (), aDue.getFillColor ());
    assertTrue (aDue.isBold ());

    // A new style can start from another one
    final PdfBlockStyle aMine = aCtx.getBlockStyle ("mine");
    assertNotNull (aMine);
    assertEquals (new PLColor (0, 255, 0), aMine.getFillColor ());
    assertEquals (PLColor.WHITE, aMine.getTextColor ());
    assertTrue (!aMine.isBold ());
    assertEquals (2f, aMine.getBorderWidth (), 0.01f);
    assertEquals (new PLColor (0, 0, 0), aMine.getBorderBottomColor ());

    // reset starts from nothing, although there is no style of that name yet
    final PdfBlockStyle aPlain = aCtx.getBlockStyle ("plain");
    assertNull (aPlain.getFillColor ());
    assertEquals (Float.valueOf (12f), aPlain.getFontSize ());
  }

  @Test
  public void testErrors ()
  {
    final String [] aInvalid = { "{\"pdf\":{\"pageSzie\":\"a4\"}}",
                                 "{\"pdf\":{\"pageSize\":\"a9\"}}",
                                 "{\"pdf\":{\"bodyFont\":{\"colour\":\"#000\"}}}",
                                 "{\"pdf\":{\"blockStyles\":{\"x\":{\"extends\":\"nothing\"}}}}",
                                 "{\"pdf\":{\"blockStyles\":{\"x\":{\"borderTop\":{\"width\":1}}}}}",
                                 "{\"pdf\":{\"blockStyles\":{\"x\":true}}}" };
    for (final String sJson : aInvalid)
      try
      {
        _apply (sJson);
        fail ("Expected an error for " + sJson);
      }
      catch (final PhloxThemeException ex)
      {
        assertTrue (ex.getMessage (), ex.getMessage ().contains ("pdf."));
      }
  }
}
