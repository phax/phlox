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
package com.helger.phlox.api.i18n;

import java.util.Locale;

import org.jspecify.annotations.NonNull;

/**
 * Lookup of human-readable labels for EN 16931 BG/BT identifiers, plus code-list values for the
 * UNTDID lists used in invoices (VAT category, payment means, etc.).
 *
 * @author Philip Helger
 */
public interface IPhloxLabelResolver
{
  /**
   * Look up a section/BG/BT label.
   *
   * @param sKey
   *        The key (e.g. {@code "BT-1"} or {@code "section.parties.title"}). Never
   *        {@code null}.
   * @param aLocale
   *        The target locale. Never {@code null}.
   * @return The translated label, or the English one if the language does not have the key, or the
   *         key itself if no translation exists in any bundle (so the gap is visible in output).
   */
  @NonNull
  String getLabel (@NonNull String sKey, @NonNull Locale aLocale);

  /**
   * Look up a code-list value description.
   *
   * @param sListName
   *        The logical code list name (e.g. {@code "vat.category"} or {@code "paymentmeans"}).
   *        Never {@code null}.
   * @param sCode
   *        The code value (e.g. {@code "S"} or {@code "30"}). Never {@code null}.
   * @param aLocale
   *        The target locale. Never {@code null}.
   * @return The translated description, or the code itself if no translation exists.
   */
  @NonNull
  String getCodeLabel (@NonNull String sListName, @NonNull String sCode, @NonNull Locale aLocale);
}
