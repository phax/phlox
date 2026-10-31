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
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.AllowanceChargeReasonCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.DocumentCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.FormattedDateTimeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.PaymentMeansCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.ReferenceCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.TaxCategoryCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.TimeReferenceCodeType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.CreditorFinancialAccountType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.CreditorFinancialInstitutionType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.DebtorFinancialAccountType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.DocumentLineDocumentType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ExchangedDocumentType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.HeaderTradeAgreementType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.HeaderTradeDeliveryType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.HeaderTradeSettlementType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.LegalOrganizationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.LineTradeAgreementType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.LineTradeDeliveryType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.LineTradeSettlementType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.NoteType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ProcuringProjectType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ProductCharacteristicType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ProductClassificationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ReferencedDocumentType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.SpecifiedPeriodType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.SupplyChainEventType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.SupplyChainTradeLineItemType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.SupplyChainTradeTransactionType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TaxRegistrationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeAddressType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeAllowanceChargeType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeContactType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePartyType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePaymentTermsType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePriceType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeProductType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeSettlementHeaderMonetarySummationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeSettlementPaymentMeansType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeTaxType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.UniversalCommunicationType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.AmountType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.CodeType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.DateTimeType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.DateType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.IDType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.QuantityType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.TextType;

/**
 * Reads CII D16B {@code CrossIndustryInvoice} documents (EN 16931:2017) into a
 * {@link PhloxInvoice}. Package-private worker of {@link CiiInvoiceReader}.
 *
 * @author Philip Helger
 */
final class CiiD16BInvoiceReader extends AbstractCiiInvoiceReader
{
  private static final Logger LOGGER = LoggerFactory.getLogger (CiiD16BInvoiceReader.class);

  // EN 16931 credit-note type codes per Validation Artifacts 1.3.x. Mirrors the set used by
  // en16931-cii2ubl. A non-credit-note code is treated as INVOICE only if it is also present
  // in INVOICE_TYPE_CODES; unknown codes are reported as a warning.
  private static final Set <String> CREDIT_NOTE_TYPE_CODES = Set.of ("81",
                                                                     "83",
                                                                     "261",
                                                                     "262",
                                                                     "296",
                                                                     "308",
                                                                     "381",
                                                                     "396",
                                                                     "420",
                                                                     "458",
                                                                     "532");

  // EN 16931 invoice type codes per Validation Artifacts 1.3.15 (sync with en16931-cii2ubl).
  private static final Set <String> INVOICE_TYPE_CODES = Set.of ("71",
                                                                 "80",
                                                                 "81",
                                                                 "82",
                                                                 "84",
                                                                 "102",
                                                                 "130",
                                                                 "202",
                                                                 "203",
                                                                 "204",
                                                                 "211",
                                                                 "218",
                                                                 "219",
                                                                 "295",
                                                                 "325",
                                                                 "326",
                                                                 "331",
                                                                 "380",
                                                                 "382",
                                                                 "383",
                                                                 "384",
                                                                 "385",
                                                                 "386",
                                                                 "387",
                                                                 "388",
                                                                 "389",
                                                                 "390",
                                                                 "393",
                                                                 "394",
                                                                 "395",
                                                                 "456",
                                                                 "457",
                                                                 "471",
                                                                 "472",
                                                                 "473",
                                                                 "500",
                                                                 "501",
                                                                 "502",
                                                                 "503",
                                                                 "527",
                                                                 "553",
                                                                 "575",
                                                                 "623",
                                                                 "633",
                                                                 "751",
                                                                 "780",
                                                                 "817",
                                                                 "870",
                                                                 "875",
                                                                 "876",
                                                                 "877",
                                                                 "935");

  // Default date format code per EN 16931 (UN/EDIFACT DTM 2379).
  private static final String DEFAULT_DATE_FORMAT_CODE = "102";

  CiiD16BInvoiceReader ()
  {}

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
    }
    return aOut;
  }

  @Nullable
  private static String _codeValue (@Nullable final AllowanceChargeReasonCodeType aCode)
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
    if (!aTP.getID ().isEmpty ())
      aOut.setIdentifier (new PhloxIdentifier (aTP.getID ().get (0).getValue (), aTP.getID ().get (0).getSchemeID ()));
    else
      if (!aTP.getGlobalID ().isEmpty ())
        aOut.setIdentifier (new PhloxIdentifier (aTP.getGlobalID ().get (0).getValue (),
                                                 aTP.getGlobalID ().get (0).getSchemeID ()));
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
    aOut.setName (_text (aTC.getPersonName ()));
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
    // BT-29
    for (final IDType aID : aTP.getID ())
      aSeller.identifiers ().add (new PhloxIdentifier (aID.getValue (), aID.getSchemeID ()));
    for (final IDType aID : aTP.getGlobalID ())
      aSeller.identifiers ().add (new PhloxIdentifier (aID.getValue (), aID.getSchemeID ()));
    // BT-31 / BT-32 via SpecifiedTaxRegistration schemeID
    for (final TaxRegistrationType aTR : aTP.getSpecifiedTaxRegistration ())
      if (aTR.getID () != null)
      {
        final String sScheme = aTR.getID ().getSchemeID ();
        if ("VA".equalsIgnoreCase (sScheme))
          aSeller.setVatIdentifier (aTR.getID ().getValue ());
        else
          if ("FC".equalsIgnoreCase (sScheme))
            aSeller.setTaxRegistration (aTR.getID ().getValue ());
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
    for (final IDType aID : aTP.getID ())
      aBuyer.identifiers ().add (new PhloxIdentifier (aID.getValue (), aID.getSchemeID ()));
    for (final IDType aID : aTP.getGlobalID ())
      aBuyer.identifiers ().add (new PhloxIdentifier (aID.getValue (), aID.getSchemeID ()));
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
    aOut.setReasonCode (_codeValue (aAC.getReasonCode ()));
    if (!aAC.getCategoryTradeTax ().isEmpty ())
    {
      final TradeTaxType aTT = aAC.getCategoryTradeTax ().get (0);
      aOut.setVatCategoryCode (_codeValue (aTT.getCategoryCode ()));
      if (aTT.getRateApplicablePercent () != null)
        aOut.setVatRate (aTT.getRateApplicablePercent ().getValue ());
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

  @NonNull
  private static PhloxItem _readItem (@NonNull final TradeProductType aP)
  {
    final PhloxItem aOut = new PhloxItem ();
    if (!aP.getName ().isEmpty ())
      aOut.setName (_text (aP.getName ().get (0)));
    aOut.setDescription (_text (aP.getDescription ()));
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
      final String sDesc = aPC.getDescription ().isEmpty () ? null : _text (aPC.getDescription ().get (0));
      final String sVal = aPC.getValue ().isEmpty () ? null : _text (aPC.getValue ().get (0));
      if (sDesc != null || sVal != null)
        aOut.attributes ().add (new PhloxItemAttribute (sDesc, sVal));
    }
    return aOut;
  }

  /**
   * Map UN/EDIFACT DTM 2379 format codes to {@link DateTimeFormatter} patterns. Mirrors
   * {@code AbstractCIIToUBLConverter.getDatePattern(...)} in en16931-cii2ubl. Returns {@code null}
   * for unsupported codes.
   */
  @Nullable
  private static String _patternFor (@NonNull final String sFormatCode)
  {
    return switch (sFormatCode)
    {
      case "2" -> "ddMMuu";
      case "3" -> "MMdduu";
      case "4" -> "ddMMuuuu";
      case "101" -> "uuMMdd";
      case "102" -> "uuuuMMdd";
      case "103" -> "YYwwee";
      case "105" -> "uuDDD";
      default -> null;
    };
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

    final String sFormatOrDefault = sFormat == null || sFormat.isEmpty () ? DEFAULT_DATE_FORMAT_CODE : sFormat;
    final String sPattern = _patternFor (sFormatOrDefault);
    if (sPattern == null)
    {
      LOGGER.warn ("Unsupported CII date format code '" + sFormatOrDefault + "' on value '" + sValue + "'");
      return null;
    }
    try
    {
      return LocalDate.parse (sValue, DateTimeFormatter.ofPattern (sPattern, Locale.ROOT));
    }
    catch (final DateTimeParseException ex)
    {
      LOGGER.warn ("Could not parse CII date '" + sValue + "' with pattern '" + sPattern + "'");
      return null;
    }
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
      if (aLTA.getBuyerOrderReferencedDocument () != null &&
          aLTA.getBuyerOrderReferencedDocument ().getLineID () != null)
        aOut.setPurchaseOrderLineRef (aLTA.getBuyerOrderReferencedDocument ().getLineID ().getValue ());
    }
    final LineTradeDeliveryType aLTD = aLine.getSpecifiedLineTradeDelivery ();
    if (aLTD != null && aLTD.getBilledQuantity () != null)
    {
      final QuantityType aQ = aLTD.getBilledQuantity ();
      aOut.setQuantity (aQ.getValue ());
      aOut.setQuantityUnit (aQ.getUnitCode ());
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
          aC.setReasonCode (_codeValue (aAC.getReasonCode ()));
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
   * Resolve the BT-3 type code into a document kind. Codes in {@link #CREDIT_NOTE_TYPE_CODES}
   * become {@link EPhloxDocumentKind#CREDIT_NOTE}; codes in {@link #INVOICE_TYPE_CODES} become
   * {@link EPhloxDocumentKind#INVOICE}; anything else defaults to {@code INVOICE} and adds a
   * warning to the error list. Mirrors the validation behaviour of en16931-cii2ubl.
   */
  @NonNull
  private static EPhloxDocumentKind _resolveKind (@Nullable final String sTypeCode, @NonNull final ErrorList aErrors)
  {
    if (sTypeCode == null || sTypeCode.isEmpty ())
      return EPhloxDocumentKind.INVOICE;
    if (CREDIT_NOTE_TYPE_CODES.contains (sTypeCode))
      return EPhloxDocumentKind.CREDIT_NOTE;
    if (INVOICE_TYPE_CODES.contains (sTypeCode))
      return EPhloxDocumentKind.INVOICE;
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

  private static void _readPaymentInstructions (@NonNull final HeaderTradeSettlementType aHTS,
                                                @NonNull final PhloxInvoice aModel)
  {
    final List <TradeSettlementPaymentMeansType> aMeans = aHTS.getSpecifiedTradeSettlementPaymentMeans ();
    final List <TradePaymentTermsType> aTerms = aHTS.getSpecifiedTradePaymentTerms ();
    if (aMeans.isEmpty () && aTerms.isEmpty ())
      return;

    final PhloxPaymentInstructions aPI = new PhloxPaymentInstructions ();
    if (!aMeans.isEmpty ())
    {
      final TradeSettlementPaymentMeansType aM = aMeans.get (0);
      aPI.setPaymentMeansCode (_codeValue (aM.getTypeCode ()));
      if (!aM.getInformation ().isEmpty ())
        aPI.setPaymentMeansText (_text (aM.getInformation ().get (0)));
      // BT-83 — remittance information lives on the parent. Multiple references are joined
      // with newlines so multi-part instructions (e.g. invoice number + PO reference) survive.
      aPI.setRemittanceInformation (_joinTexts (aHTS.getPaymentReference ()));

      // BG-17 credit transfers across all payment means
      for (final TradeSettlementPaymentMeansType aOne : aMeans)
        if (aOne.getPayeePartyCreditorFinancialAccount () != null)
        {
          final CreditorFinancialAccountType aFA = aOne.getPayeePartyCreditorFinancialAccount ();
          final PhloxCreditTransfer aCT = new PhloxCreditTransfer ();
          aCT.setPaymentMeansCode (_codeValue (aOne.getTypeCode ()));
          if (aFA.getIBANID () != null)
            aCT.setAccountIdentifier (aFA.getIBANID ().getValue ());
          else
            if (aFA.getProprietaryID () != null)
              aCT.setAccountIdentifier (aFA.getProprietaryID ().getValue ());
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

    // BG-19 + BT-89 + BT-91 from payment terms
    boolean bHasDirectDebit = false;
    for (final TradePaymentTermsType aPT : aTerms)
      if (aPT.getDirectDebitMandateID () != null && !aPT.getDirectDebitMandateID ().isEmpty ())
      {
        bHasDirectDebit = true;
        final PhloxDirectDebit aDD = new PhloxDirectDebit ();
        aDD.setMandateReferenceID (aPT.getDirectDebitMandateID ().get (0).getValue ());
        // BT-91 from payer financial account
        for (final TradeSettlementPaymentMeansType aM : aMeans)
          if (aM.getPayerPartyDebtorFinancialAccount () != null)
          {
            final DebtorFinancialAccountType aFA = aM.getPayerPartyDebtorFinancialAccount ();
            if (aFA.getIBANID () != null)
              aDD.setDebitedAccountID (aFA.getIBANID ().getValue ());
            break;
          }
        // BT-90 — creditor reference id
        if (aHTS.getCreditorReferenceID () != null)
          aDD.setBankAssignedCreditorID (aHTS.getCreditorReferenceID ().getValue ());
        aPI.setDirectDebit (aDD);
        break;
      }
    if (!bHasDirectDebit && aHTS.getCreditorReferenceID () != null && aPI.getDirectDebit () == null)
    {
      // Some senders place creditor reference even without explicit mandate
    }

    // BT-9 — payment due date (in CII for both Invoice and Credit Note)
    for (final TradePaymentTermsType aPT : aTerms)
      if (aPT.getDueDateDateTime () != null)
      {
        aModel.setPaymentDueDate (_dateTime (aPT.getDueDateDateTime ()));
        break;
      }
    // BT-20 — payment terms description
    final StringBuilder aSb = new StringBuilder ();
    for (final TradePaymentTermsType aPT : aTerms)
      for (final TextType aT : aPT.getDescription ())
      {
        final String sV = _text (aT);
        if (sV != null)
        {
          if (aSb.length () > 0)
            aSb.append ('\n');
          aSb.append (sV);
        }
      }
    if (aSb.length () > 0)
      aModel.paymentTerms ().add (new PhloxPaymentTerms ().setText (aSb.toString ()));

    if (aPI.getPaymentMeansCode () != null || !aPI.creditTransfers ().isEmpty () || aPI.getDirectDebit () != null)
      aModel.setPaymentInstructions (aPI);
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
    aModel.setEdition (EEN16931Edition.EN2017);

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
      aModel.setIssueDate (_dateTime (aED.getIssueDateTime ()));
      // BT-3 — type code routing.
      final String sTypeCode = aED.getTypeCodeValue ();
      aModel.setTypeCode (sTypeCode);
      aModel.setKind (_resolveKind (sTypeCode, aErrors));
      // BG-1 — notes. Multiple Content lines per note are joined with newlines to preserve
      // every line; the EN 16931 model exposes a single BT-22 string per note.
      for (final NoteType aNote : aED.getIncludedNote ())
      {
        final PhloxInvoiceNote aOut = new PhloxInvoiceNote ();
        aOut.setSubjectCode (_codeValue (aNote.getSubjectCode ()));
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
      // BT-10
      final String sBuyerReference = _text (aHTA.getBuyerReference ());
      if (sBuyerReference != null)
        aModel.buyerReferences ().add (new PhloxIdentifier (sBuyerReference, null));
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

      // BT-83 — remittance information
      if (!aHTS.getPaymentReference ().isEmpty ())
        aModel.setBuyerAccountingReference (null); // BT-19 handled below
      if (!aHTS.getPaymentReference ().isEmpty () && aModel.getPaymentInstructions () == null)
      {
        // Will set after PaymentInstructions populated
      }

      // BT-19 — buyer accounting reference
      final var aAcctList = aHTS.getReceivableSpecifiedTradeAccountingAccount ();
      if (aAcctList != null && !aAcctList.isEmpty ())
        aModel.setBuyerAccountingReference (_idValue (aAcctList.get (0).getID ()));

      // BG-10 — Payee
      if (aHTS.getPayeeTradeParty () != null)
        aModel.setPayee (_readPayee (aHTS.getPayeeTradeParty ()));

      // BG-3 — preceding invoice references (scalar in CII D16B)
      if (aHTS.getInvoiceReferencedDocument () != null)
      {
        final ReferencedDocumentType aRD = aHTS.getInvoiceReferencedDocument ();
        final PhloxPrecedingInvoiceRef aRef = new PhloxPrecedingInvoiceRef ();
        aRef.setNumber (_idValue (aRD.getIssuerAssignedID ()));
        aRef.setIssueDate (_formattedDateTime (aRD.getFormattedIssueDateTime ()));
        if (aRef.getNumber () != null)
          aModel.precedingInvoiceRefs ().add (aRef);
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

      // BG-16/17/18/19 + BT-9 + BT-20 — payment instructions and terms
      _readPaymentInstructions (aHTS, aModel);

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
