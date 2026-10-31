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
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931DateFormatCode;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.en16931.basics.codelist.EEN16931InvoiceTypeCode;
import com.helger.en16931.basics.codelist.EEN16931InvoiceTypeCodeRole;
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

import un.unece.uncefact.data.standard.cii.d25a.CrossIndustryInvoiceType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.AllowanceChargeReasonCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.CurrencyCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.DocumentCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.FormattedDateTimeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.PaymentMeansCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.ReferenceCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.TaxCategoryCodeType;
import un.unece.uncefact.data.standard.cii.d25a.qdt.TimeReferenceCodeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.CreditorFinancialAccountType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.CreditorFinancialInstitutionType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DebtorFinancialAccountType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DebtorFinancialInstitutionType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.DocumentLineDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ExchangedDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.FinancialAdjustmentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeAgreementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeDeliveryType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.HeaderTradeSettlementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LegalOrganizationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LineTradeAgreementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LineTradeDeliveryType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.LineTradeSettlementType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.NoteType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ProcuringProjectType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ProductCharacteristicType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ProductClassificationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.ReferencedDocumentType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SpecifiedPeriodType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SupplyChainEventType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SupplyChainTradeLineItemType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.SupplyChainTradeTransactionType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TaxRegistrationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeAddressType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeAllowanceChargeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeContactType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeCurrencyExchangeType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePartyType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentDiscountTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentPenaltyTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePaymentTermsType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradePriceType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeProductType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementHeaderMonetarySummationType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeSettlementPaymentMeansType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.TradeTaxType;
import un.unece.uncefact.data.standard.cii.d25a.rabie.UniversalCommunicationType;
import un.unece.uncefact.data.standard.cii.d25a.udt.AmountType;
import un.unece.uncefact.data.standard.cii.d25a.udt.CodeType;
import un.unece.uncefact.data.standard.cii.d25a.udt.DateTimeType;
import un.unece.uncefact.data.standard.cii.d25a.udt.DateType;
import un.unece.uncefact.data.standard.cii.d25a.udt.IDType;
import un.unece.uncefact.data.standard.cii.d25a.udt.QuantityType;
import un.unece.uncefact.data.standard.cii.d25a.udt.TextType;

/**
 * Reads CII D25A {@code CrossIndustryInvoice} documents (EN 16931:2026) into a
 * {@link PhloxInvoice}. Package-private worker of {@link CiiInvoiceReader}.
 *
 * @author Philip Helger
 */
final class CiiD25AInvoiceReader extends AbstractCiiInvoiceReader
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CiiD25AInvoiceReader.class);

  /** UNTDID 5153 list identifier that marks BT-177 / BT-193 non-VAT tax codes. */
  private static final String NON_VAT_TAX_CODE_LIST_ID = "5153";

  /** Format code of a date with time and offset (UN/EDIFACT 2379), used for BT-166. */
  private static final String FORMAT_CODE_DATE_TIME = EEN16931DateFormatCode.CCYYMMDDHHMMSSZHHMM.getID ();

  CiiD25AInvoiceReader ()
  {}

  /**
   * Get the {@code true} if the reason code carries {@code @listID="5153"} (UNTDID 5153 duty or tax.
   *
   * @return {@code true} if the reason code carries {@code @listID="5153"} (UNTDID 5153 duty or tax
   *         type), i.e. it is a non-VAT tax code (BT-177 / BT-193) and not a charge reason code.
   *         The CII schema declares a default for {@code @listID}, so only the fixed value counts.
   */
  private static boolean _isNonVatTaxCodeList (@Nullable final AllowanceChargeReasonCodeType aCode)
  {
    return aCode != null && NON_VAT_TAX_CODE_LIST_ID.equals (aCode.getListID ());
  }

  private static void _readTotals (@NonNull final TradeSettlementHeaderMonetarySummationType aS,
                                   @NonNull final PhloxInvoice aModel)
  {
    if (!aS.getLineTotalAmount ().isEmpty ())
      aModel.getDocumentTotals ().setLineNetTotal (aS.getLineTotalAmount ().get (0).getValue ());
    if (!aS.getAllowanceTotalAmount ().isEmpty ())
      aModel.getDocumentTotals ().setAllowanceTotal (aS.getAllowanceTotalAmount ().get (0).getValue ());
    if (!aS.getChargeTotalAmount ().isEmpty ())
      aModel.getDocumentTotals ().setChargeTotal (aS.getChargeTotalAmount ().get (0).getValue ());
    if (!aS.getTaxBasisTotalAmount ().isEmpty ())
      aModel.getDocumentTotals ().setTaxExclusiveTotal (aS.getTaxBasisTotalAmount ().get (0).getValue ());
    if (!aS.getGrandTotalAmount ().isEmpty ())
      aModel.getDocumentTotals ().setTaxInclusiveTotal (aS.getGrandTotalAmount ().get (0).getValue ());
    if (!aS.getTotalPrepaidAmount ().isEmpty ())
      aModel.getDocumentTotals ().setPrepaidAmount (aS.getTotalPrepaidAmount ().get (0).getValue ());
    if (!aS.getRoundingAmount ().isEmpty ())
      aModel.getDocumentTotals ().setRoundingAmount (aS.getRoundingAmount ().get (0).getValue ());
    if (!aS.getDuePayableAmount ().isEmpty ())
      aModel.getDocumentTotals ().setPayableAmount (aS.getDuePayableAmount ().get (0).getValue ());

    // BT-110 (main) / BT-111 (accounting currency)
    final String sAcct = aModel.getVatAccountingCurrency ();
    for (final AmountType aA : aS.getTaxTotalAmount ())
    {
      if (isVatAccountingAmount (aModel.getCurrency (), sAcct, aA.getCurrencyID ()))
        aModel.getDocumentTotals ().setTaxTotalAccountingCurrency (aA.getValue ());
      else
        if (aModel.getDocumentTotals ().getTaxTotal () == null)
          aModel.getDocumentTotals ().setTaxTotal (aA.getValue ());
    }
  }

  @NonNull
  private static PhloxPriceDetails _readPrice (@NonNull final LineTradeAgreementType aLTA)
  {
    final PhloxPriceDetails aOut = new PhloxPriceDetails ();
    final TradePriceType aNet = aLTA.getNetPriceProductTradePrice ();
    if (aNet != null)
    {
      if (!aNet.getChargeAmount ().isEmpty ())
        aOut.setNetPrice (aNet.getChargeAmount ().get (0).getValue ());
      if (aNet.getBasisQuantity () != null)
      {
        aOut.setBaseQuantity (aNet.getBasisQuantity ().getValue ());
        aOut.setBaseQuantityUnit (aNet.getBasisQuantity ().getUnitCode ());
      }
    }
    final TradePriceType aGross = aLTA.getGrossPriceProductTradePrice ();
    if (aGross != null)
    {
      if (!aGross.getChargeAmount ().isEmpty ())
        aOut.setGrossPrice (aGross.getChargeAmount ().get (0).getValue ());
      for (final TradeAllowanceChargeType aAC : aGross.getAppliedTradeAllowanceCharge ())
        if (aAC.getChargeIndicator () == null || !Boolean.TRUE.equals (aAC.getChargeIndicator ().isIndicator ()))
        {
          if (!aAC.getActualAmount ().isEmpty ())
            aOut.setDiscount (aAC.getActualAmount ().get (0).getValue ());
          break;
        }
      // BT-149 / BT-150 — since EN 16931:2026 the base quantity may sit on the gross price as well
      if (aOut.getBaseQuantity () == null && aGross.getBasisQuantity () != null)
      {
        aOut.setBaseQuantity (aGross.getBasisQuantity ().getValue ());
        aOut.setBaseQuantityUnit (aGross.getBasisQuantity ().getUnitCode ());
      }
    }
    return aOut;
  }

  @Nullable
  private static String _codeValue (@Nullable final AllowanceChargeReasonCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final CurrencyCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final DocumentCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final PaymentMeansCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final ReferenceCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final TaxCategoryCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final TimeReferenceCodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _codeValue (@Nullable final CodeType aCode)
  {
    return aCode == null ? null : str (aCode.getValue ());
  }

  @Nullable
  private static String _idValue (@Nullable final IDType aID)
  {
    return aID == null ? null : str (aID.getValue ());
  }

  /**
   * Read the party identifiers (BT-29, BT-46, BT-60, BT-71 and their scheme identifiers). Since EN
   * 16931:2026 only {@code ram:GlobalID} carries them; {@code ram:ID} is only used as a fallback
   * for documents that still follow the 2017 binding.
   */
  private static void _readPartyIdentifiers (@NonNull final TradePartyType aTP,
                                             @NonNull final List <PhloxIdentifier> aTarget)
  {
    for (final IDType aID : aTP.getGlobalID ())
      aTarget.add (new PhloxIdentifier (_idValue (aID), str (aID.getSchemeID ())));
    if (aTarget.isEmpty ())
      for (final IDType aID : aTP.getID ())
        aTarget.add (new PhloxIdentifier (_idValue (aID), str (aID.getSchemeID ())));
  }

  @Nullable
  private static String _text (@Nullable final TextType aT)
  {
    return aT == null ? null : str (aT.getValue ());
  }

  @NonNull
  private static PhloxPayee _readPayee (@NonNull final TradePartyType aTP)
  {
    final PhloxPayee aOut = new PhloxPayee ();
    aOut.setName (_text (aTP.getName ()));
    final ICommonsList <PhloxIdentifier> aIDs = new CommonsArrayList <> ();
    _readPartyIdentifiers (aTP, aIDs);
    if (!aIDs.isEmpty ())
      aOut.setIdentifier (aIDs.getFirstOrNull ());
    final LegalOrganizationType aLO = aTP.getSpecifiedLegalOrganization ();
    if (aLO != null && aLO.getID () != null)
      aOut.setLegalRegistration (new PhloxIdentifier (aLO.getID ().getValue (), aLO.getID ().getSchemeID ()));
    return aOut;
  }

  @NonNull
  private static PhloxPostalAddress _readAddress (@NonNull final TradeAddressType aAddr)
  {
    final PhloxPostalAddress aOut = new PhloxPostalAddress ();
    aOut.setLine1 (_text (aAddr.getLineOne ()));
    aOut.setLine2 (_text (aAddr.getLineTwo ()));
    aOut.setLine3 (_text (aAddr.getLineThree ()));
    aOut.setCity (_text (aAddr.getCityName ()));
    aOut.setPostCode (_codeValue (aAddr.getPostcodeCode ()));
    if (aAddr.getCountryID () != null)
      aOut.setCountryCode (aAddr.getCountryID ().getValue ());
    if (!aAddr.getCountrySubDivisionName ().isEmpty ())
      aOut.setCountrySubdivision (_text (aAddr.getCountrySubDivisionName ().get (0)));
    return aOut;
  }

  @NonNull
  private static PhloxSellerTaxRepresentative _readTaxRep (@NonNull final TradePartyType aTP)
  {
    final PhloxSellerTaxRepresentative aOut = new PhloxSellerTaxRepresentative ();
    aOut.setName (_text (aTP.getName ()));
    for (final TaxRegistrationType aTR : aTP.getSpecifiedTaxRegistration ())
      if (aTR.getID () != null && "VA".equalsIgnoreCase (aTR.getID ().getSchemeID ()))
        aOut.setVatIdentifier (aTR.getID ().getValue ());
    if (aTP.getPostalTradeAddress () != null)
      aOut.setPostalAddress (_readAddress (aTP.getPostalTradeAddress ()));
    return aOut;
  }

  @NonNull
  private static PhloxContact _readContact (@NonNull final TradeContactType aTC)
  {
    final PhloxContact aOut = new PhloxContact ();
    // BT-41 / BT-56 — PersonName or DepartmentName (CII-SR-502 / CII-SR-503)
    final String sPersonName = _text (aTC.getPersonName ());
    aOut.setName (sPersonName != null ? sPersonName : _text (aTC.getDepartmentName ()));
    if (aTC.getTelephoneUniversalCommunication () != null)
      aOut.setTelephone (_text (aTC.getTelephoneUniversalCommunication ().getCompleteNumber ()));
    if (aTC.getEmailURIUniversalCommunication () != null &&
        aTC.getEmailURIUniversalCommunication ().getURIID () != null)
      aOut.setEmail (aTC.getEmailURIUniversalCommunication ().getURIID ().getValue ());
    return aOut;
  }

  private static void _readSeller (@NonNull final TradePartyType aTP, @NonNull final PhloxSeller aSeller)
  {
    aSeller.setName (_text (aTP.getName ())); // BT-27
    final LegalOrganizationType aLO = aTP.getSpecifiedLegalOrganization ();
    if (aLO != null)
    {
      aSeller.setTradingName (_text (aLO.getTradingBusinessName ())); // BT-28
      if (aLO.getID () != null)
        aSeller.setLegalRegistration (new PhloxIdentifier (aLO.getID ().getValue (), aLO.getID ().getSchemeID ()));
    }
    // BT-29 / BT-29-1
    _readPartyIdentifiers (aTP, aSeller.identifiers ());
    // BT-31 / BT-32 / BT-32-1 via SpecifiedTaxRegistration schemeID
    for (final TaxRegistrationType aTR : aTP.getSpecifiedTaxRegistration ())
      if (aTR.getID () != null)
      {
        final String sScheme = aTR.getID ().getSchemeID ();
        if ("VA".equalsIgnoreCase (sScheme))
          aSeller.setVatIdentifier (_idValue (aTR.getID ()));
        else
          if (aSeller.getTaxRegistration () == null)
          {
            // "FC" by default; special schemes from ISO/IEC 6523 may be used as well
            aSeller.setTaxRegistration (_idValue (aTR.getID ()));
            aSeller.setTaxRegistrationSchemeID (str (sScheme));
          }
      }
    // BT-33
    if (!aTP.getDescription ().isEmpty ())
      aSeller.setAdditionalLegalInfo (_text (aTP.getDescription ().get (0)));
    // BT-34 / BT-34-1
    for (final UniversalCommunicationType aUC : aTP.getURIUniversalCommunication ())
      if (aUC.getURIID () != null)
      {
        aSeller.setElectronicAddress (new PhloxIdentifier (aUC.getURIID ().getValue (),
                                                           aUC.getURIID ().getSchemeID ()));
        break;
      }
    // BG-5
    if (aTP.getPostalTradeAddress () != null)
      aSeller.setPostalAddress (_readAddress (aTP.getPostalTradeAddress ()));
    // BG-6
    if (!aTP.getDefinedTradeContact ().isEmpty ())
      aSeller.setContact (_readContact (aTP.getDefinedTradeContact ().get (0)));
  }

  private static void _readBuyer (@NonNull final TradePartyType aTP, @NonNull final PhloxBuyer aBuyer)
  {
    aBuyer.setName (_text (aTP.getName ())); // BT-44
    final LegalOrganizationType aLO = aTP.getSpecifiedLegalOrganization ();
    if (aLO != null)
    {
      aBuyer.setTradingName (_text (aLO.getTradingBusinessName ()));
      if (aLO.getID () != null)
        aBuyer.setLegalRegistration (new PhloxIdentifier (aLO.getID ().getValue (), aLO.getID ().getSchemeID ()));
    }
    _readPartyIdentifiers (aTP, aBuyer.identifiers ());
    for (final TaxRegistrationType aTR : aTP.getSpecifiedTaxRegistration ())
      if (aTR.getID () != null && "VA".equalsIgnoreCase (aTR.getID ().getSchemeID ()))
        aBuyer.setVatIdentifier (aTR.getID ().getValue ());
    for (final UniversalCommunicationType aUC : aTP.getURIUniversalCommunication ())
      if (aUC.getURIID () != null)
      {
        aBuyer.setElectronicAddress (new PhloxIdentifier (aUC.getURIID ().getValue (), aUC.getURIID ().getSchemeID ()));
        break;
      }
    if (aTP.getPostalTradeAddress () != null)
      aBuyer.setPostalAddress (_readAddress (aTP.getPostalTradeAddress ()));
    if (!aTP.getDefinedTradeContact ().isEmpty ())
      aBuyer.setContact (_readContact (aTP.getDefinedTradeContact ().get (0)));
  }

  @NonNull
  private static PhloxDocumentAllowance _readDocAllowance (@NonNull final TradeAllowanceChargeType aAC)
  {
    final PhloxDocumentAllowance aOut = new PhloxDocumentAllowance ();
    if (!aAC.getActualAmount ().isEmpty ())
      aOut.setAmount (aAC.getActualAmount ().get (0).getValue ());
    if (aAC.getBasisAmount () != null)
      aOut.setBaseAmount (aAC.getBasisAmount ().getValue ());
    if (aAC.getCalculationPercent () != null)
      aOut.setPercentage (aAC.getCalculationPercent ().getValue ());
    aOut.setReason (_text (aAC.getReason ()));
    aOut.setReasonCode (_codeValue (aAC.getReasonCode ()));
    if (!aAC.getCategoryTradeTax ().isEmpty ())
    {
      final TradeTaxType aTT = aAC.getCategoryTradeTax ().get (0);
      aOut.setVatCategoryCode (_codeValue (aTT.getCategoryCode ()));
      if (aTT.getRateApplicablePercent () != null)
        aOut.setVatRate (aTT.getRateApplicablePercent ().getValue ());
      aOut.setExemptionReasonText (_text (aTT.getExemptionReason ())); // BT-173
      aOut.setExemptionReasonCode (_codeValue (aTT.getExemptionReasonCode ())); // BT-174
      aOut.setGoodsServicesCode (_codeValue (aTT.getSupplyTypeCode ())); // BT-213
    }
    return aOut;
  }

  @NonNull
  private static PhloxDocumentCharge _readDocCharge (@NonNull final TradeAllowanceChargeType aAC)
  {
    final PhloxDocumentCharge aOut = new PhloxDocumentCharge ();
    if (!aAC.getActualAmount ().isEmpty ())
      aOut.setAmount (aAC.getActualAmount ().get (0).getValue ());
    if (aAC.getBasisAmount () != null)
      aOut.setBaseAmount (aAC.getBasisAmount ().getValue ());
    if (aAC.getCalculationPercent () != null)
      aOut.setPercentage (aAC.getCalculationPercent ().getValue ());
    aOut.setReason (_text (aAC.getReason ()));
    // BT-105 and BT-177 share ram:ReasonCode and are told apart by @listID
    final String sReasonCode = _codeValue (aAC.getReasonCode ());
    if (_isNonVatTaxCodeList (aAC.getReasonCode ()))
    {
      aOut.setNonVatTaxCode (sReasonCode);
      aOut.setNonVatTaxCodeListID (aAC.getReasonCode ().getListID ());
    }
    else
      aOut.setReasonCode (sReasonCode);
    if (!aAC.getCategoryTradeTax ().isEmpty ())
    {
      final TradeTaxType aTT = aAC.getCategoryTradeTax ().get (0);
      aOut.setVatCategoryCode (_codeValue (aTT.getCategoryCode ()));
      if (aTT.getRateApplicablePercent () != null)
        aOut.setVatRate (aTT.getRateApplicablePercent ().getValue ());
      aOut.setExemptionReasonText (_text (aTT.getExemptionReason ())); // BT-175
      aOut.setExemptionReasonCode (_codeValue (aTT.getExemptionReasonCode ())); // BT-176
      aOut.setGoodsServicesCode (_codeValue (aTT.getSupplyTypeCode ())); // BT-214
    }
    return aOut;
  }

  @NonNull
  private static PhloxSupportingDocument _readSupportingDocument (@NonNull final ReferencedDocumentType aRD)
  {
    final PhloxSupportingDocument aOut = new PhloxSupportingDocument ();
    aOut.setReference (_idValue (aRD.getIssuerAssignedID ()));
    if (!aRD.getName ().isEmpty ())
      aOut.setDescription (_text (aRD.getName ().get (0)));
    if (aRD.getURIID () != null)
      aOut.setExternalLocation (aRD.getURIID ().getValue ());
    if (!aRD.getAttachmentBinaryObject ().isEmpty ())
    {
      final PhloxAttachment aBin = new PhloxAttachment ();
      aBin.setBinary (aRD.getAttachmentBinaryObject ().get (0).getValue ());
      aBin.setMimeCode (aRD.getAttachmentBinaryObject ().get (0).getMimeCode ());
      aBin.setFilename (aRD.getAttachmentBinaryObject ().get (0).getFilename ());
      aOut.setAttachment (aBin);
    }
    return aOut;
  }

  /**
   * Parse a date string according to its UN/EDIFACT 2379 format code. Returns {@code null} when the
   * value or pattern cannot be resolved; logs a warning for unsupported format codes so operators
   * notice the data loss.
   */
  @Nullable
  private static LocalDate _parseFormatted (@Nullable final String sValue, @Nullable final String sFormat)
  {
    if (sValue == null || sValue.isEmpty ())
      return null;

    final EEN16931DateFormatCode eFormat = EEN16931DateFormatCode.getFromIDOrDefault (sFormat);
    if (eFormat == null)
    {
      LOGGER.warn ("Unsupported CII date format code '" + sFormat + "' on value '" + sValue + "'");
      return null;
    }
    final LocalDate ret = eFormat.parseLocalDate (sValue);
    if (ret == null)
      LOGGER.warn ("Could not parse CII date '" + sValue + "' with format code '" + eFormat.getID () + "'");
    return ret;
  }

  /**
   * Returns the {@code getValue()} of any CII code type. CII has dozens of qualified subtypes
   * (TaxCategoryCodeType, DocumentCodeType, ...), none of which share a Java base. Reflection keeps
   * the helper code small and avoids declaring 20 overloads.
   */
  /**
   * Parse a CII {@code DateTimeType} into a {@link LocalDate}. Reads the {@code @format} attribute
   * on the inner {@code DateTimeString} and dispatches to the right pattern; missing format
   * defaults to "102" (yyyyMMdd) per EN 16931.
   */
  @Nullable
  private static LocalDate _dateTime (@Nullable final DateTimeType aDT)
  {
    if (aDT == null || aDT.getDateTimeString () == null)
      return null;

    return _parseFormatted (aDT.getDateTimeString ().getValue (), aDT.getDateTimeString ().getFormat ());
  }

  @NonNull
  private static PhloxInvoicingPeriod _readPeriod (@NonNull final SpecifiedPeriodType aSP)
  {
    final PhloxInvoicingPeriod aOut = new PhloxInvoicingPeriod ();
    aOut.setStartDate (_dateTime (aSP.getStartDateTime ()));
    aOut.setEndDate (_dateTime (aSP.getEndDateTime ()));
    return aOut;
  }

  @NonNull
  private static PhloxLineDelivery _readLineDelivery (@NonNull final LineTradeDeliveryType aLTD)
  {
    final PhloxLineDelivery aOut = new PhloxLineDelivery ();
    final TradePartyType aShipTo = aLTD.getShipToTradeParty ();
    if (aShipTo != null)
    {
      aOut.setPartyName (_text (aShipTo.getName ())); // BT-185
      final ICommonsList <PhloxIdentifier> aIDs = new CommonsArrayList <> ();
      _readPartyIdentifiers (aShipTo, aIDs);
      if (!aIDs.isEmpty ())
        aOut.setLocationIdentifier (aIDs.getFirstOrNull ()); // BT-186 / BT-186-1
      if (aShipTo.getPostalTradeAddress () != null)
        aOut.setPostalAddress (_readAddress (aShipTo.getPostalTradeAddress ())); // BG-38
    }
    // BT-187
    if (aLTD.getActualDeliverySupplyChainEvent () != null)
      aOut.setActualDeliveryDate (_dateTime (aLTD.getActualDeliverySupplyChainEvent ().getOccurrenceDateTime ()));
    return aOut;
  }

  /**
   * Parse a CII {@code DateType} into a {@link LocalDate}. Honours the {@code @format} attribute on
   * the inner {@code DateString}; if {@code DateString} is absent the {@code DateLocal} value is
   * used.
   */
  @Nullable
  private static LocalDate _dateOnly (@Nullable final DateType aDT)
  {
    if (aDT == null)
      return null;

    if (aDT.getDateString () != null)
      return _parseFormatted (aDT.getDateString ().getValue (), aDT.getDateString ().getFormat ());
    return aDT.getDateLocal ();
  }

  /**
   * Parse a CII {@code FormattedDateTimeType} (qualified namespace) into a {@link LocalDate}.
   * Format codes follow the same UN/EDIFACT 2379 dispatch as {@link #_dateTime(DateTimeType)}.
   */
  @Nullable
  private static LocalDate _formattedDateTime (@Nullable final FormattedDateTimeType aFDT)
  {
    if (aFDT == null || aFDT.getDateTimeString () == null)
      return null;

    return _parseFormatted (aFDT.getDateTimeString ().getValue (), aFDT.getDateTimeString ().getFormat ());
  }

  /**
   * Read the combined BT-2 issue date and BT-166 issue time. Format code 208 carries date, time and
   * offset; every other format code only carries the date.
   */
  private static void _readIssueDateTime (@Nullable final DateTimeType aDT,
                                          @NonNull final PhloxInvoice aModel,
                                          @NonNull final ErrorList aErrors)
  {
    if (aDT == null || aDT.getDateTimeString () == null)
      return;

    final DateTimeType.DateTimeString aDTS = aDT.getDateTimeString ();
    if (FORMAT_CODE_DATE_TIME.equals (aDTS.getFormat ()))
    {
      final XMLOffsetDateTime aIssueDateTime = EEN16931DateFormatCode.CCYYMMDDHHMMSSZHHMM.parseDateTime (aDTS.getValue ());
      if (aIssueDateTime == null)
      {
        aErrors.add (SingleError.builderWarn ()
                                .errorText ("Could not parse the invoice issue date and time '" +
                                            aDTS.getValue () +
                                            "' (BT-2 / BT-166) with format code 208")
                                .build ());
        return;
      }
      aModel.setIssueDate (aIssueDateTime.toLocalDate ());
      aModel.setIssueTime (aIssueDateTime.toOffsetTime ());
    }
    else
      aModel.setIssueDate (_parseFormatted (aDTS.getValue (), aDTS.getFormat ()));
  }

  /**
   * Resolve the BT-3 type code into a document kind. Codes of the EN 16931 invoice type code
   * registry ({@link EEN16931InvoiceTypeCode}) with the role credit note become
   * {@link EPhloxDocumentKind#CREDIT_NOTE}, all other registry codes
   * {@link EPhloxDocumentKind#INVOICE}; unknown codes default to {@code INVOICE} and add a warning
   * to the error list.
   */
  @NonNull
  private static EPhloxDocumentKind _resolveKind (@Nullable final String sTypeCode, @NonNull final ErrorList aErrors)
  {
    if (sTypeCode == null || sTypeCode.isEmpty ())
      return EPhloxDocumentKind.INVOICE;
    final EEN16931InvoiceTypeCode eTypeCode = EEN16931InvoiceTypeCode.getFromIDOrNull (sTypeCode);
    if (eTypeCode != null)
      return eTypeCode.getRole () == EEN16931InvoiceTypeCodeRole.CREDIT_NOTE ? EPhloxDocumentKind.CREDIT_NOTE
                                                                             : EPhloxDocumentKind.INVOICE;
    aErrors.add (SingleError.builderWarn ()
                            .errorText ("Unknown BT-3 invoice type code '" + sTypeCode + "' — defaulting to INVOICE")
                            .build ());
    return EPhloxDocumentKind.INVOICE;
  }

  /**
   * Join the non-blank values of a list of {@link TextType} elements with newlines. Returns
   * {@code null} when the list is empty or contains no usable values.
   */
  @Nullable
  private static String _joinTexts (@Nullable final List <TextType> aTexts)
  {
    if (aTexts == null || aTexts.isEmpty ())
      return null;

    final StringBuilder aSB = new StringBuilder ();
    for (final TextType aText : aTexts)
    {
      final String sVal = _text (aText);
      if (sVal == null)
        continue;
      if (aSB.length () > 0)
        aSB.append ('\n');
      aSB.append (sVal);
    }
    return aSB.length () == 0 ? null : aSB.toString ();
  }

  /**
   * Read the BG-33 payment terms (BT-20) with the nested BG-35 early payment discount and BG-36
   * late payment penalty. One {@link PhloxPaymentTerms} is created per CII {@code
   * SpecifiedTradePaymentTerms} that carries a text, a discount or a penalty.
   */
  private static void _readPaymentTerms (@NonNull final List <TradePaymentTermsType> aTerms,
                                         @NonNull final PhloxInvoice aModel)
  {
    for (final TradePaymentTermsType aPT : aTerms)
    {
      final PhloxPaymentTerms aOut = new PhloxPaymentTerms ();
      aOut.setText (_joinTexts (aPT.getDescription ())); // BT-20

      final TradePaymentDiscountTermsType aDiscount = aPT.getApplicableTradePaymentDiscountTerms ();
      if (aDiscount != null)
      {
        final PhloxEarlyPaymentDiscount aED = new PhloxEarlyPaymentDiscount ();
        aED.setEndDate (_dateTime (aDiscount.getBasisDateTime ())); // BT-170
        if (aDiscount.getCalculationPercent () != null)
          aED.setPercentage (aDiscount.getCalculationPercent ().getValue ()); // BT-171
        if (aDiscount.getActualDiscountAmount () != null)
          aED.setAmount (aDiscount.getActualDiscountAmount ().getValue ()); // BT-172
        if (aED.getEndDate () != null || aED.getPercentage () != null || aED.getAmount () != null)
          aOut.earlyDiscounts ().add (aED);
      }

      final TradePaymentPenaltyTermsType aPenalty = aPT.getApplicableTradePaymentPenaltyTerms ();
      if (aPenalty != null)
      {
        final PhloxLatePaymentPenalty aLP = new PhloxLatePaymentPenalty ();
        aLP.setStartDate (_dateTime (aPenalty.getBasisDateTime ())); // BT-181
        if (aPenalty.getCalculationPercent () != null)
          aLP.setYearlyInterestPercentage (aPenalty.getCalculationPercent ().getValue ()); // BT-182
        if (aPenalty.getActualPenaltyAmount () != null)
          aLP.setAmount (aPenalty.getActualPenaltyAmount ().getValue ()); // BT-183
        if (aLP.getStartDate () != null || aLP.getYearlyInterestPercentage () != null || aLP.getAmount () != null)
          aOut.latePenalties ().add (aLP);
      }

      if (aOut.getText () != null || !aOut.earlyDiscounts ().isEmpty () || !aOut.latePenalties ().isEmpty ())
        aModel.paymentTerms ().add (aOut);
    }
  }

  private static void _readPaymentInstructions (@NonNull final HeaderTradeSettlementType aHTS,
                                                @NonNull final PhloxInvoice aModel)
  {
    final List <TradeSettlementPaymentMeansType> aMeans = aHTS.getSpecifiedTradeSettlementPaymentMeans ();
    final List <TradePaymentTermsType> aTerms = aHTS.getSpecifiedTradePaymentTerms ();

    // BG-33 / BG-35 / BG-36
    _readPaymentTerms (aTerms, aModel);

    // BT-9 — payment due date
    for (final TradePaymentTermsType aPT : aTerms)
      if (aPT.getDueDateDateTime () != null)
      {
        aModel.setPaymentDueDate (_dateTime (aPT.getDueDateDateTime ()));
        break;
      }

    if (aMeans.isEmpty () && aTerms.isEmpty () && aHTS.getPaymentReference ().isEmpty ())
      return;

    final PhloxPaymentInstructions aPI = new PhloxPaymentInstructions ();
    // BT-83 — remittance information lives on the parent. Multiple references are joined
    // with newlines so multi-part instructions (e.g. invoice number + PO reference) survive.
    aPI.setRemittanceInformation (_joinTexts (aHTS.getPaymentReference ()));
    if (!aMeans.isEmpty ())
    {
      final TradeSettlementPaymentMeansType aM = aMeans.get (0);
      aPI.setPaymentMeansCode (_codeValue (aM.getTypeCode ()));
      if (!aM.getInformation ().isEmpty ())
        aPI.setPaymentMeansText (_text (aM.getInformation ().get (0)));

      // BG-17 credit transfers across all payment means
      for (final TradeSettlementPaymentMeansType aOne : aMeans)
        for (final CreditorFinancialAccountType aFA : aOne.getPayeePartyCreditorFinancialAccount ())
        {
          final PhloxCreditTransfer aCT = new PhloxCreditTransfer ();
          aCT.setPaymentMeansCode (_codeValue (aOne.getTypeCode ()));
          // BT-84 — IBANID if applicable, ProprietaryID else
          aCT.setAccountIdentifier (aFA.getIBANID () != null ? _idValue (aFA.getIBANID ())
                                                             : _idValue (aFA.getProprietaryID ()));
          aCT.setAccountName (_text (aFA.getAccountName ()));
          final CreditorFinancialInstitutionType aFI = aOne.getPayeeSpecifiedCreditorFinancialInstitution ();
          if (aFI != null && aFI.getBICID () != null)
            aCT.setServiceProviderID (aFI.getBICID ().getValue ());
          aPI.creditTransfers ().add (aCT);
        }

      // BG-18 — card
      if (aM.getApplicableTradeSettlementFinancialCard () != null)
      {
        final PhloxPaymentCard aPC = new PhloxPaymentCard ();
        aPC.setPan (_idValue (aM.getApplicableTradeSettlementFinancialCard ().getID ()));
        aPC.setHolderName (_text (aM.getApplicableTradeSettlementFinancialCard ().getCardholderName ()));
        aPI.setPaymentCard (aPC);
      }
    }

    // BG-19 + BT-89 .. BT-91 + BT-215 + BT-216. BT-89 is optional, so the direct debit is also
    // recognised by the payer account or, for the creditor reference, by payment means code 59
    final PhloxDirectDebit aDD = new PhloxDirectDebit ();
    boolean bDirectDebit = false;
    for (final TradePaymentTermsType aPT : aTerms)
      if (!aPT.getDirectDebitMandateID ().isEmpty ())
      {
        aDD.setMandateReferenceID (_idValue (aPT.getDirectDebitMandateID ().get (0)));
        bDirectDebit = true;
        break;
      }
    // BT-91 / BT-216 from the payer financial account, BT-215 from the payer institution
    for (final TradeSettlementPaymentMeansType aM : aMeans)
    {
      final DebtorFinancialAccountType aFA = aM.getPayerPartyDebtorFinancialAccount ();
      if (aFA != null)
      {
        aDD.setDebitedAccountID (aFA.getIBANID () != null ? _idValue (aFA.getIBANID ())
                                                          : _idValue (aFA.getProprietaryID ()));
        aDD.setDebitedAccountName (_text (aFA.getAccountName ()));
      }
      final DebtorFinancialInstitutionType aFI = aM.getPayerSpecifiedDebtorFinancialInstitution ();
      if (aFI != null)
        aDD.setDebitedAccountProviderID (_idValue (aFI.getBICID ()));
      if (aFA != null || aFI != null)
      {
        bDirectDebit = true;
        break;
      }
    }
    // BT-90 — creditor reference id
    if (aHTS.getCreditorReferenceID () != null && (bDirectDebit || "59".equals (aPI.getPaymentMeansCode ())))
    {
      aDD.setBankAssignedCreditorID (_idValue (aHTS.getCreditorReferenceID ()));
      bDirectDebit = true;
    }
    if (bDirectDebit)
      aPI.setDirectDebit (aDD);

    if (aPI.getPaymentMeansCode () != null ||
        aPI.getRemittanceInformation () != null ||
        !aPI.creditTransfers ().isEmpty () ||
        aPI.getDirectDebit () != null)
      aModel.setPaymentInstructions (aPI);
  }

  @NonNull
  private static PhloxItem _readItem (@NonNull final TradeProductType aP)
  {
    final PhloxItem aOut = new PhloxItem ();
    if (!aP.getName ().isEmpty ())
      aOut.setName (_text (aP.getName ().get (0)));
    aOut.setDescription (_joinTexts (aP.getDescription ()));
    if (aP.getSellerAssignedID () != null)
      aOut.setSellerItemID (aP.getSellerAssignedID ().getValue ());
    if (aP.getBuyerAssignedID () != null)
      aOut.setBuyerItemID (aP.getBuyerAssignedID ().getValue ());
    if (aP.getGlobalID () != null)
      aOut.setStandardItemID (new PhloxIdentifier (aP.getGlobalID ().getValue (), aP.getGlobalID ().getSchemeID ()));
    if (aP.getOriginTradeCountry () != null && aP.getOriginTradeCountry ().getID () != null)
      aOut.setCountryOfOrigin (aP.getOriginTradeCountry ().getID ().getValue ());
    for (final ProductClassificationType aPC : aP.getDesignatedProductClassification ())
      if (aPC.getClassCode () != null)
      {
        final PhloxItemClassification aIC = new PhloxItemClassification ();
        aIC.setCode (aPC.getClassCode ().getValue ());
        aIC.setListID (aPC.getClassCode ().getListID ());
        aIC.setListVersionID (aPC.getClassCode ().getListVersionID ());
        aOut.classifications ().add (aIC);
      }
    for (final ProductCharacteristicType aPC : aP.getApplicableProductCharacteristic ())
    {
      final PhloxItemAttribute aAttr = new PhloxItemAttribute ();
      aAttr.setName (aPC.getDescription ().isEmpty () ? null : _text (aPC.getDescription ().get (0))); // BT-160
      aAttr.setCode (_codeValue (aPC.getTypeCode ())); // BT-211
      // BT-161a text or BT-161b numeric value with BT-212 as its unit (CII-SR-504)
      if (aPC.getValueMeasure () != null && aPC.getValueMeasure ().getValue () != null)
      {
        aAttr.setValue (aPC.getValueMeasure ().getValue ().toPlainString ());
        aAttr.setValueUnit (aPC.getValueMeasure ().getUnitCode ());
      }
      else
        aAttr.setValue (aPC.getValue ().isEmpty () ? null : _text (aPC.getValue ().get (0)));
      if (aAttr.getName () != null || aAttr.getCode () != null || aAttr.getValue () != null)
        aOut.attributes ().add (aAttr);
    }
    return aOut;
  }

  @NonNull
  private static PhloxInvoiceLine _readLine (@NonNull final SupplyChainTradeLineItemType aLine)
  {
    final PhloxInvoiceLine aOut = new PhloxInvoiceLine ();
    final DocumentLineDocumentType aLD = aLine.getAssociatedDocumentLineDocument ();
    if (aLD != null)
    {
      aOut.setID (_idValue (aLD.getLineID ()));
      if (!aLD.getIncludedNote ().isEmpty () && !aLD.getIncludedNote ().get (0).getContent ().isEmpty ())
        aOut.setNote (_text (aLD.getIncludedNote ().get (0).getContent ().get (0)));
    }
    final TradeProductType aTPr = aLine.getSpecifiedTradeProduct ();
    if (aTPr != null)
      aOut.setItem (_readItem (aTPr));
    final LineTradeAgreementType aLTA = aLine.getSpecifiedLineTradeAgreement ();
    if (aLTA != null)
    {
      aOut.setPrice (_readPrice (aLTA));
      final ReferencedDocumentType aBuyerOrder = aLTA.getBuyerOrderReferencedDocument ();
      if (aBuyerOrder != null)
      {
        aOut.setPurchaseOrderLineRef (_idValue (aBuyerOrder.getLineID ())); // BT-132
        aOut.setPurchaseOrderReference (_idValue (aBuyerOrder.getIssuerAssignedID ())); // BT-188
      }
      final ReferencedDocumentType aSellerOrder = aLTA.getSellerOrderReferencedDocument ();
      if (aSellerOrder != null)
      {
        aOut.setSalesOrderReference (_idValue (aSellerOrder.getIssuerAssignedID ())); // BT-200
        aOut.setSalesOrderLineReference (_idValue (aSellerOrder.getLineID ())); // BT-201
      }
    }
    final LineTradeDeliveryType aLTD = aLine.getSpecifiedLineTradeDelivery ();
    if (aLTD != null)
    {
      if (aLTD.getBilledQuantity () != null)
      {
        final QuantityType aQ = aLTD.getBilledQuantity ();
        aOut.setQuantity (aQ.getValue ());
        aOut.setQuantityUnit (aQ.getUnitCode ());
      }
      // BT-189 / BT-190
      if (aLTD.getDespatchAdviceReferencedDocument () != null)
      {
        aOut.setDespatchAdviceReference (_idValue (aLTD.getDespatchAdviceReferencedDocument ().getIssuerAssignedID ()));
        aOut.setDespatchAdviceLineReference (_idValue (aLTD.getDespatchAdviceReferencedDocument ().getLineID ()));
      }
      // BT-191 / BT-192
      if (aLTD.getReceivingAdviceReferencedDocument () != null)
      {
        aOut.setReceivingAdviceReference (_idValue (aLTD.getReceivingAdviceReferencedDocument ()
                                                        .getIssuerAssignedID ()));
        aOut.setReceivingAdviceLineReference (_idValue (aLTD.getReceivingAdviceReferencedDocument ().getLineID ()));
      }
      // BT-198 / BT-199
      if (aLTD.getDeliveryNoteReferencedDocument () != null)
      {
        aOut.setDeliveryNoteReference (_idValue (aLTD.getDeliveryNoteReferencedDocument ().getIssuerAssignedID ()));
        aOut.setDeliveryNoteLineReference (_idValue (aLTD.getDeliveryNoteReferencedDocument ().getLineID ()));
      }
      // BG-37 / BG-38
      if (aLTD.getShipToTradeParty () != null || aLTD.getActualDeliverySupplyChainEvent () != null)
      {
        final PhloxLineDelivery aLineDelivery = _readLineDelivery (aLTD);
        if (!aLineDelivery.isEmpty ())
          aOut.setDelivery (aLineDelivery);
      }
    }
    final LineTradeSettlementType aLTS = aLine.getSpecifiedLineTradeSettlement ();
    if (aLTS != null)
    {
      if (aLTS.getSpecifiedTradeSettlementLineMonetarySummation () != null &&
          !aLTS.getSpecifiedTradeSettlementLineMonetarySummation ().getLineTotalAmount ().isEmpty ())
        aOut.setNetAmount (aLTS.getSpecifiedTradeSettlementLineMonetarySummation ()
                               .getLineTotalAmount ()
                               .get (0)
                               .getValue ());
      if (aLTS.getReceivableSpecifiedTradeAccountingAccount () != null &&
          !aLTS.getReceivableSpecifiedTradeAccountingAccount ().isEmpty ())
        aOut.setAccountingCost (_idValue (aLTS.getReceivableSpecifiedTradeAccountingAccount ().get (0).getID ()));
      if (aLTS.getBillingSpecifiedPeriod () != null)
      {
        final PhloxInvoicingPeriod aP = _readPeriod (aLTS.getBillingSpecifiedPeriod ());
        if (!aP.isEmpty ())
          aOut.setPeriod (aP);
      }
      // BG-39 — line-level preceding invoice references
      for (final ReferencedDocumentType aRD : aLTS.getInvoiceReferencedDocument ())
      {
        final PhloxLinePrecedingInvoiceRef aRef = new PhloxLinePrecedingInvoiceRef ();
        aRef.setNumber (_idValue (aRD.getIssuerAssignedID ())); // BT-217
        aRef.setIssueDate (_formattedDateTime (aRD.getFormattedIssueDateTime ())); // BT-218
        aRef.setTypeCode (_codeValue (aRD.getTypeCode ())); // BT-219
        aRef.setLineReference (_idValue (aRD.getLineID ())); // BT-220
        if (aRef.getNumber () != null)
          aOut.precedingInvoiceRefs ().add (aRef);
      }
      // BT-128 — line object identifier
      for (final ReferencedDocumentType aRD : aLTS.getAdditionalReferencedDocument ())
        if ("130".equals (_codeValue (aRD.getTypeCode ())) && aRD.getIssuerAssignedID () != null)
        {
          final String sScheme = _codeValue (aRD.getReferenceTypeCode ());
          aOut.setObjectIdentifier (new PhloxIdentifier (aRD.getIssuerAssignedID ().getValue (), sScheme));
        }
      // BG-30 — line VAT
      for (final TradeTaxType aTT : aLTS.getApplicableTradeTax ())
      {
        final PhloxLineVat aVat = new PhloxLineVat ();
        aVat.setCategoryCode (_codeValue (aTT.getCategoryCode ()));
        if (aTT.getRateApplicablePercent () != null)
          aVat.setRate (aTT.getRateApplicablePercent ().getValue ());
        aVat.setExemptionReasonText (_text (aTT.getExemptionReason ())); // BT-194
        aVat.setExemptionReasonCode (_codeValue (aTT.getExemptionReasonCode ())); // BT-195
        // BT-196 belongs to the item information (BG-31)
        if (aOut.getItem () != null)
          aOut.getItem ().setGoodsServicesCode (_codeValue (aTT.getSupplyTypeCode ()));
        aOut.setLineVat (aVat);
        break;
      }
      // BG-27 / BG-28 — line allowances / charges
      for (final TradeAllowanceChargeType aAC : aLTS.getSpecifiedTradeAllowanceCharge ())
        if (aAC.getChargeIndicator () != null && Boolean.TRUE.equals (aAC.getChargeIndicator ().isIndicator ()))
        {
          final PhloxLineCharge aC = new PhloxLineCharge ();
          if (!aAC.getActualAmount ().isEmpty ())
            aC.setAmount (aAC.getActualAmount ().get (0).getValue ());
          if (aAC.getBasisAmount () != null)
            aC.setBaseAmount (aAC.getBasisAmount ().getValue ());
          if (aAC.getCalculationPercent () != null)
            aC.setPercentage (aAC.getCalculationPercent ().getValue ());
          aC.setReason (_text (aAC.getReason ()));
          // BT-145 and BT-193 share ram:ReasonCode and are told apart by @listID
          final String sReasonCode = _codeValue (aAC.getReasonCode ());
          if (_isNonVatTaxCodeList (aAC.getReasonCode ()))
          {
            aC.setNonVatTaxTypeCode (sReasonCode);
            aC.setNonVatTaxTypeCodeListID (aAC.getReasonCode ().getListID ());
          }
          else
            aC.setReasonCode (sReasonCode);
          aOut.charges ().add (aC);
        }
        else
        {
          final PhloxLineAllowance aA = new PhloxLineAllowance ();
          if (!aAC.getActualAmount ().isEmpty ())
            aA.setAmount (aAC.getActualAmount ().get (0).getValue ());
          if (aAC.getBasisAmount () != null)
            aA.setBaseAmount (aAC.getBasisAmount ().getValue ());
          if (aAC.getCalculationPercent () != null)
            aA.setPercentage (aAC.getCalculationPercent ().getValue ());
          aA.setReason (_text (aAC.getReason ()));
          aA.setReasonCode (_codeValue (aAC.getReasonCode ()));
          aOut.allowances ().add (aA);
        }
    }
    return aOut;
  }

  /**
   * Read an already-parsed CII document into a {@link PhloxInvoice}. Convenience overload that
   * discards any collected warnings.
   *
   * @param aCii
   *        The JAXB invoice. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  PhloxInvoice readCrossIndustryInvoice (@NonNull final CrossIndustryInvoiceType aCii)
  {
    return readCrossIndustryInvoice (aCii, new ErrorList ());
  }

  /**
   * Read an already-parsed CII document into a {@link PhloxInvoice}, collecting non-fatal issues
   * (unknown type codes, unsupported date formats) into the supplied error list.
   *
   * @param aCii
   *        The JAXB invoice. Never {@code null}.
   * @param aErrors
   *        Error list for warnings. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  PhloxInvoice readCrossIndustryInvoice (@NonNull final CrossIndustryInvoiceType aCii, @NonNull final ErrorList aErrors)
  {
    final PhloxInvoice aModel = new PhloxInvoice ();
    aModel.setEdition (EEN16931Edition.EN2026);

    // BG-2 — Process control
    if (aCii.getExchangedDocumentContext () != null)
    {
      final var aBP = aCii.getExchangedDocumentContext ().getBusinessProcessSpecifiedDocumentContextParameter ();
      if (aBP != null && !aBP.isEmpty ())
        aModel.getProcessControl ().setBusinessProcessType (_idValue (aBP.get (0).getID ()));
      final var aGL = aCii.getExchangedDocumentContext ().getGuidelineSpecifiedDocumentContextParameter ();
      if (aGL != null && !aGL.isEmpty ())
        aModel.getProcessControl ().setSpecificationIdentifier (_idValue (aGL.get (0).getID ()));
    }

    // BG-1 + header BTs
    final ExchangedDocumentType aED = aCii.getExchangedDocument ();
    if (aED != null)
    {
      aModel.setNumber (_idValue (aED.getID ()));
      // BT-2 / BT-166 — one element; @format 102 = date only, 208 = date and time
      _readIssueDateTime (aED.getIssueDateTime (), aModel, aErrors);
      // BT-3 — type code routing.
      final String sTypeCode = aED.getTypeCodeValue ();
      aModel.setTypeCode (sTypeCode);
      aModel.setKind (_resolveKind (sTypeCode, aErrors));
      // BG-1 — notes. Multiple Content lines per note are joined with newlines to preserve
      // every line; the EN 16931 model exposes a single BT-22 string per note.
      for (final NoteType aNote : aED.getIncludedNote ())
      {
        final PhloxInvoiceNote aOut = new PhloxInvoiceNote ();
        aOut.setSubjectCode (aNote.getSubjectCode ().isEmpty () ? null : _codeValue (aNote.getSubjectCode ().get (0)));
        aOut.setContent (_joinTexts (aNote.getContent ()));
        if (aOut.getContent () != null)
          aModel.invoiceNotes ().add (aOut);
      }
    }

    final SupplyChainTradeTransactionType aSCTT = aCii.getSupplyChainTradeTransaction ();
    if (aSCTT == null)
      return aModel;

    final HeaderTradeAgreementType aHTA = aSCTT.getApplicableHeaderTradeAgreement ();
    if (aHTA != null)
    {
      // BT-10 / BT-10-1 — repeatable since EN 16931:2026
      for (final IDType aID : aHTA.getBuyerReferenceID ())
      {
        final String sBuyerReference = str (aID.getValue ());
        if (sBuyerReference != null)
          aModel.buyerReferences ().add (new PhloxIdentifier (sBuyerReference, str (aID.getSchemeID ())));
      }
      // BT-12 — contract
      if (aHTA.getContractReferencedDocument () != null)
        aModel.setContractReference (_idValue (aHTA.getContractReferencedDocument ().getIssuerAssignedID ()));
      // BT-13 — purchase order
      if (aHTA.getBuyerOrderReferencedDocument () != null)
        aModel.setPurchaseOrderReference (_idValue (aHTA.getBuyerOrderReferencedDocument ().getIssuerAssignedID ()));
      // BT-14 — sales order
      if (aHTA.getSellerOrderReferencedDocument () != null)
        aModel.setSalesOrderReference (_idValue (aHTA.getSellerOrderReferencedDocument ().getIssuerAssignedID ()));
      // BT-11 — project reference
      final ProcuringProjectType aProj = aHTA.getSpecifiedProcuringProject ();
      if (aProj != null)
        aModel.setProjectReference (_idValue (aProj.getID ()));

      // BG-4 + BG-5 + BG-6
      if (aHTA.getSellerTradeParty () != null)
        _readSeller (aHTA.getSellerTradeParty (), aModel.getSeller ());
      // BG-7 + BG-8 + BG-9
      if (aHTA.getBuyerTradeParty () != null)
        _readBuyer (aHTA.getBuyerTradeParty (), aModel.getBuyer ());
      // BG-11 + BG-12
      if (aHTA.getSellerTaxRepresentativeTradeParty () != null)
        aModel.setSellerTaxRepresentative (_readTaxRep (aHTA.getSellerTaxRepresentativeTradeParty ()));

      // BT-17 (TypeCode="50") / BT-18 (TypeCode="130") / BG-24 (TypeCode="916")
      for (final ReferencedDocumentType aRD : aHTA.getAdditionalReferencedDocument ())
      {
        final String sTypeCode = _codeValue (aRD.getTypeCode ());
        if ("50".equals (sTypeCode))
          aModel.setTenderOrLotReference (_idValue (aRD.getIssuerAssignedID ()));
        else
          if ("130".equals (sTypeCode))
          {
            final String sScheme = _codeValue (aRD.getReferenceTypeCode ());
            aModel.setInvoicedObjectIdentifier (new PhloxIdentifier (_idValue (aRD.getIssuerAssignedID ()), sScheme));
          }
          else
            if ("916".equals (sTypeCode) || sTypeCode == null)
              aModel.supportingDocuments ().add (_readSupportingDocument (aRD));
      }
    }

    final HeaderTradeDeliveryType aHTD = aSCTT.getApplicableHeaderTradeDelivery ();
    if (aHTD != null)
    {
      // BT-15 — receiving advice
      if (aHTD.getReceivingAdviceReferencedDocument () != null)
        aModel.setReceivingAdviceReference (_idValue (aHTD.getReceivingAdviceReferencedDocument ()
                                                          .getIssuerAssignedID ()));
      // BT-16 — despatch advice
      if (aHTD.getDespatchAdviceReferencedDocument () != null)
        aModel.setDespatchAdviceReference (_idValue (aHTD.getDespatchAdviceReferencedDocument ()
                                                         .getIssuerAssignedID ()));
      // BT-197 — delivery note (EN 16931:2026)
      if (aHTD.getDeliveryNoteReferencedDocument () != null)
        aModel.setDeliveryNoteReference (_idValue (aHTD.getDeliveryNoteReferencedDocument ().getIssuerAssignedID ()));
      // BG-13 + BG-15 — Delivery
      if (aHTD.getShipToTradeParty () != null || aHTD.getActualDeliverySupplyChainEvent () != null)
      {
        final PhloxDeliveryInformation aDI = new PhloxDeliveryInformation ();
        final TradePartyType aShipTo = aHTD.getShipToTradeParty ();
        if (aShipTo != null)
        {
          aDI.setPartyName (_text (aShipTo.getName ()));
          if (!aShipTo.getGlobalID ().isEmpty ())
          {
            final IDType aGID = aShipTo.getGlobalID ().get (0);
            aDI.setLocationIdentifier (new PhloxIdentifier (aGID.getValue (), aGID.getSchemeID ()));
          }
          else
            if (!aShipTo.getID ().isEmpty ())
            {
              final IDType aID = aShipTo.getID ().get (0);
              aDI.setLocationIdentifier (new PhloxIdentifier (aID.getValue (), aID.getSchemeID ()));
            }
          if (aShipTo.getPostalTradeAddress () != null)
            aDI.setPostalAddress (_readAddress (aShipTo.getPostalTradeAddress ()));
        }
        final SupplyChainEventType aEvent = aHTD.getActualDeliverySupplyChainEvent ();
        if (aEvent != null)
          aDI.setActualDeliveryDate (_dateTime (aEvent.getOccurrenceDateTime ()));
        if (!aDI.isEmpty ())
          aModel.setDeliveryInformation (aDI);
      }
    }

    final HeaderTradeSettlementType aHTS = aSCTT.getApplicableHeaderTradeSettlement ();
    if (aHTS != null)
    {
      // BT-5 / BT-6
      if (aHTS.getInvoiceCurrencyCode () != null)
        aModel.setCurrency (aHTS.getInvoiceCurrencyCode ().getValue ());
      if (aHTS.getTaxCurrencyCode () != null)
        aModel.setVatAccountingCurrency (aHTS.getTaxCurrencyCode ().getValue ());

      // BT-19 — buyer accounting reference
      final var aAcctList = aHTS.getReceivableSpecifiedTradeAccountingAccount ();
      if (aAcctList != null && !aAcctList.isEmpty ())
        aModel.setBuyerAccountingReference (_idValue (aAcctList.get (0).getID ()));

      // BG-10 — Payee
      if (aHTS.getPayeeTradeParty () != null)
        aModel.setPayee (_readPayee (aHTS.getPayeeTradeParty ()));

      // BG-3 — preceding invoice references (repeatable since CII D22B)
      for (final ReferencedDocumentType aRD : aHTS.getInvoiceReferencedDocument ())
      {
        final PhloxPrecedingInvoiceRef aRef = new PhloxPrecedingInvoiceRef ();
        aRef.setNumber (_idValue (aRD.getIssuerAssignedID ()));
        aRef.setIssueDate (_formattedDateTime (aRD.getFormattedIssueDateTime ()));
        aRef.setTypeCode (_codeValue (aRD.getTypeCode ())); // BT-202
        if (aRef.getNumber () != null)
          aModel.precedingInvoiceRefs ().add (aRef);
      }

      // BT-167 / BT-167-1 / BT-167-2 — VAT accounting currency exchange rate
      final TradeCurrencyExchangeType aExchange = aHTS.getInvoiceApplicableTradeCurrencyExchange ();
      if (aExchange != null && aExchange.getConversionRate () != null)
      {
        aModel.setVatExchangeRate (aExchange.getConversionRate ().getValue ());
        aModel.setVatExchangeRateTargetCurrency (_codeValue (aExchange.getTargetCurrencyCode ()));
        aModel.setVatExchangeRateSourceCurrency (_codeValue (aExchange.getSourceCurrencyCode ()));
      }

      // BG-14 — invoicing period
      if (aHTS.getBillingSpecifiedPeriod () != null)
      {
        final PhloxInvoicingPeriod aP = _readPeriod (aHTS.getBillingSpecifiedPeriod ());
        if (!aP.isEmpty ())
          aModel.setInvoicingPeriod (aP);
      }

      // BG-23 — VAT breakdown + BT-7/BT-8 + main tax total
      for (final TradeTaxType aTT : aHTS.getApplicableTradeTax ())
      {
        final PhloxVatBreakdown aBD = new PhloxVatBreakdown ();
        if (aTT.getBasisAmount () != null && !aTT.getBasisAmount ().isEmpty ())
          aBD.setTaxableAmount (aTT.getBasisAmount ().get (0).getValue ());
        if (aTT.getCalculatedAmount () != null && !aTT.getCalculatedAmount ().isEmpty ())
          aBD.setTaxAmount (aTT.getCalculatedAmount ().get (0).getValue ());
        aBD.setCategoryCode (_codeValue (aTT.getCategoryCode ()));
        if (aTT.getRateApplicablePercent () != null)
          aBD.setRate (aTT.getRateApplicablePercent ().getValue ());
        aBD.setExemptionReason (_text (aTT.getExemptionReason ()));
        aBD.setExemptionReasonCode (_codeValue (aTT.getExemptionReasonCode ()));
        aBD.setCurrency (_codeValue (aTT.getCurrencyCode ())); // BT-184
        aBD.setGoodsServicesCode (_codeValue (aTT.getSupplyTypeCode ())); // BT-210
        aModel.vatBreakdowns ().add (aBD);

        // BT-7 / BT-8 (carried on the first applicable trade tax)
        if (aModel.getVatPointDate () == null)
          aModel.setVatPointDate (_dateOnly (aTT.getTaxPointDate ()));
        if (aModel.getVatPointDateCode () == null)
          aModel.setVatPointDateCode (_codeValue (aTT.getDueDateTypeCode ()));
      }

      // BG-20 + BG-21 — document-level allowances/charges
      for (final TradeAllowanceChargeType aAC : aHTS.getSpecifiedTradeAllowanceCharge ())
      {
        if (aAC.getChargeIndicator () != null && Boolean.TRUE.equals (aAC.getChargeIndicator ().isIndicator ()))
          aModel.documentCharges ().add (_readDocCharge (aAC));
        else
          aModel.documentAllowances ().add (_readDocAllowance (aAC));
      }

      // BG-16/17/18/19 + BT-9 + BG-33/35/36 — payment instructions and terms
      _readPaymentInstructions (aHTS, aModel);

      // BG-34 — charges on behalf of a third party (EN 16931:2026)
      for (final FinancialAdjustmentType aFA : aHTS.getSpecifiedFinancialAdjustment ())
      {
        final PhloxThirdPartyCharge aTPC = new PhloxThirdPartyCharge ();
        if (!aFA.getActualAmount ().isEmpty ())
          aTPC.setAmount (aFA.getActualAmount ().get (0).getValue ()); // BT-179
        aTPC.setSpecification (_joinTexts (aFA.getReason ())); // BT-180
        aModel.getDocumentTotals ().thirdPartyCharges ().add (aTPC);
      }

      // BG-22 — totals
      if (aHTS.getSpecifiedTradeSettlementHeaderMonetarySummation () != null)
        _readTotals (aHTS.getSpecifiedTradeSettlementHeaderMonetarySummation (), aModel);
    }

    // BG-25 — lines
    for (final SupplyChainTradeLineItemType aLine : aSCTT.getIncludedSupplyChainTradeLineItem ())
      aModel.lines ().add (_readLine (aLine));

    return aModel;
  }
}
