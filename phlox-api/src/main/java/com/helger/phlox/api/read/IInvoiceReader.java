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
package com.helger.phlox.api.read;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.phlox.api.model.PhloxInvoice;

/**
 * SPI for source readers. An implementation parses an electronic invoice from a source of type
 * {@code SRC} and populates a {@link PhloxInvoice}.
 * <p>
 * Reading is considered successful only when both: the returned model is non-{@code null} AND
 * the passed-in {@link ErrorList} does not contain any error entries. Warnings are tolerated: they
 * name input that was ignored or defaulted, for example a date that could not be parsed. The
 * {@link ErrorList} contains the error of the XSD validation of the source, if there is one.
 *
 * @author Philip Helger
 * @param <SRC>
 *        The source type, e.g. {@code InputStream}.
 */
public interface IInvoiceReader <SRC>
{
  /**
   * Read the source and produce an intermediate-model invoice.
   *
   * @param aSource
   *        The source to read from. Never {@code null}.
   * @param aErrors
   *        The error list to populate with any parsing/mapping issues. Never {@code null}.
   * @return The populated invoice on success, or {@code null} on catastrophic failure (malformed
   *         XML, wrong root element, etc.).
   */
  @Nullable
  PhloxInvoice read (@NonNull SRC aSource, @NonNull ErrorList aErrors);
}
