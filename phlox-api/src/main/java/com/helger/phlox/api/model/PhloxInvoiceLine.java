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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.style.ReturnsMutableObject;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;

/**
 * BG-25 INVOICE LINE (1..n) plus all the nested line-level groups (BG-26 invoice line period,
 * BG-27 line allowances, BG-28 line charges, BG-29 price details, BG-30 line VAT, BG-31 item
 * information with BG-32 item attributes). Carries BT-126 (line identifier), BT-127 (line note),
 * BT-128/BT-128-1 (line object identifier), BT-129 (quantity), BT-130 (quantity unit),
 * BT-131 (line net amount), BT-132 (referenced purchase order line) and BT-133 (line buyer
 * accounting reference).
 *
 * @author Philip Helger
 */
public class PhloxInvoiceLine
{
  private String m_sID;
  private String m_sNote;
  private PhloxIdentifier m_aObjectIdentifier;
  private BigDecimal m_aQuantity;
  private String m_sQuantityUnit;
  private BigDecimal m_aNetAmount;
  private String m_sPurchaseOrderLineRef;
  private String m_sAccountingCost;
  private PhloxInvoicingPeriod m_aPeriod;
  private final ICommonsList <PhloxLineAllowance> m_aAllowances = new CommonsArrayList <> ();
  private final ICommonsList <PhloxLineCharge> m_aCharges = new CommonsArrayList <> ();
  private PhloxPriceDetails m_aPrice;
  private PhloxLineVat m_aLineVat;
  private PhloxItem m_aItem;
  private String m_sPurchaseOrderReference;
  private String m_sSalesOrderReference;
  private String m_sSalesOrderLineReference;
  private String m_sDespatchAdviceReference;
  private String m_sDespatchAdviceLineReference;
  private String m_sReceivingAdviceReference;
  private String m_sReceivingAdviceLineReference;
  private String m_sDeliveryNoteReference;
  private String m_sDeliveryNoteLineReference;
  private PhloxLineDelivery m_aDelivery;
  private final ICommonsList <PhloxLinePrecedingInvoiceRef> m_aPrecedingInvoiceRefs = new CommonsArrayList <> ();
  private final ICommonsList <PhloxAdditionalTerm> m_aAdditionalTerms = new CommonsArrayList <> ();

  /** Create an empty invoice line. */
  public PhloxInvoiceLine ()
  {}

  /**
   * Get the mutable list of the values of the line that the model has no place for.
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
   * Get the invoice line identifier (BT-126).
   *
   * @return Invoice line identifier (BT-126). May be {@code null}.
   */
  @Nullable
  public String getID ()
  {
    return m_sID;
  }

  /**
   * Set the invoice line identifier (BT-126).
   *
   * @param sID
   *        Line ID; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setID (@Nullable final String sID)
  {
    m_sID = sID;
    return this;
  }

  /**
   * Get the invoice line note (BT-127).
   *
   * @return Invoice line note (BT-127). May be {@code null}.
   */
  @Nullable
  public String getNote ()
  {
    return m_sNote;
  }

  /**
   * Set the invoice line note (BT-127).
   *
   * @param sNote
   *        Note text; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setNote (@Nullable final String sNote)
  {
    m_sNote = sNote;
    return this;
  }

  /**
   * Get the invoice line object identifier (BT-128 with BT-128-1 scheme ID).
   *
   * @return Invoice line object identifier (BT-128 with BT-128-1 scheme ID). May be {@code null}.
   */
  @Nullable
  public PhloxIdentifier getObjectIdentifier ()
  {
    return m_aObjectIdentifier;
  }

  /**
   * Set the invoice line object identifier (BT-128 / BT-128-1).
   *
   * @param aObjectIdentifier
   *        Identifier; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setObjectIdentifier (@Nullable final PhloxIdentifier aObjectIdentifier)
  {
    m_aObjectIdentifier = aObjectIdentifier;
    return this;
  }

  /**
   * Get the invoiced quantity (BT-129) — credit-note credited quantity also stored here.
   *
   * @return Invoiced quantity (BT-129) — credit-note credited quantity also stored here. May be {@code null}.
   */
  @Nullable
  public BigDecimal getQuantity ()
  {
    return m_aQuantity;
  }

  /**
   * Set the invoiced quantity (BT-129).
   *
   * @param aQuantity
   *        Quantity; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setQuantity (@Nullable final BigDecimal aQuantity)
  {
    m_aQuantity = aQuantity;
    return this;
  }

  /**
   * Get the invoiced quantity unit of measure (BT-130) — UN/ECE Rec 20/21. May be {@code null}.
   *
   * @return Invoiced quantity unit of measure (BT-130) — UN/ECE Rec 20/21. May be {@code null}.
   */
  @Nullable
  public String getQuantityUnit ()
  {
    return m_sQuantityUnit;
  }

  /**
   * Set the invoiced quantity unit of measure (BT-130).
   *
   * @param sQuantityUnit
   *        Unit code; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setQuantityUnit (@Nullable final String sQuantityUnit)
  {
    m_sQuantityUnit = sQuantityUnit;
    return this;
  }

  /**
   * Get the invoice line net amount (BT-131).
   *
   * @return Invoice line net amount (BT-131). May be {@code null}.
   */
  @Nullable
  public BigDecimal getNetAmount ()
  {
    return m_aNetAmount;
  }

  /**
   * Set the invoice line net amount (BT-131).
   *
   * @param aNetAmount
   *        Net amount; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setNetAmount (@Nullable final BigDecimal aNetAmount)
  {
    m_aNetAmount = aNetAmount;
    return this;
  }

  /**
   * Get the referenced purchase order line reference (BT-132).
   *
   * @return Referenced purchase order line reference (BT-132). May be {@code null}.
   */
  @Nullable
  public String getPurchaseOrderLineRef ()
  {
    return m_sPurchaseOrderLineRef;
  }

  /**
   * Set the referenced purchase order line reference (BT-132).
   *
   * @param sPurchaseOrderLineRef
   *        PO line reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setPurchaseOrderLineRef (@Nullable final String sPurchaseOrderLineRef)
  {
    m_sPurchaseOrderLineRef = sPurchaseOrderLineRef;
    return this;
  }

  /**
   * Get the invoice line buyer accounting reference (BT-133).
   *
   * @return Invoice line buyer accounting reference (BT-133). May be {@code null}.
   */
  @Nullable
  public String getAccountingCost ()
  {
    return m_sAccountingCost;
  }

  /**
   * Set the invoice line buyer accounting reference (BT-133).
   *
   * @param sAccountingCost
   *        Accounting cost reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setAccountingCost (@Nullable final String sAccountingCost)
  {
    m_sAccountingCost = sAccountingCost;
    return this;
  }

  /**
   * Get the invoice line period (BG-26 — BT-134 / BT-135).
   *
   * @return Invoice line period (BG-26 — BT-134 / BT-135). May be {@code null}.
   */
  @Nullable
  public PhloxInvoicingPeriod getPeriod ()
  {
    return m_aPeriod;
  }

  /**
   * Set the invoice line period (BG-26).
   *
   * @param aPeriod
   *        Period; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setPeriod (@Nullable final PhloxInvoicingPeriod aPeriod)
  {
    m_aPeriod = aPeriod;
    return this;
  }

  /**
   * Get the mutable list of line allowances (BG-27).
   *
   * @return Mutable list of line allowances (BG-27). Never {@code null}; empty when none are
   *         present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxLineAllowance> allowances ()
  {
    return m_aAllowances;
  }

  /**
   * Get the mutable list of line charges (BG-28).
   *
   * @return Mutable list of line charges (BG-28). Never {@code null}; empty when none are
   *         present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxLineCharge> charges ()
  {
    return m_aCharges;
  }

  /**
   * Get the price details (BG-29).
   *
   * @return Price details (BG-29). May be {@code null}.
   */
  @Nullable
  public PhloxPriceDetails getPrice ()
  {
    return m_aPrice;
  }

  /**
   * Set the price details (BG-29).
   *
   * @param aPrice
   *        Price details; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setPrice (@Nullable final PhloxPriceDetails aPrice)
  {
    m_aPrice = aPrice;
    return this;
  }

  /**
   * Get the line VAT information (BG-30).
   *
   * @return Line VAT information (BG-30). May be {@code null}.
   */
  @Nullable
  public PhloxLineVat getLineVat ()
  {
    return m_aLineVat;
  }

  /**
   * Set the line VAT information (BG-30).
   *
   * @param aLineVat
   *        Line VAT; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setLineVat (@Nullable final PhloxLineVat aLineVat)
  {
    m_aLineVat = aLineVat;
    return this;
  }

  /**
   * Get the item information (BG-31).
   *
   * @return Item information (BG-31). May be {@code null}.
   */
  @Nullable
  public PhloxItem getItem ()
  {
    return m_aItem;
  }

  /**
   * Set the item information (BG-31).
   *
   * @param aItem
   *        Item; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setItem (@Nullable final PhloxItem aItem)
  {
    m_aItem = aItem;
    return this;
  }

  /**
   * Get the invoice line purchase order reference (BT-188).
   *
   * @return Invoice line purchase order reference (BT-188). May be {@code null}.
   */
  @Nullable
  public String getPurchaseOrderReference ()
  {
    return m_sPurchaseOrderReference;
  }

  /**
   * Set the invoice line purchase order reference (BT-188).
   *
   * @param sPurchaseOrderReference
   *        Invoice line purchase order reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setPurchaseOrderReference (@Nullable final String sPurchaseOrderReference)
  {
    m_sPurchaseOrderReference = sPurchaseOrderReference;
    return this;
  }

  /**
   * Get the invoice line sales order reference (BT-200).
   *
   * @return Invoice line sales order reference (BT-200). May be {@code null}.
   */
  @Nullable
  public String getSalesOrderReference ()
  {
    return m_sSalesOrderReference;
  }

  /**
   * Set the invoice line sales order reference (BT-200).
   *
   * @param sSalesOrderReference
   *        Invoice line sales order reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setSalesOrderReference (@Nullable final String sSalesOrderReference)
  {
    m_sSalesOrderReference = sSalesOrderReference;
    return this;
  }

  /**
   * Get the invoice line sales order line reference (BT-201).
   *
   * @return Invoice line sales order line reference (BT-201). May be {@code null}.
   */
  @Nullable
  public String getSalesOrderLineReference ()
  {
    return m_sSalesOrderLineReference;
  }

  /**
   * Set the invoice line sales order line reference (BT-201).
   *
   * @param sSalesOrderLineReference
   *        Invoice line sales order line reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setSalesOrderLineReference (@Nullable final String sSalesOrderLineReference)
  {
    m_sSalesOrderLineReference = sSalesOrderLineReference;
    return this;
  }

  /**
   * Get the invoice line despatch advice reference (BT-189).
   *
   * @return Invoice line despatch advice reference (BT-189). May be {@code null}.
   */
  @Nullable
  public String getDespatchAdviceReference ()
  {
    return m_sDespatchAdviceReference;
  }

  /**
   * Set the invoice line despatch advice reference (BT-189).
   *
   * @param sDespatchAdviceReference
   *        Invoice line despatch advice reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setDespatchAdviceReference (@Nullable final String sDespatchAdviceReference)
  {
    m_sDespatchAdviceReference = sDespatchAdviceReference;
    return this;
  }

  /**
   * Get the invoice line despatch advice line reference (BT-190).
   *
   * @return Invoice line despatch advice line reference (BT-190). May be {@code null}.
   */
  @Nullable
  public String getDespatchAdviceLineReference ()
  {
    return m_sDespatchAdviceLineReference;
  }

  /**
   * Set the invoice line despatch advice line reference (BT-190).
   *
   * @param sDespatchAdviceLineReference
   *        Invoice line despatch advice line reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setDespatchAdviceLineReference (@Nullable final String sDespatchAdviceLineReference)
  {
    m_sDespatchAdviceLineReference = sDespatchAdviceLineReference;
    return this;
  }

  /**
   * Get the invoice line receiving advice reference (BT-191).
   *
   * @return Invoice line receiving advice reference (BT-191). May be {@code null}.
   */
  @Nullable
  public String getReceivingAdviceReference ()
  {
    return m_sReceivingAdviceReference;
  }

  /**
   * Set the invoice line receiving advice reference (BT-191).
   *
   * @param sReceivingAdviceReference
   *        Invoice line receiving advice reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setReceivingAdviceReference (@Nullable final String sReceivingAdviceReference)
  {
    m_sReceivingAdviceReference = sReceivingAdviceReference;
    return this;
  }

  /**
   * Get the invoice line receiving advice line reference (BT-192).
   *
   * @return Invoice line receiving advice line reference (BT-192). May be {@code null}.
   */
  @Nullable
  public String getReceivingAdviceLineReference ()
  {
    return m_sReceivingAdviceLineReference;
  }

  /**
   * Set the invoice line receiving advice line reference (BT-192).
   *
   * @param sReceivingAdviceLineReference
   *        Invoice line receiving advice line reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setReceivingAdviceLineReference (@Nullable final String sReceivingAdviceLineReference)
  {
    m_sReceivingAdviceLineReference = sReceivingAdviceLineReference;
    return this;
  }

  /**
   * Get the invoice line delivery note reference (BT-198).
   *
   * @return Invoice line delivery note reference (BT-198). May be {@code null}.
   */
  @Nullable
  public String getDeliveryNoteReference ()
  {
    return m_sDeliveryNoteReference;
  }

  /**
   * Set the invoice line delivery note reference (BT-198).
   *
   * @param sDeliveryNoteReference
   *        Invoice line delivery note reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setDeliveryNoteReference (@Nullable final String sDeliveryNoteReference)
  {
    m_sDeliveryNoteReference = sDeliveryNoteReference;
    return this;
  }

  /**
   * Get the invoice line delivery note line reference (BT-199).
   *
   * @return Invoice line delivery note line reference (BT-199). May be {@code null}.
   */
  @Nullable
  public String getDeliveryNoteLineReference ()
  {
    return m_sDeliveryNoteLineReference;
  }

  /**
   * Set the invoice line delivery note line reference (BT-199).
   *
   * @param sDeliveryNoteLineReference
   *        Invoice line delivery note line reference; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setDeliveryNoteLineReference (@Nullable final String sDeliveryNoteLineReference)
  {
    m_sDeliveryNoteLineReference = sDeliveryNoteLineReference;
    return this;
  }

  /**
   * Get the invoice line delivery information (BG-37).
   *
   * @return Invoice line delivery information (BG-37). May be {@code null}.
   */
  @Nullable
  public PhloxLineDelivery getDelivery ()
  {
    return m_aDelivery;
  }

  /**
   * Set the invoice line delivery information (BG-37).
   *
   * @param aDelivery
   *        Invoice line delivery information; may be {@code null}.
   * @return this for chaining.
   */
  @NonNull
  public PhloxInvoiceLine setDelivery (@Nullable final PhloxLineDelivery aDelivery)
  {
    m_aDelivery = aDelivery;
    return this;
  }

  /**
   * Get the mutable list of line-level preceding invoice references (BG-39).
   *
   * @return Mutable list of line-level preceding invoice references (BG-39). Never {@code null}; empty when none are present.
   */
  @NonNull
  @ReturnsMutableObject
  public ICommonsList <PhloxLinePrecedingInvoiceRef> precedingInvoiceRefs ()
  {
    return m_aPrecedingInvoiceRefs;
  }
}
