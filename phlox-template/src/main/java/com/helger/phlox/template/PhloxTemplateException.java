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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.phlox.api.PhloxRuntimeException;

/**
 * Thrown when a phlox template fails to substitute, parse, or emit. Carries the pipeline stage the
 * failure happened in plus the BG/BT path being resolved (when applicable), so callers can report
 * errors with usable context.
 *
 * @author Philip Helger
 */
public class PhloxTemplateException extends PhloxRuntimeException
{
  /**
   * Stage of the template pipeline. See {@code wiki/Template-Processing.md} § Error handling.
   */
  public enum EStage
  {
    /** Loading the template, before any invoice is involved. */
    LOAD,
    /** Mustache substitution of the invoice into the template. */
    SUBSTITUTE,
    /** CommonMark parsing of the substituted text. */
    PARSE,
    /** Emitting the output (HTML or PDF) from the parsed AST. */
    EMIT
  }

  /** The pipeline stage in which the failure occurred. */
  private final EStage m_eStage;
  /** The BG/BT path being resolved or {@code null}. */
  private final String m_sPath;

  @NonNull
  private static String _build (@NonNull final EStage eStage,
                                @Nullable final String sPath,
                                @NonNull final String sMessage)
  {
    if (sPath == null || sPath.isEmpty ())
      return "[" + eStage + "] " + sMessage;
    return "[" + eStage + " " + sPath + "] " + sMessage;
  }

  /**
   * Construct an exception without an underlying cause.
   *
   * @param eStage
   *        Pipeline stage in which the failure occurred.
   * @param sPath
   *        BG/BT path being resolved, or {@code null} when the error is not path-related.
   * @param sMessage
   *        Human-readable description of the failure.
   */
  public PhloxTemplateException (@NonNull final EStage eStage,
                                 @Nullable final String sPath,
                                 @NonNull final String sMessage)
  {
    super (_build (eStage, sPath, sMessage));
    m_eStage = eStage;
    m_sPath = sPath;
  }

  /**
   * Construct an exception wrapping a lower-level cause.
   *
   * @param eStage
   *        Pipeline stage in which the failure occurred.
   * @param sPath
   *        BG/BT path being resolved, or {@code null} when the error is not path-related.
   * @param sMessage
   *        Human-readable description of the failure.
   * @param aCause
   *        Underlying exception.
   */
  public PhloxTemplateException (@NonNull final EStage eStage,
                                 @Nullable final String sPath,
                                 @NonNull final String sMessage,
                                 @NonNull final Throwable aCause)
  {
    super (_build (eStage, sPath, sMessage), aCause);
    m_eStage = eStage;
    m_sPath = sPath;
  }

  /**
   * Get the pipeline stage that raised the error.
   *
   * @return The pipeline stage that raised the error.
   */
  @NonNull
  public EStage getStage ()
  {
    return m_eStage;
  }

  /**
   * Get the BG/BT path being resolved when the error fired, or {@code null}.
   *
   * @return The BG/BT path being resolved when the error fired, or {@code null}.
   */
  @Nullable
  public String getPath ()
  {
    return m_sPath;
  }
}
