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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.helger.base.io.nonblocking.NonBlockingByteArrayOutputStream;
import com.helger.phlox.api.PhloxRuntimeException;

/**
 * Creates QR codes as PNG images. The size of the image only defines the sharpness, because the
 * writers scale it to the requested height.
 *
 * @author Philip Helger
 */
public final class PhloxQrCodeGenerator
{
  /** The MIME type of the created images. */
  public static final String MIME_TYPE = "image/png";

  private static final int PIXELS_PER_MODULE = 8;
  private static final int WHITE = 0xffffff;
  private static final int BLACK = 0x000000;

  private PhloxQrCodeGenerator ()
  {}

  @NonNull
  private static BufferedImage _createImage (@NonNull final BitMatrix aMatrix)
  {
    final int nSize = aMatrix.getWidth () * PIXELS_PER_MODULE;
    final BufferedImage aImage = new BufferedImage (nSize, nSize, BufferedImage.TYPE_INT_RGB);
    for (int nY = 0; nY < nSize; ++nY)
      for (int nX = 0; nX < nSize; ++nX)
        aImage.setRGB (nX, nY, aMatrix.get (nX / PIXELS_PER_MODULE, nY / PIXELS_PER_MODULE) ? BLACK : WHITE);
    return aImage;
  }

  /**
   * Create a QR code with error correction level M. The text is encoded as UTF-8.
   *
   * @param sText
   *        The text to encode. May neither be {@code null} nor empty.
   * @return The PNG image, or {@code null} if the text is too long for a QR code.
   */
  public static byte @Nullable [] createPng (@NonNull final String sText)
  {
    final Map <EncodeHintType, Object> aHints = new EnumMap <> (EncodeHintType.class);
    // The default of ZXing is ISO-8859-1
    aHints.put (EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name ());
    aHints.put (EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
    final BitMatrix aMatrix;
    try
    {
      // A size of 0 gives exactly one bit per module
      aMatrix = new QRCodeWriter ().encode (sText, BarcodeFormat.QR_CODE, 0, 0, aHints);
    }
    catch (final WriterException ex)
    {
      return null;
    }

    try (final NonBlockingByteArrayOutputStream aOut = new NonBlockingByteArrayOutputStream ())
    {
      // Not ImageIO.write to a stream: that caches in a temporary file by default and therefore
      // fails on a read-only file system
      try (final MemoryCacheImageOutputStream aIOS = new MemoryCacheImageOutputStream (aOut))
      {
        if (!ImageIO.write (_createImage (aMatrix), "PNG", aIOS))
          throw new IOException ("No ImageIO writer for PNG");
      }
      return aOut.toByteArray ();
    }
    catch (final IOException ex)
    {
      throw new PhloxRuntimeException ("Failed to write the QR code image", ex);
    }
  }
}
