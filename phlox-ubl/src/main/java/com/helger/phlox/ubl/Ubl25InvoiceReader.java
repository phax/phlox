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
import java.time.OffsetTime;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.datetime.xml.XMLOffsetTime;
import com.helger.diagnostics.error.SingleError;
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
import com.helger.phlox.api.model.PhloxEarlyPaymentDiscount;
import com.helger.phlox.api.model.PhloxIdentifier;
import com.helger.phlox.api.model.PhloxInvoice;
import com.helger.phlox.api.model.PhloxInvoiceLine;
import com.helger.phlox.api.model.PhloxInvoiceNote;
import com.helger.phlox.api.model.PhloxInvoicingPeriod;
import com.helger.phlox.api.model.PhloxItem;
import com.helger.phlox.api.model.PhloxItemAttribute;
import com.helger.phlox.api.model.PhloxItemClassification;
import com.helger.phlox.api.model.PhloxLatePaymentPenalty;
import com.helger.phlox.api.model.PhloxLineAllowance;
import com.helger.phlox.api.model.PhloxLineCharge;
import com.helger.phlox.api.model.PhloxLineDelivery;
import com.helger.phlox.api.model.PhloxLinePrecedingInvoiceRef;
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
import com.helger.phlox.api.model.PhloxThirdPartyCharge;
import com.helger.phlox.api.model.PhloxVatBreakdown;
import com.helger.ubl25.UBL25Marshaller;
import com.helger.xsds.ccts.cct.schemamodule.AmountType;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AddressLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AddressType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AllowanceChargeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AnnotationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.AttachmentType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.BillingReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.BranchType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.BuyerAssignedReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CardAccountType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CommodityClassificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ContactType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CountryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CreditNoteLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.CustomerPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.DeliveryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.DocumentReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ExchangeRateType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ExternalReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.FinancialAccountType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.InvoiceLineType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ItemIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ItemPropertyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ItemType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.LineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.MonetaryTotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.OrderLineReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.OrderReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyIdentificationType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyLegalEntityType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyNameType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyTaxSchemeType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentMandateType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentMeansType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PaymentTermsType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PeriodType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.PriceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.ProjectReferenceType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.SupplierPartyType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxCategoryType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxSubtotalType;
import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_25.TaxTotalType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_25.BuyerReferenceType;
import oasis.names.specification.ubl.schema.xsd.creditnote_25.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_25.InvoiceType;

/**
 * Reads UBL 2.5 Invoice and CreditNote documents (EN 16931:2026 binding) into a
 * {@link PhloxInvoice}. Used by {@link UblInvoiceReader}.
 *
 * @author Philip Helger
 */
final class Ubl25InvoiceReader extends AbstractUblInvoiceReader
{
  /** UNTDID 5153 list identifier used to tell BT-177/BT-193 from the reason codes BT-105/BT-145. */
  private static final String NON_VAT_TAX_CODE_LIST_ID = "5153";

  Ubl25InvoiceReader ()
  {}

  private static void _readIssueTime (@Nullable final XMLOffsetTime aTime,
                                      @NonNull final PhloxInvoice aModel,
                                      @NonNull final ErrorList aErrors)
  {
    // BT-166 — without a time offset the time cannot be represented as OffsetTime
    if (aTime != null)
      if (aTime.hasOffset ())
        aModel.setIssueTime (OffsetTime.of (aTime.toLocalTime (), aTime.getOffset ()));
      else
        aErrors.add (SingleError.builderWarn ()
                                .errorText ("The invoice issue time (BT-166) '" +
                                            aTime +
                                            "' has no time offset and is therefore ignored")
                                .build ());
  }

  private static void _readAnnotations (@NonNull final List <AnnotationType> aAnnotations,
                                        @NonNull final PhloxInvoice aModel)
  {
    // BG-1 — BT-21 subject code and BT-22 note
    for (final AnnotationType aAnnotation : aAnnotations)
    {
      final String sSubjectCode = str (aAnnotation.getSubjectCodeValue ());
      // Repeated content is joined with newlines, like in CII
      final String sContent = str (joinNotes (aAnnotation.getAnnotationContent ()));
      if (sSubjectCode != null || sContent != null)
        aModel.invoiceNotes ().add (new PhloxInvoiceNote (sSubjectCode, sContent));
    }
  }

  private static void _readBuyerReferences (@NonNull final List <BuyerAssignedReferenceType> aRefs,
                                            @NonNull final PhloxInvoice aModel)
  {
    // BT-10 with BT-10-1 (UNTDID 1153 scheme identifier)
    for (final BuyerAssignedReferenceType aRef : aRefs)
    {
      final String sScheme = str (aRef.getBuyerReferenceCodeValue ());
      for (final BuyerReferenceType aBuyerReference : aRef.getBuyerReference ())
      {
        final String sValue = str (aBuyerReference.getValue ());
        if (sValue != null)
          aModel.buyerReferences ().add (new PhloxIdentifier (sValue, sScheme));
      }
    }
  }

  private static void _readExchangeRate (@Nullable final ExchangeRateType aRate, @NonNull final PhloxInvoice aModel)
  {
    // BT-167 with BT-167-1 (target currency) and BT-167-2 (source currency)
    if (aRate != null)
    {
      aModel.setVatExchangeRate (aRate.getCalculationRateValue ());
      aModel.setVatExchangeRateTargetCurrency (str (aRate.getTargetCurrencyCodeValue ()));
      aModel.setVatExchangeRateSourceCurrency (str (aRate.getSourceCurrencyCodeValue ()));
    }
  }

  private static void _readPaymentTerms (@NonNull final List <PaymentTermsType> aPaymentTerms,
                                         @NonNull final PhloxInvoice aModel)
  {
    // BG-33 PAYMENT TERMS with BG-35 EARLY PAYMENT DISCOUNT and BG-36 LATE PAYMENT PENALTY
    for (final PaymentTermsType aPT : aPaymentTerms)
    {
      final PhloxPaymentTerms aOut = new PhloxPaymentTerms ();
      // BT-20
      if (!aPT.getNote ().isEmpty ())
        aOut.setText (joinNotes (aPT.getNote ()));

      // BG-35
      final PhloxEarlyPaymentDiscount aDiscount = new PhloxEarlyPaymentDiscount ();
      if (aPT.getSettlementPeriod () != null)
        aDiscount.setEndDate (d (aPT.getSettlementPeriod ().getEndDateValue ()));
      aDiscount.setPercentage (aPT.getSettlementDiscountPercentValue ());
      if (aPT.getSettlementDiscountAmount () != null)
        aDiscount.setAmount (aPT.getSettlementDiscountAmount ().getValue ());
      if (aDiscount.getEndDate () != null || aDiscount.getPercentage () != null || aDiscount.getAmount () != null)
        aOut.earlyDiscounts ().add (aDiscount);

      // BG-36
      final PhloxLatePaymentPenalty aPenalty = new PhloxLatePaymentPenalty ();
      if (aPT.getPenaltyPeriod () != null)
        aPenalty.setStartDate (d (aPT.getPenaltyPeriod ().getStartDateValue ()));
      if (aPT.getPenaltyInterestRate () != null)
        aPenalty.setYearlyInterestPercentage (aPT.getPenaltyInterestRate ().getInterestRatePercentValue ());
      if (aPT.getPenaltyAmount () != null)
        aPenalty.setAmount (aPT.getPenaltyAmount ().getValue ());
      if (aPenalty.getStartDate () != null ||
          aPenalty.getYearlyInterestPercentage () != null ||
          aPenalty.getAmount () != null)
        aOut.latePenalties ().add (aPenalty);

      if (aOut.getText () != null || !aOut.earlyDiscounts ().isEmpty () || !aOut.latePenalties ().isEmpty ())
        aModel.paymentTerms ().add (aOut);
    }
  }

  private static void _readThirdPartyCharge (@Nullable final AmountType aAmount,
                                             @Nullable final ItemType aItem,
                                             @NonNull final PhloxInvoice aModel)
  {
    // BG-34 — BT-179 charge amount and BT-180 charges specification
    final PhloxThirdPartyCharge aOut = new PhloxThirdPartyCharge ();
    if (aAmount != null)
      aOut.setAmount (aAmount.getValue ());
    if (aItem != null && !aItem.getDescription ().isEmpty ())
      aOut.setSpecification (str (aItem.getDescription ().get (0).getValue ()));
    if (aOut.getAmount () != null || aOut.getSpecification () != null)
      aModel.getDocumentTotals ().thirdPartyCharges ().add (aOut);
  }

  private static void _readPrecedingInvoiceRefs (@NonNull final List <BillingReferenceType> aRefs,
                                                 @NonNull final PhloxInvoice aModel)
  {
    // BG-3 — BT-25, BT-26 and BT-202
    for (final BillingReferenceType aBR : aRefs)
      if (aBR.getInvoiceDocumentReference () != null)
      {
        final DocumentReferenceType aDR = aBR.getInvoiceDocumentReference ();
        aModel.precedingInvoiceRefs ()
              .add (new PhloxPrecedingInvoiceRef ().setNumber (str (aDR.getIDValue ()))
                                                   .setIssueDate (d (aDR.getIssueDateValue ()))
                                                   .setTypeCode (str (aDR.getDocumentTypeCodeValue ())));
      }
  }

  private static void _readInvoicePeriod (@NonNull final List <PeriodType> aPeriods, @NonNull final PhloxInvoice aModel)
  {
    // BG-14
    if (!aPeriods.isEmpty ())
    {
      final PeriodType aPeriod = aPeriods.get (0);
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      // BT-8 — VAT point date code (lives in InvoicePeriod for UBL)
      aModel.setVatPointDateCode (str (aPeriod.getDescriptionCode ().isEmpty () ? null
                                                                                 : aPeriod.getDescriptionCode ()
                                                                                          .get (0)
                                                                                          .getValue ()));
      if (!aIP.isEmpty ())
        aModel.setInvoicingPeriod (aIP);
    }
  }

  private static void _readCommonReferences (@Nullable final OrderReferenceType aOrder,
                                             @NonNull final List <DocumentReferenceType> aContract,
                                             @NonNull final List <DocumentReferenceType> aReceipt,
                                             @NonNull final List <DocumentReferenceType> aDespatch,
                                             @NonNull final List <DocumentReferenceType> aDeliveryNote,
                                             @NonNull final List <DocumentReferenceType> aOriginator,
                                             @NonNull final List <ProjectReferenceType> aProject,
                                             @NonNull final PhloxInvoice aModel)
  {
    // BT-13 / BT-14
    if (aOrder != null)
    {
      aModel.setPurchaseOrderReference (str (aOrder.getIDValue ()));
      aModel.setSalesOrderReference (str (aOrder.getSalesOrderIDValue ()));
    }
    // BT-12 — contract reference
    if (!aContract.isEmpty ())
      aModel.setContractReference (str (aContract.get (0).getIDValue ()));
    // BT-15 — receiving advice
    if (!aReceipt.isEmpty ())
      aModel.setReceivingAdviceReference (str (aReceipt.get (0).getIDValue ()));
    // BT-16 — despatch advice
    if (!aDespatch.isEmpty ())
      aModel.setDespatchAdviceReference (str (aDespatch.get (0).getIDValue ()));
    // BT-197 — delivery note reference
    if (!aDeliveryNote.isEmpty ())
      aModel.setDeliveryNoteReference (str (aDeliveryNote.get (0).getIDValue ()));
    // BT-17 — tender or lot
    if (!aOriginator.isEmpty ())
      aModel.setTenderOrLotReference (str (aOriginator.get (0).getIDValue ()));
    // BT-11 — project reference
    if (!aProject.isEmpty ())
      aModel.setProjectReference (str (aProject.get (0).getIDValue ()));
  }

  @NonNull
  private static PhloxPostalAddress _readAddress (@NonNull final AddressType aAddr)
  {
    final PhloxPostalAddress aOut = new PhloxPostalAddress ();
    aOut.setLine1 (firstText (aAddr.getStreetName ()));
    aOut.setLine2 (firstText (aAddr.getAdditionalStreetName ()));
    if (!aAddr.getAddressLine ().isEmpty ())
    {
      final AddressLineType aAL = aAddr.getAddressLine ().get (0);
      aOut.setLine3 (firstText (aAL.getLine ()));
    }
    aOut.setCity (firstText (aAddr.getCityName ()));
    aOut.setPostCode (firstText (aAddr.getPostalZone ()));
    aOut.setCountrySubdivision (firstText (aAddr.getCountrySubentity ()));
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
      aSeller.setAdditionalLegalInfo (firstText (aLE.getCompanyLegalForm ()));
    }
    // BT-28
    if (!aParty.getPartyName ().isEmpty ())
      aSeller.setTradingName (str (aParty.getPartyName ().get (0).getNameValue ()));
    // BT-29
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      if (aPI.getID () != null && !SEPA_SCHEME_ID.equals (aPI.getID ().getSchemeID ()))
        aSeller.identifiers ().add (identifier (aPI.getID ()));
    // BT-31 (tax scheme "VAT") / BT-32 + BT-32-1 (tax scheme "LOC")
    for (final PartyTaxSchemeType aPTS : aParty.getPartyTaxScheme ())
      if (aPTS.getTaxScheme () != null)
      {
        if ("VAT".equals (aPTS.getTaxScheme ().getIDValue ()))
          aSeller.setVatIdentifier (str (aPTS.getCompanyIDValue ()));
        else
        {
          aSeller.setTaxRegistration (str (aPTS.getCompanyIDValue ()));
          if (aPTS.getCompanyID () != null)
            aSeller.setTaxRegistrationSchemeID (str (aPTS.getCompanyID ().getSchemeID ()));
        }
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

  private static void _readParties (@Nullable final SupplierPartyType aSupplier,
                                    @Nullable final CustomerPartyType aCustomer,
                                    @Nullable final PartyType aPayee,
                                    @Nullable final PartyType aTaxRepresentative,
                                    @NonNull final PhloxInvoice aModel)
  {
    // BG-4 + BG-5 + BG-6 — Seller
    if (aSupplier != null)
      _readSeller (aSupplier, aModel.getSeller ());
    // BG-7 + BG-8 + BG-9 — Buyer
    if (aCustomer != null)
      _readBuyer (aCustomer, aModel.getBuyer ());
    // BG-10 — Payee
    if (aPayee != null)
      aModel.setPayee (_readPayee (aPayee));
    // BG-11 + BG-12 — Seller tax representative
    if (aTaxRepresentative != null)
      aModel.setSellerTaxRepresentative (_readSellerTaxRepresentative (aTaxRepresentative));
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

  private static void _readHeaderDelivery (@NonNull final List <DeliveryType> aDeliveries,
                                           @NonNull final PhloxInvoice aModel)
  {
    // BG-13 + BG-15
    if (!aDeliveries.isEmpty ())
    {
      final PhloxDeliveryInformation aDI = _readDelivery (aDeliveries.get (0));
      if (!aDI.isEmpty ())
        aModel.setDeliveryInformation (aDI);
    }
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
      if (aCardMeans == null && !aOnePM.getCardAccount ().isEmpty ())
        aCardMeans = aOnePM;
      if (aMandateMeans == null && aOnePM.getPaymentMandate () != null)
        aMandateMeans = aOnePM;
    }

    // BG-18 — payment card
    if (aCardMeans != null)
    {
      final CardAccountType aCardAccount = aCardMeans.getCardAccount ().get (0);
      final PhloxPaymentCard aCard = new PhloxPaymentCard ();
      aCard.setPan (str (aCardAccount.getPrimaryAccountNumberIDValue ()));
      aCard.setHolderName (str (aCardAccount.getHolderNameValue ()));
      aOut.setPaymentCard (aCard);
    }

    // BG-19 — direct debit
    if (aMandateMeans != null)
    {
      final PaymentMandateType aMandate = aMandateMeans.getPaymentMandate ();
      final PhloxDirectDebit aDD = new PhloxDirectDebit ();
      aDD.setMandateReferenceID (str (aMandate.getIDValue ()));
      if (aMandate.getPayerFinancialAccount () != null)
      {
        final FinancialAccountType aPayerAccount = aMandate.getPayerFinancialAccount ();
        aDD.setDebitedAccountID (str (aPayerAccount.getIDValue ()));
        // BT-216 — debited account name
        aDD.setDebitedAccountName (str (aPayerAccount.getNameValue ()));
        // BT-215 — debited account payment service provider identifier
        if (aPayerAccount.getFinancialInstitutionBranch () != null)
          aDD.setDebitedAccountProviderID (str (aPayerAccount.getFinancialInstitutionBranch ().getIDValue ()));
      }
      // BT-90 — bank assigned creditor id (UBL: from supplier/payee PartyIdentification with SEPA
      // scheme)
      aDD.setBankAssignedCreditorID (_findSepaCreditorID (aSupplier, aPayee));
      aOut.setDirectDebit (aDD);
    }
    return aOut;
  }

  /**
   * Get the first exemption reason text of the tax category or {@code null}.
   *
   * @return The first exemption reason text of the tax category or {@code null}.
   */
  @Nullable
  private static String _firstExemptionReason (@NonNull final TaxCategoryType aTC)
  {
    return aTC.getTaxExemptionReason ().isEmpty () ? null : str (aTC.getTaxExemptionReason ().get (0).getValue ());
  }

  /**
   * Check if the reason code of an allowance/charge is a non-VAT tax code.
   *
   * @return {@code true} if the reason code of the allowance/charge is a non-VAT tax code (UNTDID
   *         5153: BT-177 respectively BT-193) instead of an allowance/charge reason code.
   */
  private static boolean _isNonVatTaxCode (@NonNull final AllowanceChargeType aAC)
  {
    return aAC.getAllowanceChargeReasonCode () != null &&
           NON_VAT_TAX_CODE_LIST_ID.equals (aAC.getAllowanceChargeReasonCode ().getListID ());
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
      // BT-173
      aOut.setExemptionReasonText (_firstExemptionReason (aTC));
      // BT-174
      aOut.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
      // BT-213
      aOut.setGoodsServicesCode (str (aTC.getSupplyTypeCodeValue ()));
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
    // BT-105 (UNTDID 7161) or BT-177 + BT-177-1 (UNTDID 5153, told apart by @listID)
    if (_isNonVatTaxCode (aAC))
    {
      aOut.setNonVatTaxCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
      aOut.setNonVatTaxCodeListID (str (aAC.getAllowanceChargeReasonCode ().getListID ()));
    }
    else
      aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    if (aAC.getTaxCategory () != null && !aAC.getTaxCategory ().isEmpty ())
    {
      final TaxCategoryType aTC = aAC.getTaxCategory ().get (0);
      aOut.setVatCategoryCode (str (aTC.getIDValue ()));
      if (aTC.getPercent () != null)
        aOut.setVatRate (aTC.getPercent ().getValue ());
      // BT-175
      aOut.setExemptionReasonText (_firstExemptionReason (aTC));
      // BT-176
      aOut.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
      // BT-214
      aOut.setGoodsServicesCode (str (aTC.getSupplyTypeCodeValue ()));
    }
    return aOut;
  }

  private static void _readDocumentAllowanceCharges (@NonNull final List <AllowanceChargeType> aACs,
                                                     @NonNull final PhloxInvoice aModel)
  {
    // BG-20 / BG-21
    for (final AllowanceChargeType aAC : aACs)
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aModel.documentCharges ().add (_readDocumentCharge (aAC));
      else
        aModel.documentAllowances ().add (_readDocumentAllowance (aAC));
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
  private static PhloxVatBreakdown _readVatBreakdown (@NonNull final TaxSubtotalType aSub,
                                                      @Nullable final String sInvoiceCurrency)
  {
    final PhloxVatBreakdown aOut = new PhloxVatBreakdown ();
    if (aSub.getTaxableAmount () != null)
      aOut.setTaxableAmount (aSub.getTaxableAmount ().getValue ());
    if (aSub.getTaxAmount () != null)
      aOut.setTaxAmount (aSub.getTaxAmount ().getValue ());
    // BT-184 — only meaningful if it differs from the invoice currency BT-5
    if (aSub.getTaxAmount () != null)
    {
      final String sCurrencyID = str (aSub.getTaxAmount ().getCurrencyID ());
      if (sCurrencyID != null && !sCurrencyID.equals (sInvoiceCurrency))
        aOut.setCurrency (sCurrencyID);
    }
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
      // BT-210
      aOut.setGoodsServicesCode (str (aTC.getSupplyTypeCodeValue ()));
    }
    return aOut;
  }

  private static void _readTaxTotals (@NonNull final List <TaxTotalType> aTaxTotals, @NonNull final PhloxInvoice aModel)
  {
    final String sCurrency = aModel.getCurrency ();
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
        aModel.vatBreakdowns ().add (_readVatBreakdown (aSub, sCurrency));
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

  private static void _readAdditionalDocumentReferences (@NonNull final List <DocumentReferenceType> aRefs,
                                                         @NonNull final PhloxInvoice aModel)
  {
    // BT-18 (type code 130) and BG-24 (type code 916)
    for (final DocumentReferenceType aRef : aRefs)
      if ("130".equals (aRef.getDocumentTypeCodeValue ()))
      {
        // BT-18: Invoiced object identifier
        if (aRef.getID () != null)
          aModel.setInvoicedObjectIdentifier (identifier (aRef.getID ()));
      }
      else
        aModel.supportingDocuments ().add (_readSupportingDocument (aRef));
  }

  /**
   * Get the line delivery or {@code null} if the source carries none of its terms.
   *
   * @return The line delivery or {@code null} if the source carries none of its terms.
   */
  @Nullable
  private static PhloxLineDelivery _readLineDelivery (@NonNull final DeliveryType aDelivery)
  {
    final PhloxLineDelivery aOut = new PhloxLineDelivery ();
    boolean bAny = false;
    // BT-185
    if (aDelivery.getDeliveryParty () != null && !aDelivery.getDeliveryParty ().getPartyName ().isEmpty ())
    {
      aOut.setPartyName (str (aDelivery.getDeliveryParty ().getPartyName ().get (0).getNameValue ()));
      bAny |= aOut.getPartyName () != null;
    }
    if (aDelivery.getDeliveryLocation () != null)
    {
      // BT-186 + BT-186-1
      if (aDelivery.getDeliveryLocation ().getID () != null)
      {
        aOut.setLocationIdentifier (identifier (aDelivery.getDeliveryLocation ().getID ()));
        bAny = true;
      }
      // BG-38 — BT-203 to BT-209
      if (aDelivery.getDeliveryLocation ().getAddress () != null)
      {
        final PhloxPostalAddress aAddress = _readAddress (aDelivery.getDeliveryLocation ().getAddress ());
        if (!aAddress.isEmpty ())
        {
          aOut.setPostalAddress (aAddress);
          bAny = true;
        }
      }
    }
    // BT-187
    aOut.setActualDeliveryDate (d (aDelivery.getActualDeliveryDateValue ()));
    bAny |= aOut.getActualDeliveryDate () != null;
    return bAny ? aOut : null;
  }

  private static void _readLineReferences (@NonNull final List <OrderLineReferenceType> aOrderLineRefs,
                                           @NonNull final List <LineReferenceType> aDespatchLineRefs,
                                           @NonNull final List <LineReferenceType> aReceiptLineRefs,
                                           @NonNull final List <DeliveryType> aDeliveries,
                                           @NonNull final PhloxInvoiceLine aOut)
  {
    // BT-132 + BT-188 + BT-200 + BT-201 — order line reference
    if (!aOrderLineRefs.isEmpty ())
    {
      final OrderLineReferenceType aOLR = aOrderLineRefs.get (0);
      aOut.setPurchaseOrderLineRef (str (aOLR.getLineIDValue ()));
      aOut.setSalesOrderLineReference (str (aOLR.getSalesOrderLineIDValue ()));
      if (aOLR.getOrderReference () != null)
      {
        aOut.setPurchaseOrderReference (str (aOLR.getOrderReference ().getIDValue ()));
        aOut.setSalesOrderReference (str (aOLR.getOrderReference ().getSalesOrderIDValue ()));
      }
    }
    // BT-189 + BT-190 — despatch advice reference
    if (!aDespatchLineRefs.isEmpty ())
    {
      final LineReferenceType aLR = aDespatchLineRefs.get (0);
      aOut.setDespatchAdviceLineReference (str (aLR.getLineIDValue ()));
      if (aLR.getDocumentReference () != null)
        aOut.setDespatchAdviceReference (str (aLR.getDocumentReference ().getIDValue ()));
    }
    // BT-191 + BT-192 — receiving advice reference
    if (!aReceiptLineRefs.isEmpty ())
    {
      final LineReferenceType aLR = aReceiptLineRefs.get (0);
      aOut.setReceivingAdviceLineReference (str (aLR.getLineIDValue ()));
      if (aLR.getDocumentReference () != null)
        aOut.setReceivingAdviceReference (str (aLR.getDocumentReference ().getIDValue ()));
    }
    if (!aDeliveries.isEmpty ())
    {
      final DeliveryType aDelivery = aDeliveries.get (0);
      // BT-198 + BT-199 — delivery note reference
      if (!aDelivery.getDeliveryNoteDocumentReference ().isEmpty ())
        aOut.setDeliveryNoteReference (str (aDelivery.getDeliveryNoteDocumentReference ().get (0).getIDValue ()));
      if (!aDelivery.getDeliveryNoteLineReference ().isEmpty ())
        aOut.setDeliveryNoteLineReference (str (aDelivery.getDeliveryNoteLineReference ().get (0).getLineIDValue ()));
      // BG-37 + BG-38 — line delivery information
      final PhloxLineDelivery aLineDelivery = _readLineDelivery (aDelivery);
      if (aLineDelivery != null)
        aOut.setDelivery (aLineDelivery);
    }
  }

  private static void _readLinePrecedingInvoiceRefs (@NonNull final List <BillingReferenceType> aRefs,
                                                     @NonNull final PhloxInvoiceLine aOut)
  {
    // BG-39 — BT-217 to BT-220
    for (final BillingReferenceType aBR : aRefs)
      if (aBR.getInvoiceDocumentReference () != null)
      {
        final DocumentReferenceType aDR = aBR.getInvoiceDocumentReference ();
        final PhloxLinePrecedingInvoiceRef aRef = new PhloxLinePrecedingInvoiceRef ();
        aRef.setNumber (str (aDR.getIDValue ()));
        aRef.setIssueDate (d (aDR.getIssueDateValue ()));
        aRef.setTypeCode (str (aDR.getDocumentTypeCodeValue ()));
        if (!aBR.getBillingReferenceLine ().isEmpty ())
          aRef.setLineReference (str (aBR.getBillingReferenceLine ().get (0).getIDValue ()));
        aOut.precedingInvoiceRefs ().add (aRef);
      }
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
    // BT-145 (UNTDID 7161) or BT-193 + BT-193-1 (UNTDID 5153, told apart by @listID)
    if (_isNonVatTaxCode (aAC))
    {
      aOut.setNonVatTaxTypeCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
      aOut.setNonVatTaxTypeCodeListID (str (aAC.getAllowanceChargeReasonCode ().getListID ()));
    }
    else
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
    aOut.setName (firstText (aItem.getName ()));
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
    {
      final PhloxItemAttribute aAttr = new PhloxItemAttribute ();
      // BT-160
      aAttr.setName (str (aIP.getNameValue ()));
      // BT-211
      aAttr.setCode (str (aIP.getNameCodeValue ()));
      if (aIP.getValueQuantity () != null)
      {
        // BT-161b numeric value with BT-212 unit of measure code
        if (aIP.getValueQuantityValue () != null)
          aAttr.setValue (aIP.getValueQuantityValue ().toPlainString ());
        aAttr.setValueUnit (str (aIP.getValueQuantity ().getUnitCode ()));
      }
      else
        // BT-161a text value
        aAttr.setValue (str (aIP.getValueValue ()));
      aOut.attributes ().add (aAttr);
    }
    return aOut;
  }

  private static void _readLineCommon (@NonNull final List <DocumentReferenceType> aDocRefs,
                                       @NonNull final List <PeriodType> aPeriods,
                                       @NonNull final List <AllowanceChargeType> aACs,
                                       @Nullable final PriceType aPrice,
                                       @Nullable final ItemType aItem,
                                       @NonNull final PhloxInvoiceLine aOut)
  {
    // BT-128
    if (!aDocRefs.isEmpty () && aDocRefs.get (0).getID () != null)
    {
      final DocumentReferenceType aDR = aDocRefs.get (0);
      aOut.setObjectIdentifier (identifier (aDR.getID ()));
    }
    // BG-26
    if (!aPeriods.isEmpty ())
    {
      final PeriodType aPeriod = aPeriods.get (0);
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      if (!aIP.isEmpty ())
        aOut.setPeriod (aIP);
    }
    // BG-27 / BG-28
    for (final AllowanceChargeType aAC : aACs)
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aOut.charges ().add (_readLineCharge (aAC));
      else
        aOut.allowances ().add (_readLineAllowance (aAC));
    // BG-29
    if (aPrice != null)
      aOut.setPrice (_readPriceDetails (aPrice));
    if (aItem != null)
    {
      // BG-31 + BG-32
      aOut.setItem (_readItem (aItem));
      // BG-30 from ClassifiedTaxCategory of the item
      if (!aItem.getClassifiedTaxCategory ().isEmpty ())
      {
        final TaxCategoryType aTC = aItem.getClassifiedTaxCategory ().get (0);
        final PhloxLineVat aVat = new PhloxLineVat ();
        aVat.setCategoryCode (str (aTC.getIDValue ()));
        if (aTC.getPercent () != null)
          aVat.setRate (aTC.getPercent ().getValue ());
        // BT-194
        aVat.setExemptionReasonText (_firstExemptionReason (aTC));
        // BT-195
        aVat.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
        aOut.setLineVat (aVat);
        // BT-196 — read from the line VAT category, stored with the item
        aOut.getItem ().setGoodsServicesCode (str (aTC.getSupplyTypeCodeValue ()));
      }
    }
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
    _readLineReferences (aLine.getOrderLineReference (),
                         aLine.getDespatchLineReference (),
                         aLine.getReceiptLineReference (),
                         aLine.getDelivery (),
                         aOut);
    _readLinePrecedingInvoiceRefs (aLine.getBillingReference (), aOut);
    _readLineCommon (aLine.getDocumentReference (),
                     aLine.getInvoicePeriod (),
                     aLine.getAllowanceCharge (),
                     aLine.getPrice (),
                     aLine.getItem (),
                     aOut);
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
    _readLineReferences (aLine.getOrderLineReference (),
                         aLine.getDespatchLineReference (),
                         aLine.getReceiptLineReference (),
                         aLine.getDelivery (),
                         aOut);
    _readLinePrecedingInvoiceRefs (aLine.getBillingReference (), aOut);
    _readLineCommon (aLine.getDocumentReference (),
                     aLine.getInvoicePeriod (),
                     aLine.getAllowanceCharge (),
                     aLine.getPrice (),
                     aLine.getItem (),
                     aOut);
    return aOut;
  }

  @Override
  @NonNull
  protected String _getUblVersion ()
  {
    return "2.5";
  }

  /**
   * Read an already-parsed UBL 2.5 Invoice into a {@link PhloxInvoice}.
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
    final PhloxInvoice aModel = new PhloxInvoice ();
    aModel.setKind (EPhloxDocumentKind.INVOICE);
    aModel.setEdition (EEN16931Edition.EN2026);

    // BT-1
    aModel.setNumber (str (aInvoice.getIDValue ()));
    // BT-2 + BT-166
    aModel.setIssueDate (d (aInvoice.getIssueDateValue ()));
    _readIssueTime (aInvoice.getIssueTimeValue (), aModel, aErrors);
    // BT-3
    aModel.setTypeCode (str (aInvoice.getInvoiceTypeCodeValue ()));
    // BG-1 — notes
    _readAnnotations (aInvoice.getAnnotation (), aModel);
    // BT-5 / BT-6 / BT-167
    aModel.setCurrency (aInvoice.getDocumentCurrencyCodeValue ());
    aModel.setVatAccountingCurrency (aInvoice.getTaxCurrencyCodeValue ());
    _readExchangeRate (aInvoice.getTaxExchangeRate (), aModel);
    // BT-7
    aModel.setVatPointDate (d (aInvoice.getTaxPointDateValue ()));
    // BT-9 (Invoice: header DueDate)
    aModel.setPaymentDueDate (d (aInvoice.getDueDateValue ()));
    // BT-10
    _readBuyerReferences (aInvoice.getBuyerAssignedReference (), aModel);
    // BT-19
    aModel.setBuyerAccountingReference (str (aInvoice.getAccountingCostValue ()));
    // BG-2
    aModel.getProcessControl ().setBusinessProcessType (str (aInvoice.getProfileIDValue ()));
    aModel.getProcessControl ().setSpecificationIdentifier (str (aInvoice.getCustomizationIDValue ()));
    // BT-11 to BT-17, BT-197
    _readCommonReferences (aInvoice.getOrderReference (),
                           aInvoice.getContractDocumentReference (),
                           aInvoice.getReceiptDocumentReference (),
                           aInvoice.getDespatchDocumentReference (),
                           aInvoice.getDeliveryNoteDocumentReference (),
                           aInvoice.getOriginatorDocumentReference (),
                           aInvoice.getProjectReference (),
                           aModel);
    // BT-18 + BG-24
    _readAdditionalDocumentReferences (aInvoice.getAdditionalDocumentReference (), aModel);
    // BG-3
    _readPrecedingInvoiceRefs (aInvoice.getBillingReference (), aModel);
    // BG-4 to BG-12
    _readParties (aInvoice.getAccountingSupplierParty (),
                  aInvoice.getAccountingCustomerParty (),
                  aInvoice.getPayeeParty (),
                  aInvoice.getTaxRepresentativeParty (),
                  aModel);
    // BG-13 + BG-15 — Delivery
    _readHeaderDelivery (aInvoice.getDelivery (), aModel);
    // BG-14 — Invoicing period
    _readInvoicePeriod (aInvoice.getInvoicePeriod (), aModel);

    // BG-16/17/18/19 — Payment instructions
    if (!aInvoice.getPaymentMeans ().isEmpty ())
      aModel.setPaymentInstructions (_readPaymentInstructions (aInvoice.getPaymentMeans (),
                                                               aInvoice.getAccountingSupplierParty (),
                                                               aInvoice.getPayeeParty ()));
    // BG-33 / BG-35 / BG-36 — payment terms
    _readPaymentTerms (aInvoice.getPaymentTerms (), aModel);

    // BG-20 / BG-21 — document-level allowances and charges
    _readDocumentAllowanceCharges (aInvoice.getAllowanceCharge (), aModel);

    // BG-22 — totals
    if (aInvoice.getLegalMonetaryTotal () != null)
      _readTotals (aInvoice.getLegalMonetaryTotal (), aModel);
    // BG-34 — charges on behalf of a third party
    for (final InvoiceLineType aCollectionLine : aInvoice.getCollectionInvoiceLine ())
      _readThirdPartyCharge (aCollectionLine.getTaxInclusiveLineExtensionAmount (), aCollectionLine.getItem (), aModel);

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
    final InvoiceType aInvoice = UBL25Marshaller.invoice ().read (aDoc);
    return aInvoice == null ? null : readInvoice (aInvoice, aErrors);
  }

  /**
   * Read an already-parsed UBL 2.5 CreditNote into a {@link PhloxInvoice}.
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
    final PhloxInvoice aModel = new PhloxInvoice ();
    aModel.setKind (EPhloxDocumentKind.CREDIT_NOTE);
    aModel.setEdition (EEN16931Edition.EN2026);

    aModel.setNumber (str (aCreditNote.getIDValue ()));
    aModel.setIssueDate (d (aCreditNote.getIssueDateValue ()));
    _readIssueTime (aCreditNote.getIssueTimeValue (), aModel, aErrors);
    aModel.setTypeCode (str (aCreditNote.getCreditNoteTypeCodeValue ()));
    _readAnnotations (aCreditNote.getAnnotation (), aModel);
    aModel.setCurrency (aCreditNote.getDocumentCurrencyCodeValue ());
    aModel.setVatAccountingCurrency (aCreditNote.getTaxCurrencyCodeValue ());
    _readExchangeRate (aCreditNote.getTaxExchangeRate (), aModel);
    aModel.setVatPointDate (d (aCreditNote.getTaxPointDateValue ()));
    // BT-9 in CN: cbc:DueDate since UBL 2.2, same as the invoice
    aModel.setPaymentDueDate (d (aCreditNote.getDueDateValue ()));
    _readBuyerReferences (aCreditNote.getBuyerAssignedReference (), aModel);
    aModel.setBuyerAccountingReference (str (aCreditNote.getAccountingCostValue ()));
    aModel.getProcessControl ().setBusinessProcessType (str (aCreditNote.getProfileIDValue ()));
    aModel.getProcessControl ().setSpecificationIdentifier (str (aCreditNote.getCustomizationIDValue ()));
    _readCommonReferences (aCreditNote.getOrderReference (),
                           aCreditNote.getContractDocumentReference (),
                           aCreditNote.getReceiptDocumentReference (),
                           aCreditNote.getDespatchDocumentReference (),
                           aCreditNote.getDeliveryNoteDocumentReference (),
                           aCreditNote.getOriginatorDocumentReference (),
                           aCreditNote.getProjectReference (),
                           aModel);
    _readAdditionalDocumentReferences (aCreditNote.getAdditionalDocumentReference (), aModel);
    _readPrecedingInvoiceRefs (aCreditNote.getBillingReference (), aModel);
    _readParties (aCreditNote.getAccountingSupplierParty (),
                  aCreditNote.getAccountingCustomerParty (),
                  aCreditNote.getPayeeParty (),
                  aCreditNote.getTaxRepresentativeParty (),
                  aModel);
    _readHeaderDelivery (aCreditNote.getDelivery (), aModel);
    _readInvoicePeriod (aCreditNote.getInvoicePeriod (), aModel);

    if (!aCreditNote.getPaymentMeans ().isEmpty ())
      aModel.setPaymentInstructions (_readPaymentInstructions (aCreditNote.getPaymentMeans (),
                                                               aCreditNote.getAccountingSupplierParty (),
                                                               aCreditNote.getPayeeParty ()));
    _readPaymentTerms (aCreditNote.getPaymentTerms (), aModel);

    _readDocumentAllowanceCharges (aCreditNote.getAllowanceCharge (), aModel);

    if (aCreditNote.getLegalMonetaryTotal () != null)
      _readTotals (aCreditNote.getLegalMonetaryTotal (), aModel);
    for (final CreditNoteLineType aCollectionLine : aCreditNote.getCollectionCreditNoteLine ())
      _readThirdPartyCharge (aCollectionLine.getTaxInclusiveLineExtensionAmount (), aCollectionLine.getItem (), aModel);
    _readTaxTotals (aCreditNote.getTaxTotal (), aModel);

    for (final CreditNoteLineType aLine : aCreditNote.getCreditNoteLine ())
      aModel.lines ().add (_readCreditNoteLine (aLine));

    return aModel;
  }

  @Override
  @Nullable
  protected PhloxInvoice readCreditNoteDocument (@NonNull final Document aDoc, @NonNull final ErrorList aErrors)
  {
    final CreditNoteType aCreditNote = UBL25Marshaller.creditNote ().read (aDoc);
    return aCreditNote == null ? null : readCreditNote (aCreditNote, aErrors);
  }
}
