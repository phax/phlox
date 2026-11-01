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
package com.helger.phlox.api.model;

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.mime.CMimeType;

/**
 * Binary attachment. Used for BT-125 attached document with BT-125-1 mime code and BT-125-2
 * filename inside BG-24 supporting documents. Also reused by
 * {@code AbstractRenderContext.setLogo(...)} for the header logo.
 *
 * @author Philip Helger
 */
public class PhloxAttachment
{
  private byte [] m_aBinary;
  private String m_sMimeCode;
  private String m_sFilename;

  /** Create an empty attachment. */
  public PhloxAttachment ()
  {}

  /**
   * Get the binary payload (BT-125).
   *
   * @return The binary payload (BT-125). May be {@code null}.
   */
  public byte @Nullable [] getBinary ()
  {
    return m_aBinary;
  }

  /**
   * Set the binary payload (BT-125).
   *
   * @param aBinary
   *        The bytes; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxAttachment setBinary (final byte @Nullable [] aBinary)
  {
    m_aBinary = aBinary;
    return this;
  }

  /**
   * Get the MIME type of the attachment (BT-125-1).
   *
   * @return The MIME type of the attachment (BT-125-1). May be {@code null}.
   */
  @Nullable
  public String getMimeCode ()
  {
    return m_sMimeCode;
  }

  /**
   * Set the MIME type of the attachment (BT-125-1).
   *
   * @param sMimeCode
   *        The MIME type; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxAttachment setMimeCode (@Nullable final String sMimeCode)
  {
    m_sMimeCode = sMimeCode;
    return this;
  }

  /**
   * Get the original filename of the attachment (BT-125-2).
   *
   * @return The original filename of the attachment (BT-125-2). May be {@code null}.
   */
  @Nullable
  public String getFilename ()
  {
    return m_sFilename;
  }

  /**
   * Set the original filename of the attachment (BT-125-2).
   *
   * @param sFilename
   *        The filename; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxAttachment setFilename (@Nullable final String sFilename)
  {
    m_sFilename = sFilename;
    return this;
  }

  /**
   * Get the MIME type of an image from its file name.
   *
   * @param sFilename
   *        The file name or path. Never {@code null}.
   * @return The MIME type of a PNG, JPEG, GIF or SVG file, {@code null} for any other file.
   */
  @Nullable
  public static String getImageMimeCodeOrNull (@NonNull final String sFilename)
  {
    final String sLower = sFilename.toLowerCase (Locale.ROOT);
    if (sLower.endsWith (".png"))
      return CMimeType.IMAGE_PNG.getAsString ();
    if (sLower.endsWith (".jpg") || sLower.endsWith (".jpeg"))
      return CMimeType.IMAGE_JPG.getAsString ();
    if (sLower.endsWith (".gif"))
      return CMimeType.IMAGE_GIF.getAsString ();
    if (sLower.endsWith (".svg"))
      return "image/svg+xml";
    return null;
  }

  /**
   * Create the attachment of a logo that was read from a file.
   *
   * @param aBinary
   *        The content of the file. Never {@code null}.
   * @param sFilename
   *        The name of the file. Never {@code null}.
   * @return The attachment, with PNG as the MIME type of an unknown file type. Never {@code null}.
   */
  @NonNull
  public static PhloxAttachment createLogo (final byte @NonNull [] aBinary, @NonNull final String sFilename)
  {
    final String sMimeCode = getImageMimeCodeOrNull (sFilename);
    return new PhloxAttachment ().setBinary (aBinary)
                                 .setMimeCode (sMimeCode != null ? sMimeCode : CMimeType.IMAGE_PNG.getAsString ())
                                 .setFilename (sFilename);
  }
}
