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
package com.helger.phlox.ubl;

import java.time.LocalDate;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.annotation.Nonempty;
import com.helger.datetime.xml.XMLOffsetDate;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.codelist.EN16931CodeLists;
import com.helger.jaxb.GenericJAXBMarshaller;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceNote;
import com.helger.xsds.ccts.cct.schemamodule.IdentifierType;
import com.helger.xsds.ccts.cct.schemamodule.TextType;

/**
 * Base class of the edition specific UBL readers. It only contains helpers that work on types that
 * are shared between UBL 2.1 and UBL 2.5 (plain strings, dates and the CCTS base types that all UBL
 * basic components extend). Everything that touches the version specific JAXB types of
 * {@code commonaggregatecomponents_21} / {@code _25} has to stay in the edition specific reader,
 * because the two sets of classes are unrelated.
 *
 * @author Philip Helger
 */
abstract class AbstractUblInvoiceReader
{
  /** The scheme identifier of the SEPA creditor identifier (BT-90) in a party identification. */
  protected static final String SEPA_SCHEME_ID = EN16931CodeLists.CREDITOR_REFERENCE_SCHEME_ID;

  private static final String ROOT_INVOICE = "Invoice";
  private static final String ROOT_CREDIT_NOTE = "CreditNote";

  /**
   * Get the UBL version this reader handles.
   *
   * @return The UBL version this reader handles, e.g. {@code 2.1}. Used for messages only.
   */
  @NonNull
  @Nonempty
  protected abstract String _getUblVersion ();

  /**
   * Read the already parsed document as UBL Invoice.
   *
   * @param aDoc
   *        The parsed document, whose root element is {@code Invoice}. Never {@code null}.
   * @param aErrors
   *        The error list to fill. Never {@code null}.
   * @return {@code null} if the document does not match the UBL Invoice schema.
   */
  @Nullable
  protected abstract PhloxInvoice readInvoiceDocument (@NonNull Document aDoc, @NonNull ErrorList aErrors);

  /**
   * Read the already parsed document as UBL CreditNote.
   *
   * @param aDoc
   *        The parsed document, whose root element is {@code CreditNote}. Never {@code null}.
   * @param aErrors
   *        The error list to fill. Never {@code null}.
   * @return {@code null} if the document does not match the UBL CreditNote schema.
   */
  @Nullable
  protected abstract PhloxInvoice readCreditNoteDocument (@NonNull Document aDoc, @NonNull ErrorList aErrors);

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

    final String sTrim = sValue.trim ();
    return sTrim.isEmpty () ? null : sTrim;
  }

  /**
   * Get a value that UBL requires even if the business term is absent: the binding prescribes the
   * filler {@link EN16931CodeLists#MISSING_VALUE_PLACEHOLDER} for BT-13, BT-132, BT-190, BT-192,
   * BT-199 and BT-201.
   *
   * @param sValue
   *        The value of the element. May be {@code null}.
   * @return The trimmed value or {@code null} if it is empty or the filler.
   */
  @Nullable
  protected static String strWithoutFiller (@Nullable final String sValue)
  {
    final String ret = str (sValue);
    return EN16931CodeLists.MISSING_VALUE_PLACEHOLDER.equals (ret) ? null : ret;
  }

  @Nullable
  protected static String strFromCode (@Nullable final String sValue)
  {
    return str (sValue);
  }

  @Nullable
  protected static LocalDate d (@Nullable final XMLOffsetDate aDate)
  {
    return aDate == null ? null : aDate.toLocalDate ();
  }

  @Nullable
  protected static String firstText (@NonNull final List <? extends TextType> aTexts)
  {
    return aTexts.isEmpty () ? null : str (aTexts.get (0).getValue ());
  }

  @NonNull
  protected static String joinNotes (@NonNull final List <? extends TextType> aNotes)
  {
    final StringBuilder aSb = new StringBuilder ();
    for (final TextType aN : aNotes)
    {
      final String sNote = str (aN.getValue ());
      if (sNote == null)
        continue;
      if (aSb.length () > 0)
        aSb.append ('\n');
      aSb.append (sNote);
    }
    return aSb.toString ();
  }

  /**
   * Convert any UBL identifier element (ID, CompanyID, EndpointID, ...) into a model identifier
   * without touching the value.
   *
   * @param aID
   *        The identifier element. May be {@code null}.
   * @return {@code null} if the element is {@code null}, otherwise the value together with its
   *         scheme ID.
   */
  @Nullable
  protected static PhloxIdentifier identifier (@Nullable final IdentifierType aID)
  {
    return aID == null ? null : new PhloxIdentifier (str (aID.getValue ()), str (aID.getSchemeID ()));
  }

  /**
   * Add an identifier to a list, if it has a value.
   *
   * @param aID
   *        The identifier element. May be {@code null}.
   * @param aTarget
   *        The list to add to. Never {@code null}.
   */
  protected static void addIdentifier (@Nullable final IdentifierType aID,
                                       @NonNull final List <PhloxIdentifier> aTarget)
  {
    final PhloxIdentifier aOut = identifier (aID);
    if (aOut != null && aOut.getValue () != null)
      aTarget.add (aOut);
  }

  /**
   * Check if a party identification is the bank assigned creditor identifier BT-90 and not a party
   * identifier.
   *
   * @param aID
   *        The identifier element. May be {@code null}.
   * @return {@code true} if it has the scheme of BT-90-1.
   */
  protected static boolean isSepaCreditorID (@Nullable final IdentifierType aID)
  {
    return aID != null && SEPA_SCHEME_ID.equals (str (aID.getSchemeID ()));
  }

  /**
   * Parse a {@code cbc:Note}. The convention is a subject code embedded as "#code#" prefix, e.g.
   * "#AAB#Some note".
   *
   * @param sRaw
   *        The value of the note. May be {@code null}.
   * @return The note or {@code null} if it is empty.
   */
  @Nullable
  protected static PhloxInvoiceNote parseInvoiceNote (@Nullable final String sRaw)
  {
    final String sNote = str (sRaw);
    if (sNote == null)
      return null;

    if (sNote.startsWith ("#"))
    {
      final int nEnd = sNote.indexOf ('#', 1);
      if (nEnd > 1)
        return new PhloxInvoiceNote (sNote.substring (1, nEnd), sNote.substring (nEnd + 1));
    }
    return new PhloxInvoiceNote (null, sNote);
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
  protected static <T> T unmarshal (@NonNull final GenericJAXBMarshaller <T> aMarshaller,
                                    @NonNull final Document aDoc,
                                    @NonNull final ErrorList aErrors)
  {
    aMarshaller.setCollectErrors (aErrors);
    // The events are in the error list already, the default callback logs them with their value
    aMarshaller.readExceptionCallbacks ().removeAll ();
    return aMarshaller.read (aDoc);
  }

  /**
   * Read an already parsed UBL document. The root element decides whether the document is read as
   * Invoice or as CreditNote, so the document is neither parsed again nor tried against both
   * schemas. The document kind of the result comes from BT-3; the root element only decides it if
   * BT-3 is missing or unknown.
   *
   * @param aDoc
   *        The parsed document. Never {@code null}.
   * @param aErrors
   *        The error list to fill. Never {@code null}.
   * @return The populated model or {@code null} if the document is neither a valid Invoice nor a
   *         valid CreditNote of this reader's UBL version.
   */
  @Nullable
  final PhloxInvoice read (@NonNull final Document aDoc, @NonNull final ErrorList aErrors)
  {
    final String sRootName = aDoc.getDocumentElement ().getLocalName ();
    final PhloxInvoice ret;
    if (ROOT_INVOICE.equals (sRootName))
      ret = readInvoiceDocument (aDoc, aErrors);
    else
      if (ROOT_CREDIT_NOTE.equals (sRootName))
        ret = readCreditNoteDocument (aDoc, aErrors);
      else
        ret = null;

    if (ret == null)
      aErrors.add (SingleError.builderError ()
                              .errorText ("Source could not be parsed as UBL " +
                                          _getUblVersion () +
                                          " Invoice or CreditNote")
                              .build ());
    return ret;
  }
}
