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

import java.math.BigDecimal;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.phlox.api.model.EPhloxDocumentKind;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxBuyer;
import com.helger.phlox.api.model.PhloxContact;
import com.helger.phlox.api.model.PhloxCreditTransfer;
import com.helger.phlox.api.model.PhloxDeliveryInformation;
import com.helger.phlox.api.model.PhloxDirectDebit;
import com.helger.phlox.api.model.PhloxDocumentAllowance;
import com.helger.phlox.api.model.PhloxDocumentCharge;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxInvoiceNote;
import com.helger.phlox.api.model.PhloxInvoicingPeriod;
import com.helger.phlox.api.model.PhloxItem;
import com.helger.phlox.api.model.PhloxItemAttribute;
import com.helger.phlox.api.model.PhloxItemClassification;
import com.helger.phlox.api.model.PhloxLineAllowance;
import com.helger.phlox.api.model.PhloxLineCharge;
import com.helger.phlox.api.model.PhloxLineVat;
import com.helger.phlox.api.model.PhloxPayee;
import com.helger.phlox.api.model.PhloxPaymentCard;
import com.helger.phlox.api.model.PhloxPaymentInstructions;
import com.helger.phlox.api.model.PhloxPaymentTerms;
import com.helger.phlox.api.model.PhloxPostalAddress;
import com.helger.phlox.api.model.PhloxPrecedingInvoiceRef;
import com.helger.phlox.api.model.PhloxPriceDetails;
import com.helger.phlox.api.model.PhloxSeller;
import com.helger.phlox.api.model.PhloxSellerTaxRepresentative;
import com.helger.phlox.api.model.PhloxSupportingDocument;
import com.helger.phlox.api.model.PhloxVatBreakdown;
import com.helger.ubl21.UBL21Marshaller;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.BillingReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.BranchType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CommodityClassificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CountryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ExternalReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.FinancialAccountType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemPropertyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.OrderLineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.OrderReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentMandateType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.NoteType;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;

/**
 * Reads UBL 2.1 Invoice and CreditNote documents (EN 16931:2017 binding) into a
 * {@link PhloxInvoice}. Used by {@link UblInvoiceReader}.
 *
 * @author Philip Helger
 */
final class Ubl21InvoiceReader extends AbstractUblInvoiceReader
{
  Ubl21InvoiceReader ()
  {}

  private static void _addBuyerReference (@NonNull final PhloxInvoice aModel, @Nullable final String sBuyerReference)
  {
    // BT-10 — EN 16931:2017 has no scheme identifier (BT-10-1)
    if (sBuyerReference != null)
      aModel.buyerReferences ().add (new PhloxIdentifier (sBuyerReference, null));
  }

  @NonNull
  private static PhloxPostalAddress _readAddress (@NonNull final AddressType aAddr)
  {
    final PhloxPostalAddress aOut = new PhloxPostalAddress ();
    aOut.setLine1 (str (aAddr.getStreetNameValue ()));
    aOut.setLine2 (str (aAddr.getAdditionalStreetNameValue ()));
    if (!aAddr.getAddressLine ().isEmpty ())
    {
      final AddressLineType aAL = aAddr.getAddressLine ().get (0);
      aOut.setLine3 (str (aAL.getLineValue ()));
    }
    aOut.setCity (str (aAddr.getCityNameValue ()));
    aOut.setPostCode (str (aAddr.getPostalZoneValue ()));
    aOut.setCountrySubdivision (str (aAddr.getCountrySubentityValue ()));
    final CountryType aCountry = aAddr.getCountry ();
    if (aCountry != null)
      aOut.setCountryCode (str (aCountry.getIdentificationCodeValue ()));
    return aOut;
  }

  @NonNull
  private static PhloxContact _readContact (@NonNull final ContactType aContact)
  {
    final PhloxContact aOut = new PhloxContact ();
    aOut.setName (str (aContact.getNameValue ()));
    aOut.setTelephone (str (aContact.getTelephoneValue ()));
    aOut.setEmail (str (aContact.getElectronicMailValue ()));
    return aOut;
  }

  private static void _readBuyer (@NonNull final CustomerPartyType aCP, @NonNull final PhloxBuyer aBuyer)
  {
    if (aCP.getParty () == null)
      return;
    final PartyType aParty = aCP.getParty ();
    // BT-44
    if (!aParty.getPartyLegalEntity ().isEmpty ())
    {
      final PartyLegalEntityType aLE = aParty.getPartyLegalEntity ().get (0);
      aBuyer.setName (str (aLE.getRegistrationNameValue ()));
      if (aLE.getCompanyID () != null)
        aBuyer.setLegalRegistration (identifier (aLE.getCompanyID ()));
    }
    // BT-45 — trading name
    if (!aParty.getPartyName ().isEmpty ())
    {
      final PartyNameType aPN = aParty.getPartyName ().get (0);
      aBuyer.setTradingName (str (aPN.getNameValue ()));
    }
    // BT-46 — identifiers
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      if (aPI.getID () != null)
        aBuyer.identifiers ().add (identifier (aPI.getID ()));
    // BT-48 — VAT
    for (final PartyTaxSchemeType aPTS : aParty.getPartyTaxScheme ())
      if (aPTS.getTaxScheme () != null && "VAT".equals (aPTS.getTaxScheme ().getIDValue ()))
        aBuyer.setVatIdentifier (str (aPTS.getCompanyIDValue ()));
    // BT-49
    if (aParty.getEndpointID () != null)
      aBuyer.setElectronicAddress (identifier (aParty.getEndpointID ()));
    // BG-8
    if (aParty.getPostalAddress () != null)
      aBuyer.setPostalAddress (_readAddress (aParty.getPostalAddress ()));
    // BG-9
    if (aParty.getContact () != null)
      aBuyer.setContact (_readContact (aParty.getContact ()));
  }

  private static void _readPartyCommon (@NonNull final PartyType aParty, @NonNull final PhloxSeller aSeller)
  {
    // BT-27
    if (!aParty.getPartyLegalEntity ().isEmpty ())
    {
      final PartyLegalEntityType aLE = aParty.getPartyLegalEntity ().get (0);
      aSeller.setName (str (aLE.getRegistrationNameValue ()));
      if (aLE.getCompanyID () != null)
        aSeller.setLegalRegistration (identifier (aLE.getCompanyID ()));
      // BT-33
      aSeller.setAdditionalLegalInfo (str (aLE.getCompanyLegalFormValue ()));
    }
    // BT-28
    if (!aParty.getPartyName ().isEmpty ())
      aSeller.setTradingName (str (aParty.getPartyName ().get (0).getNameValue ()));
    // BT-29
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      if (aPI.getID () != null && !SEPA_SCHEME_ID.equals (aPI.getID ().getSchemeID ()))
        aSeller.identifiers ().add (identifier (aPI.getID ()));
    // BT-31 / BT-32
    for (final PartyTaxSchemeType aPTS : aParty.getPartyTaxScheme ())
      if (aPTS.getTaxScheme () != null)
      {
        if ("VAT".equals (aPTS.getTaxScheme ().getIDValue ()))
          aSeller.setVatIdentifier (str (aPTS.getCompanyIDValue ()));
        else
          aSeller.setTaxRegistration (str (aPTS.getCompanyIDValue ()));
      }
    // BT-34
    if (aParty.getEndpointID () != null)
      aSeller.setElectronicAddress (identifier (aParty.getEndpointID ()));
    // BG-5
    if (aParty.getPostalAddress () != null)
      aSeller.setPostalAddress (_readAddress (aParty.getPostalAddress ()));
    // BG-6
    if (aParty.getContact () != null)
      aSeller.setContact (_readContact (aParty.getContact ()));
  }

  private static void _readSeller (@NonNull final SupplierPartyType aSP, @NonNull final PhloxSeller aSeller)
  {
    if (aSP.getParty () == null)
      return;
    final PartyType aParty = aSP.getParty ();
    _readPartyCommon (aParty, aSeller);
  }

  @Nullable
  private static PhloxPayee _readPayee (@NonNull final PartyType aParty)
  {
    final PhloxPayee aOut = new PhloxPayee ();
    if (!aParty.getPartyName ().isEmpty ())
      aOut.setName (str (aParty.getPartyName ().get (0).getNameValue ()));
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      if (aPI.getID () != null && !SEPA_SCHEME_ID.equals (aPI.getID ().getSchemeID ()))
      {
        aOut.setIdentifier (identifier (aPI.getID ()));
        break;
      }
    if (!aParty.getPartyLegalEntity ().isEmpty ())
    {
      final PartyLegalEntityType aLE = aParty.getPartyLegalEntity ().get (0);
      if (aLE.getCompanyID () != null)
        aOut.setLegalRegistration (identifier (aLE.getCompanyID ()));
    }
    return aOut;
  }

  @NonNull
  private static PhloxSellerTaxRepresentative _readSellerTaxRepresentative (@NonNull final PartyType aParty)
  {
    final PhloxSellerTaxRepresentative aOut = new PhloxSellerTaxRepresentative ();
    if (!aParty.getPartyName ().isEmpty ())
      aOut.setName (str (aParty.getPartyName ().get (0).getNameValue ()));
    for (final PartyTaxSchemeType aPTS : aParty.getPartyTaxScheme ())
      if (aPTS.getTaxScheme () != null && "VAT".equals (aPTS.getTaxScheme ().getIDValue ()))
        aOut.setVatIdentifier (str (aPTS.getCompanyIDValue ()));
    if (aParty.getPostalAddress () != null)
      aOut.setPostalAddress (_readAddress (aParty.getPostalAddress ()));
    return aOut;
  }

  @NonNull
  private static PhloxDeliveryInformation _readDelivery (@NonNull final DeliveryType aDelivery)
  {
    final PhloxDeliveryInformation aOut = new PhloxDeliveryInformation ();
    if (aDelivery.getDeliveryParty () != null && !aDelivery.getDeliveryParty ().getPartyName ().isEmpty ())
      aOut.setPartyName (str (aDelivery.getDeliveryParty ().getPartyName ().get (0).getNameValue ()));
    if (aDelivery.getDeliveryLocation () != null)
    {
      if (aDelivery.getDeliveryLocation ().getID () != null)
        aOut.setLocationIdentifier (identifier (aDelivery.getDeliveryLocation ().getID ()));
      if (aDelivery.getDeliveryLocation ().getAddress () != null)
        aOut.setPostalAddress (_readAddress (aDelivery.getDeliveryLocation ().getAddress ()));
    }
    aOut.setActualDeliveryDate (d (aDelivery.getActualDeliveryDateValue ()));
    return aOut;
  }

  @Nullable
  private static String _findSepaCreditorID (@Nullable final SupplierPartyType aSupplier,
                                             @Nullable final PartyType aPayee)
  {
    if (aPayee != null)
      for (final PartyIdentificationType aPI : aPayee.getPartyIdentification ())
        if (aPI.getID () != null && SEPA_SCHEME_ID.equals (aPI.getID ().getSchemeID ()))
          return aPI.getIDValue ();
    if (aSupplier != null && aSupplier.getParty () != null)
      for (final PartyIdentificationType aPI : aSupplier.getParty ().getPartyIdentification ())
        if (aPI.getID () != null && SEPA_SCHEME_ID.equals (aPI.getID ().getSchemeID ()))
          return aPI.getIDValue ();
    return null;
  }

  @NonNull
  private static PhloxCreditTransfer _readCreditTransfer (@NonNull final FinancialAccountType aAcc)
  {
    final PhloxCreditTransfer aOut = new PhloxCreditTransfer ();
    aOut.setAccountIdentifier (str (aAcc.getIDValue ()));
    aOut.setAccountName (str (aAcc.getNameValue ()));
    final BranchType aBranch = aAcc.getFinancialInstitutionBranch ();
    if (aBranch != null)
      aOut.setServiceProviderID (str (aBranch.getIDValue ()));
    return aOut;
  }

  @NonNull
  private static PhloxPaymentInstructions _readPaymentInstructions (@NonNull final List <PaymentMeansType> aPaymentMeans,
                                                                    @Nullable final SupplierPartyType aSupplier,
                                                                    @Nullable final PartyType aPayee)
  {
    final PhloxPaymentInstructions aOut = new PhloxPaymentInstructions ();
    final PaymentMeansType aPM = aPaymentMeans.get (0);
    aOut.setPaymentMeansCode (str (aPM.getPaymentMeansCodeValue ()));
    if (aPM.getPaymentMeansCode () != null)
      aOut.setPaymentMeansText (aPM.getPaymentMeansCode ().getName ());
    if (!aPM.getPaymentID ().isEmpty ())
      aOut.setRemittanceInformation (str (aPM.getPaymentID ().get (0).getValue ()));

    // BG-17 — credit transfers across ALL payment means
    for (final PaymentMeansType aOnePM : aPaymentMeans)
      if (aOnePM.getPayeeFinancialAccount () != null)
        aOut.creditTransfers ().add (_readCreditTransfer (aOnePM.getPayeeFinancialAccount ()));

    // BG-18 / BG-19 may sit in any of the payment means, not only in the first one
    PaymentMeansType aCardMeans = null;
    PaymentMeansType aMandateMeans = null;
    for (final PaymentMeansType aOnePM : aPaymentMeans)
    {
      if (aCardMeans == null && aOnePM.getCardAccount () != null)
        aCardMeans = aOnePM;
      if (aMandateMeans == null && aOnePM.getPaymentMandate () != null)
        aMandateMeans = aOnePM;
    }

    // BG-18 — payment card
    if (aCardMeans != null)
    {
      final PhloxPaymentCard aCard = new PhloxPaymentCard ();
      aCard.setPan (str (aCardMeans.getCardAccount ().getPrimaryAccountNumberIDValue ()));
      aCard.setHolderName (str (aCardMeans.getCardAccount ().getHolderNameValue ()));
      aOut.setPaymentCard (aCard);
    }

    // BG-19 — direct debit
    if (aMandateMeans != null)
    {
      final PaymentMandateType aMandate = aMandateMeans.getPaymentMandate ();
      final PhloxDirectDebit aDD = new PhloxDirectDebit ();
      aDD.setMandateReferenceID (str (aMandate.getIDValue ()));
      if (aMandate.getPayerFinancialAccount () != null)
        aDD.setDebitedAccountID (str (aMandate.getPayerFinancialAccount ().getIDValue ()));
      // BT-90 — bank assigned creditor id (UBL: from supplier/payee PartyIdentification with SEPA
      // scheme)
      aDD.setBankAssignedCreditorID (_findSepaCreditorID (aSupplier, aPayee));
      aOut.setDirectDebit (aDD);
    }
    return aOut;
  }

  @NonNull
  private static PhloxDocumentAllowance _readDocumentAllowance (@NonNull final AllowanceChargeType aAC)
  {
    final PhloxDocumentAllowance aOut = new PhloxDocumentAllowance ();
    if (aAC.getAmount () != null)
      aOut.setAmount (aAC.getAmount ().getValue ());
    if (aAC.getBaseAmount () != null)
      aOut.setBaseAmount (aAC.getBaseAmount ().getValue ());
    if (aAC.getMultiplierFactorNumeric () != null)
      aOut.setPercentage (aAC.getMultiplierFactorNumeric ().getValue ());
    aOut.setReason (aAC.getAllowanceChargeReason ().isEmpty () ? null
                                                               : str (aAC.getAllowanceChargeReason ()
                                                                          .get (0)
                                                                          .getValue ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    if (aAC.getTaxCategory () != null && !aAC.getTaxCategory ().isEmpty ())
    {
      final TaxCategoryType aTC = aAC.getTaxCategory ().get (0);
      aOut.setVatCategoryCode (str (aTC.getIDValue ()));
      if (aTC.getPercent () != null)
        aOut.setVatRate (aTC.getPercent ().getValue ());
    }
    return aOut;
  }

  @NonNull
  private static PhloxDocumentCharge _readDocumentCharge (@NonNull final AllowanceChargeType aAC)
  {
    final PhloxDocumentCharge aOut = new PhloxDocumentCharge ();
    if (aAC.getAmount () != null)
      aOut.setAmount (aAC.getAmount ().getValue ());
    if (aAC.getBaseAmount () != null)
      aOut.setBaseAmount (aAC.getBaseAmount ().getValue ());
    if (aAC.getMultiplierFactorNumeric () != null)
      aOut.setPercentage (aAC.getMultiplierFactorNumeric ().getValue ());
    aOut.setReason (aAC.getAllowanceChargeReason ().isEmpty () ? null
                                                               : str (aAC.getAllowanceChargeReason ()
                                                                          .get (0)
                                                                          .getValue ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    if (aAC.getTaxCategory () != null && !aAC.getTaxCategory ().isEmpty ())
    {
      final TaxCategoryType aTC = aAC.getTaxCategory ().get (0);
      aOut.setVatCategoryCode (str (aTC.getIDValue ()));
      if (aTC.getPercent () != null)
        aOut.setVatRate (aTC.getPercent ().getValue ());
    }
    return aOut;
  }

  private static void _readTotals (@NonNull final MonetaryTotalType aLMT, @NonNull final PhloxInvoice aModel)
  {
    if (aLMT.getLineExtensionAmount () != null)
      aModel.getDocumentTotals ().setLineNetTotal (aLMT.getLineExtensionAmount ().getValue ());
    if (aLMT.getAllowanceTotalAmount () != null)
      aModel.getDocumentTotals ().setAllowanceTotal (aLMT.getAllowanceTotalAmount ().getValue ());
    if (aLMT.getChargeTotalAmount () != null)
      aModel.getDocumentTotals ().setChargeTotal (aLMT.getChargeTotalAmount ().getValue ());
    if (aLMT.getTaxExclusiveAmount () != null)
      aModel.getDocumentTotals ().setTaxExclusiveTotal (aLMT.getTaxExclusiveAmount ().getValue ());
    if (aLMT.getTaxInclusiveAmount () != null)
      aModel.getDocumentTotals ().setTaxInclusiveTotal (aLMT.getTaxInclusiveAmount ().getValue ());
    if (aLMT.getPrepaidAmount () != null)
      aModel.getDocumentTotals ().setPrepaidAmount (aLMT.getPrepaidAmount ().getValue ());
    if (aLMT.getPayableRoundingAmount () != null)
      aModel.getDocumentTotals ().setRoundingAmount (aLMT.getPayableRoundingAmount ().getValue ());
    if (aLMT.getPayableAmount () != null)
      aModel.getDocumentTotals ().setPayableAmount (aLMT.getPayableAmount ().getValue ());
  }

  @NonNull
  private static PhloxVatBreakdown _readVatBreakdown (@NonNull final TaxSubtotalType aSub)
  {
    final PhloxVatBreakdown aOut = new PhloxVatBreakdown ();
    if (aSub.getTaxableAmount () != null)
      aOut.setTaxableAmount (aSub.getTaxableAmount ().getValue ());
    if (aSub.getTaxAmount () != null)
      aOut.setTaxAmount (aSub.getTaxAmount ().getValue ());
    if (aSub.getTaxCategory () != null)
    {
      final TaxCategoryType aTC = aSub.getTaxCategory ();
      aOut.setCategoryCode (str (aTC.getIDValue ()));
      if (aTC.getPercent () != null)
        aOut.setRate (aTC.getPercent ().getValue ());
      aOut.setExemptionReason (aTC.getTaxExemptionReason ().isEmpty () ? null
                                                                       : str (aTC.getTaxExemptionReason ()
                                                                                  .get (0)
                                                                                  .getValue ()));
      aOut.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
    }
    return aOut;
  }

  private static void _readTaxTotals (@NonNull final List <TaxTotalType> aTaxTotals, @NonNull final PhloxInvoice aModel)
  {
    final String sAccountingCurrency = aModel.getVatAccountingCurrency ();
    boolean bMainTaxTotalSet = false;
    for (final TaxTotalType aTT : aTaxTotals)
    {
      if (aTT.getTaxAmount () != null)
      {
        final BigDecimal aAmount = aTT.getTaxAmount ().getValue ();
        final String sCurrencyID = aTT.getTaxAmount ().getCurrencyID ();
        if (isVatAccountingAmount (aModel.getCurrency (), sAccountingCurrency, sCurrencyID))
          aModel.getDocumentTotals ().setTaxTotalAccountingCurrency (aAmount);
        else
          if (!bMainTaxTotalSet)
          {
            aModel.getDocumentTotals ().setTaxTotal (aAmount);
            bMainTaxTotalSet = true;
          }
      }
      for (final TaxSubtotalType aSub : aTT.getTaxSubtotal ())
        aModel.vatBreakdowns ().add (_readVatBreakdown (aSub));
    }
  }

  @NonNull
  private static PhloxSupportingDocument _readSupportingDocument (@NonNull final DocumentReferenceType aRef)
  {
    final PhloxSupportingDocument aOut = new PhloxSupportingDocument ();
    aOut.setReference (str (aRef.getIDValue ()));
    aOut.setDescription (firstText (aRef.getDocumentDescription ()));
    if (aRef.getAttachment () != null)
    {
      final AttachmentType aAtt = aRef.getAttachment ();
      if (aAtt.getExternalReference () != null)
      {
        final ExternalReferenceType aER = aAtt.getExternalReference ();
        aOut.setExternalLocation (str (aER.getURIValue ()));
      }
      if (aAtt.getEmbeddedDocumentBinaryObject () != null)
      {
        final PhloxAttachment aBin = new PhloxAttachment ();
        aBin.setBinary (aAtt.getEmbeddedDocumentBinaryObject ().getValue ());
        aBin.setMimeCode (aAtt.getEmbeddedDocumentBinaryObject ().getMimeCode ());
        aBin.setFilename (aAtt.getEmbeddedDocumentBinaryObject ().getFilename ());
        aOut.setAttachment (aBin);
      }
    }
    return aOut;
  }

  @NonNull
  private static PhloxLineAllowance _readLineAllowance (@NonNull final AllowanceChargeType aAC)
  {
    final PhloxLineAllowance aOut = new PhloxLineAllowance ();
    if (aAC.getAmount () != null)
      aOut.setAmount (aAC.getAmount ().getValue ());
    if (aAC.getBaseAmount () != null)
      aOut.setBaseAmount (aAC.getBaseAmount ().getValue ());
    if (aAC.getMultiplierFactorNumeric () != null)
      aOut.setPercentage (aAC.getMultiplierFactorNumeric ().getValue ());
    aOut.setReason (aAC.getAllowanceChargeReason ().isEmpty () ? null
                                                               : str (aAC.getAllowanceChargeReason ()
                                                                          .get (0)
                                                                          .getValue ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    return aOut;
  }

  @NonNull
  private static PhloxLineCharge _readLineCharge (@NonNull final AllowanceChargeType aAC)
  {
    final PhloxLineCharge aOut = new PhloxLineCharge ();
    if (aAC.getAmount () != null)
      aOut.setAmount (aAC.getAmount ().getValue ());
    if (aAC.getBaseAmount () != null)
      aOut.setBaseAmount (aAC.getBaseAmount ().getValue ());
    if (aAC.getMultiplierFactorNumeric () != null)
      aOut.setPercentage (aAC.getMultiplierFactorNumeric ().getValue ());
    aOut.setReason (aAC.getAllowanceChargeReason ().isEmpty () ? null
                                                               : str (aAC.getAllowanceChargeReason ()
                                                                          .get (0)
                                                                          .getValue ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    return aOut;
  }

  @NonNull
  private static PhloxPriceDetails _readPriceDetails (@NonNull final PriceType aPrice)
  {
    final PhloxPriceDetails aOut = new PhloxPriceDetails ();
    if (aPrice.getPriceAmount () != null)
      aOut.setNetPrice (aPrice.getPriceAmount ().getValue ());
    if (aPrice.getBaseQuantity () != null)
    {
      aOut.setBaseQuantity (aPrice.getBaseQuantity ().getValue ());
      aOut.setBaseQuantityUnit (aPrice.getBaseQuantity ().getUnitCode ());
    }
    for (final AllowanceChargeType aAC : aPrice.getAllowanceCharge ())
      if (aAC.getChargeIndicator () == null || !aAC.getChargeIndicator ().isValue ())
      {
        if (aAC.getAmount () != null)
          aOut.setDiscount (aAC.getAmount ().getValue ());
        if (aAC.getBaseAmount () != null)
          aOut.setGrossPrice (aAC.getBaseAmount ().getValue ());
      }
    return aOut;
  }

  @NonNull
  private static PhloxItem _readItem (@NonNull final ItemType aItem)
  {
    final PhloxItem aOut = new PhloxItem ();
    aOut.setName (str (aItem.getNameValue ()));
    aOut.setDescription (!aItem.getDescription ().isEmpty () ? aItem.getDescription ().get (0).getValue () : null);
    if (aItem.getSellersItemIdentification () != null)
      aOut.setSellerItemID (str (aItem.getSellersItemIdentification ().getIDValue ()));
    if (aItem.getBuyersItemIdentification () != null)
      aOut.setBuyerItemID (str (aItem.getBuyersItemIdentification ().getIDValue ()));
    if (aItem.getStandardItemIdentification () != null)
    {
      final ItemIdentificationType aSII = aItem.getStandardItemIdentification ();
      if (aSII.getID () != null)
        aOut.setStandardItemID (identifier (aSII.getID ()));
    }
    if (aItem.getOriginCountry () != null)
      aOut.setCountryOfOrigin (str (aItem.getOriginCountry ().getIdentificationCodeValue ()));
    for (final CommodityClassificationType aCC : aItem.getCommodityClassification ())
      if (aCC.getItemClassificationCode () != null)
      {
        final PhloxItemClassification aIC = new PhloxItemClassification ();
        aIC.setCode (aCC.getItemClassificationCode ().getValue ());
        aIC.setListID (aCC.getItemClassificationCode ().getListID ());
        aIC.setListVersionID (aCC.getItemClassificationCode ().getListVersionID ());
        aOut.classifications ().add (aIC);
      }
    for (final ItemPropertyType aIP : aItem.getAdditionalItemProperty ())
      aOut.attributes ().add (new PhloxItemAttribute (str (aIP.getNameValue ()), str (aIP.getValueValue ())));
    return aOut;
  }

  @NonNull
  private static PhloxInvoiceLine _readInvoiceLine (@NonNull final InvoiceLineType aLine)
  {
    final PhloxInvoiceLine aOut = new PhloxInvoiceLine ();
    aOut.setID (str (aLine.getIDValue ()));
    if (!aLine.getNote ().isEmpty ())
      aOut.setNote (str (aLine.getNote ().get (0).getValue ()));
    if (aLine.getInvoicedQuantity () != null)
    {
      aOut.setQuantity (aLine.getInvoicedQuantity ().getValue ());
      aOut.setQuantityUnit (aLine.getInvoicedQuantity ().getUnitCode ());
    }
    if (aLine.getLineExtensionAmount () != null)
      aOut.setNetAmount (aLine.getLineExtensionAmount ().getValue ());
    aOut.setAccountingCost (str (aLine.getAccountingCostValue ()));
    if (!aLine.getOrderLineReference ().isEmpty ())
    {
      final OrderLineReferenceType aOLR = aLine.getOrderLineReference ().get (0);
      aOut.setPurchaseOrderLineRef (str (aOLR.getLineIDValue ()));
    }
    if (!aLine.getDocumentReference ().isEmpty () && aLine.getDocumentReference ().get (0).getID () != null)
    {
      final DocumentReferenceType aDR = aLine.getDocumentReference ().get (0);
      aOut.setObjectIdentifier (identifier (aDR.getID ()));
    }
    if (!aLine.getInvoicePeriod ().isEmpty ())
    {
      final PeriodType aPeriod = aLine.getInvoicePeriod ().get (0);
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      if (!aIP.isEmpty ())
        aOut.setPeriod (aIP);
    }
    for (final AllowanceChargeType aAC : aLine.getAllowanceCharge ())
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aOut.charges ().add (_readLineCharge (aAC));
      else
        aOut.allowances ().add (_readLineAllowance (aAC));
    if (aLine.getPrice () != null)
      aOut.setPrice (_readPriceDetails (aLine.getPrice ()));
    if (aLine.getItem () != null)
    {
      aOut.setItem (_readItem (aLine.getItem ()));
      // BG-30 from ClassifiedTaxCategory of the item
      if (!aLine.getItem ().getClassifiedTaxCategory ().isEmpty ())
      {
        final TaxCategoryType aTC = aLine.getItem ().getClassifiedTaxCategory ().get (0);
        final PhloxLineVat aVat = new PhloxLineVat ();
        aVat.setCategoryCode (str (aTC.getIDValue ()));
        if (aTC.getPercent () != null)
          aVat.setRate (aTC.getPercent ().getValue ());
        aOut.setLineVat (aVat);
      }
    }
    return aOut;
  }

  @NonNull
  private static PhloxInvoiceLine _readCreditNoteLine (@NonNull final CreditNoteLineType aLine)
  {
    final PhloxInvoiceLine aOut = new PhloxInvoiceLine ();
    aOut.setID (str (aLine.getIDValue ()));
    if (!aLine.getNote ().isEmpty ())
      aOut.setNote (str (aLine.getNote ().get (0).getValue ()));
    if (aLine.getCreditedQuantity () != null)
    {
      aOut.setQuantity (aLine.getCreditedQuantity ().getValue ());
      aOut.setQuantityUnit (aLine.getCreditedQuantity ().getUnitCode ());
    }
    if (aLine.getLineExtensionAmount () != null)
      aOut.setNetAmount (aLine.getLineExtensionAmount ().getValue ());
    aOut.setAccountingCost (str (aLine.getAccountingCostValue ()));
    if (!aLine.getOrderLineReference ().isEmpty ())
      aOut.setPurchaseOrderLineRef (str (aLine.getOrderLineReference ().get (0).getLineIDValue ()));
    if (!aLine.getDocumentReference ().isEmpty () && aLine.getDocumentReference ().get (0).getID () != null)
    {
      final DocumentReferenceType aDR = aLine.getDocumentReference ().get (0);
      aOut.setObjectIdentifier (identifier (aDR.getID ()));
    }
    if (!aLine.getInvoicePeriod ().isEmpty ())
    {
      final PeriodType aPeriod = aLine.getInvoicePeriod ().get (0);
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      if (!aIP.isEmpty ())
        aOut.setPeriod (aIP);
    }
    for (final AllowanceChargeType aAC : aLine.getAllowanceCharge ())
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aOut.charges ().add (_readLineCharge (aAC));
      else
        aOut.allowances ().add (_readLineAllowance (aAC));
    if (aLine.getPrice () != null)
      aOut.setPrice (_readPriceDetails (aLine.getPrice ()));
    if (aLine.getItem () != null)
    {
      aOut.setItem (_readItem (aLine.getItem ()));
      if (!aLine.getItem ().getClassifiedTaxCategory ().isEmpty ())
      {
        final TaxCategoryType aTC = aLine.getItem ().getClassifiedTaxCategory ().get (0);
        final PhloxLineVat aVat = new PhloxLineVat ();
        aVat.setCategoryCode (str (aTC.getIDValue ()));
        if (aTC.getPercent () != null)
          aVat.setRate (aTC.getPercent ().getValue ());
        aOut.setLineVat (aVat);
      }
    }
    return aOut;
  }

  @NonNull
  private static PhloxInvoiceNote _parseInvoiceNote (@NonNull final String sRaw)
  {
    // UBL convention: subject code embedded as "#code#" prefix; e.g. "#AAB#Some note"
    if (sRaw.startsWith ("#"))
    {
      final int n = sRaw.indexOf ('#', 1);
      if (n > 1)
        return new PhloxInvoiceNote (sRaw.substring (1, n), sRaw.substring (n + 1));
    }
    return new PhloxInvoiceNote (null, sRaw);
  }

  @Override
  @NonNull
  protected String _getUblVersion ()
  {
    return "2.1";
  }

  /**
   * Read an already-parsed UBL Invoice into a {@link PhloxInvoice}.
   *
   * @param aInvoice
   *        The JAXB invoice. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readInvoice (@NonNull final InvoiceType aInvoice)
  {
    final PhloxInvoice aModel = new PhloxInvoice ();
    aModel.setKind (EPhloxDocumentKind.INVOICE);
    aModel.setEdition (EEN16931Edition.EN2017);

    // BT-1
    aModel.setNumber (str (aInvoice.getIDValue ()));
    // BT-2
    aModel.setIssueDate (d (aInvoice.getIssueDateValue ()));
    // BT-3
    aModel.setTypeCode (strFromCode (aInvoice.getInvoiceTypeCodeValue ()));
    // BG-1 — notes
    for (final NoteType aNote : aInvoice.getNote ())
      if (aNote.getValue () != null)
        aModel.invoiceNotes ().add (_parseInvoiceNote (aNote.getValue ()));
    // BT-5 / BT-6
    aModel.setCurrency (aInvoice.getDocumentCurrencyCodeValue ());
    aModel.setVatAccountingCurrency (aInvoice.getTaxCurrencyCodeValue ());
    // BT-7
    aModel.setVatPointDate (d (aInvoice.getTaxPointDateValue ()));
    // BT-9 (Invoice: header DueDate)
    aModel.setPaymentDueDate (d (aInvoice.getDueDateValue ()));
    // BT-10
    _addBuyerReference (aModel, str (aInvoice.getBuyerReferenceValue ()));
    // BT-19
    aModel.setBuyerAccountingReference (str (aInvoice.getAccountingCostValue ()));
    // BG-2
    aModel.getProcessControl ().setBusinessProcessType (str (aInvoice.getProfileIDValue ()));
    aModel.getProcessControl ().setSpecificationIdentifier (str (aInvoice.getCustomizationIDValue ()));
    // BT-13
    if (aInvoice.getOrderReference () != null)
    {
      final OrderReferenceType aOrder = aInvoice.getOrderReference ();
      aModel.setPurchaseOrderReference (str (aOrder.getIDValue ()));
      aModel.setSalesOrderReference (str (aOrder.getSalesOrderIDValue ()));
    }
    // BT-12 — contract reference
    if (!aInvoice.getContractDocumentReference ().isEmpty ())
      aModel.setContractReference (str (aInvoice.getContractDocumentReference ().get (0).getIDValue ()));
    // BT-15 — receiving advice
    if (!aInvoice.getReceiptDocumentReference ().isEmpty ())
      aModel.setReceivingAdviceReference (str (aInvoice.getReceiptDocumentReference ().get (0).getIDValue ()));
    // BT-16 — despatch advice
    if (!aInvoice.getDespatchDocumentReference ().isEmpty ())
      aModel.setDespatchAdviceReference (str (aInvoice.getDespatchDocumentReference ().get (0).getIDValue ()));
    // BT-17 — tender or lot
    if (!aInvoice.getOriginatorDocumentReference ().isEmpty ())
      aModel.setTenderOrLotReference (str (aInvoice.getOriginatorDocumentReference ().get (0).getIDValue ()));
    // BT-11 — project reference
    if (!aInvoice.getProjectReference ().isEmpty () && aInvoice.getProjectReference ().get (0).getID () != null)
      aModel.setProjectReference (aInvoice.getProjectReference ().get (0).getID ().getValue ());

    // BT-18 + BG-24 — additional document references
    for (final DocumentReferenceType aRef : aInvoice.getAdditionalDocumentReference ())
    {
      final String sTypeCode = aRef.getDocumentTypeCodeValue ();
      if ("130".equals (sTypeCode))
      {
        // BT-18: Invoiced object identifier
        if (aRef.getID () != null)
          aModel.setInvoicedObjectIdentifier (identifier (aRef.getID ()));
      }
      else
      {
        // BG-24: supporting document
        aModel.supportingDocuments ().add (_readSupportingDocument (aRef));
      }
    }

    // BG-3 — preceding invoice references
    for (final BillingReferenceType aBR : aInvoice.getBillingReference ())
      if (aBR.getInvoiceDocumentReference () != null)
      {
        final DocumentReferenceType aDR = aBR.getInvoiceDocumentReference ();
        aModel.precedingInvoiceRefs ()
              .add (new PhloxPrecedingInvoiceRef ().setNumber (str (aDR.getIDValue ()))
                                                   .setIssueDate (d (aDR.getIssueDateValue ())));
      }

    // BG-4 + BG-5 + BG-6 — Seller
    if (aInvoice.getAccountingSupplierParty () != null)
      _readSeller (aInvoice.getAccountingSupplierParty (), aModel.getSeller ());
    // BG-7 + BG-8 + BG-9 — Buyer
    if (aInvoice.getAccountingCustomerParty () != null)
      _readBuyer (aInvoice.getAccountingCustomerParty (), aModel.getBuyer ());
    // BG-10 — Payee
    if (aInvoice.getPayeeParty () != null)
      aModel.setPayee (_readPayee (aInvoice.getPayeeParty ()));
    // BG-11 + BG-12 — Seller tax representative
    if (aInvoice.getTaxRepresentativeParty () != null)
      aModel.setSellerTaxRepresentative (_readSellerTaxRepresentative (aInvoice.getTaxRepresentativeParty ()));
    // BG-13 + BG-15 — Delivery
    if (!aInvoice.getDelivery ().isEmpty ())
    {
      final PhloxDeliveryInformation aDI = _readDelivery (aInvoice.getDelivery ().get (0));
      if (!aDI.isEmpty ())
        aModel.setDeliveryInformation (aDI);
    }
    // BG-14 — Invoicing period
    if (!aInvoice.getInvoicePeriod ().isEmpty ())
    {
      final PeriodType aPeriod = aInvoice.getInvoicePeriod ().get (0);
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      // BT-8 — VAT point date code (lives in InvoicePeriod for UBL)
      aModel.setVatPointDateCode (str ((aPeriod.getDescriptionCode ().isEmpty () ? null
                                                                                  : aPeriod.getDescriptionCode ()
                                                                                           .get (0)
                                                                                           .getValue ())));
      if (!aIP.isEmpty ())
        aModel.setInvoicingPeriod (aIP);
    }

    // BG-16/17/18/19 — Payment instructions
    if (!aInvoice.getPaymentMeans ().isEmpty ())
      aModel.setPaymentInstructions (_readPaymentInstructions (aInvoice.getPaymentMeans (),
                                                               aInvoice.getAccountingSupplierParty (),
                                                               aInvoice.getPayeeParty ()));
    // BT-20 — payment terms (text)
    if (!aInvoice.getPaymentTerms ().isEmpty ())
    {
      final PaymentTermsType aPT = aInvoice.getPaymentTerms ().get (0);
      if (!aPT.getNote ().isEmpty ())
        aModel.paymentTerms ().add (new PhloxPaymentTerms ().setText (joinNotes (aPT.getNote ())));
    }

    // BG-20 / BG-21 — document-level allowances and charges
    for (final AllowanceChargeType aAC : aInvoice.getAllowanceCharge ())
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aModel.documentCharges ().add (_readDocumentCharge (aAC));
      else
        aModel.documentAllowances ().add (_readDocumentAllowance (aAC));

    // BG-22 — totals
    if (aInvoice.getLegalMonetaryTotal () != null)
      _readTotals (aInvoice.getLegalMonetaryTotal (), aModel);

    // BG-23 — VAT breakdowns
    _readTaxTotals (aInvoice.getTaxTotal (), aModel);

    // BG-25 — lines
    for (final InvoiceLineType aLine : aInvoice.getInvoiceLine ())
      aModel.lines ().add (_readInvoiceLine (aLine));

    return aModel;
  }

  @Override
  @Nullable
  protected PhloxInvoice readInvoiceDocument (@NonNull final Document aDoc, @NonNull final ErrorList aErrors)
  {
    final InvoiceType aInvoice = UBL21Marshaller.invoice ().read (aDoc);
    return aInvoice == null ? null : readInvoice (aInvoice);
  }

  /**
   * Read an already-parsed UBL CreditNote into a {@link PhloxInvoice}.
   *
   * @param aCreditNote
   *        The JAXB credit note.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readCreditNote (@NonNull final CreditNoteType aCreditNote)
  {
    final PhloxInvoice aModel = new PhloxInvoice ();
    aModel.setKind (EPhloxDocumentKind.CREDIT_NOTE);
    aModel.setEdition (EEN16931Edition.EN2017);

    aModel.setNumber (str (aCreditNote.getIDValue ()));
    aModel.setIssueDate (d (aCreditNote.getIssueDateValue ()));
    aModel.setTypeCode (strFromCode (aCreditNote.getCreditNoteTypeCodeValue ()));
    for (final NoteType aNote : aCreditNote.getNote ())
      if (aNote.getValue () != null)
        aModel.invoiceNotes ().add (_parseInvoiceNote (aNote.getValue ()));
    aModel.setCurrency (aCreditNote.getDocumentCurrencyCodeValue ());
    aModel.setVatAccountingCurrency (aCreditNote.getTaxCurrencyCodeValue ());
    aModel.setVatPointDate (d (aCreditNote.getTaxPointDateValue ()));
    _addBuyerReference (aModel, str (aCreditNote.getBuyerReferenceValue ()));
    aModel.setBuyerAccountingReference (str (aCreditNote.getAccountingCostValue ()));
    aModel.getProcessControl ().setBusinessProcessType (str (aCreditNote.getProfileIDValue ()));
    aModel.getProcessControl ().setSpecificationIdentifier (str (aCreditNote.getCustomizationIDValue ()));
    if (aCreditNote.getOrderReference () != null)
    {
      final OrderReferenceType aOrder = aCreditNote.getOrderReference ();
      aModel.setPurchaseOrderReference (str (aOrder.getIDValue ()));
      aModel.setSalesOrderReference (str (aOrder.getSalesOrderIDValue ()));
    }
    if (!aCreditNote.getContractDocumentReference ().isEmpty ())
      aModel.setContractReference (str (aCreditNote.getContractDocumentReference ().get (0).getIDValue ()));
    if (!aCreditNote.getReceiptDocumentReference ().isEmpty ())
      aModel.setReceivingAdviceReference (str (aCreditNote.getReceiptDocumentReference ().get (0).getIDValue ()));
    if (!aCreditNote.getDespatchDocumentReference ().isEmpty ())
      aModel.setDespatchAdviceReference (str (aCreditNote.getDespatchDocumentReference ().get (0).getIDValue ()));
    if (!aCreditNote.getOriginatorDocumentReference ().isEmpty ())
      aModel.setTenderOrLotReference (str (aCreditNote.getOriginatorDocumentReference ().get (0).getIDValue ()));

    // BT-18 + BG-24 from additional document references
    for (final DocumentReferenceType aRef : aCreditNote.getAdditionalDocumentReference ())
    {
      final String sTypeCode = aRef.getDocumentTypeCodeValue ();
      if ("130".equals (sTypeCode) || "ATS".equalsIgnoreCase (sTypeCode))
      {
        if (aRef.getID () != null)
          aModel.setInvoicedObjectIdentifier (identifier (aRef.getID ()));
      }
      else
      {
        aModel.supportingDocuments ().add (_readSupportingDocument (aRef));
      }
    }

    for (final BillingReferenceType aBR : aCreditNote.getBillingReference ())
      if (aBR.getInvoiceDocumentReference () != null)
      {
        final DocumentReferenceType aDR = aBR.getInvoiceDocumentReference ();
        aModel.precedingInvoiceRefs ()
              .add (new PhloxPrecedingInvoiceRef ().setNumber (str (aDR.getIDValue ()))
                                                   .setIssueDate (d (aDR.getIssueDateValue ())));
      }

    if (aCreditNote.getAccountingSupplierParty () != null)
      _readSeller (aCreditNote.getAccountingSupplierParty (), aModel.getSeller ());
    if (aCreditNote.getAccountingCustomerParty () != null)
      _readBuyer (aCreditNote.getAccountingCustomerParty (), aModel.getBuyer ());
    if (aCreditNote.getPayeeParty () != null)
      aModel.setPayee (_readPayee (aCreditNote.getPayeeParty ()));
    if (aCreditNote.getTaxRepresentativeParty () != null)
      aModel.setSellerTaxRepresentative (_readSellerTaxRepresentative (aCreditNote.getTaxRepresentativeParty ()));
    if (!aCreditNote.getDelivery ().isEmpty ())
    {
      final PhloxDeliveryInformation aDI = _readDelivery (aCreditNote.getDelivery ().get (0));
      if (!aDI.isEmpty ())
        aModel.setDeliveryInformation (aDI);
    }
    if (!aCreditNote.getInvoicePeriod ().isEmpty ())
    {
      final PeriodType aPeriod = aCreditNote.getInvoicePeriod ().get (0);
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      aModel.setVatPointDateCode (str ((aPeriod.getDescriptionCode ().isEmpty () ? null
                                                                                  : aPeriod.getDescriptionCode ()
                                                                                           .get (0)
                                                                                           .getValue ())));
      if (!aIP.isEmpty ())
        aModel.setInvoicingPeriod (aIP);
    }

    if (!aCreditNote.getPaymentMeans ().isEmpty ())
    {
      aModel.setPaymentInstructions (_readPaymentInstructions (aCreditNote.getPaymentMeans (),
                                                               aCreditNote.getAccountingSupplierParty (),
                                                               aCreditNote.getPayeeParty ()));
      // BT-9 in CN: PaymentMeans/PaymentDueDate
      for (final PaymentMeansType aPM : aCreditNote.getPaymentMeans ())
        if (d (aPM.getPaymentDueDateValue ()) != null)
        {
          aModel.setPaymentDueDate (d (aPM.getPaymentDueDateValue ()));
          break;
        }
    }
    if (!aCreditNote.getPaymentTerms ().isEmpty ())
    {
      final PaymentTermsType aPT = aCreditNote.getPaymentTerms ().get (0);
      if (!aPT.getNote ().isEmpty ())
        aModel.paymentTerms ().add (new PhloxPaymentTerms ().setText (joinNotes (aPT.getNote ())));
    }

    for (final AllowanceChargeType aAC : aCreditNote.getAllowanceCharge ())
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aModel.documentCharges ().add (_readDocumentCharge (aAC));
      else
        aModel.documentAllowances ().add (_readDocumentAllowance (aAC));

    if (aCreditNote.getLegalMonetaryTotal () != null)
      _readTotals (aCreditNote.getLegalMonetaryTotal (), aModel);
    _readTaxTotals (aCreditNote.getTaxTotal (), aModel);

    for (final CreditNoteLineType aLine : aCreditNote.getCreditNoteLine ())
      aModel.lines ().add (_readCreditNoteLine (aLine));

    return aModel;
  }

  @Override
  @Nullable
  protected PhloxInvoice readCreditNoteDocument (@NonNull final Document aDoc, @NonNull final ErrorList aErrors)
  {
    final CreditNoteType aCreditNote = UBL21Marshaller.creditNote ().read (aDoc);
    return aCreditNote == null ? null : readCreditNote (aCreditNote);
  }
}
