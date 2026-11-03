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
import java.time.LocalDate;
import java.time.OffsetTime;
import java.util.List;
import java.util.function.Consumer;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;

import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.xml.XMLOffsetTime;
import com.helger.diagnostics.error.list.ErrorList;
import com.helger.en16931.basics.EEN16931Edition;
import com.helger.en16931.basics.codelist.EN16931CodeLists;
import com.helger.phlox.api.model.*;
import com.helger.phlox.api.read.PhloxReaderHelper;
import com.helger.ubl21.UBL21Marshaller;

import oasis.names.specification.ubl.schema.xsd.commonaggregatecomponents_21.*;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.DescriptionCodeType;
import oasis.names.specification.ubl.schema.xsd.commonbasiccomponents_21.InstructionNoteType;
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
  /** The {@code cbc:DocumentType} that marks BT-18 in a credit note of the 2017 binding. */
  private static final String CREDIT_NOTE_OBJECT_DOCUMENT_TYPE = "ATS";

  Ubl21InvoiceReader ()
  {}

  private static void _readIssueTime (@Nullable final XMLOffsetTime aTime, @NonNull final PhloxInvoice aModel)
  {
    // BT-166 — without a time offset the time cannot be represented as OffsetTime, so it stays a
    // text
    if (aTime != null)
      if (aTime.hasOffset ())
        aModel.setIssueTime (OffsetTime.of (aTime.toLocalTime (), aTime.getOffset ()));
      else
        aModel.additionalTerms ().add (new PhloxAdditionalTerm ("BT-166", aTime.toString ()));
  }

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
    // BT-162 — all address lines, each on a line of its own
    final StringBuilder aLine3 = new StringBuilder ();
    for (final AddressLineType aAL : aAddr.getAddressLine ())
    {
      final String sLine = str (aAL.getLineValue ());
      if (sLine != null)
        aLine3.append (aLine3.length () > 0 ? "\n" : "").append (sLine);
    }
    aOut.setLine3 (str (aLine3.toString ()));
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

  /** Get the values of a party name (BT-28, BT-45, BT-59, BT-62, BT-70, BT-185). */
  @NonNull
  private static ICommonsList <String> _getPartyNames (@NonNull final List <PartyNameType> aPartyNames)
  {
    return new CommonsArrayList <> (aPartyNames, aPN -> str (aPN.getNameValue ()));
  }

  /** Get the VAT identifiers of a party (BT-31, BT-48, BT-63). */
  @NonNull
  private static ICommonsList <String> _getVatIdentifiers (@NonNull final PartyType aParty)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    for (final PartyTaxSchemeType aPTS : aParty.getPartyTaxScheme ())
      if (aPTS.getTaxScheme () != null && TAX_SCHEME_VAT.equals (str (aPTS.getTaxScheme ().getIDValue ())))
        ret.add (str (aPTS.getCompanyIDValue ()));
    return ret;
  }

  /**
   * Get the tax registrations of a party that are no VAT identifiers (BT-32 for the seller). The
   * scheme is the one of the identifier, else the tax scheme, unless that is the fixed {@code LOC}.
   */
  @NonNull
  private static ICommonsList <PhloxIdentifier> _getTaxRegistrations (@NonNull final PartyType aParty)
  {
    final ICommonsList <PhloxIdentifier> ret = new CommonsArrayList <> ();
    for (final PartyTaxSchemeType aPTS : aParty.getPartyTaxScheme ())
      if (aPTS.getTaxScheme () != null && !TAX_SCHEME_VAT.equals (str (aPTS.getTaxScheme ().getIDValue ())))
      {
        final String sTaxScheme = str (aPTS.getTaxScheme ().getIDValue ());
        final PhloxIdentifier aID = identifier (aPTS.getCompanyID ());
        if (aID != null && aID.getValue () != null)
          ret.add (aID.getSchemeID () != null || TAX_SCHEME_LOCAL.equals (sTaxScheme) ? aID
                                                                                      : new PhloxIdentifier (aID.getValue (),
                                                                                                             sTaxScheme));
      }
    return ret;
  }

  private static void _readBuyer (@NonNull final CustomerPartyType aCP, @NonNull final PhloxBuyer aBuyer)
  {
    if (aCP.getParty () == null)
      return;
    final PartyType aParty = aCP.getParty ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aBuyer.additionalTerms ();
    // BT-44 + BT-47
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aParty.getPartyLegalEntity (),
                                                                  aLE -> str (aLE.getRegistrationNameValue ())),
                                         "BT-44",
                                         aBuyer::setName,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aParty.getPartyLegalEntity (),
                                                                  aLE -> identifier (aLE.getCompanyID ())),
                                         "BT-47",
                                         aBuyer::setLegalRegistration,
                                         aTerms);
    // BT-45 — trading name
    PhloxReaderHelper.setAndAddRepeated (_getPartyNames (aParty.getPartyName ()),
                                         "BT-45",
                                         aBuyer::setTradingName,
                                         aTerms);
    // BT-46 — identifiers
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      addIdentifier (aPI.getID (), aBuyer.identifiers ());
    // BT-48 — VAT
    PhloxReaderHelper.setAndAddRepeated (_getVatIdentifiers (aParty), "BT-48", aBuyer::setVatIdentifier, aTerms);
    // EN 16931 has no other tax registration of the buyer, but it must not get lost
    for (final PhloxIdentifier aID : _getTaxRegistrations (aParty))
      aTerms.add (new PhloxAdditionalTerm (TERM_TAX_REGISTRATION, aID));
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
    final ICommonsList <PhloxAdditionalTerm> aTerms = aSeller.additionalTerms ();
    // BT-27 + BT-30 + BT-33
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aParty.getPartyLegalEntity (),
                                                                  aLE -> str (aLE.getRegistrationNameValue ())),
                                         "BT-27",
                                         aSeller::setName,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aParty.getPartyLegalEntity (),
                                                                  aLE -> identifier (aLE.getCompanyID ())),
                                         "BT-30",
                                         aSeller::setLegalRegistration,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aParty.getPartyLegalEntity (),
                                                                  aLE -> str (aLE.getCompanyLegalFormValue ())),
                                         "BT-33",
                                         aSeller::setAdditionalLegalInfo,
                                         aTerms);
    // BT-28
    PhloxReaderHelper.setAndAddRepeated (_getPartyNames (aParty.getPartyName ()),
                                         "BT-28",
                                         aSeller::setTradingName,
                                         aTerms);
    // BT-29
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      if (!isSepaCreditorID (aPI.getID ()))
        addIdentifier (aPI.getID (), aSeller.identifiers ());
    // BT-31 (tax scheme "VAT") / BT-32 + BT-32-1 (tax scheme "LOC", the 2017 binding: not "VAT")
    PhloxReaderHelper.setAndAddRepeated (_getVatIdentifiers (aParty), "BT-31", aSeller::setVatIdentifier, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getTaxRegistrations (aParty),
                                         "BT-32",
                                         aID -> aSeller.setTaxRegistration (aID.getValue ())
                                                       .setTaxRegistrationSchemeID (aID.getSchemeID ()),
                                         aTerms);
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

  @NonNull
  private static PhloxPayee _readPayee (@NonNull final PartyType aParty, @NonNull final PhloxInvoice aModel)
  {
    final PhloxPayee aOut = new PhloxPayee ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aModel.additionalTerms ();
    PhloxReaderHelper.setAndAddRepeated (_getPartyNames (aParty.getPartyName ()), "BT-59", aOut::setName, aTerms);
    final ICommonsList <PhloxIdentifier> aIDs = new CommonsArrayList <> ();
    for (final PartyIdentificationType aPI : aParty.getPartyIdentification ())
      if (!isSepaCreditorID (aPI.getID ()))
        addIdentifier (aPI.getID (), aIDs);
    PhloxReaderHelper.setAndAddRepeated (aIDs, "BT-60", aOut::setIdentifier, aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aParty.getPartyLegalEntity (),
                                                                  aLE -> identifier (aLE.getCompanyID ())),
                                         "BT-61",
                                         aOut::setLegalRegistration,
                                         aTerms);
    return aOut;
  }

  @NonNull
  private static PhloxSellerTaxRepresentative _readSellerTaxRepresentative (@NonNull final PartyType aParty,
                                                                            @NonNull final PhloxInvoice aModel)
  {
    final PhloxSellerTaxRepresentative aOut = new PhloxSellerTaxRepresentative ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aModel.additionalTerms ();
    PhloxReaderHelper.setAndAddRepeated (_getPartyNames (aParty.getPartyName ()), "BT-62", aOut::setName, aTerms);
    PhloxReaderHelper.setAndAddRepeated (_getVatIdentifiers (aParty), "BT-63", aOut::setVatIdentifier, aTerms);
    for (final PhloxIdentifier aID : _getTaxRegistrations (aParty))
      aTerms.add (new PhloxAdditionalTerm (TERM_TAX_REGISTRATION, aID));
    if (aParty.getPostalAddress () != null)
      aOut.setPostalAddress (_readAddress (aParty.getPostalAddress ()));
    return aOut;
  }

  @NonNull
  private static PhloxDeliveryInformation _readDelivery (@NonNull final DeliveryType aDelivery,
                                                         @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxDeliveryInformation aOut = new PhloxDeliveryInformation ();
    if (aDelivery.getDeliveryParty () != null)
      PhloxReaderHelper.setAndAddRepeated (_getPartyNames (aDelivery.getDeliveryParty ().getPartyName ()),
                                           "BT-70",
                                           aOut::setPartyName,
                                           aTerms);
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
    for (final DeliveryType aDelivery : aDeliveries)
    {
      final PhloxDeliveryInformation aDI = _readDelivery (aDelivery, aModel.additionalTerms ());
      if (!aDI.isEmpty ())
        if (aModel.getDeliveryInformation () == null)
          aModel.setDeliveryInformation (aDI);
        else
          PhloxReaderHelper.addDelivery (aDI, aModel.additionalTerms ());
    }
  }

  /** Get all SEPA creditor identifiers (BT-90), the one of the payee first. */
  @NonNull
  private static ICommonsList <String> _getSepaCreditorIDs (@Nullable final SupplierPartyType aSupplier,
                                                            @Nullable final PartyType aPayee)
  {
    final ICommonsList <String> ret = new CommonsArrayList <> ();
    if (aPayee != null)
      for (final PartyIdentificationType aPI : aPayee.getPartyIdentification ())
        if (isSepaCreditorID (aPI.getID ()))
          ret.add (str (aPI.getIDValue ()));
    if (aSupplier != null && aSupplier.getParty () != null)
      for (final PartyIdentificationType aPI : aSupplier.getParty ().getPartyIdentification ())
        if (isSepaCreditorID (aPI.getID ()))
          ret.add (str (aPI.getIDValue ()));
    return ret;
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
  private static PhloxDirectDebit _readDirectDebit (@NonNull final PaymentMandateType aMandate)
  {
    final PhloxDirectDebit ret = new PhloxDirectDebit ();
    ret.setMandateReferenceID (str (aMandate.getIDValue ()));
    if (aMandate.getPayerFinancialAccount () != null)
    {
      final FinancialAccountType aPayerAccount = aMandate.getPayerFinancialAccount ();
      ret.setDebitedAccountID (str (aPayerAccount.getIDValue ()));
      // BT-216 — debited account name
      ret.setDebitedAccountName (str (aPayerAccount.getNameValue ()));
      // BT-215 — debited account payment service provider identifier
      if (aPayerAccount.getFinancialInstitutionBranch () != null)
        ret.setDebitedAccountProviderID (str (aPayerAccount.getFinancialInstitutionBranch ().getIDValue ()));
    }
    return ret;
  }

  private static void _readPaymentInstructions (@NonNull final List <PaymentMeansType> aPaymentMeans,
                                                @Nullable final SupplierPartyType aSupplier,
                                                @Nullable final PartyType aPayee,
                                                @NonNull final PhloxInvoice aModel)
  {
    final ICommonsList <String> aCreditorIDs = _getSepaCreditorIDs (aSupplier, aPayee);
    if (aPaymentMeans.isEmpty () && aCreditorIDs.isEmpty ())
      return;

    final PhloxPaymentInstructions aOut = new PhloxPaymentInstructions ();
    final ICommonsList <PhloxAdditionalTerm> aTerms = aOut.additionalTerms ();
    boolean bDirectDebitMeans = false;
    boolean bFirst = true;
    for (final PaymentMeansType aPM : aPaymentMeans)
    {
      final String sCode = str (aPM.getPaymentMeansCodeValue ());
      final String sText = aPM.getPaymentMeansCode () == null ? null : str (aPM.getPaymentMeansCode ().getName ());
      final ICommonsList <String> aRemittance = new CommonsArrayList <> (aPM.getPaymentID (),
                                                                         aID -> str (aID.getValue ()));
      if (bFirst)
      {
        aOut.setPaymentMeansCode (sCode);
        aOut.setPaymentMeansText (sText);
        PhloxReaderHelper.setAndAddRepeated (aRemittance, "BT-83", aOut::setRemittanceInformation, aTerms);
      }
      else
      {
        // BT-81 .. BT-83 are 0..1, but each payment means of the document has them
        if (sCode != null)
          aTerms.add (new PhloxAdditionalTerm ("BT-81", sCode, "paymentmeans"));
        PhloxReaderHelper.addIfPresent ("BT-82", sText, aTerms);
        for (final String sRemittance : aRemittance)
          PhloxReaderHelper.addIfPresent ("BT-83", sRemittance, aTerms);
      }
      bFirst = false;
      if (EN16931CodeLists.isPaymentMeansCodeDirectDebit (sCode))
        bDirectDebitMeans = true;

      // No term of EN 16931
      for (final InstructionNoteType aNote : aPM.getInstructionNote ())
      {
        final String sNote = str (aNote.getValue ());
        if (sNote != null)
          aTerms.add (new PhloxAdditionalTerm (TERM_PAYMENT_INSTRUCTION_NOTE, sNote));
      }

      // BG-17 — credit transfers across ALL payment means
      if (aPM.getPayeeFinancialAccount () != null)
      {
        final PhloxCreditTransfer aCT = _readCreditTransfer (aPM.getPayeeFinancialAccount ());
        if (!aCT.isEmpty ())
          aOut.creditTransfers ().add (aCT.setPaymentMeansCode (sCode));
      }

      // BG-18 — payment card, in any of the payment means
      final CardAccountType aCardAccount = aPM.getCardAccount ();
      if (aCardAccount != null)
      {
        final PhloxPaymentCard aCard = new PhloxPaymentCard ();
        aCard.setPan (str (aCardAccount.getPrimaryAccountNumberIDValue ()));
        aCard.setHolderName (str (aCardAccount.getHolderNameValue ()));
        if (aOut.getPaymentCard () == null)
          aOut.setPaymentCard (aCard);
        else
          PhloxReaderHelper.addPaymentCard (aCard, aTerms);
        // No term of EN 16931
        final String sNetworkID = str (aCardAccount.getNetworkIDValue ());
        if (sNetworkID != null)
          aTerms.add (new PhloxAdditionalTerm (TERM_CARD_NETWORK, sNetworkID));
      }

      // BG-19 — direct debit, in any of the payment means
      if (aPM.getPaymentMandate () != null)
      {
        final PhloxDirectDebit aDD = _readDirectDebit (aPM.getPaymentMandate ());
        if (aOut.getDirectDebit () == null)
          aOut.setDirectDebit (aDD);
        else
          PhloxReaderHelper.addDirectDebit (aDD, aTerms);
      }
    }

    // BT-90 — bank assigned creditor id (UBL: from supplier/payee PartyIdentification with SEPA
    // scheme). BT-89 is optional, so BT-90 alone is a direct debit if a payment means says so
    PhloxReaderHelper.addCreditorIDs (aCreditorIDs, bDirectDebitMeans, aOut);
    aModel.setPaymentInstructions (aOut);
  }

  /** BT-9 of the 2017 binding of the credit note, which has it per payment means. */
  private static void _readPaymentDueDates (@NonNull final List <PaymentMeansType> aPaymentMeans,
                                            @NonNull final PhloxInvoice aModel)
  {
    for (final PaymentMeansType aPM : aPaymentMeans)
    {
      final LocalDate aDate = d (aPM.getPaymentDueDateValue ());
      if (aDate != null)
        if (aModel.getPaymentDueDate () == null)
          aModel.setPaymentDueDate (aDate);
        else
          if (!aDate.equals (aModel.getPaymentDueDate ()))
            aModel.additionalTerms ().add (new PhloxAdditionalTerm ("BT-9", aDate));
    }
  }

  /** BT-95 / BT-102 and their rates are 0..1, the other tax categories are shown as well. */
  private static void _addFurtherTaxCategories (@NonNull final List <TaxCategoryType> aTCs,
                                                @NonNull final String sCategoryTerm,
                                                @NonNull final String sRateTerm,
                                                @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    for (int i = 1; i < aTCs.size (); ++i)
    {
      final TaxCategoryType aTC = aTCs.get (i);
      PhloxReaderHelper.addIfPresent (sCategoryTerm, str (aTC.getIDValue ()), aTerms);
      if (aTC.getPercentValue () != null)
        aTerms.add (new PhloxAdditionalTerm (sRateTerm, aTC.getPercentValue ().toPlainString ()));
    }
  }

  @NonNull
  private static PhloxDocumentAllowance _readDocumentAllowance (@NonNull final AllowanceChargeType aAC,
                                                                @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxDocumentAllowance aOut = new PhloxDocumentAllowance ();
    if (aAC.getAmount () != null)
      aOut.setAmount (aAC.getAmount ().getValue ());
    if (aAC.getBaseAmount () != null)
      aOut.setBaseAmount (aAC.getBaseAmount ().getValue ());
    if (aAC.getMultiplierFactorNumeric () != null)
      aOut.setPercentage (aAC.getMultiplierFactorNumeric ().getValue ());
    aOut.setReason (joinTexts (aAC.getAllowanceChargeReason ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    if (aAC.getTaxCategory () != null && !aAC.getTaxCategory ().isEmpty ())
    {
      final TaxCategoryType aTC = aAC.getTaxCategory ().get (0);
      aOut.setVatCategoryCode (str (aTC.getIDValue ()));
      if (aTC.getPercent () != null)
        aOut.setVatRate (aTC.getPercent ().getValue ());
      // BT-173
      aOut.setExemptionReasonText (joinTexts (aTC.getTaxExemptionReason ()));
      // BT-174
      aOut.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
    }
    _addFurtherTaxCategories (aAC.getTaxCategory (), "BT-95", "BT-96", aTerms);
    return aOut;
  }

  @NonNull
  private static PhloxDocumentCharge _readDocumentCharge (@NonNull final AllowanceChargeType aAC,
                                                          @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxDocumentCharge aOut = new PhloxDocumentCharge ();
    if (aAC.getAmount () != null)
      aOut.setAmount (aAC.getAmount ().getValue ());
    if (aAC.getBaseAmount () != null)
      aOut.setBaseAmount (aAC.getBaseAmount ().getValue ());
    if (aAC.getMultiplierFactorNumeric () != null)
      aOut.setPercentage (aAC.getMultiplierFactorNumeric ().getValue ());
    aOut.setReason (joinTexts (aAC.getAllowanceChargeReason ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    if (aAC.getTaxCategory () != null && !aAC.getTaxCategory ().isEmpty ())
    {
      final TaxCategoryType aTC = aAC.getTaxCategory ().get (0);
      aOut.setVatCategoryCode (str (aTC.getIDValue ()));
      if (aTC.getPercent () != null)
        aOut.setVatRate (aTC.getPercent ().getValue ());
      // BT-175
      aOut.setExemptionReasonText (joinTexts (aTC.getTaxExemptionReason ()));
      // BT-176
      aOut.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
    }
    _addFurtherTaxCategories (aAC.getTaxCategory (), "BT-102", "BT-103", aTerms);
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
  private static PhloxVatBreakdown _readVatBreakdown (@NonNull final TaxSubtotalType aSub,
                                                      @Nullable final String sInvoiceCurrency)
  {
    final PhloxVatBreakdown aOut = new PhloxVatBreakdown ();
    if (aSub.getTaxableAmount () != null)
      aOut.setTaxableAmount (aSub.getTaxableAmount ().getValue ());
    if (aSub.getTaxAmount () != null)
    {
      aOut.setTaxAmount (aSub.getTaxAmount ().getValue ());
      // BT-184 — EN 16931:2017 does not have it, but a subtotal in the accounting currency is
      // possible
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
      aOut.setExemptionReason (joinTexts (aTC.getTaxExemptionReason ()));
      aOut.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
    }
    return aOut;
  }

  private static void _readTaxTotals (@NonNull final List <TaxTotalType> aTaxTotals, @NonNull final PhloxInvoice aModel)
  {
    final String sCurrency = aModel.getCurrency ();
    final String sAccountingCurrency = aModel.getVatAccountingCurrency ();
    final PhloxDocumentTotals aTotals = aModel.getDocumentTotals ();
    for (final TaxTotalType aTT : aTaxTotals)
    {
      if (aTT.getTaxAmount () != null)
      {
        final BigDecimal aAmount = aTT.getTaxAmount ().getValue ();
        final String sCurrencyID = aTT.getTaxAmount ().getCurrencyID ();
        // A further tax total keeps its currency, because it may be any
        final boolean bAccounting = isVatAccountingAmount (sCurrency, sAccountingCurrency, sCurrencyID);
        if (bAccounting && aTotals.getTaxTotalAccountingCurrency () == null)
          aTotals.setTaxTotalAccountingCurrency (aAmount);
        else
          if (!bAccounting && aTotals.getTaxTotal () == null)
            aTotals.setTaxTotal (aAmount);
          else
            aModel.additionalTerms ()
                  .add (new PhloxAdditionalTerm (bAccounting ? "BT-111" : "BT-110",
                                                 aAmount.toPlainString () + " " + sCurrencyID));
      }
      for (final TaxSubtotalType aSub : aTT.getTaxSubtotal ())
        aModel.vatBreakdowns ().add (_readVatBreakdown (aSub, sCurrency));
    }
  }

  /**
   * @return The type of a supporting document unless it is the fixed type code 916 of the binding.
   *         May be {@code null}.
   */
  @Nullable
  private static String _getDocumentType (@NonNull final DocumentReferenceType aRef)
  {
    final String sCode = str (aRef.getDocumentTypeCodeValue ());
    final String sText = str (aRef.getDocumentTypeValue ());
    if (EN16931CodeLists.DOCUMENT_TYPE_CODE_SUPPORTING_DOCUMENT.equals (sCode))
      return sText;
    return sCode == null ? sText : sText == null ? sCode : sCode + ", " + sText;
  }

  @NonNull
  private static PhloxSupportingDocument _readSupportingDocument (@NonNull final DocumentReferenceType aRef)
  {
    final PhloxSupportingDocument aOut = new PhloxSupportingDocument ();
    aOut.setReference (str (aRef.getIDValue ()));
    aOut.setDocumentTypeCode (_getDocumentType (aRef));
    aOut.setDescription (joinTexts (aRef.getDocumentDescription ()));
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
        aBin.setMimeCode (str (aAtt.getEmbeddedDocumentBinaryObject ().getMimeCode ()));
        aBin.setFilename (str (aAtt.getEmbeddedDocumentBinaryObject ().getFilename ()));
        aOut.setAttachment (aBin);
      }
    }
    return aOut;
  }

  /**
   * The binding of BT-11 in the credit note of UBL 2.1 is
   * <code>cac:AdditionalDocumentReference/cbc:ID</code> without a discriminator, so what tells it
   * from BG-24 is that the reference has nothing else.
   *
   * @return {@code true} if the reference has an ID and no other content.
   */
  private static boolean _isOnlyAnID (@NonNull final DocumentReferenceType aRef)
  {
    return str (aRef.getIDValue ()) != null &&
           aRef.getDocumentType () == null &&
           aRef.getDocumentTypeCode () == null &&
           aRef.getDocumentDescription ().isEmpty () &&
           aRef.getAttachment () == null;
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
    aOut.setReason (joinTexts (aAC.getAllowanceChargeReason ()));
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
    aOut.setReason (joinTexts (aAC.getAllowanceChargeReason ()));
    aOut.setReasonCode (str (aAC.getAllowanceChargeReasonCodeValue ()));
    return aOut;
  }

  @NonNull
  private static PhloxPriceDetails _readPriceDetails (@NonNull final PriceType aPrice,
                                                      @NonNull final PhloxInvoiceLine aLine)
  {
    final PhloxPriceDetails aOut = new PhloxPriceDetails ();
    if (aPrice.getPriceAmount () != null)
      aOut.setNetPrice (aPrice.getPriceAmount ().getValue ());
    if (aPrice.getBaseQuantity () != null)
    {
      aOut.setBaseQuantity (aPrice.getBaseQuantity ().getValue ());
      aOut.setBaseQuantityUnit (str (aPrice.getBaseQuantity ().getUnitCode ()));
    }
    // BT-147 and BT-148 are 0..1: the first discount goes there, the others are shown as well
    final ICommonsList <PhloxAdditionalTerm> aTerms = aLine.additionalTerms ();
    boolean bFirstDiscount = true;
    for (final AllowanceChargeType aAC : aPrice.getAllowanceCharge ())
    {
      final BigDecimal aAmount = aAC.getAmount () == null ? null : aAC.getAmount ().getValue ();
      final BigDecimal aBaseAmount = aAC.getBaseAmount () == null ? null : aAC.getBaseAmount ().getValue ();
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
      {
        PhloxReaderHelper.addIfPresent (TERM_PRICE_CHARGE, aAmount, aTerms);
        PhloxReaderHelper.addIfPresent ("BT-148", aBaseAmount, aTerms);
      }
      else
        if (bFirstDiscount)
        {
          aOut.setDiscount (aAmount);
          aOut.setGrossPrice (aBaseAmount);
          bFirstDiscount = false;
        }
        else
        {
          PhloxReaderHelper.addIfPresent ("BT-147", aAmount, aTerms);
          PhloxReaderHelper.addIfPresent ("BT-148", aBaseAmount, aTerms);
        }
    }
    return aOut;
  }

  @NonNull
  private static PhloxItem _readItem (@NonNull final ItemType aItem)
  {
    final PhloxItem aOut = new PhloxItem ();
    aOut.setName (str (aItem.getNameValue ()));
    aOut.setDescription (joinTexts (aItem.getDescription ()));
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
      if (aCC.getItemClassificationCode () != null && str (aCC.getItemClassificationCode ().getValue ()) != null)
      {
        final PhloxItemClassification aIC = new PhloxItemClassification ();
        aIC.setCode (str (aCC.getItemClassificationCode ().getValue ()));
        aIC.setListID (str (aCC.getItemClassificationCode ().getListID ()));
        aIC.setListVersionID (str (aCC.getItemClassificationCode ().getListVersionID ()));
        aOut.classifications ().add (aIC);
      }
    for (final ItemPropertyType aIP : aItem.getAdditionalItemProperty ())
    {
      final PhloxItemAttribute aAttr = new PhloxItemAttribute ();
      // BT-160
      aAttr.setName (str (aIP.getNameValue ()));
      // BT-211
      aAttr.setCode (str (aIP.getNameCodeValue ()));
      // BT-161a text value and BT-161b numeric value with the BT-212 unit of measure code
      final String sText = str (aIP.getValueValue ());
      final BigDecimal aNumber = aIP.getValueQuantity () == null ? null : aIP.getValueQuantityValue ();
      if (aNumber != null)
      {
        aAttr.setValue (sText == null ? aNumber.toPlainString () : sText + " / " + aNumber.toPlainString ());
        aAttr.setValueUnit (str (aIP.getValueQuantity ().getUnitCode ()));
      }
      else
        aAttr.setValue (sText);
      if (aAttr.getName () != null || aAttr.getCode () != null || aAttr.getValue () != null)
        aOut.attributes ().add (aAttr);
    }
    return aOut;
  }

  /**
   * Get the line delivery or {@code null} if the source carries none of its terms.
   *
   * @return The line delivery or {@code null} if the source carries none of its terms.
   */
  @Nullable
  private static PhloxLineDelivery _readLineDelivery (@NonNull final DeliveryType aDelivery,
                                                      @NonNull final List <PhloxAdditionalTerm> aTerms)
  {
    final PhloxLineDelivery aOut = new PhloxLineDelivery ();
    boolean bAny = false;
    // BT-185
    if (aDelivery.getDeliveryParty () != null)
      PhloxReaderHelper.setAndAddRepeated (_getPartyNames (aDelivery.getDeliveryParty ().getPartyName ()),
                                           "BT-185",
                                           aOut::setPartyName,
                                           aTerms);
    bAny |= aOut.getPartyName () != null;
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
    final ICommonsList <PhloxAdditionalTerm> aTerms = aOut.additionalTerms ();
    // BT-132 + BT-188 + BT-200 + BT-201 — order line reference
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aOrderLineRefs,
                                                                  aOLR -> strWithoutFiller (aOLR.getLineIDValue ())),
                                         "BT-132",
                                         aOut::setPurchaseOrderLineRef,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aOrderLineRefs,
                                                                  aOLR -> strWithoutFiller (aOLR.getSalesOrderLineIDValue ())),
                                         "BT-201",
                                         aOut::setSalesOrderLineReference,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aOrderLineRefs,
                                                                  aOLR -> aOLR.getOrderReference () == null ? null
                                                                                                            : str (aOLR.getOrderReference ()
                                                                                                                       .getIDValue ())),
                                         "BT-188",
                                         aOut::setPurchaseOrderReference,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aOrderLineRefs,
                                                                  aOLR -> aOLR.getOrderReference () == null ? null
                                                                                                            : str (aOLR.getOrderReference ()
                                                                                                                       .getSalesOrderIDValue ())),
                                         "BT-200",
                                         aOut::setSalesOrderReference,
                                         aTerms);
    // BT-189 + BT-190 — despatch advice reference
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aDespatchLineRefs,
                                                                  aLR -> strWithoutFiller (aLR.getLineIDValue ())),
                                         "BT-190",
                                         aOut::setDespatchAdviceLineReference,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aDespatchLineRefs,
                                                                  aLR -> aLR.getDocumentReference () == null ? null
                                                                                                             : str (aLR.getDocumentReference ()
                                                                                                                       .getIDValue ())),
                                         "BT-189",
                                         aOut::setDespatchAdviceReference,
                                         aTerms);
    // BT-191 + BT-192 — receiving advice reference
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aReceiptLineRefs,
                                                                  aLR -> strWithoutFiller (aLR.getLineIDValue ())),
                                         "BT-192",
                                         aOut::setReceivingAdviceLineReference,
                                         aTerms);
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aReceiptLineRefs,
                                                                  aLR -> aLR.getDocumentReference () == null ? null
                                                                                                             : str (aLR.getDocumentReference ()
                                                                                                                       .getIDValue ())),
                                         "BT-191",
                                         aOut::setReceivingAdviceReference,
                                         aTerms);
    for (final DeliveryType aDelivery : aDeliveries)
    {
      // BG-37 + BG-38 — line delivery information
      final PhloxLineDelivery aLineDelivery = _readLineDelivery (aDelivery, aTerms);
      if (aLineDelivery != null)
        if (aOut.getDelivery () == null)
          aOut.setDelivery (aLineDelivery);
        else
          PhloxReaderHelper.addLineDelivery (aLineDelivery, aTerms);
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
        PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aBR.getBillingReferenceLine (),
                                                                      aLine -> str (aLine.getIDValue ())),
                                             "BT-220",
                                             aRef::setLineReference,
                                             aOut.additionalTerms ());
        if (aRef.getNumber () != null || aRef.getIssueDate () != null || aRef.getTypeCode () != null)
          aOut.precedingInvoiceRefs ().add (aRef);
      }
  }

  /**
   * BT-128 is the document reference with type code 130 (BT-128-2). The others have no term, and
   * show their identifier and their type code.
   */
  private static void _readLineDocumentReferences (@NonNull final List <DocumentReferenceType> aDocRefs,
                                                   @NonNull final PhloxInvoiceLine aOut)
  {
    final ICommonsList <PhloxIdentifier> aObjectIDs = new CommonsArrayList <> ();
    for (final DocumentReferenceType aDR : aDocRefs)
      if (aDR.getID () != null)
      {
        final String sTypeCode = str (aDR.getDocumentTypeCodeValue ());
        if (EN16931CodeLists.DOCUMENT_TYPE_CODE_OBJECT_DOCUMENT.equals (sTypeCode))
          aObjectIDs.add (identifier (aDR.getID ()));
        else
          aOut.additionalTerms ()
              .add (new PhloxAdditionalTerm (TERM_LINE_DOCUMENT, getLineDocumentText (aDR.getID (), sTypeCode)));
      }
    PhloxReaderHelper.setAndAddRepeated (aObjectIDs, "BT-128", aOut::setObjectIdentifier, aOut.additionalTerms ());
  }

  private static void _readLineCommon (@NonNull final List <DocumentReferenceType> aDocRefs,
                                       @NonNull final List <PeriodType> aPeriods,
                                       @NonNull final List <AllowanceChargeType> aACs,
                                       @Nullable final PriceType aPrice,
                                       @Nullable final ItemType aItem,
                                       @NonNull final PhloxInvoiceLine aOut)
  {
    final ICommonsList <PhloxAdditionalTerm> aTerms = aOut.additionalTerms ();
    _readLineDocumentReferences (aDocRefs, aOut);
    // BG-26
    for (final PeriodType aPeriod : aPeriods)
    {
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      if (!aIP.isEmpty ())
        if (aOut.getPeriod () == null)
          aOut.setPeriod (aIP);
        else
          PhloxReaderHelper.addPeriod (aIP, "BT-134", "BT-135", aTerms);
    }
    // BG-27 / BG-28
    for (final AllowanceChargeType aAC : aACs)
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aOut.charges ().add (_readLineCharge (aAC));
      else
        aOut.allowances ().add (_readLineAllowance (aAC));
    // BG-29
    if (aPrice != null)
      aOut.setPrice (_readPriceDetails (aPrice, aOut));
    if (aItem != null)
    {
      // BG-31 + BG-32
      aOut.setItem (_readItem (aItem));
      // BG-30 from ClassifiedTaxCategory of the item
      for (final TaxCategoryType aTC : aItem.getClassifiedTaxCategory ())
      {
        final PhloxLineVat aVat = new PhloxLineVat ();
        aVat.setCategoryCode (str (aTC.getIDValue ()));
        if (aTC.getPercent () != null)
          aVat.setRate (aTC.getPercent ().getValue ());
        // BT-194
        aVat.setExemptionReasonText (joinTexts (aTC.getTaxExemptionReason ()));
        // BT-195
        aVat.setExemptionReasonCode (str (aTC.getTaxExemptionReasonCodeValue ()));
        if (aOut.getLineVat () == null)
        {
          aOut.setLineVat (aVat);
        }
        else
        {
          // BT-151 and the following are 0..1
          PhloxReaderHelper.addIfPresent ("BT-151", aVat.getCategoryCode (), aTerms);
          PhloxReaderHelper.addIfPresent ("BT-152",
                                          aVat.getRate () == null ? null : aVat.getRate ().toPlainString (),
                                          aTerms);
          PhloxReaderHelper.addIfPresent ("BT-194", aVat.getExemptionReasonText (), aTerms);
          PhloxReaderHelper.addIfPresent ("BT-195", aVat.getExemptionReasonCode (), aTerms);
        }
      }
    }
  }

  @NonNull
  private static PhloxInvoiceLine _readInvoiceLine (@NonNull final InvoiceLineType aLine)
  {
    final PhloxInvoiceLine aOut = new PhloxInvoiceLine ();
    aOut.setID (str (aLine.getIDValue ()));
    aOut.setNote (joinTexts (aLine.getNote ()));
    if (aLine.getInvoicedQuantity () != null)
    {
      aOut.setQuantity (aLine.getInvoicedQuantity ().getValue ());
      aOut.setQuantityUnit (str (aLine.getInvoicedQuantity ().getUnitCode ()));
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
    aOut.setNote (joinTexts (aLine.getNote ()));
    if (aLine.getCreditedQuantity () != null)
    {
      aOut.setQuantity (aLine.getCreditedQuantity ().getValue ());
      aOut.setQuantityUnit (str (aLine.getCreditedQuantity ().getUnitCode ()));
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

  private static void _readNotes (@NonNull final List <NoteType> aNotes, @NonNull final PhloxInvoice aModel)
  {
    // BG-1
    for (final NoteType aNote : aNotes)
    {
      final PhloxInvoiceNote aOut = parseInvoiceNote (aNote.getValue ());
      if (aOut != null)
        aModel.invoiceNotes ().add (aOut);
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

  private static void _readInvoicePeriod (@NonNull final List <PeriodType> aPeriods, @NonNull final PhloxInvoice aModel)
  {
    // BG-14
    final ICommonsList <String> aVatPointDateCodes = new CommonsArrayList <> ();
    for (final PeriodType aPeriod : aPeriods)
    {
      final PhloxInvoicingPeriod aIP = new PhloxInvoicingPeriod ();
      aIP.setStartDate (d (aPeriod.getStartDateValue ()));
      aIP.setEndDate (d (aPeriod.getEndDateValue ()));
      // BT-8 — VAT point date code (lives in InvoicePeriod for UBL)
      for (final DescriptionCodeType aCode : aPeriod.getDescriptionCode ())
        aVatPointDateCodes.add (str (aCode.getValue ()));
      if (!aIP.isEmpty ())
        if (aModel.getInvoicingPeriod () == null)
          aModel.setInvoicingPeriod (aIP);
        else
          PhloxReaderHelper.addPeriod (aIP, "BT-73", "BT-74", aModel.additionalTerms ());
    }
    PhloxReaderHelper.setAndAddRepeated (aVatPointDateCodes,
                                         "BT-8",
                                         aModel::setVatPointDateCode,
                                         aModel.additionalTerms ());
  }

  private static void _readDocumentReferences (@NonNull final List <DocumentReferenceType> aRefs,
                                               @NonNull final String sTerm,
                                               @NonNull final Consumer <String> aSetter,
                                               @NonNull final PhloxInvoice aModel)
  {
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aRefs, aRef -> str (aRef.getIDValue ())),
                                         sTerm,
                                         aSetter,
                                         aModel.additionalTerms ());
  }

  private static void _readCommonReferences (@Nullable final OrderReferenceType aOrder,
                                             @NonNull final List <DocumentReferenceType> aContract,
                                             @NonNull final List <DocumentReferenceType> aReceipt,
                                             @NonNull final List <DocumentReferenceType> aDespatch,
                                             @NonNull final List <DocumentReferenceType> aOriginator,
                                             @NonNull final List <ProjectReferenceType> aProject,
                                             @NonNull final PhloxInvoice aModel)
  {
    // BT-13 / BT-14
    if (aOrder != null)
    {
      aModel.setPurchaseOrderReference (strWithoutFiller (aOrder.getIDValue ()));
      aModel.setSalesOrderReference (str (aOrder.getSalesOrderIDValue ()));
    }
    // BT-12 — contract reference
    _readDocumentReferences (aContract, "BT-12", aModel::setContractReference, aModel);
    // BT-15 — receiving advice
    _readDocumentReferences (aReceipt, "BT-15", aModel::setReceivingAdviceReference, aModel);
    // BT-16 — despatch advice
    _readDocumentReferences (aDespatch, "BT-16", aModel::setDespatchAdviceReference, aModel);
    // BT-17 — tender or lot
    _readDocumentReferences (aOriginator, "BT-17", aModel::setTenderOrLotReference, aModel);
    // BT-11 — project reference
    PhloxReaderHelper.setAndAddRepeated (new CommonsArrayList <> (aProject, aRef -> str (aRef.getIDValue ())),
                                         "BT-11",
                                         aModel::setProjectReference,
                                         aModel.additionalTerms ());
  }

  private static void _readPrecedingInvoiceRefs (@NonNull final List <BillingReferenceType> aRefs,
                                                 @NonNull final PhloxInvoice aModel)
  {
    // BG-3
    for (final BillingReferenceType aBR : aRefs)
      if (aBR.getInvoiceDocumentReference () != null)
      {
        final DocumentReferenceType aDR = aBR.getInvoiceDocumentReference ();
        final PhloxPrecedingInvoiceRef aRef = new PhloxPrecedingInvoiceRef ().setNumber (str (aDR.getIDValue ()))
                                                                             .setIssueDate (d (aDR.getIssueDateValue ()))
                                                                             .setTypeCode (str (aDR.getDocumentTypeCodeValue ()));
        if (aRef.getNumber () != null || aRef.getIssueDate () != null || aRef.getTypeCode () != null)
          aModel.precedingInvoiceRefs ().add (aRef);
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
      aOut.setText (joinTexts (aPT.getNote ()));

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
      aPenalty.setYearlyInterestPercentage (aPT.getPenaltySurchargePercentValue ());
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
   * @param aErrors
   *        The error list to fill with warnings. Never {@code null}.
   * @return The populated model.
   */
  @NonNull
  public PhloxInvoice readInvoice (@NonNull final InvoiceType aInvoice, @NonNull final ErrorList aErrors)
  {
    final PhloxInvoice aModel = new PhloxInvoice ();
    aModel.setEdition (EEN16931Edition.EN2017);

    // BT-1
    aModel.setNumber (str (aInvoice.getIDValue ()));
    // BT-2 + BT-166
    aModel.setIssueDate (d (aInvoice.getIssueDateValue ()));
    _readIssueTime (aInvoice.getIssueTimeValue (), aModel);
    // BT-3
    aModel.setTypeCode (strFromCode (aInvoice.getInvoiceTypeCodeValue ()));
    aModel.setKind (PhloxReaderHelper.getDocumentKind (aModel.getTypeCode (), EPhloxDocumentKind.INVOICE, aErrors));
    // BG-1 — notes
    _readNotes (aInvoice.getNote (), aModel);
    // BT-5 / BT-6 / BT-167
    aModel.setCurrency (str (aInvoice.getDocumentCurrencyCodeValue ()));
    aModel.setVatAccountingCurrency (str (aInvoice.getTaxCurrencyCodeValue ()));
    _readExchangeRate (aInvoice.getTaxExchangeRate (), aModel);
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
    // BT-11 to BT-17
    _readCommonReferences (aInvoice.getOrderReference (),
                           aInvoice.getContractDocumentReference (),
                           aInvoice.getReceiptDocumentReference (),
                           aInvoice.getDespatchDocumentReference (),
                           aInvoice.getOriginatorDocumentReference (),
                           aInvoice.getProjectReference (),
                           aModel);

    // BT-18 + BG-24 — additional document references
    final ICommonsList <PhloxIdentifier> aObjectIDs = new CommonsArrayList <> ();
    for (final DocumentReferenceType aRef : aInvoice.getAdditionalDocumentReference ())
    {
      if (EN16931CodeLists.DOCUMENT_TYPE_CODE_OBJECT_DOCUMENT.equals (str (aRef.getDocumentTypeCodeValue ())))
      {
        // BT-18: Invoiced object identifier
        if (aRef.getID () != null)
          aObjectIDs.add (identifier (aRef.getID ()));
      }
      else
      {
        // BG-24: supporting document
        final PhloxSupportingDocument aDoc = _readSupportingDocument (aRef);
        if (!aDoc.isEmpty ())
          aModel.supportingDocuments ().add (aDoc);
      }
    }
    PhloxReaderHelper.setAndAddRepeated (aObjectIDs,
                                         "BT-18",
                                         aModel::setInvoicedObjectIdentifier,
                                         aModel.additionalTerms ());

    // BG-3 — preceding invoice references
    _readPrecedingInvoiceRefs (aInvoice.getBillingReference (), aModel);

    // BG-4 + BG-5 + BG-6 — Seller
    if (aInvoice.getAccountingSupplierParty () != null)
      _readSeller (aInvoice.getAccountingSupplierParty (), aModel.getSeller ());
    // BG-7 + BG-8 + BG-9 — Buyer
    if (aInvoice.getAccountingCustomerParty () != null)
      _readBuyer (aInvoice.getAccountingCustomerParty (), aModel.getBuyer ());
    // BG-10 — Payee
    if (aInvoice.getPayeeParty () != null)
      aModel.setPayee (_readPayee (aInvoice.getPayeeParty (), aModel));
    // BG-11 + BG-12 — Seller tax representative
    if (aInvoice.getTaxRepresentativeParty () != null)
      aModel.setSellerTaxRepresentative (_readSellerTaxRepresentative (aInvoice.getTaxRepresentativeParty (), aModel));
    // BG-13 + BG-15 — Delivery
    _readHeaderDelivery (aInvoice.getDelivery (), aModel);
    // BG-14 — Invoicing period
    _readInvoicePeriod (aInvoice.getInvoicePeriod (), aModel);

    // BG-16/17/18/19 — Payment instructions
    _readPaymentInstructions (aInvoice.getPaymentMeans (),
                              aInvoice.getAccountingSupplierParty (),
                              aInvoice.getPayeeParty (),
                              aModel);
    _readPaymentDueDates (aInvoice.getPaymentMeans (), aModel);
    // BT-20 — payment terms (text)
    _readPaymentTerms (aInvoice.getPaymentTerms (), aModel);

    // BG-20 / BG-21 — document-level allowances and charges
    for (final AllowanceChargeType aAC : aInvoice.getAllowanceCharge ())
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aModel.documentCharges ().add (_readDocumentCharge (aAC, aModel.additionalTerms ()));
      else
        aModel.documentAllowances ().add (_readDocumentAllowance (aAC, aModel.additionalTerms ()));

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
    final InvoiceType aInvoice = unmarshal (UBL21Marshaller.invoice (), aDoc, aErrors);
    return aInvoice == null ? null : readInvoice (aInvoice, aErrors);
  }

  /**
   * Read an already-parsed UBL CreditNote into a {@link PhloxInvoice}.
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
    aModel.setEdition (EEN16931Edition.EN2017);

    aModel.setNumber (str (aCreditNote.getIDValue ()));
    aModel.setIssueDate (d (aCreditNote.getIssueDateValue ()));
    _readIssueTime (aCreditNote.getIssueTimeValue (), aModel);
    aModel.setTypeCode (strFromCode (aCreditNote.getCreditNoteTypeCodeValue ()));
    aModel.setKind (PhloxReaderHelper.getDocumentKind (aModel.getTypeCode (), EPhloxDocumentKind.CREDIT_NOTE, aErrors));
    _readNotes (aCreditNote.getNote (), aModel);
    aModel.setCurrency (str (aCreditNote.getDocumentCurrencyCodeValue ()));
    aModel.setVatAccountingCurrency (str (aCreditNote.getTaxCurrencyCodeValue ()));
    _readExchangeRate (aCreditNote.getTaxExchangeRate (), aModel);
    aModel.setVatPointDate (d (aCreditNote.getTaxPointDateValue ()));
    _addBuyerReference (aModel, str (aCreditNote.getBuyerReferenceValue ()));
    aModel.setBuyerAccountingReference (str (aCreditNote.getAccountingCostValue ()));
    aModel.getProcessControl ().setBusinessProcessType (str (aCreditNote.getProfileIDValue ()));
    aModel.getProcessControl ().setSpecificationIdentifier (str (aCreditNote.getCustomizationIDValue ()));
    _readCommonReferences (aCreditNote.getOrderReference (),
                           aCreditNote.getContractDocumentReference (),
                           aCreditNote.getReceiptDocumentReference (),
                           aCreditNote.getDespatchDocumentReference (),
                           aCreditNote.getOriginatorDocumentReference (),
                           new CommonsArrayList <> (),
                           aModel);

    // BT-11, BT-18 + BG-24 from additional document references
    final ICommonsList <PhloxIdentifier> aObjectIDs = new CommonsArrayList <> ();
    for (final DocumentReferenceType aRef : aCreditNote.getAdditionalDocumentReference ())
    {
      // The binding of the credit note uses cbc:DocumentType, the type code of the invoice is
      // accepted as well
      if (CREDIT_NOTE_OBJECT_DOCUMENT_TYPE.equals (str (aRef.getDocumentTypeValue ())) ||
          EN16931CodeLists.DOCUMENT_TYPE_CODE_OBJECT_DOCUMENT.equals (str (aRef.getDocumentTypeCodeValue ())))
      {
        if (aRef.getID () != null)
          aObjectIDs.add (identifier (aRef.getID ()));
      }
      else
        if (aModel.getProjectReference () == null && _isOnlyAnID (aRef))
        {
          // The credit note of UBL 2.1 has no cac:ProjectReference
          aModel.setProjectReference (str (aRef.getIDValue ()));
        }
        else
        {
          final PhloxSupportingDocument aDoc = _readSupportingDocument (aRef);
          if (!aDoc.isEmpty ())
            aModel.supportingDocuments ().add (aDoc);
        }
    }
    PhloxReaderHelper.setAndAddRepeated (aObjectIDs,
                                         "BT-18",
                                         aModel::setInvoicedObjectIdentifier,
                                         aModel.additionalTerms ());

    _readPrecedingInvoiceRefs (aCreditNote.getBillingReference (), aModel);

    if (aCreditNote.getAccountingSupplierParty () != null)
      _readSeller (aCreditNote.getAccountingSupplierParty (), aModel.getSeller ());
    if (aCreditNote.getAccountingCustomerParty () != null)
      _readBuyer (aCreditNote.getAccountingCustomerParty (), aModel.getBuyer ());
    if (aCreditNote.getPayeeParty () != null)
      aModel.setPayee (_readPayee (aCreditNote.getPayeeParty (), aModel));
    if (aCreditNote.getTaxRepresentativeParty () != null)
      aModel.setSellerTaxRepresentative (_readSellerTaxRepresentative (aCreditNote.getTaxRepresentativeParty (),
                                                                       aModel));
    _readHeaderDelivery (aCreditNote.getDelivery (), aModel);
    _readInvoicePeriod (aCreditNote.getInvoicePeriod (), aModel);

    _readPaymentInstructions (aCreditNote.getPaymentMeans (),
                              aCreditNote.getAccountingSupplierParty (),
                              aCreditNote.getPayeeParty (),
                              aModel);
    // BT-9 in CN: PaymentMeans/PaymentDueDate
    _readPaymentDueDates (aCreditNote.getPaymentMeans (), aModel);
    _readPaymentTerms (aCreditNote.getPaymentTerms (), aModel);

    for (final AllowanceChargeType aAC : aCreditNote.getAllowanceCharge ())
      if (aAC.getChargeIndicator () != null && aAC.getChargeIndicator ().isValue ())
        aModel.documentCharges ().add (_readDocumentCharge (aAC, aModel.additionalTerms ()));
      else
        aModel.documentAllowances ().add (_readDocumentAllowance (aAC, aModel.additionalTerms ()));

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
    final CreditNoteType aCreditNote = unmarshal (UBL21Marshaller.creditNote (), aDoc, aErrors);
    return aCreditNote == null ? null : readCreditNote (aCreditNote, aErrors);
  }
}
