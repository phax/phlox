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

import com.helger.annotation.concurrent.Immutable;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.codelist.EEN16931InvoiceTypeCode;
import com.helger.en16931.basics.codelist.EEN16931InvoiceTypeCodeRole;
import com.helger.phlox.api.PhloxLogHelper;
import com.helger.phlox.api.model.EPhloxDocumentKind;

/**
 * Helpers that all {@link IInvoiceReader} implementations share, so that the same value leads to
 * the same result in every syntax.
 *
 * @author Philip Helger
 */
@Immutable
public final class PhloxReaderHelper
{
  private PhloxReaderHelper ()
  {}

  /**
   * Determine the document kind from BT-3 (Invoice type code). The EN 16931 code list
   * ({@link EEN16931InvoiceTypeCode}) assigns every code to exactly one kind.
   *
   * @param sTypeCode
   *        BT-3. May be {@code null}.
   * @param eDefaultKind
   *        The kind to use if BT-3 is missing or unknown. For UBL that is the kind of the root
   *        element, CII has only {@link EPhloxDocumentKind#INVOICE} as an option. Never
   *        {@code null}.
   * @param aErrors
   *        The error list that receives a warning for an unknown code. Never {@code null}.
   * @return The document kind. Never {@code null}.
   */
  @NonNull
  public static EPhloxDocumentKind getDocumentKind (@Nullable final String sTypeCode,
                                                    @NonNull final EPhloxDocumentKind eDefaultKind,
                                                    @NonNull final ErrorList aErrors)
  {
    if (sTypeCode == null || sTypeCode.isEmpty ())
      return eDefaultKind;

    final EEN16931InvoiceTypeCode eTypeCode = EEN16931InvoiceTypeCode.getFromIDOrNull (sTypeCode);
    if (eTypeCode != null)
      return eTypeCode.getRole () == EEN16931InvoiceTypeCodeRole.CREDIT_NOTE ? EPhloxDocumentKind.CREDIT_NOTE
                                                                             : EPhloxDocumentKind.INVOICE;

    aErrors.add (SingleError.builderWarn ()
                            .errorText ("Unknown BT-3 invoice type code '" +
                                        PhloxLogHelper.sanitise (sTypeCode) +
                                        "' — defaulting to " +
                                        eDefaultKind)
                            .build ());
    return eDefaultKind;
  }
}
