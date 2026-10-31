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
package com.helger.phlox.template;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jspecify.annotations.NonNull;
import org.junit.Test;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.helger.base.io.nonblocking.NonBlockingByteArrayInputStream;

/**
 * Test class for class {@link PhloxQrCodeGenerator}.
 *
 * @author Philip Helger
 */
public final class PhloxQrCodeGeneratorTest
{
  @NonNull
  private static String _decode (final byte @NonNull [] aPng) throws IOException, ReaderException
  {
    final BufferedImage aImage = ImageIO.read (new NonBlockingByteArrayInputStream (aPng));
    assertNotNull (aImage);
    assertEquals (aImage.getWidth (), aImage.getHeight ());
    final int nSize = aImage.getWidth ();
    final int [] aPixels = aImage.getRGB (0, 0, nSize, nSize, null, 0, nSize);
    final BinaryBitmap aBitmap = new BinaryBitmap (new HybridBinarizer (new RGBLuminanceSource (nSize, nSize, aPixels)));
    return new QRCodeReader ().decode (aBitmap, Map.of (DecodeHintType.PURE_BARCODE, Boolean.TRUE)).getText ();
  }

  @Test
  public void testRoundTrip () throws IOException, ReaderException
  {
    for (final String sText : new String [] { "x",
                                              "https://portal.example.com/inv/12?a=1&b=2",
                                              "Müller & Łódź\nZeile 2 €",
                                              "y".repeat (1000) })
      assertEquals (sText, _decode (PhloxQrCodeGenerator.createPng (sText)));
  }

  @Test
  public void testTooLong ()
  {
    assertNull (PhloxQrCodeGenerator.createPng ("x".repeat (10_000)));
  }
}
