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
package com.helger.phlox.template.adapter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.string.StringHelper;
import com.helger.phlox.api.model.PhloxCreditTransfer;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxPaymentInstructions;

/**
 * Builds the text of an EPC QR code ("GiroCode") as defined by EPC069-12 v3.1, version 002 with the
 * character set UTF-8. This is the data of a SEPA credit transfer that a banking app reads from an
 * invoice.
 * <p>
 * This is no validation. If the invoice data does not fit into the format there simply is no
 * payload.
 *
 * @author Philip Helger
 */
public final class PhloxEpcPayload
{
  // UNCL 4461: credit transfer and SEPA credit transfer
  private static final Set <String> CREDIT_TRANSFER_CODES = Set.of ("30", "58");
  private static final String EUR = "EUR";
  private static final BigDecimal MIN_AMOUNT = new BigDecimal ("0.01");
  private static final BigDecimal MAX_AMOUNT = new BigDecimal ("999999999.99");

  private static final Pattern IBAN = Pattern.compile ("[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}");
  private static final Pattern BIC = Pattern.compile ("[A-Z0-9]{8}(?:[A-Z0-9]{3})?");
  private static final Pattern RF_REFERENCE = Pattern.compile ("RF[0-9]{2}[A-Z0-9]{1,21}");
  private static final Pattern WHITESPACE = Pattern.compile ("\\s+");

  private static final int MAX_NAME_LENGTH = 70;
  private static final int MAX_UNSTRUCTURED_LENGTH = 140;
  private static final int MAX_PAYLOAD_BYTES = 331;

  private PhloxEpcPayload ()
  {}

  /** @return <code>true</code> for what the regular expression class <code>\s</code> matches. */
  private static boolean _isWhitespace (final char c)
  {
    return c == ' ' || c == '\t' || c == '\n' || c == '\u000B' || c == '\f' || c == '\r';
  }

  /**
   * @return The text on one line, because the elements of the payload are separated by lines. Each
   *         run of white space with a line break becomes one space. This is a scan and not a regular
   *         expression, whose time would grow quadratically with a long run of spaces.
   */
  @NonNull
  private static String _getSingleLine (@Nullable final String sText)
  {
    if (sText == null)
      return "";
    final String sStripped = sText.strip ();
    final int nLen = sStripped.length ();
    final StringBuilder aSB = new StringBuilder (nLen);
    int i = 0;
    while (i < nLen)
    {
      final int nRunStart = i;
      boolean bLineBreak = false;
      while (i < nLen && _isWhitespace (sStripped.charAt (i)))
      {
        bLineBreak |= sStripped.charAt (i) == '\r' || sStripped.charAt (i) == '\n';
        ++i;
      }
      if (bLineBreak)
        aSB.append (' ');
      else
        aSB.append (sStripped, nRunStart, i);
      if (i < nLen)
        aSB.append (sStripped.charAt (i++));
    }
    return aSB.toString ();
  }

  @NonNull
  private static String _getCompact (@Nullable final String sText)
  {
    return sText == null ? "" : WHITESPACE.matcher (sText).replaceAll ("").toUpperCase (Locale.ROOT);
  }

  /**
   * @return The first account of a credit transfer. The other accounts are not considered, even if
   *         this one turns out to be unusable.
   */
  @Nullable
  private static PhloxCreditTransfer _getAccount (@NonNull final PhloxPaymentInstructions aPayment)
  {
    for (final PhloxCreditTransfer aAccount : aPayment.creditTransfers ())
      if (aAccount.getPaymentMeansCode () != null && CREDIT_TRANSFER_CODES.contains (aAccount.getPaymentMeansCode ()))
        return aAccount;
    return null;
  }

  /** @return The amount like <code>EUR12.30</code> or <code>null</code> if it is not possible. */
  @Nullable
  private static String _getAmount (@NonNull final PhloxInvoice aInvoice)
  {
    final BigDecimal aAmount = aInvoice.getDocumentTotals ().getPayableAmount ();
    if (aAmount == null || aAmount.compareTo (MIN_AMOUNT) < 0 || aAmount.compareTo (MAX_AMOUNT) > 0)
      return null;
    // Rounding would change the amount that is asked for
    if (aAmount.stripTrailingZeros ().scale () > 2)
      return null;
    return EUR + aAmount.setScale (2).toPlainString ();
  }

  /** @return The name of the beneficiary, which is an empty string if there is none. */
  @NonNull
  private static String _getName (@NonNull final PhloxInvoice aInvoice, @NonNull final PhloxCreditTransfer aAccount)
  {
    String ret = _getSingleLine (aAccount.getAccountName ());
    if (ret.isEmpty () && aInvoice.getPayee () != null)
      ret = _getSingleLine (aInvoice.getPayee ().getName ());
    if (ret.isEmpty ())
      ret = _getSingleLine (aInvoice.getSeller ().getName ());
    return ret;
  }

  /**
   * Get the text for the EPC QR code of an invoice.
   *
   * @param aInvoice
   *        The invoice. Never {@code null}.
   * @return The payload, or {@code null} if the invoice cannot be paid with an EPC QR code: it is a
   *         credit note, the currency is not EUR, the amount is out of range, there is no credit
   *         transfer (code 30 or 58), its account is no IBAN, or the data does not fit.
   */
  @Nullable
  public static String getPayload (@NonNull final PhloxInvoice aInvoice)
  {
    final PhloxPaymentInstructions aPayment = aInvoice.getPaymentInstructions ();
    if (aPayment == null || aInvoice.getKind ().isCreditNote () || !EUR.equals (aInvoice.getCurrency ()))
      return null;

    final PhloxCreditTransfer aAccount = _getAccount (aPayment);
    if (aAccount == null)
      return null;
    final String sIBAN = _getCompact (aAccount.getAccountIdentifier ());
    if (!IBAN.matcher (sIBAN).matches ())
      return null;
    final String sAmount = _getAmount (aInvoice);
    if (sAmount == null)
      return null;
    final String sName = _getName (aInvoice, aAccount);
    if (sName.isEmpty () || sName.length () > MAX_NAME_LENGTH)
      return null;

    // The BIC is optional, so one that does not fit is left out
    final String sBIC = _getCompact (aAccount.getServiceProviderID ());

    // Only one of the two remittance elements may be used
    String sStructured = "";
    String sUnstructured = _getSingleLine (aPayment.getRemittanceInformation ());
    if (RF_REFERENCE.matcher (_getCompact (sUnstructured)).matches ())
    {
      sStructured = _getCompact (sUnstructured);
      sUnstructured = "";
    }
    else
      sUnstructured = StringHelper.getCutAfterLength (sUnstructured, MAX_UNSTRUCTURED_LENGTH);

    final StringBuilder aSB = new StringBuilder ();
    aSB.append ("BCD\n002\n1\nSCT\n");
    aSB.append (BIC.matcher (sBIC).matches () ? sBIC : "").append ('\n');
    aSB.append (sName).append ('\n');
    aSB.append (sIBAN).append ('\n');
    aSB.append (sAmount);
    // The last populated element is not followed by a separator. The purpose stays empty.
    if (!sStructured.isEmpty () || !sUnstructured.isEmpty ())
      aSB.append ("\n\n").append (sStructured);
    if (!sUnstructured.isEmpty ())
      aSB.append ('\n').append (sUnstructured);

    final String ret = aSB.toString ();
    return ret.getBytes (StandardCharsets.UTF_8).length > MAX_PAYLOAD_BYTES ? null : ret;
  }
}
