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
package com.helger.phlox.cii;

import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931DateFormatCode;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phlox.api.PhloxLogHelper;

/**
 * Base class of the edition specific CII readers. It only contains helpers that do not depend on
 * the JAXB types of a specific CII release. The CII D16B and CII D25A JAXB classes (identifier,
 * text, date, party, ...) are generated independently and share no base classes, so everything that
 * touches them has to stay in the edition specific reader.
 *
 * @author Philip Helger
 */
abstract class AbstractCiiInvoiceReader
{
  /**
   * Check if a tax amount is the invoice total VAT amount in the VAT accounting currency (BT-111).
   * An amount in the invoice currency is always BT-110, also if BT-6 names the same currency.
   *
   * @param sInvoiceCurrency
   *        BT-5. May be {@code null}.
   * @param sAccountingCurrency
   *        BT-6. May be {@code null}.
   * @param sAmountCurrency
   *        The currency of the tax amount. May be {@code null}.
   * @return {@code true} for BT-111, {@code false} for BT-110.
   */
  protected static boolean isVatAccountingAmount (@Nullable final String sInvoiceCurrency,
                                                  @Nullable final String sAccountingCurrency,
                                                  @Nullable final String sAmountCurrency)
  {
    return sAccountingCurrency != null &&
           sAccountingCurrency.equals (sAmountCurrency) &&
           !sAccountingCurrency.equals (sInvoiceCurrency);
  }

  /**
   * Trim a value of the source. All readers do this with every text they take over.
   *
   * @param sValue
   *        The value. May be {@code null}.
   * @return The trimmed value or {@code null} if nothing remains.
   */
  @Nullable
  protected static String str (@Nullable final String sValue)
  {
    if (sValue == null)
      return null;

    final String sTrimmed = sValue.trim ();
    return sTrimmed.isEmpty () ? null : sTrimmed;
  }

  /**
   * Parse a date string according to its UN/EDIFACT 2379 format code. A value that cannot be read
   * results in a warning, so the caller notices the data loss.
   *
   * @param sValue
   *        The date string. May be {@code null}.
   * @param sFormat
   *        The format code. A missing code means "102" (yyyyMMdd) per EN 16931.
   * @param aErrors
   *        The error list to fill with warnings. Never {@code null}.
   * @return The date or {@code null} if the value is empty or cannot be read.
   */
  @Nullable
  protected static LocalDate parseFormattedDate (@Nullable final String sValue,
                                                 @Nullable final String sFormat,
                                                 @NonNull final ErrorList aErrors)
  {
    final String sDate = str (sValue);
    if (sDate == null)
      return null;

    final String sFormatCode = str (sFormat);
    final EEN16931DateFormatCode eFormat = EEN16931DateFormatCode.getFromIDOrDefault (sFormatCode);
    if (eFormat == null)
    {
      aErrors.add (SingleError.builderWarn ()
                              .errorText ("Unsupported CII date format code '" +
                                          PhloxLogHelper.sanitise (sFormatCode) +
                                          "' on value '" +
                                          PhloxLogHelper.sanitise (sDate) +
                                          "'")
                              .build ());
      return null;
    }
    final LocalDate ret = eFormat.parseLocalDate (sDate);
    if (ret == null)
      aErrors.add (SingleError.builderWarn ()
                              .errorText ("Could not parse CII date '" +
                                          PhloxLogHelper.sanitise (sDate) +
                                          "' with format code '" +
                                          eFormat.getID () +
                                          "'")
                              .build ());
    return ret;
  }

  /**
   * Unmarshal a document. The events of the XSD validation go to the error list instead of the log.
   * The first error ends the reading, as with the default event handler.
   *
   * @param <T>
   *        The JAXB type.
   * @param aMarshaller
   *        The marshaller to use. It is changed by this method. Never {@code null}.
   * @param aDoc
   *        The parsed document. Never {@code null}.
   * @param aErrors
   *        The error list to fill. Never {@code null}.
   * @return {@code null} if the document does not match the schema.
   */
  @Nullable
  static <T> T unmarshal (@NonNull final GenericJAXBMarshaller <T> aMarshaller,
                          @NonNull final Document aDoc,
                          @NonNull final ErrorList aErrors)
  {
    aMarshaller.setCollectErrors (aErrors);
    // The events are in the error list already, the default callback logs them with their value
    aMarshaller.readExceptionCallbacks ().removeAll ();
    return aMarshaller.read (aDoc);
  }
}
