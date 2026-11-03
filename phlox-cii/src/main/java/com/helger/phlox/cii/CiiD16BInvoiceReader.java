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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.equals.EqualsHelper;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.diagnostics.error.SingleError;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931DateFormatCode;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.en16931.basics.codelist.EN16931CodeLists;
import com.helger.phlox.api.PhloxLogHelper;
import com.helger.phlox.api.model.EPhloxDocumentKind;
import com.helger.phlox.api.model.PhloxAdditionalTerm;
import com.helger.phlox.api.model.PhloxAttachment;
import com.helger.phlox.api.model.PhloxBuyer;
import com.helger.phlox.api.model.PhloxContact;
import com.helger.phlox.api.model.PhloxCreditTransfer;
import com.helger.phlox.api.model.PhloxDeliveryInformation;
import com.helger.phlox.api.model.PhloxDirectDebit;
import com.helger.phlox.api.model.PhloxDocumentAllowance;
import com.helger.phlox.api.model.PhloxDocumentCharge;
import com.helger.phlox.api.model.PhloxDocumentTotals;
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
import com.helger.phlox.api.read.PhloxReaderHelper;

import un.unece.uncefact.data.standard.crossindustryinvoice._100.CrossIndustryInvoiceType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.AllowanceChargeReasonCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.CurrencyCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.DocumentCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.FormattedDateTimeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.PaymentMeansCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.ReferenceCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.TaxCategoryCodeType;
import un.unece.uncefact.data.standard.qualifieddatatype._100.TimeReferenceCodeType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.CreditorFinancialAccountType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.CreditorFinancialInstitutionType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.DebtorFinancialAccountType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.DebtorFinancialInstitutionType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.DocumentLineDocumentType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.ExchangedDocumentType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.FinancialAdjustmentType;
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
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeCurrencyExchangeType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePartyType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePaymentDiscountTermsType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePaymentPenaltyTermsType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePaymentTermsType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradePriceType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeProductType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeSettlementHeaderMonetarySummationType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeSettlementPaymentMeansType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.TradeTaxType;
import un.unece.uncefact.data.standard.reusableaggregatebusinessinformationentity._100.UniversalCommunicationType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.AmountType;
import un.unece.uncefact.data.standard.unqualifieddatatype._100.BinaryObjectType;
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
  /** UNTDID 5153 list identifier that marks BT-177 / BT-193 non-VAT tax codes. */
  private static final String NON_VAT_TAX_CODE_LIST_ID = "5153";

  /** Format code of a date with time and offset (UN/EDIFACT 2379), used for BT-166. */
  private static final String FORMAT_CODE_DATE_TIME = EEN16931DateFormatCode.CCYYMMDDHHMMSSZHHMM.getID ();

  CiiD16BInvoiceReader ()
  {}

  /**
   * Check if the reason code is a non-VAT tax code.
   *
   * @return {@code true} if the reason code carries {@code @listID="5153"} (UNTDID 5153 duty or tax
   *         type), i.e. it is a non-VAT tax code (BT-177 / BT-193) and not a charge reason code.
   *         The CII schema declares a default for {@code @listID}, so only the fixed value counts.
   */
  private static boolean _isNonVatTaxCodeList (@Nullable final AllowanceChargeReasonCodeType aCode)
  {
    return aCode != null && NON_VAT_TAX_CODE_LIST_ID.equals (aCode.getListID ());
  }

  @NonNull
  private static ICommonsList <BigDecimal> _getAmounts (@NonNull final List <AmountType> aAmounts)
  {
    return new CommonsArrayList <> (aAmounts, AmountType::getValue);
  }

  private static void _readTotals (@NonNull final TradeSettlementHeaderMonetarySummationType aS,
                                   @NonNull final PhloxInvoice aModel)
  {
    final PhloxDocumentTotals aTotals = aModel.getDocumentTotals ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aModel.additionalTerms ();
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getLineTotalAmount ()), "BT-106", aTotals::setLineNetTotal, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getAllowanceTotalAmount ()),
                                         "BT-107",
                                         aTotals::setAllowanceTotal,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getChargeTotalAmount ()), "BT-108", aTotals::setChargeTotal, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getTaxBasisTotalAmount ()),
                                         "BT-109",
                                         aTotals::setTaxExclusiveTotal,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getGrandTotalAmount ()),
                                         "BT-112",
                                         aTotals::setTaxInclusiveTotal,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getTotalPrepaidAmount ()), "BT-113", aTotals::setPrepaidAmount, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getRoundingAmount ()), "BT-114", aTotals::setRoundingAmount, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aS.getDuePayableAmount ()), "BT-115", aTotals::setPayableAmount, aTerms);

    // BT-110 (main) / BT-111 (accounting currency). A further tax total keeps its currency, because
    // it may be any
    final String sAcct = aModel.getVatAccountingCurrency ();
    for (final AmountType aA : aS.getTaxTotalAmount ())
    {
      final boolean bAccounting = isVatAccountingAmount (aModel.getCurrency (), sAcct, aA.getCurrencyID ());
      if (bAccounting && aTotals.getTaxTotalAccountingCurrency () == null)
        aTotals.setTaxTotalAccountingCurrency (aA.getValue ());
      else
        if (!bAccounting && aTotals.getTaxTotal () == null)
          aTotals.setTaxTotal (aA.getValue ());
        else
          aTerms.add (new PhloxAdditionalTerm (bAccounting ? "BT-111" : "BT-110",
                                               aA.getValue ().toPlainString () + " " + aA.getCurrencyID ()));
    }
  }

  @NonNull
  private static PhloxPriceDetails _readPrice (@NonNull final LineTradeAgreementType aLTA,
                                               @NonNull final PhloxInvoiceLine aLine)
  {
    final PhloxPriceDetails aOut = new PhloxPriceDetails ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aLine.additionalTerms ();
    final TradePriceType aNet = aLTA.getNetPriceProductTradePrice ();
    if (aNet != null)
    {
      PhloxReaderHelper.setAndAddRepeated (_getAmounts (aNet.getChargeAmount ()), "BT-146", aOut::setNetPrice, aTerms);
      if (aNet.getBasisQuantity () != null)
      {
        aOut.setBaseQuantity (aNet.getBasisQuantity ().getValue ());
        aOut.setBaseQuantityUnit (str (aNet.getBasisQuantity ().getUnitCode ()));
      }
    }
    final TradePriceType aGross = aLTA.getGrossPriceProductTradePrice ();
    if (aGross != null)
    {
      PhloxReaderHelper.setAndAddRepeated (_getAmounts (aGross.getChargeAmount ()), "BT-148", aOut::setGrossPrice, aTerms);
      // BT-147 is 0..1: the first discount goes there, the others are shown as well
      boolean bFirstDiscount = true;
      for (final TradeAllowanceChargeType aAC : aGross.getAppliedTradeAllowanceCharge ())
      {
        final boolean bCharge = aAC.getChargeIndicator () != null &&
                                Boolean.TRUE.equals (aAC.getChargeIndicator ().isIndicator ());
        for (final BigDecimal aAmount : _getAmounts (aAC.getActualAmount ()))
          if (bCharge)
            PhloxReaderHelper.addIfPresent (TERM_PRICE_CHARGE, aAmount, aTerms);
          else
            if (bFirstDiscount)
            {
              aOut.setDiscount (aAmount);
              bFirstDiscount = false;
            }
            else
              PhloxReaderHelper.addIfPresent ("BT-147", aAmount, aTerms);
      }
      // BT-149 / BT-150 — since EN 16931:2026 the base quantity may sit on the gross price as well
      if (aGross.getBasisQuantity () != null)
      {
        final QuantityType aGrossQuantity = aGross.getBasisQuantity ();
        if (aOut.getBaseQuantity () == null)
        {
          aOut.setBaseQuantity (aGrossQuantity.getValue ());
          aOut.setBaseQuantityUnit (str (aGrossQuantity.getUnitCode ()));
        }
        else
          if (aGrossQuantity.getValue () != null &&
              (aOut.getBaseQuantity ().compareTo (aGrossQuantity.getValue ()) != 0 ||
               !EqualsHelper.equals (aOut.getBaseQuantityUnit (), str (aGrossQuantity.getUnitCode ()))))
            aTerms.add (new PhloxAdditionalTerm ("BT-149",
                                                 aGrossQuantity.getValue ().toPlainString () +
                                                           " " +
                                                           StringHelper.getNotNull (str (aGrossQuantity.getUnitCode ()))));
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

  @Nullable
  private static PhloxIdentifier _identifier (@Nullable final IDType aID)
  {
    return aID == null ? null : new PhloxIdentifier (_idValue (aID), str (aID.getSchemeID ()));
  }

  private static void _addIdentifier (@Nullable final IDType aID, @NonNull final List <PhloxIdentifier> aTarget)
  {
    if (_idValue (aID) != null)
      aTarget.add (_identifier (aID));
  }

  /**
   * Read the party identifiers (BT-29, BT-46, BT-60, BT-71 and their scheme identifiers).
   */
  private static void _readPartyIdentifiers (@NonNull final TradePartyType aTP,
                                             @NonNull final List <PhloxIdentifier> aTarget)
  {
    for (final IDType aID : aTP.getGlobalID ())
      _addIdentifier (aID, aTarget);
    for (final IDType aID : aTP.getID ())
      _addIdentifier (aID, aTarget);
  }

  @Nullable
  private static String _text (@Nullable final TextType aT)
  {
    return aT == null ? null : str (aT.getValue ());
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

  /** Get the VAT identifiers of a party (BT-31, BT-48, BT-63). */
  @NonNull
  private static ICommonsList <String> _getVatIdentifiers (@NonNull final TradePartyType aTP)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final TaxRegistrationType aTR : aTP.getSpecifiedTaxRegistration ())
      if (aTR.getID () != null && TAX_SCHEME_VAT.equalsIgnoreCase (str (aTR.getID ().getSchemeID ())))
        ret.add (_idValue (aTR.getID ()));
    return ret;
  }

  /**
   * Get the tax registrations of a party that are no VAT identifiers (BT-32 for the seller). The
   * scheme "FC" is the default, special schemes from ISO/IEC 6523 may be used as well.
   */
  @NonNull
  private static ICommonsList <PhloxIdentifier> _getTaxRegistrations (@NonNull final TradePartyType aTP)
  {
    final ICommonsList <PhloxIdentifier> ret = new CommonsArrayList <> ();
    for (final TaxRegistrationType aTR : aTP.getSpecifiedTaxRegistration ())
      if (aTR.getID () != null && !TAX_SCHEME_VAT.equalsIgnoreCase (str (aTR.getID ().getSchemeID ())))
        _addIdentifier (aTR.getID (), ret);
    return ret;
  }

  @NonNull
  private static PhloxPayee _readPayee (@NonNull final TradePartyType aTP, @NonNull final PhloxInvoice aModel)
  {
    final PhloxPayee aOut = new PhloxPayee ();
    aOut.setName (_text (aTP.getName ()));
    final ICommonsList <PhloxIdentifier> aIDs = new CommonsArrayList <> ();
    _readPartyIdentifiers (aTP, aIDs);
    PhloxReaderHelper.setAndAddRepeated (aIDs, "BT-60", aOut::setIdentifier, aModel.additionalTerms ());
    final LegalOrganizationType aLO = aTP.getSpecifiedLegalOrganization ();
    if (aLO != null)
    {
      if (aLO.getID () != null)
        aOut.setLegalRegistration (_identifier (aLO.getID ()));
      // EN 16931 has no trading name of the payee, but it must not get lost
      PhloxReaderHelper.addIfPresent ("BT-59", _text (aLO.getTradingBusinessName ()), aModel.additionalTerms ());
    }
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
      aOut.setCountryCode (str (aAddr.getCountryID ().getValue ()));
    aOut.setCountrySubdivision (_joinTexts (aAddr.getCountrySubDivisionName ()));
    return aOut;
  }

  @NonNull
  private static PhloxSellerTaxRepresentative _readTaxRep (@NonNull final TradePartyType aTP,
                                                           @NonNull final PhloxInvoice aModel)
  {
    final PhloxSellerTaxRepresentative aOut = new PhloxSellerTaxRepresentative ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aModel.additionalTerms ();
    aOut.setName (_text (aTP.getName ()));
    PhloxReaderHelper.setAndAddRepeated (_getVatIdentifiers (aTP), "BT-63", aOut::setVatIdentifier, aTerms);
    for (final PhloxIdentifier aID : _getTaxRegistrations (aTP))
      aTerms.add (new PhloxAdditionalTerm (TERM_TAX_REGISTRATION, aID));
    if (aTP.getPostalTradeAddress () != null)
      aOut.setPostalAddress (_readAddress (aTP.getPostalTradeAddress ()));
    return aOut;
  }

  @NonNull
  private static PhloxContact _readContact (@NonNull final TradeContactType aTC)
  {
    final PhloxContact aOut = new PhloxContact ();
    // BT-41 / BT-56 — PersonName and DepartmentName
    final String sPersonName = _text (aTC.getPersonName ());
    final String sDepartmentName = _text (aTC.getDepartmentName ());
    aOut.setName (sPersonName == null ? sDepartmentName
                                      : sDepartmentName == null ? sPersonName : sPersonName + ", " + sDepartmentName);
    if (aTC.getTelephoneUniversalCommunication () != null)
      aOut.setTelephone (_text (aTC.getTelephoneUniversalCommunication ().getCompleteNumber ()));
    if (aTC.getEmailURIUniversalCommunication () != null &&
        aTC.getEmailURIUniversalCommunication ().getURIID () != null)
      aOut.setEmail (str (aTC.getEmailURIUniversalCommunication ().getURIID ().getValue ()));
    return aOut;
  }

  /**
   * Get the electronic addresses of a party (BT-34, BT-49 with the scheme identifiers).
   */
  @NonNull
  private static ICommonsList <PhloxIdentifier> _getElectronicAddresses (@NonNull final TradePartyType aTP)
  {
    final ICommonsList <PhloxIdentifier> ret = new CommonsArrayList <> ();
    for (final UniversalCommunicationType aUC : aTP.getURIUniversalCommunication ())
      if (aUC.getURIID () != null)
        ret.add (_identifier (aUC.getURIID ()));
    return ret;
  }

  private static void _readContacts (@NonNull final TradePartyType aTP,
                                     @NonNull final Consumer <PhloxContact> aSetter,
                                     @NonNull final String sNameTerm,
                                     @NonNull final String sTelephoneTerm,
                                     @NonNull final String sEmailTerm,
                                     @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    boolean bFirst = true;
    for (final TradeContactType aTC : aTP.getDefinedTradeContact ())
    {
      final PhloxContact aContact = _readContact (aTC);
      if (bFirst)
        aSetter.accept (aContact);
      else
        PhloxReaderHelper.addContact (aContact, sNameTerm, sTelephoneTerm, sEmailTerm, aTerms);
      bFirst = false;
    }
  }

  private static void _readSeller (@NonNull final TradePartyType aTP, @NonNull final PhloxSeller aSeller)
  {
    final ICommonsList <PhloxAdditionalTerm> aTerms = aSeller.additionalTerms ();
    aSeller.setName (_text (aTP.getName ())); // BT-27
    final LegalOrganizationType aLO = aTP.getSpecifiedLegalOrganization ();
    if (aLO != null)
    {
      aSeller.setTradingName (_text (aLO.getTradingBusinessName ())); // BT-28
      if (aLO.getID () != null)
        aSeller.setLegalRegistration (_identifier (aLO.getID ()));
    }
    // BT-29 / BT-29-1
    _readPartyIdentifiers (aTP, aSeller.identifiers ());
    // BT-31 / BT-32 / BT-32-1 via SpecifiedTaxRegistration schemeID
    PhloxReaderHelper.setAndAddRepeated (_getVatIdentifiers (aTP), "BT-31", aSeller::setVatIdentifier, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getTaxRegistrations (aTP),
                                         "BT-32",
                                         aID -> aSeller.setTaxRegistration (aID.getValue ())
                                                       .setTaxRegistrationSchemeID (aID.getSchemeID ()),
                                         aTerms);
    // BT-33
    aSeller.setAdditionalLegalInfo (_joinTexts (aTP.getDescription ()));
    // BT-34 / BT-34-1
    PhloxReaderHelper.setAndAddRepeated (_getElectronicAddresses (aTP),
                                         "BT-34",
                                         aSeller::setElectronicAddress,
                                         aTerms);
    // BG-5
    if (aTP.getPostalTradeAddress () != null)
      aSeller.setPostalAddress (_readAddress (aTP.getPostalTradeAddress ()));
    // BG-6
    _readContacts (aTP, aSeller::setContact, "BT-41", "BT-42", "BT-43", aTerms);
  }

  private static void _readBuyer (@NonNull final TradePartyType aTP, @NonNull final PhloxBuyer aBuyer)
  {
    final ICommonsList <PhloxAdditionalTerm> aTerms = aBuyer.additionalTerms ();
    aBuyer.setName (_text (aTP.getName ())); // BT-44
    final LegalOrganizationType aLO = aTP.getSpecifiedLegalOrganization ();
    if (aLO != null)
    {
      aBuyer.setTradingName (_text (aLO.getTradingBusinessName ()));
      if (aLO.getID () != null)
        aBuyer.setLegalRegistration (_identifier (aLO.getID ()));
    }
    _readPartyIdentifiers (aTP, aBuyer.identifiers ());
    PhloxReaderHelper.setAndAddRepeated (_getVatIdentifiers (aTP), "BT-48", aBuyer::setVatIdentifier, aTerms);
    // EN 16931 has no other tax registration of the buyer, but it must not get lost
    for (final PhloxIdentifier aID : _getTaxRegistrations (aTP))
      aTerms.add (new PhloxAdditionalTerm (TERM_TAX_REGISTRATION, aID));
    PhloxReaderHelper.setAndAddRepeated (_getElectronicAddresses (aTP),
                                         "BT-49",
                                         aBuyer::setElectronicAddress,
                                         aTerms);
    if (aTP.getPostalTradeAddress () != null)
      aBuyer.setPostalAddress (_readAddress (aTP.getPostalTradeAddress ()));
    _readContacts (aTP, aBuyer::setContact, "BT-56", "BT-57", "BT-58", aTerms);
  }

  /** BT-95 / BT-102 and their rates are 0..1, the other tax categories are shown as well. */
  private static void _addFurtherTaxCategories (@NonNull final List <TradeTaxType> aTTs,
                                                @NonNull final String sCategoryTerm,
                                                @NonNull final String sRateTerm,
                                                @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    for (int i = 1; i < aTTs.size (); ++i)
    {
      final TradeTaxType aTT = aTTs.get (i);
      PhloxReaderHelper.addIfPresent (sCategoryTerm, _codeValue (aTT.getCategoryCode ()), aTerms);
      if (aTT.getRateApplicablePercent () != null)
        aTerms.add (new PhloxAdditionalTerm (sRateTerm, aTT.getRateApplicablePercent ().getValue ().toPlainString ()));
    }
  }

  @NonNull
  private static PhloxDocumentAllowance _readDocAllowance (@NonNull final TradeAllowanceChargeType aAC,
                                                           @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxDocumentAllowance aOut = new PhloxDocumentAllowance ();
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aAC.getActualAmount ()), "BT-92", aOut::setAmount, aTerms);
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
    }
    _addFurtherTaxCategories (aAC.getCategoryTradeTax (), "BT-95", "BT-96", aTerms);
    return aOut;
  }

  @NonNull
  private static PhloxDocumentCharge _readDocCharge (@NonNull final TradeAllowanceChargeType aAC,
                                                     @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxDocumentCharge aOut = new PhloxDocumentCharge ();
    PhloxReaderHelper.setAndAddRepeated (_getAmounts (aAC.getActualAmount ()), "BT-99", aOut::setAmount, aTerms);
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
    }
    _addFurtherTaxCategories (aAC.getCategoryTradeTax (), "BT-102", "BT-103", aTerms);
    return aOut;
  }

  @NonNull
  private static PhloxSupportingDocument _readSupportingDocument (@NonNull final ReferencedDocumentType aRD,
                                                                  @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxSupportingDocument aOut = new PhloxSupportingDocument ();
    aOut.setReference (_idValue (aRD.getIssuerAssignedID ()));
    // BT-122-1 — the binding only knows the fixed value 916
    final String sTypeCode = _codeValue (aRD.getTypeCode ());
    if (!EN16931CodeLists.DOCUMENT_TYPE_CODE_SUPPORTING_DOCUMENT.equals (sTypeCode))
      aOut.setDocumentTypeCode (sTypeCode);
    aOut.setDescription (_joinTexts (aRD.getName ()));
    if (aRD.getURIID () != null)
      aOut.setExternalLocation (str (aRD.getURIID ().getValue ()));
    boolean bFirst = true;
    for (final BinaryObjectType aBinary : aRD.getAttachmentBinaryObject ())
    {
      final PhloxAttachment aBin = new PhloxAttachment ();
      aBin.setBinary (aBinary.getValue ());
      aBin.setMimeCode (str (aBinary.getMimeCode ()));
      aBin.setFilename (str (aBinary.getFilename ()));
      if (bFirst)
        aOut.setAttachment (aBin);
      else
      {
        // BT-125 is 0..1: the content of a further attachment cannot be embedded
        PhloxReaderHelper.addIfPresent ("BT-125-2", aBin.getFilename (), aTerms);
        PhloxReaderHelper.addIfPresent ("BT-125-1", aBin.getMimeCode (), aTerms);
      }
      bFirst = false;
    }
    return aOut;
  }

  @NonNull
  private static PhloxItem _readItem (@NonNull final TradeProductType aP)
  {
    final PhloxItem aOut = new PhloxItem ();
    aOut.setName (_joinTexts (aP.getName ()));
    aOut.setDescription (_text (aP.getDescription ()));
    if (aP.getSellerAssignedID () != null)
      aOut.setSellerItemID (str (aP.getSellerAssignedID ().getValue ()));
    if (aP.getBuyerAssignedID () != null)
      aOut.setBuyerItemID (str (aP.getBuyerAssignedID ().getValue ()));
    if (aP.getGlobalID () != null)
      aOut.setStandardItemID (_identifier (aP.getGlobalID ()));
    if (aP.getOriginTradeCountry () != null && aP.getOriginTradeCountry ().getID () != null)
      aOut.setCountryOfOrigin (str (aP.getOriginTradeCountry ().getID ().getValue ()));
    for (final ProductClassificationType aPC : aP.getDesignatedProductClassification ())
      if (aPC.getClassCode () != null && str (aPC.getClassCode ().getValue ()) != null)
      {
        final PhloxItemClassification aIC = new PhloxItemClassification ();
        aIC.setCode (str (aPC.getClassCode ().getValue ()));
        aIC.setListID (str (aPC.getClassCode ().getListID ()));
        aIC.setListVersionID (str (aPC.getClassCode ().getListVersionID ()));
        aOut.classifications ().add (aIC);
      }
    for (final ProductCharacteristicType aPC : aP.getApplicableProductCharacteristic ())
    {
      final PhloxItemAttribute aAttr = new PhloxItemAttribute ();
      aAttr.setName (_joinTexts (aPC.getDescription ())); // BT-160
      aAttr.setCode (_codeValue (aPC.getTypeCode ())); // BT-211
      // BT-161a text and BT-161b numeric value with BT-212 as its unit (CII-SR-504)
      final String sText = _joinTexts (aPC.getValue ());
      if (aPC.getValueMeasure () != null && aPC.getValueMeasure ().getValue () != null)
      {
        final String sNumber = aPC.getValueMeasure ().getValue ().toPlainString ();
        aAttr.setValue (sText == null ? sNumber : sText + " / " + sNumber);
        aAttr.setValueUnit (str (aPC.getValueMeasure ().getUnitCode ()));
      }
      else
        aAttr.setValue (sText);
      if (aAttr.getName () != null || aAttr.getCode () != null || aAttr.getValue () != null)
        aOut.attributes ().add (aAttr);
    }
    return aOut;
  }

  /**
   * Parse a CII {@code DateTimeType} into a {@link LocalDate}. Reads the {@code @format} attribute
   * on the inner {@code DateTimeString} and dispatches to the right pattern; missing format
   * defaults to "102" (yyyyMMdd) per EN 16931.
   */
  @Nullable
  private static LocalDate _dateTime (@Nullable final DateTimeType aDT,
                                      @NonNull final String sTerm,
                                      @NonNull final List <PhloxAdditionalTerm> aTerms,
                                      @NonNull final ErrorList aErrors)
  {
    if (aDT == null || aDT.getDateTimeString () == null)
      return null;

    return parseFormattedDate (aDT.getDateTimeString ().getValue (),
                               aDT.getDateTimeString ().getFormat (),
                               sTerm,
                               aTerms,
                               aErrors);
  }

  @NonNull
  private static PhloxInvoicingPeriod _readPeriod (@NonNull final SpecifiedPeriodType aSP,
                                                   @NonNull final String sStartTerm,
                                                   @NonNull final String sEndTerm,
                                                   @NonNull final List <PhloxAdditionalTerm> aTerms,
                                                   @NonNull final ErrorList aErrors)
  {
    final PhloxInvoicingPeriod aOut = new PhloxInvoicingPeriod ();
    aOut.setStartDate (_dateTime (aSP.getStartDateTime (), sStartTerm, aTerms, aErrors));
    aOut.setEndDate (_dateTime (aSP.getEndDateTime (), sEndTerm, aTerms, aErrors));
    return aOut;
  }

  /**
   * Parse a CII {@code DateType} into a {@link LocalDate}. Honours the {@code @format} attribute on
   * the inner {@code DateString}; if {@code DateString} is absent the {@code DateLocal} value is
   * used.
   */
  @Nullable
  private static LocalDate _dateOnly (@Nullable final DateType aDT,
                                      @NonNull final String sTerm,
                                      @NonNull final List <PhloxAdditionalTerm> aTerms,
                                      @NonNull final ErrorList aErrors)
  {
    if (aDT == null)
      return null;

    if (aDT.getDateString () != null)
      return parseFormattedDate (aDT.getDateString ().getValue (), aDT.getDateString ().getFormat (), sTerm, aTerms, aErrors);
    return aDT.getDateLocal ();
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
    if (FORMAT_CODE_DATE_TIME.equals (str (aDTS.getFormat ())))
    {
      final XMLOffsetDateTime aIssueDateTime = EEN16931DateFormatCode.CCYYMMDDHHMMSSZHHMM.parseDateTime (str (aDTS.getValue ()));
      if (aIssueDateTime == null)
      {
        aErrors.add (SingleError.builderWarn ()
                                .errorText ("Could not parse the invoice issue date and time '" +
                                            PhloxLogHelper.sanitise (aDTS.getValue ()) +
                                            "' (BT-2 / BT-166) with format code 208")
                                .build ());
        // A value that cannot be read is shown as it is
        PhloxReaderHelper.addIfPresent ("BT-2", str (aDTS.getValue ()), aModel.additionalTerms ());
        return;
      }
      aModel.setIssueDate (aIssueDateTime.toLocalDate ());
      aModel.setIssueTime (aIssueDateTime.toOffsetTime ());
    }
    else
      aModel.setIssueDate (parseFormattedDate (aDTS.getValue (),
                                               aDTS.getFormat (),
                                               "BT-2",
                                               aModel.additionalTerms (),
                                               aErrors));
  }

  /**
   * Parse a CII {@code FormattedDateTimeType} (qualified namespace) into a {@link LocalDate}.
   * Format codes follow the same UN/EDIFACT 2379 dispatch as {@code _dateTime}.
   */
  @Nullable
  private static LocalDate _formattedDateTime (@Nullable final FormattedDateTimeType aFDT,
                                               @NonNull final String sTerm,
                                               @NonNull final List <PhloxAdditionalTerm> aTerms,
                                               @NonNull final ErrorList aErrors)
  {
    if (aFDT == null || aFDT.getDateTimeString () == null)
      return null;

    return parseFormattedDate (aFDT.getDateTimeString ().getValue (),
                               aFDT.getDateTimeString ().getFormat (),
                               sTerm,
                               aTerms,
                               aErrors);
  }

  /**
   * Read the BG-33 payment terms (BT-20) with the nested BG-35 early payment discount and BG-36
   * late payment penalty. One {@link PhloxPaymentTerms} is created per CII {@code
   * SpecifiedTradePaymentTerms} that carries a text, a discount or a penalty.
   */
  private static void _readPaymentTerms (@NonNull final List <TradePaymentTermsType> aTerms,
                                         @NonNull final PhloxInvoice aModel,
                                         @NonNull final ErrorList aErrors)
  {
    for (final TradePaymentTermsType aPT : aTerms)
    {
      final PhloxPaymentTerms aOut = new PhloxPaymentTerms ();
      aOut.setText (_joinTexts (aPT.getDescription ())); // BT-20

      final TradePaymentDiscountTermsType aDiscount = aPT.getApplicableTradePaymentDiscountTerms ();
      if (aDiscount != null)
      {
        final PhloxEarlyPaymentDiscount aED = new PhloxEarlyPaymentDiscount ();
        aED.setEndDate (_dateTime (aDiscount.getBasisDateTime (), "BT-170", aModel.additionalTerms (), aErrors)); // BT-170
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
        aLP.setStartDate (_dateTime (aPenalty.getBasisDateTime (), "BT-181", aModel.additionalTerms (), aErrors)); // BT-181
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

  /** Get the identifiers of an account: IBANID if applicable and ProprietaryID, each 0..1 */
  @NonNull
  private static ICommonsList <String> _getAccountIDs (@Nullable final IDType aIBANID, @Nullable final IDType aProprietaryID)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    ret.add (_idValue (aIBANID));
    ret.add (_idValue (aProprietaryID));
    return ret;
  }

  private static void _readPaymentInstructions (@NonNull final HeaderTradeSettlementType aHTS,
                                                @NonNull final PhloxInvoice aModel,
                                                @NonNull final ErrorList aErrors)
  {
    final List <TradeSettlementPaymentMeansType> aMeans = aHTS.getSpecifiedTradeSettlementPaymentMeans ();
    final List <TradePaymentTermsType> aTerms = aHTS.getSpecifiedTradePaymentTerms ();

    // BG-33 / BG-35 / BG-36
    _readPaymentTerms (aTerms, aModel, aErrors);

    // BT-9 — payment due date
    for (final TradePaymentTermsType aPT : aTerms)
      if (aPT.getDueDateDateTime () != null)
      {
        final LocalDate aDueDate = _dateTime (aPT.getDueDateDateTime (), "BT-9", aModel.additionalTerms (), aErrors);
        if (aDueDate != null)
          if (aModel.getPaymentDueDate () == null)
            aModel.setPaymentDueDate (aDueDate);
          else
            if (!aDueDate.equals (aModel.getPaymentDueDate ()))
              aModel.additionalTerms ().add (new PhloxAdditionalTerm ("BT-9", aDueDate));
      }

    final String sCreditorID = _idValue (aHTS.getCreditorReferenceID ());
    if (aMeans.isEmpty () && aTerms.isEmpty () && aHTS.getPaymentReference ().isEmpty () && sCreditorID == null)
      return;

    final PhloxPaymentInstructions aPI = new PhloxPaymentInstructions ();
    final ICommonsList <PhloxAdditionalTerm> aPITerms = aPI.additionalTerms ();
    // BT-83 — remittance information lives on the parent. Multiple references are joined
    // with newlines so multi-part instructions (e.g. invoice number + PO reference) survive.
    aPI.setRemittanceInformation (_joinTexts (aHTS.getPaymentReference ()));

    // BG-17 / BG-18 may sit in any of the payment means, not only in the first one
    boolean bDirectDebitMeans = false;
    boolean bFirst = true;
    for (final TradeSettlementPaymentMeansType aM : aMeans)
    {
      final String sCode = _codeValue (aM.getTypeCode ());
      final String sText = _joinTexts (aM.getInformation ());
      if (bFirst)
      {
        aPI.setPaymentMeansCode (sCode);
        aPI.setPaymentMeansText (sText);
      }
      else
      {
        // BT-81 and BT-82 are 0..1, but each payment means of the document has them
        if (sCode != null)
          aPITerms.add (new PhloxAdditionalTerm ("BT-81", sCode, "paymentmeans"));
        PhloxReaderHelper.addIfPresent ("BT-82", sText, aPITerms);
      }
      bFirst = false;
      if (EN16931CodeLists.isPaymentMeansCodeDirectDebit (sCode))
        bDirectDebitMeans = true;

      // BG-17 credit transfers across all payment means
        final CreditorFinancialAccountType aFA = aM.getPayeePartyCreditorFinancialAccount ();
        if (aFA != null)
        {
          final PhloxCreditTransfer aCT = new PhloxCreditTransfer ();
          aCT.setPaymentMeansCode (sCode);
          // BT-84 — IBANID if applicable, ProprietaryID else
          PhloxReaderHelper.setAndAddRepeated (_getAccountIDs (aFA.getIBANID (), aFA.getProprietaryID ()),
                                               "BT-84",
                                               aCT::setAccountIdentifier,
                                               aPITerms);
          aCT.setAccountName (_text (aFA.getAccountName ()));
          final CreditorFinancialInstitutionType aFI = aM.getPayeeSpecifiedCreditorFinancialInstitution ();
          if (aFI != null)
            aCT.setServiceProviderID (_idValue (aFI.getBICID ()));
          if (!aCT.isEmpty ())
            aPI.creditTransfers ().add (aCT);
        }

      // BG-18 — card
      if (aM.getApplicableTradeSettlementFinancialCard () != null)
      {
        final PhloxPaymentCard aPC = new PhloxPaymentCard ();
        aPC.setPan (_idValue (aM.getApplicableTradeSettlementFinancialCard ().getID ()));
        aPC.setHolderName (_text (aM.getApplicableTradeSettlementFinancialCard ().getCardholderName ()));
        if (aPI.getPaymentCard () == null)
          aPI.setPaymentCard (aPC);
        else
          PhloxReaderHelper.addPaymentCard (aPC, aPITerms);
      }
    }

    // BG-19 + BT-89 .. BT-91 + BT-215 + BT-216. BT-89 is optional, so the direct debit is also
    // recognised by the payer account or, for the creditor reference, by a direct debit payment
    // means
    final PhloxDirectDebit aDD = new PhloxDirectDebit ();
    boolean bDirectDebit = false;
    final ICommonsList <String> aMandateIDs = new CommonsArrayList <> ();
    for (final TradePaymentTermsType aPT : aTerms)
      for (final IDType aMandateID : aPT.getDirectDebitMandateID ())
      {
        aMandateIDs.add (_idValue (aMandateID));
        bDirectDebit = true;
      }
    PhloxReaderHelper.setAndAddRepeated (aMandateIDs, "BT-89", aDD::setMandateReferenceID, aPITerms);
    // BT-91 / BT-216 from the payer financial account, BT-215 from the payer institution
    boolean bFirstPayer = true;
    for (final TradeSettlementPaymentMeansType aM : aMeans)
    {
      final DebtorFinancialAccountType aFA = aM.getPayerPartyDebtorFinancialAccount ();
      final DebtorFinancialInstitutionType aFI = aM.getPayerSpecifiedDebtorFinancialInstitution ();
      if (aFA != null || aFI != null)
      {
        final PhloxDirectDebit aPayer = bFirstPayer ? aDD : new PhloxDirectDebit ();
        if (aFA != null)
        {
          // IBANID if applicable, ProprietaryID else
          PhloxReaderHelper.setAndAddRepeated (_getAccountIDs (aFA.getIBANID (), aFA.getProprietaryID ()),
                                               "BT-91",
                                               aPayer::setDebitedAccountID,
                                               aPITerms);
          aPayer.setDebitedAccountName (_text (aFA.getAccountName ()));
        }
        if (aFI != null)
          aPayer.setDebitedAccountProviderID (_idValue (aFI.getBICID ()));
        if (!bFirstPayer)
          PhloxReaderHelper.addDirectDebit (aPayer, aPITerms);
        bFirstPayer = false;
        bDirectDebit = true;
      }
    }
    if (bDirectDebit)
      aPI.setDirectDebit (aDD);
    // BT-90 — creditor reference id
    PhloxReaderHelper.addCreditorIDs (new CommonsArrayList <> (sCreditorID), bDirectDebitMeans, aPI);

    if (aPI.getPaymentMeansCode () != null ||
        aPI.getPaymentMeansText () != null ||
        aPI.getRemittanceInformation () != null ||
        !aPI.creditTransfers ().isEmpty () ||
        aPI.getPaymentCard () != null ||
        aPI.getDirectDebit () != null ||
        !aPITerms.isEmpty ())
      aModel.setPaymentInstructions (aPI);
  }

  @NonNull
  private static PhloxLineDelivery _readLineDelivery (@NonNull final LineTradeDeliveryType aLTD,
                                                      @NonNull final PhloxInvoiceLine aLine,
                                                      @NonNull final ErrorList aErrors)
  {
    final PhloxLineDelivery aOut = new PhloxLineDelivery ();
    final TradePartyType aShipTo = aLTD.getShipToTradeParty ();
    if (aShipTo != null)
    {
      aOut.setPartyName (_text (aShipTo.getName ())); // BT-185
      final ICommonsList <PhloxIdentifier> aIDs = new CommonsArrayList <> ();
      _readPartyIdentifiers (aShipTo, aIDs);
      // BT-186 / BT-186-1
      PhloxReaderHelper.setAndAddRepeated (aIDs, "BT-186", aOut::setLocationIdentifier, aLine.additionalTerms ());
      if (aShipTo.getPostalTradeAddress () != null)
        aOut.setPostalAddress (_readAddress (aShipTo.getPostalTradeAddress ())); // BG-38
    }
    // BT-187
    if (aLTD.getActualDeliverySupplyChainEvent () != null)
      aOut.setActualDeliveryDate (_dateTime (aLTD.getActualDeliverySupplyChainEvent ().getOccurrenceDateTime (),
                                             "BT-187",
                                             aLine.additionalTerms (),
                                             aErrors));
    return aOut;
  }

  private static void _readLineVat (@NonNull final List <TradeTaxType> aTTs, @NonNull final PhloxInvoiceLine aOut)
  {
    // BG-30 — line VAT. BT-151 and the following are 0..1, the other taxes are shown as well
    final ICommonsList <PhloxAdditionalTerm> aTerms = aOut.additionalTerms ();
    for (final TradeTaxType aTT : aTTs)
    {
      final PhloxLineVat aVat = new PhloxLineVat ();
      aVat.setCategoryCode (_codeValue (aTT.getCategoryCode ()));
      if (aTT.getRateApplicablePercent () != null)
        aVat.setRate (aTT.getRateApplicablePercent ().getValue ());
      aVat.setExemptionReasonText (_text (aTT.getExemptionReason ())); // BT-194
      aVat.setExemptionReasonCode (_codeValue (aTT.getExemptionReasonCode ())); // BT-195
      if (aOut.getLineVat () == null)
      {
        aOut.setLineVat (aVat);
      }
      else
      {
        PhloxReaderHelper.addIfPresent ("BT-151", aVat.getCategoryCode (), aTerms);
        PhloxReaderHelper.addIfPresent ("BT-152", aVat.getRate () == null ? null : aVat.getRate ().toPlainString (), aTerms);
        PhloxReaderHelper.addIfPresent ("BT-194", aVat.getExemptionReasonText (), aTerms);
        PhloxReaderHelper.addIfPresent ("BT-195", aVat.getExemptionReasonCode (), aTerms);
      }
    }
  }

  /** A document reference of a line that is no BT-128: the identifier, its scheme and the type code. */
  @NonNull
  private static String _getLineDocumentText (@NonNull final IDType aID, @Nullable final String sTypeCode)
  {
    final String sScheme = str (aID.getSchemeID ());
    return (sScheme == null ? "" : sScheme + ":") +
           StringHelper.getNotNull (_idValue (aID)) +
           (sTypeCode == null ? "" : " (" + sTypeCode + ")");
  }

  private static void _readLineDocumentReferences (@NonNull final List <ReferencedDocumentType> aRDs,
                                                   @NonNull final PhloxInvoiceLine aOut)
  {
    // BT-128 — line object identifier. The other documents have no term, and show their identifier
    // and their type code
    final ICommonsList <PhloxIdentifier> aObjectIDs = new CommonsArrayList <> ();
    for (final ReferencedDocumentType aRD : aRDs)
      if (aRD.getIssuerAssignedID () != null)
      {
        final String sTypeCode = _codeValue (aRD.getTypeCode ());
        final String sScheme = _codeValue (aRD.getReferenceTypeCode ());
        if ("130".equals (sTypeCode))
          aObjectIDs.add (new PhloxIdentifier (_idValue (aRD.getIssuerAssignedID ()),
                                               sScheme != null ? sScheme : str (aRD.getIssuerAssignedID ().getSchemeID ())));
        else
          aOut.additionalTerms ()
              .add (new PhloxAdditionalTerm (TERM_LINE_DOCUMENT,
                                             _getLineDocumentText (aRD.getIssuerAssignedID (), sTypeCode)));
      }
    PhloxReaderHelper.setAndAddRepeated (aObjectIDs, "BT-128", aOut::setObjectIdentifier, aOut.additionalTerms ());
  }

  @NonNull
  private static PhloxInvoiceLine _readLine (@NonNull final SupplyChainTradeLineItemType aLine,
                                             @NonNull final ErrorList aErrors)
  {
    final PhloxInvoiceLine aOut = new PhloxInvoiceLine ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aOut.additionalTerms ();
    final DocumentLineDocumentType aLD = aLine.getAssociatedDocumentLineDocument ();
    if (aLD != null)
    {
      aOut.setID (_idValue (aLD.getLineID ()));
      // BT-127 — all notes with all their content
      final StringBuilder aNote = new StringBuilder ();
      for (final NoteType aLineNote : aLD.getIncludedNote ())
      {
        final String sContent = _joinTexts (aLineNote.getContent ());
        if (sContent != null)
          aNote.append (aNote.length () > 0 ? "\n" : "").append (sContent);
      }
      aOut.setNote (str (aNote.toString ()));
    }
    final TradeProductType aTPr = aLine.getSpecifiedTradeProduct ();
    if (aTPr != null)
      aOut.setItem (_readItem (aTPr));
    final LineTradeAgreementType aLTA = aLine.getSpecifiedLineTradeAgreement ();
    if (aLTA != null)
    {
      aOut.setPrice (_readPrice (aLTA, aOut));
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
        aOut.setQuantityUnit (str (aQ.getUnitCode ()));
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
        final PhloxLineDelivery aLineDelivery = _readLineDelivery (aLTD, aOut, aErrors);
        if (!aLineDelivery.isEmpty ())
          aOut.setDelivery (aLineDelivery);
      }
    }
    final LineTradeSettlementType aLTS = aLine.getSpecifiedLineTradeSettlement ();
    if (aLTS != null)
    {
      if (aLTS.getSpecifiedTradeSettlementLineMonetarySummation () != null)
      {
        PhloxReaderHelper.setAndAddRepeated (_getAmounts (aLTS.getSpecifiedTradeSettlementLineMonetarySummation ()
                                                              .getLineTotalAmount ()),
                                             "BT-131",
                                             aOut::setNetAmount,
                                             aTerms);
        for (final BigDecimal aAmount : _getAmounts (aLTS.getSpecifiedTradeSettlementLineMonetarySummation ()
                                                         .getTotalAllowanceChargeAmount ()))
          aTerms.add (new PhloxAdditionalTerm (TERM_LINE_ALLOWANCE_CHARGE_TOTAL, aAmount));
      }
      PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aLTS.getReceivableSpecifiedTradeAccountingAccount (),
                                                                    aAccount -> _idValue (aAccount.getID ())),
                                           "BT-133",
                                           aOut::setAccountingCost,
                                           aTerms);
      if (aLTS.getBillingSpecifiedPeriod () != null)
      {
        final PhloxInvoicingPeriod aP = _readPeriod (aLTS.getBillingSpecifiedPeriod (),
                                                     "BT-134",
                                                     "BT-135",
                                                     aTerms,
                                                     aErrors);
        if (!aP.isEmpty ())
          aOut.setPeriod (aP);
      }
      // BG-39 — line-level preceding invoice references
      final ReferencedDocumentType aRD = aLTS.getInvoiceReferencedDocument ();
      if (aRD != null)
      {
        final PhloxLinePrecedingInvoiceRef aRef = new PhloxLinePrecedingInvoiceRef ();
        aRef.setNumber (_idValue (aRD.getIssuerAssignedID ())); // BT-217
        aRef.setIssueDate (_formattedDateTime (aRD.getFormattedIssueDateTime (), "BT-218", aTerms, aErrors)); // BT-218
        aRef.setTypeCode (_codeValue (aRD.getTypeCode ())); // BT-219
        aRef.setLineReference (_idValue (aRD.getLineID ())); // BT-220
        if (aRef.getNumber () != null ||
            aRef.getIssueDate () != null ||
            aRef.getTypeCode () != null ||
            aRef.getLineReference () != null)
          aOut.precedingInvoiceRefs ().add (aRef);
      }
      _readLineDocumentReferences (aLTS.getAdditionalReferencedDocument (), aOut);
      _readLineVat (aLTS.getApplicableTradeTax (), aOut);
      // BG-27 / BG-28 — line allowances / charges
      for (final TradeAllowanceChargeType aAC : aLTS.getSpecifiedTradeAllowanceCharge ())
        if (aAC.getChargeIndicator () != null && Boolean.TRUE.equals (aAC.getChargeIndicator ().isIndicator ()))
        {
          final PhloxLineCharge aC = new PhloxLineCharge ();
          PhloxReaderHelper.setAndAddRepeated (_getAmounts (aAC.getActualAmount ()), "BT-141", aC::setAmount, aTerms);
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
          PhloxReaderHelper.setAndAddRepeated (_getAmounts (aAC.getActualAmount ()), "BT-136", aA::setAmount, aTerms);
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
    aModel.setEdition (EEN16931Edition.EN2017);

    // BG-2 — Process control
    if (aCii.getExchangedDocumentContext () != null)
    {
      final var aBP = aCii.getExchangedDocumentContext ().getBusinessProcessSpecifiedDocumentContextParameter ();
      if (aBP != null && !aBP.isEmpty ())
        PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aBP, aParam -> _idValue (aParam.getID ())),
                                             "BT-23",
                                             aModel.getProcessControl ()::setBusinessProcessType,
                                             aModel.additionalTerms ());
      final var aGL = aCii.getExchangedDocumentContext ().getGuidelineSpecifiedDocumentContextParameter ();
      if (aGL != null && !aGL.isEmpty ())
        PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aGL, aParam -> _idValue (aParam.getID ())),
                                             "BT-24",
                                             aModel.getProcessControl ()::setSpecificationIdentifier,
                                             aModel.additionalTerms ());
    }

    // BG-1 + header BTs
    final ExchangedDocumentType aED = aCii.getExchangedDocument ();
    if (aED != null)
    {
      aModel.setNumber (_idValue (aED.getID ()));
      // BT-2 / BT-166 — one element; @format 102 = date only, 208 = date and time
      _readIssueDateTime (aED.getIssueDateTime (), aModel, aErrors);
      // BT-3 — type code routing.
      final String sTypeCode = str (aED.getTypeCodeValue ());
      aModel.setTypeCode (sTypeCode);
      aModel.setKind (PhloxReaderHelper.getDocumentKind (sTypeCode, EPhloxDocumentKind.INVOICE, aErrors));
      // BG-1 — notes. Multiple Content lines per note are joined with newlines to preserve
      // every line; the EN 16931 model exposes a single BT-22 string per note.
      for (final NoteType aNote : aED.getIncludedNote ())
      {
        final PhloxInvoiceNote aOut = new PhloxInvoiceNote ();
        aOut.setSubjectCode (_codeValue (aNote.getSubjectCode ()));
        aOut.setContent (_joinTexts (aNote.getContent ()));
        if (aOut.getContent () != null || aOut.getSubjectCode () != null)
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
      {
        aModel.setProjectReference (_idValue (aProj.getID ()));
        // BT-11-1 — mandatory in CII, so it repeats BT-11 or is the filler of the binding
        final String sProjectName = _text (aProj.getName ());
        if (sProjectName != null &&
            !sProjectName.equals (aModel.getProjectReference ()) &&
            !EN16931CodeLists.MISSING_VALUE_PLACEHOLDER.equals (sProjectName))
          aModel.setProjectName (sProjectName);
      }

      // BG-4 + BG-5 + BG-6
      if (aHTA.getSellerTradeParty () != null)
        _readSeller (aHTA.getSellerTradeParty (), aModel.getSeller ());
      // BG-7 + BG-8 + BG-9
      if (aHTA.getBuyerTradeParty () != null)
        _readBuyer (aHTA.getBuyerTradeParty (), aModel.getBuyer ());
      // BG-11 + BG-12
      if (aHTA.getSellerTaxRepresentativeTradeParty () != null)
        aModel.setSellerTaxRepresentative (_readTaxRep (aHTA.getSellerTaxRepresentativeTradeParty (), aModel));

      // BT-17 (TypeCode="50") / BT-18 (TypeCode="130") / BG-24 (TypeCode="916")
      final ICommonsList <String> aTenderIDs = new CommonsArrayList <> ();
      final ICommonsList <PhloxIdentifier> aObjectIDs = new CommonsArrayList <> ();
      for (final ReferencedDocumentType aRD : aHTA.getAdditionalReferencedDocument ())
      {
        final String sTypeCode = _codeValue (aRD.getTypeCode ());
        if (EN16931CodeLists.DOCUMENT_TYPE_CODE_ORIGINATOR_DOCUMENT.equals (sTypeCode))
          aTenderIDs.add (_idValue (aRD.getIssuerAssignedID ()));
        else
          if (EN16931CodeLists.DOCUMENT_TYPE_CODE_OBJECT_DOCUMENT.equals (sTypeCode))
          {
            final String sScheme = _codeValue (aRD.getReferenceTypeCode ());
            if (_idValue (aRD.getIssuerAssignedID ()) != null)
              aObjectIDs.add (new PhloxIdentifier (_idValue (aRD.getIssuerAssignedID ()), sScheme));
          }
          else
          {
            // BG-24 — the binding has type code 916, UBL takes everything that is not BT-18
            final PhloxSupportingDocument aDoc = _readSupportingDocument (aRD, aModel.additionalTerms ());
            if (!aDoc.isEmpty ())
              aModel.supportingDocuments ().add (aDoc);
          }
      }
      PhloxReaderHelper.setAndAddRepeated (aTenderIDs,
                                           "BT-17",
                                           aModel::setTenderOrLotReference,
                                           aModel.additionalTerms ());
      PhloxReaderHelper.setAndAddRepeated (aObjectIDs,
                                           "BT-18",
                                           aModel::setInvoicedObjectIdentifier,
                                           aModel.additionalTerms ());
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
      // BT-197 — delivery note
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
          final ICommonsList <PhloxIdentifier> aLocationIDs = new CommonsArrayList <> ();
          _readPartyIdentifiers (aShipTo, aLocationIDs);
          PhloxReaderHelper.setAndAddRepeated (aLocationIDs,
                                               "BT-71",
                                               aDI::setLocationIdentifier,
                                               aModel.additionalTerms ());
          if (aShipTo.getPostalTradeAddress () != null)
            aDI.setPostalAddress (_readAddress (aShipTo.getPostalTradeAddress ()));
        }
        final SupplyChainEventType aEvent = aHTD.getActualDeliverySupplyChainEvent ();
        if (aEvent != null)
          aDI.setActualDeliveryDate (_dateTime (aEvent.getOccurrenceDateTime (), "BT-72", aModel.additionalTerms (), aErrors));
        if (!aDI.isEmpty ())
          aModel.setDeliveryInformation (aDI);
      }
    }

    final HeaderTradeSettlementType aHTS = aSCTT.getApplicableHeaderTradeSettlement ();
    if (aHTS != null)
    {
      // BT-5 / BT-6
      if (aHTS.getInvoiceCurrencyCode () != null)
        aModel.setCurrency (str (aHTS.getInvoiceCurrencyCode ().getValue ()));
      if (aHTS.getTaxCurrencyCode () != null)
        aModel.setVatAccountingCurrency (str (aHTS.getTaxCurrencyCode ().getValue ()));

      // BT-19 — buyer accounting reference
      PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aHTS.getReceivableSpecifiedTradeAccountingAccount (),
                                                                    aAccount -> _idValue (aAccount.getID ())),
                                           "BT-19",
                                           aModel::setBuyerAccountingReference,
                                           aModel.additionalTerms ());

      // BG-10 — Payee
      if (aHTS.getPayeeTradeParty () != null)
        aModel.setPayee (_readPayee (aHTS.getPayeeTradeParty (), aModel));

      // BG-3 — preceding invoice references (scalar in CII D16B)
      if (aHTS.getInvoiceReferencedDocument () != null)
      {
        final ReferencedDocumentType aRD = aHTS.getInvoiceReferencedDocument ();
        final PhloxPrecedingInvoiceRef aRef = new PhloxPrecedingInvoiceRef ();
        aRef.setNumber (_idValue (aRD.getIssuerAssignedID ()));
        aRef.setIssueDate (_formattedDateTime (aRD.getFormattedIssueDateTime (),
                                               "BT-26",
                                               aModel.additionalTerms (),
                                               aErrors));
        aRef.setTypeCode (_codeValue (aRD.getTypeCode ())); // BT-202
        if (aRef.getNumber () != null || aRef.getIssueDate () != null || aRef.getTypeCode () != null)
          aModel.precedingInvoiceRefs ().add (aRef);
      }

      // BT-167 / BT-167-1 / BT-167-2 — VAT accounting currency exchange rate
      final TradeCurrencyExchangeType aExchange = aHTS.getInvoiceApplicableTradeCurrencyExchange ();
      if (aExchange != null)
      {
        if (aExchange.getConversionRate () != null)
          aModel.setVatExchangeRate (aExchange.getConversionRate ().getValue ());
        aModel.setVatExchangeRateTargetCurrency (_codeValue (aExchange.getTargetCurrencyCode ()));
        aModel.setVatExchangeRateSourceCurrency (_codeValue (aExchange.getSourceCurrencyCode ()));
      }

      // BG-14 — invoicing period
      if (aHTS.getBillingSpecifiedPeriod () != null)
      {
        final PhloxInvoicingPeriod aP = _readPeriod (aHTS.getBillingSpecifiedPeriod (),
                                                     "BT-73",
                                                     "BT-74",
                                                     aModel.additionalTerms (),
                                                     aErrors);
        if (!aP.isEmpty ())
          aModel.setInvoicingPeriod (aP);
      }

      // BG-23 — VAT breakdown + BT-7/BT-8 + main tax total
      for (final TradeTaxType aTT : aHTS.getApplicableTradeTax ())
      {
        final PhloxVatBreakdown aBD = new PhloxVatBreakdown ();
        PhloxReaderHelper.setAndAddRepeated (_getAmounts (aTT.getBasisAmount ()),
                                             "BT-116",
                                             aBD::setTaxableAmount,
                                             aModel.additionalTerms ());
        PhloxReaderHelper.setAndAddRepeated (_getAmounts (aTT.getCalculatedAmount ()),
                                             "BT-117",
                                             aBD::setTaxAmount,
                                             aModel.additionalTerms ());
        aBD.setCategoryCode (_codeValue (aTT.getCategoryCode ()));
        if (aTT.getRateApplicablePercent () != null)
          aBD.setRate (aTT.getRateApplicablePercent ().getValue ());
        aBD.setExemptionReason (_text (aTT.getExemptionReason ()));
        aBD.setExemptionReasonCode (_codeValue (aTT.getExemptionReasonCode ()));
        // BT-184 — only meaningful if it differs from the invoice currency BT-5
        final String sBreakdownCurrency = _codeValue (aTT.getCurrencyCode ());
        if (sBreakdownCurrency != null && !sBreakdownCurrency.equals (aModel.getCurrency ()))
          aBD.setCurrency (sBreakdownCurrency);
        aModel.vatBreakdowns ().add (aBD);

        // BT-7 / BT-8 (carried on the first applicable trade tax, another value is shown as well)
        final LocalDate aVatPointDate = _dateOnly (aTT.getTaxPointDate (), "BT-7", aModel.additionalTerms (), aErrors);
        if (aVatPointDate != null)
          if (aModel.getVatPointDate () == null)
            aModel.setVatPointDate (aVatPointDate);
          else
            if (!aVatPointDate.equals (aModel.getVatPointDate ()))
              aModel.additionalTerms ().add (new PhloxAdditionalTerm ("BT-7", aVatPointDate));
        final String sDueDateTypeCode = _codeValue (aTT.getDueDateTypeCode ());
        if (sDueDateTypeCode != null)
        {
          // A code that the list does not know stays as it is
          final String sMappedCode = EN16931CodeLists.mapDueDateTypeCodeCIIToUBL (sDueDateTypeCode);
          final String sVatPointDateCode = sMappedCode != null ? sMappedCode : sDueDateTypeCode;
          if (aModel.getVatPointDateCode () == null)
            aModel.setVatPointDateCode (sVatPointDateCode);
          else
            if (!sVatPointDateCode.equals (aModel.getVatPointDateCode ()))
              aModel.additionalTerms ().add (new PhloxAdditionalTerm ("BT-8", sVatPointDateCode, "vatpointdate"));
        }
      }

      // BG-20 + BG-21 — document-level allowances/charges
      for (final TradeAllowanceChargeType aAC : aHTS.getSpecifiedTradeAllowanceCharge ())
      {
        if (aAC.getChargeIndicator () != null && Boolean.TRUE.equals (aAC.getChargeIndicator ().isIndicator ()))
          aModel.documentCharges ().add (_readDocCharge (aAC, aModel.additionalTerms ()));
        else
          aModel.documentAllowances ().add (_readDocAllowance (aAC, aModel.additionalTerms ()));
      }

      // BG-16/17/18/19 + BT-9 + BT-20 — payment instructions and terms
      _readPaymentInstructions (aHTS, aModel, aErrors);

      // BG-34 — charges on behalf of a third party (EN 16931:2026)
      for (final FinancialAdjustmentType aFA : aHTS.getSpecifiedFinancialAdjustment ())
      {
        final PhloxThirdPartyCharge aTPC = new PhloxThirdPartyCharge ();
        PhloxReaderHelper.setAndAddRepeated (_getAmounts (aFA.getActualAmount ()),
                                             "BT-179",
                                             aTPC::setAmount,
                                             aModel.additionalTerms ());
        aTPC.setSpecification (_joinTexts (aFA.getReason ())); // BT-180
        if (aTPC.getAmount () != null || aTPC.getSpecification () != null)
          aModel.getDocumentTotals ().thirdPartyCharges ().add (aTPC);
      }

      // BG-22 — totals
      if (aHTS.getSpecifiedTradeSettlementHeaderMonetarySummation () != null)
        _readTotals (aHTS.getSpecifiedTradeSettlementHeaderMonetarySummation (), aModel);
    }

    // BG-25 — lines
    for (final SupplyChainTradeLineItemType aLine : aSCTT.getIncludedSupplyChainTradeLineItem ())
      aModel.lines ().add (_readLine (aLine, aErrors));

    return aModel;
  }
}
