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

import java.io.InputStream;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.read.IInvoiceReader;
import com.helger.xml.serialize.read.DOMReader;

import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

/**
 * Reads UBL Invoice and CreditNote documents into a {@link PhloxInvoice}. The EN 16931 edition
 * determines the UBL version:
 * <ul>
 * <li>{@link EEN16931Edition#EN2017} - UBL 2.1, see {@link #fromUbl21()}</li>
 * <li>{@link EEN16931Edition#EN2026} - UBL 2.5, see {@link #fromUbl25()}</li>
 * </ul>
 * {@link #autoDetect()} (also the behaviour of the public constructor) determines the edition per
 * document from BT-24 (Specification identifier), because the edition cannot be told from the XML
 * namespaces.
 * <p>
 * The reader sets no limit on the size of its input. An application that reads invoices from
 * untrusted sources must cap the size of the XML before it calls the reader: the time and the memory
 * of reading and rendering grow with it.
 *
 * @author Philip Helger
 */
public class UblInvoiceReader implements IInvoiceReader <InputStream>
{
  private final EEN16931Edition m_eEdition;

  /**
   * Create a new reader.
   *
   * @param eEdition
   *        The fixed edition to read or {@code null} to determine it from BT-24 per document.
   */
  protected UblInvoiceReader (@Nullable final EEN16931Edition eEdition)
  {
    m_eEdition = eEdition;
  }

  /** Create a reader that determines the edition of each document from its BT-24. */
  public UblInvoiceReader ()
  {
    this (null);
  }

  /**
   * Get the reader for UBL 2.1 (EN 16931:2017 binding).
   *
   * @return A reader for UBL 2.1 (EN 16931:2017 binding). BT-24 is not inspected.
   */
  @NonNull
  public static UblInvoiceReader fromUbl21 ()
  {
    return new UblInvoiceReader (EEN16931Edition.EN2017);
  }

  /**
   * Get the reader for UBL 2.5 (EN 16931:2026 binding).
   *
   * @return A reader for UBL 2.5 (EN 16931:2026 binding). BT-24 is not inspected.
   */
  @NonNull
  public static UblInvoiceReader fromUbl25 ()
  {
    return new UblInvoiceReader (EEN16931Edition.EN2026);
  }

  /**
   * Get the reader that determines the EN 16931 edition of each document from BT-24 and reads it.
   *
   * @return A reader that determines the EN 16931 edition of each document from BT-24 and reads it
   *         with the matching UBL version. Documents without a BT-24 that identifies a known
   *         edition are rejected with an error.
   */
  @NonNull
  public static UblInvoiceReader autoDetect ()
  {
    return new UblInvoiceReader (null);
  }

  /**
   * Get the fixed EN 16931 edition of this reader or {@code null} if the edition is determined.
   *
   * @return The fixed EN 16931 edition of this reader or {@code null} if the edition is determined
   *         per document.
   */
  @Nullable
  public final EEN16931Edition getEdition ()
  {
    return m_eEdition;
  }

  @Nullable
  public PhloxInvoice read (@NonNull final InputStream aSource, @NonNull final ErrorList aErrors)
  {
    // Parse exactly once - the Document is used to detect the edition as well as to fill the JAXB
    // object
    final Document aDoc = DOMReader.readXMLDOM (aSource);
    if (aDoc == null)
    {
      aErrors.add (SingleError.builderError ().errorText ("Source is not well-formed XML").build ());
      return null;
    }

    EEN16931Edition eEdition = m_eEdition;
    if (eEdition == null)
    {
      eEdition = EEN16931Edition.detect (aDoc);
      if (eEdition == null)
      {
        aErrors.add (SingleError.builderError ()
                                .errorText ("The EN 16931 edition could not be determined, because BT-24 (Specification identifier) is missing or does not start with '" +
                                            EEN16931Edition.EN2017.getSpecificationIdentifier () +
                                            "' or '" +
                                            EEN16931Edition.EN2026.getSpecificationIdentifier () +
                                            "'. Use fromUbl21() or fromUbl25() to select the UBL version explicitly.")
                                .build ());
        return null;
      }
    }

    final AbstractUblInvoiceReader aReader = eEdition == EEN16931Edition.EN2026 ? new Ubl25InvoiceReader ()
                                                                                : new Ubl21InvoiceReader ();
    return aReader.read (aDoc, aErrors);
  }

  /**
   * Read an already-parsed UBL 2.1 Invoice (EN 16931:2017) into a {@link PhloxInvoice}, collecting
   * non-fatal issues (unknown type codes, ignored values) into the supplied error list.
   *
   * @param aInvoice
   *        The JAXB invoice. Never {@code null}.
   * @param aErrors
   *        The error list to fill with warnings. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readInvoice (@NonNull final InvoiceType aInvoice, @NonNull final ErrorList aErrors)
  {
    return new Ubl21InvoiceReader ().readInvoice (aInvoice, aErrors);
  }

  /**
   * Read an already-parsed UBL 2.1 Invoice (EN 16931:2017) into a {@link PhloxInvoice}. Convenience
   * overload that discards any collected warnings.
   *
   * @param aInvoice
   *        The JAXB invoice. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readInvoice (@NonNull final InvoiceType aInvoice)
  {
    return readInvoice (aInvoice, new ErrorList ());
  }

  /**
   * Read an already-parsed UBL 2.1 CreditNote (EN 16931:2017) into a {@link PhloxInvoice},
   * collecting non-fatal issues (unknown type codes, ignored values) into the supplied error list.
   *
   * @param aCreditNote
   *        The JAXB credit note. Never {@code null}.
   * @param aErrors
   *        The error list to fill with warnings. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readCreditNote (@NonNull final CreditNoteType aCreditNote, @NonNull final ErrorList aErrors)
  {
    return new Ubl21InvoiceReader ().readCreditNote (aCreditNote, aErrors);
  }

  /**
   * Read an already-parsed UBL 2.1 CreditNote (EN 16931:2017) into a {@link PhloxInvoice}.
   * Convenience overload that discards any collected warnings.
   *
   * @param aCreditNote
   *        The JAXB credit note. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readCreditNote (@NonNull final CreditNoteType aCreditNote)
  {
    return readCreditNote (aCreditNote, new ErrorList ());
  }

  // The UBL 2.1 and UBL 2.5 JAXB types share their simple names, so the UBL 2.5 ones are qualified
  /**
   * Read an already-parsed UBL 2.5 Invoice (EN 16931:2026) into a {@link PhloxInvoice}.
   *
   * @param aInvoice
   *        The JAXB invoice. Never {@code null}.
   * @param aErrors
   *        The error list to fill with warnings. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readInvoice25 (final oasis.names.specification.ubl.schema.xsd.invoice_25.@NonNull InvoiceType aInvoice,
                                     @NonNull final ErrorList aErrors)
  {
    return new Ubl25InvoiceReader ().readInvoice (aInvoice, aErrors);
  }

  /**
   * Read an already-parsed UBL 2.5 CreditNote (EN 16931:2026) into a {@link PhloxInvoice}.
   *
   * @param aCreditNote
   *        The JAXB credit note. Never {@code null}.
   * @param aErrors
   *        The error list to fill with warnings. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readCreditNote25 (final oasis.names.specification.ubl.schema.xsd.creditnote_25.@NonNull CreditNoteType aCreditNote,
                                        @NonNull final ErrorList aErrors)
  {
    return new Ubl25InvoiceReader ().readCreditNote (aCreditNote, aErrors);
  }
}
