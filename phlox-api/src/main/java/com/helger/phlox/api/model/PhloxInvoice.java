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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetTime;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.en16931.basics.EEN16931Edition;

/**
 * Root of the EN 16931 intermediate model. One instance represents one Invoice or one Credit Note,
 * populated by a source reader and consumed by an output writer. All optional fields are
 * {@code null} when not present in the source; all collections are non-{@code null} and empty when
 * not populated.
 * <p>
 * Header-level BTs sit directly on this class (BT-1 through BT-19, BT-166, BT-167, BT-197). Each
 * business group (BG-1 through BG-25 and BG-33) is exposed as either a single nested object or a mutable list of nested objects.
 *
 * @author Philip Helger
 */
public class PhloxInvoice
{
  private EPhloxDocumentKind m_eKind = EPhloxDocumentKind.INVOICE;

  private String m_sNumber;
  private LocalDate m_aIssueDate;
  private String m_sTypeCode;
  private String m_sCurrency;
  private String m_sVatAccountingCurrency;
  private LocalDate m_aVatPointDate;
  private String m_sVatPointDateCode;
  private LocalDate m_aPaymentDueDate;
  private String m_sProjectReference;
  private String m_sProjectName;
  private String m_sContractReference;
  private String m_sPurchaseOrderReference;
  private String m_sSalesOrderReference;
  private String m_sReceivingAdviceReference;
  private String m_sDespatchAdviceReference;
  private String m_sTenderOrLotReference;
  private PhloxIdentifier m_aInvoicedObjectIdentifier;
  private String m_sBuyerAccountingReference;

  private final ICommonsList <PhloxInvoiceNote> m_aInvoiceNotes = new CommonsArrayList <> ();
  private PhloxProcessControl m_aProcessControl = new PhloxProcessControl ();
  private final ICommonsList <PhloxPrecedingInvoiceRef> m_aPrecedingInvoiceRefs = new CommonsArrayList <> ();
  private PhloxSeller m_aSeller = new PhloxSeller ();
  private PhloxBuyer m_aBuyer = new PhloxBuyer ();
  private PhloxPayee m_aPayee;
  private PhloxSellerTaxRepresentative m_aSellerTaxRepresentative;
  private PhloxDeliveryInformation m_aDeliveryInformation;
  private PhloxInvoicingPeriod m_aInvoicingPeriod;
  private PhloxPaymentInstructions m_aPaymentInstructions;
  private final ICommonsList <PhloxDocumentAllowance> m_aDocumentAllowances = new CommonsArrayList <> ();
  private final ICommonsList <PhloxDocumentCharge> m_aDocumentCharges = new CommonsArrayList <> ();
  private PhloxDocumentTotals m_aDocumentTotals = new PhloxDocumentTotals ();
  private final ICommonsList <PhloxVatBreakdown> m_aVatBreakdowns = new CommonsArrayList <> ();
  private final ICommonsList <PhloxSupportingDocument> m_aSupportingDocuments = new CommonsArrayList <> ();
  private final ICommonsList <PhloxInvoiceLine> m_aLines = new CommonsArrayList <> ();
  private EEN16931Edition m_eEdition;
  private OffsetTime m_aIssueTime;
  private BigDecimal m_aVatExchangeRate;
  private String m_sVatExchangeRateTargetCurrency;
  private String m_sVatExchangeRateSourceCurrency;
  private String m_sDeliveryNoteReference;
  private final ICommonsList <PhloxIdentifier> m_aBuyerReferences = new CommonsArrayList <> ();
  private final ICommonsList <PhloxPaymentTerms> m_aPaymentTerms = new CommonsArrayList <> ();
  private final ICommonsList <PhloxAdditionalTerm> m_aAdditionalTerms = new CommonsArrayList <> ();

  /** Create an empty invoice (defaults to {@link EPhloxDocumentKind#INVOICE}). */
  public PhloxInvoice ()
  {}

  /**
   * Get the document kind (Invoice or Credit Note), derived from BT-3. Never {@code null}.
   *
   * @return The document kind (Invoice or Credit Note), derived from BT-3. Never {@code null}.
   */
  @NonNull
  public EPhloxDocumentKind getKind ()
  {
    return m_eKind;
  }

  /**
   * Set the document kind.
   *
   * @param eKind
   *        Kind; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setKind (@NonNull final EPhloxDocumentKind eKind)
  {
    m_eKind = eKind;
    return this;
  }

  /**
   * Get the {@code true} when {@link #getKind()} is {@link EPhloxDocumentKind#INVOICE}.
   *
   * @return {@code true} when {@link #getKind()} is {@link EPhloxDocumentKind#INVOICE}.
   */
  public boolean isInvoice ()
  {
    return m_eKind.isInvoice ();
  }

  /**
   * Get the {@code true} when {@link #getKind()} is {@link EPhloxDocumentKind#CREDIT_NOTE}.
   *
   * @return {@code true} when {@link #getKind()} is {@link EPhloxDocumentKind#CREDIT_NOTE}.
   */
  public boolean isCreditNote ()
  {
    return m_eKind.isCreditNote ();
  }

  /**
   * Get the invoice number (BT-1).
   *
   * @return Invoice number (BT-1). May be {@code null}.
   */
  @Nullable
  public String getNumber ()
  {
    return m_sNumber;
  }

  /**
   * Set the invoice number (BT-1).
   *
   * @param sNumber
   *        Invoice number; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setNumber (@Nullable final String sNumber)
  {
    m_sNumber = sNumber;
    return this;
  }

  /**
   * Get the invoice issue date (BT-2).
   *
   * @return Invoice issue date (BT-2). May be {@code null}.
   */
  @Nullable
  public LocalDate getIssueDate ()
  {
    return m_aIssueDate;
  }

  /**
   * Set the invoice issue date (BT-2).
   *
   * @param aIssueDate
   *        Issue date; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setIssueDate (@Nullable final LocalDate aIssueDate)
  {
    m_aIssueDate = aIssueDate;
    return this;
  }

  /**
   * Get the invoice type code (BT-3) — UNTDID 1001. May be {@code null}.
   *
   * @return Invoice type code (BT-3) — UNTDID 1001. May be {@code null}.
   */
  @Nullable
  public String getTypeCode ()
  {
    return m_sTypeCode;
  }

  /**
   * Set the invoice type code (BT-3).
   *
   * @param sTypeCode
   *        UNTDID 1001 code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setTypeCode (@Nullable final String sTypeCode)
  {
    m_sTypeCode = sTypeCode;
    return this;
  }

  /**
   * Get the invoice currency code (BT-5) — ISO 4217. May be {@code null}.
   *
   * @return Invoice currency code (BT-5) — ISO 4217. May be {@code null}.
   */
  @Nullable
  public String getCurrency ()
  {
    return m_sCurrency;
  }

  /**
   * Set the invoice currency code (BT-5).
   *
   * @param sCurrency
   *        ISO 4217 currency code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setCurrency (@Nullable final String sCurrency)
  {
    m_sCurrency = sCurrency;
    return this;
  }

  /**
   * Get the VAT accounting currency code (BT-6) — ISO 4217. May be {@code null}.
   *
   * @return VAT accounting currency code (BT-6) — ISO 4217. May be {@code null}.
   */
  @Nullable
  public String getVatAccountingCurrency ()
  {
    return m_sVatAccountingCurrency;
  }

  /**
   * Set the VAT accounting currency code (BT-6).
   *
   * @param sVatAccountingCurrency
   *        ISO 4217 currency code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setVatAccountingCurrency (@Nullable final String sVatAccountingCurrency)
  {
    m_sVatAccountingCurrency = sVatAccountingCurrency;
    return this;
  }

  /**
   * Get the value added tax point date (BT-7).
   *
   * @return Value added tax point date (BT-7). May be {@code null}.
   */
  @Nullable
  public LocalDate getVatPointDate ()
  {
    return m_aVatPointDate;
  }

  /**
   * Set the value added tax point date (BT-7).
   *
   * @param aVatPointDate
   *        VAT point date; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setVatPointDate (@Nullable final LocalDate aVatPointDate)
  {
    m_aVatPointDate = aVatPointDate;
    return this;
  }

  /**
   * Get the value added tax point date code (BT-8). The readers deliver the code of UNTDID 2005 that
   * the UBL binding uses; the CII readers map the UNTDID 2475 code of their binding to it.
   *
   * @return Value added tax point date code (BT-8). May be {@code null}.
   */
  @Nullable
  public String getVatPointDateCode ()
  {
    return m_sVatPointDateCode;
  }

  /**
   * Set the value added tax point date code (BT-8).
   *
   * @param sVatPointDateCode
   *        Code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setVatPointDateCode (@Nullable final String sVatPointDateCode)
  {
    m_sVatPointDateCode = sVatPointDateCode;
    return this;
  }

  /**
   * Get the payment due date (BT-9).
   *
   * @return Payment due date (BT-9). May be {@code null}.
   */
  @Nullable
  public LocalDate getPaymentDueDate ()
  {
    return m_aPaymentDueDate;
  }

  /**
   * Set the payment due date (BT-9).
   *
   * @param aPaymentDueDate
   *        Due date; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setPaymentDueDate (@Nullable final LocalDate aPaymentDueDate)
  {
    m_aPaymentDueDate = aPaymentDueDate;
    return this;
  }

  /**
   * Get the project reference (BT-11).
   *
   * @return Project reference (BT-11). May be {@code null}.
   */
  @Nullable
  public String getProjectReference ()
  {
    return m_sProjectReference;
  }

  /**
   * Set the project reference (BT-11).
   *
   * @param sProjectReference
   *        Project reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setProjectReference (@Nullable final String sProjectReference)
  {
    m_sProjectReference = sProjectReference;
    return this;
  }

  /**
   * Get the project name (BT-11-1). Only CII has it. It is {@code null} if it just repeats the project
   * reference (BT-11), as the binding asks for.
   *
   * @return Project name (BT-11-1). May be {@code null}.
   */
  @Nullable
  public String getProjectName ()
  {
    return m_sProjectName;
  }

  /**
   * Set the project name (BT-11-1).
   *
   * @param sProjectName
   *        The name; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setProjectName (@Nullable final String sProjectName)
  {
    m_sProjectName = sProjectName;
    return this;
  }

  /**
   * Get the contract reference (BT-12).
   *
   * @return Contract reference (BT-12). May be {@code null}.
   */
  @Nullable
  public String getContractReference ()
  {
    return m_sContractReference;
  }

  /**
   * Set the contract reference (BT-12).
   *
   * @param sContractReference
   *        Contract reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setContractReference (@Nullable final String sContractReference)
  {
    m_sContractReference = sContractReference;
    return this;
  }

  /**
   * Get the purchase order reference (BT-13).
   *
   * @return Purchase order reference (BT-13). May be {@code null}.
   */
  @Nullable
  public String getPurchaseOrderReference ()
  {
    return m_sPurchaseOrderReference;
  }

  /**
   * Set the purchase order reference (BT-13).
   *
   * @param sPurchaseOrderReference
   *        PO reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setPurchaseOrderReference (@Nullable final String sPurchaseOrderReference)
  {
    m_sPurchaseOrderReference = sPurchaseOrderReference;
    return this;
  }

  /**
   * Get the sales order reference (BT-14).
   *
   * @return Sales order reference (BT-14). May be {@code null}.
   */
  @Nullable
  public String getSalesOrderReference ()
  {
    return m_sSalesOrderReference;
  }

  /**
   * Set the sales order reference (BT-14).
   *
   * @param sSalesOrderReference
   *        Sales order reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setSalesOrderReference (@Nullable final String sSalesOrderReference)
  {
    m_sSalesOrderReference = sSalesOrderReference;
    return this;
  }

  /**
   * Get the receiving advice reference (BT-15).
   *
   * @return Receiving advice reference (BT-15). May be {@code null}.
   */
  @Nullable
  public String getReceivingAdviceReference ()
  {
    return m_sReceivingAdviceReference;
  }

  /**
   * Set the receiving advice reference (BT-15).
   *
   * @param sReceivingAdviceReference
   *        Receiving advice reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setReceivingAdviceReference (@Nullable final String sReceivingAdviceReference)
  {
    m_sReceivingAdviceReference = sReceivingAdviceReference;
    return this;
  }

  /**
   * Get the despatch advice reference (BT-16).
   *
   * @return Despatch advice reference (BT-16). May be {@code null}.
   */
  @Nullable
  public String getDespatchAdviceReference ()
  {
    return m_sDespatchAdviceReference;
  }

  /**
   * Set the despatch advice reference (BT-16).
   *
   * @param sDespatchAdviceReference
   *        Despatch advice reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setDespatchAdviceReference (@Nullable final String sDespatchAdviceReference)
  {
    m_sDespatchAdviceReference = sDespatchAdviceReference;
    return this;
  }

  /**
   * Get the tender or lot reference (BT-17).
   *
   * @return Tender or lot reference (BT-17). May be {@code null}.
   */
  @Nullable
  public String getTenderOrLotReference ()
  {
    return m_sTenderOrLotReference;
  }

  /**
   * Set the tender or lot reference (BT-17).
   *
   * @param sTenderOrLotReference
   *        Tender / lot reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setTenderOrLotReference (@Nullable final String sTenderOrLotReference)
  {
    m_sTenderOrLotReference = sTenderOrLotReference;
    return this;
  }

  /**
   * Get the invoiced object identifier (BT-18 with BT-18-1 scheme ID).
   *
   * @return Invoiced object identifier (BT-18 with BT-18-1 scheme ID). May be {@code null}.
   */
  @Nullable
  public PhloxIdentifier getInvoicedObjectIdentifier ()
  {
    return m_aInvoicedObjectIdentifier;
  }

  /**
   * Set the invoiced object identifier (BT-18 / BT-18-1).
   *
   * @param aInvoicedObjectIdentifier
   *        Identifier; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setInvoicedObjectIdentifier (@Nullable final PhloxIdentifier aInvoicedObjectIdentifier)
  {
    m_aInvoicedObjectIdentifier = aInvoicedObjectIdentifier;
    return this;
  }

  /**
   * Get the buyer accounting reference (BT-19).
   *
   * @return Buyer accounting reference (BT-19). May be {@code null}.
   */
  @Nullable
  public String getBuyerAccountingReference ()
  {
    return m_sBuyerAccountingReference;
  }

  /**
   * Set the buyer accounting reference (BT-19).
   *
   * @param sBuyerAccountingReference
   *        Accounting cost reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setBuyerAccountingReference (@Nullable final String sBuyerAccountingReference)
  {
    m_sBuyerAccountingReference = sBuyerAccountingReference;
    return this;
  }

  /**
   * Get the mutable list of the values of the document that the model has no place for.
   *
   * @return Mutable list of the additional terms. Never {@code null}; empty when there are none.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxAdditionalTerm> additionalTerms ()
  {
    return m_aAdditionalTerms;
  }

  /**
   * Get the mutable list of invoice notes (BG-1).
   *
   * @return Mutable list of invoice notes (BG-1). Never {@code null}; empty when none are present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxInvoiceNote> invoiceNotes ()
  {
    return m_aInvoiceNotes;
  }

  /**
   * Get the process control (BG-2).
   *
   * @return Process control (BG-2). Never {@code null} (initialised to an empty record).
   */
  @NonNull
  public PhloxProcessControl getProcessControl ()
  {
    return m_aProcessControl;
  }

  /**
   * Set the process control record (BG-2).
   *
   * @param aProcessControl
   *        Process control; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setProcessControl (@NonNull final PhloxProcessControl aProcessControl)
  {
    m_aProcessControl = aProcessControl;
    return this;
  }

  /**
   * Get the mutable list of preceding invoice references (BG-3).
   *
   * @return Mutable list of preceding invoice references (BG-3). Never {@code null}; empty when
   *         none are present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxPrecedingInvoiceRef> precedingInvoiceRefs ()
  {
    return m_aPrecedingInvoiceRefs;
  }

  /**
   * Get the seller (BG-4 with nested BG-5 and BG-6).
   *
   * @return Seller (BG-4 with nested BG-5 and BG-6). Never {@code null} (initialised).
   */
  @NonNull
  public PhloxSeller getSeller ()
  {
    return m_aSeller;
  }

  /**
   * Set the seller (BG-4).
   *
   * @param aSeller
   *        Seller; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setSeller (@NonNull final PhloxSeller aSeller)
  {
    m_aSeller = aSeller;
    return this;
  }

  /**
   * Get the buyer (BG-7 with nested BG-8 and BG-9).
   *
   * @return Buyer (BG-7 with nested BG-8 and BG-9). Never {@code null} (initialised).
   */
  @NonNull
  public PhloxBuyer getBuyer ()
  {
    return m_aBuyer;
  }

  /**
   * Set the buyer (BG-7).
   *
   * @param aBuyer
   *        Buyer; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setBuyer (@NonNull final PhloxBuyer aBuyer)
  {
    m_aBuyer = aBuyer;
    return this;
  }

  /**
   * Get the payee (BG-10).
   *
   * @return Payee (BG-10). May be {@code null}.
   */
  @Nullable
  public PhloxPayee getPayee ()
  {
    return m_aPayee;
  }

  /**
   * Set the payee (BG-10).
   *
   * @param aPayee
   *        Payee; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setPayee (@Nullable final PhloxPayee aPayee)
  {
    m_aPayee = aPayee;
    return this;
  }

  /**
   * Get the seller tax representative (BG-11 with nested BG-12).
   *
   * @return Seller tax representative (BG-11 with nested BG-12). May be {@code null}.
   */
  @Nullable
  public PhloxSellerTaxRepresentative getSellerTaxRepresentative ()
  {
    return m_aSellerTaxRepresentative;
  }

  /**
   * Set the seller tax representative (BG-11).
   *
   * @param aSellerTaxRepresentative
   *        Tax representative; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setSellerTaxRepresentative (@Nullable final PhloxSellerTaxRepresentative aSellerTaxRepresentative)
  {
    m_aSellerTaxRepresentative = aSellerTaxRepresentative;
    return this;
  }

  /**
   * Get the delivery information (BG-13 with nested BG-15).
   *
   * @return Delivery information (BG-13 with nested BG-15). May be {@code null}.
   */
  @Nullable
  public PhloxDeliveryInformation getDeliveryInformation ()
  {
    return m_aDeliveryInformation;
  }

  /**
   * Set the delivery information (BG-13).
   *
   * @param aDeliveryInformation
   *        Delivery information; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setDeliveryInformation (@Nullable final PhloxDeliveryInformation aDeliveryInformation)
  {
    m_aDeliveryInformation = aDeliveryInformation;
    return this;
  }

  /**
   * Get the invoicing period (BG-14).
   *
   * @return Invoicing period (BG-14). May be {@code null}.
   */
  @Nullable
  public PhloxInvoicingPeriod getInvoicingPeriod ()
  {
    return m_aInvoicingPeriod;
  }

  /**
   * Set the invoicing period (BG-14).
   *
   * @param aInvoicingPeriod
   *        Invoicing period; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setInvoicingPeriod (@Nullable final PhloxInvoicingPeriod aInvoicingPeriod)
  {
    m_aInvoicingPeriod = aInvoicingPeriod;
    return this;
  }

  /**
   * Get the payment instructions (BG-16 with nested BG-17/BG-18/BG-19).
   *
   * @return Payment instructions (BG-16 with nested BG-17/BG-18/BG-19). May be {@code null}.
   */
  @Nullable
  public PhloxPaymentInstructions getPaymentInstructions ()
  {
    return m_aPaymentInstructions;
  }

  /**
   * Set the payment instructions (BG-16).
   *
   * @param aPaymentInstructions
   *        Payment instructions; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setPaymentInstructions (@Nullable final PhloxPaymentInstructions aPaymentInstructions)
  {
    m_aPaymentInstructions = aPaymentInstructions;
    return this;
  }

  /**
   * Get the mutable list of document-level allowances (BG-20).
   *
   * @return Mutable list of document-level allowances (BG-20). Never {@code null}; empty when none
   *         are present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxDocumentAllowance> documentAllowances ()
  {
    return m_aDocumentAllowances;
  }

  /**
   * Get the mutable list of document-level charges (BG-21).
   *
   * @return Mutable list of document-level charges (BG-21). Never {@code null}; empty when none are
   *         present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxDocumentCharge> documentCharges ()
  {
    return m_aDocumentCharges;
  }

  /**
   * Get the document totals (BG-22).
   *
   * @return Document totals (BG-22). Never {@code null} (initialised to an empty record).
   */
  @NonNull
  public PhloxDocumentTotals getDocumentTotals ()
  {
    return m_aDocumentTotals;
  }

  /**
   * Set the document totals (BG-22).
   *
   * @param aDocumentTotals
   *        Totals; never {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setDocumentTotals (@NonNull final PhloxDocumentTotals aDocumentTotals)
  {
    m_aDocumentTotals = aDocumentTotals;
    return this;
  }

  /**
   * Get the mutable list of VAT breakdowns (BG-23).
   *
   * @return Mutable list of VAT breakdowns (BG-23). Never {@code null}; empty when none are
   *         present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxVatBreakdown> vatBreakdowns ()
  {
    return m_aVatBreakdowns;
  }

  /**
   * Get the mutable list of supporting documents (BG-24).
   *
   * @return Mutable list of supporting documents (BG-24). Never {@code null}; empty when none are
   *         present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxSupportingDocument> supportingDocuments ()
  {
    return m_aSupportingDocuments;
  }

  /**
   * Get the mutable list of invoice lines (BG-25).
   *
   * @return Mutable list of invoice lines (BG-25). Never {@code null}; the EN 16931 model requires
   *         at least one entry for a valid invoice.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxInvoiceLine> lines ()
  {
    return m_aLines;
  }

  /**
   * Get the EN 16931 edition the document was read as.
   *
   * @return EN 16931 edition the document was read as. May be {@code null}.
   */
  @Nullable
  public EEN16931Edition getEdition ()
  {
    return m_eEdition;
  }

  /**
   * Set the EN 16931 edition the document was read as.
   *
   * @param eEdition
   *        EN 16931 edition the document was read as; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setEdition (@Nullable final EEN16931Edition eEdition)
  {
    m_eEdition = eEdition;
    return this;
  }

  /**
   * Get the invoice issue time (BT-166).
   *
   * @return Invoice issue time (BT-166). May be {@code null}.
   */
  @Nullable
  public OffsetTime getIssueTime ()
  {
    return m_aIssueTime;
  }

  /**
   * Set the invoice issue time (BT-166).
   *
   * @param aIssueTime
   *        Invoice issue time; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setIssueTime (@Nullable final OffsetTime aIssueTime)
  {
    m_aIssueTime = aIssueTime;
    return this;
  }

  /**
   * Get the VAT accounting currency exchange rate (BT-167).
   *
   * @return VAT accounting currency exchange rate (BT-167). May be {@code null}.
   */
  @Nullable
  public BigDecimal getVatExchangeRate ()
  {
    return m_aVatExchangeRate;
  }

  /**
   * Set the VAT accounting currency exchange rate (BT-167).
   *
   * @param aVatExchangeRate
   *        VAT accounting currency exchange rate; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setVatExchangeRate (@Nullable final BigDecimal aVatExchangeRate)
  {
    m_aVatExchangeRate = aVatExchangeRate;
    return this;
  }

  /**
   * Get the VAT accounting currency exchange rate target currency code (BT-167-1).
   *
   * @return VAT accounting currency exchange rate target currency code (BT-167-1). May be {@code null}.
   */
  @Nullable
  public String getVatExchangeRateTargetCurrency ()
  {
    return m_sVatExchangeRateTargetCurrency;
  }

  /**
   * Set the VAT accounting currency exchange rate target currency code (BT-167-1).
   *
   * @param sVatExchangeRateTargetCurrency
   *        VAT accounting currency exchange rate target currency code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setVatExchangeRateTargetCurrency (@Nullable final String sVatExchangeRateTargetCurrency)
  {
    m_sVatExchangeRateTargetCurrency = sVatExchangeRateTargetCurrency;
    return this;
  }

  /**
   * Get the VAT accounting currency exchange rate source currency code (BT-167-2).
   *
   * @return VAT accounting currency exchange rate source currency code (BT-167-2). May be {@code null}.
   */
  @Nullable
  public String getVatExchangeRateSourceCurrency ()
  {
    return m_sVatExchangeRateSourceCurrency;
  }

  /**
   * Set the VAT accounting currency exchange rate source currency code (BT-167-2).
   *
   * @param sVatExchangeRateSourceCurrency
   *        VAT accounting currency exchange rate source currency code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setVatExchangeRateSourceCurrency (@Nullable final String sVatExchangeRateSourceCurrency)
  {
    m_sVatExchangeRateSourceCurrency = sVatExchangeRateSourceCurrency;
    return this;
  }

  /**
   * Get the delivery note reference (BT-197).
   *
   * @return Delivery note reference (BT-197). May be {@code null}.
   */
  @Nullable
  public String getDeliveryNoteReference ()
  {
    return m_sDeliveryNoteReference;
  }

  /**
   * Set the delivery note reference (BT-197).
   *
   * @param sDeliveryNoteReference
   *        Delivery note reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoice setDeliveryNoteReference (@Nullable final String sDeliveryNoteReference)
  {
    m_sDeliveryNoteReference = sDeliveryNoteReference;
    return this;
  }

  /**
   * Get the mutable list of buyer references (BT-10 / BT-10-1).
   *
   * @return Mutable list of buyer references (BT-10 / BT-10-1). Never {@code null}; empty when none are present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxIdentifier> buyerReferences ()
  {
    return m_aBuyerReferences;
  }

  /**
   * Get the mutable list of payment terms (BG-33).
   *
   * @return Mutable list of payment terms (BG-33). Never {@code null}; empty when none are present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxPaymentTerms> paymentTerms ()
  {
    return m_aPaymentTerms;
  }
}
