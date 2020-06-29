package com.syndloanhub.loansum.fpml;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import javax.xml.datatype.DatatypeConfigurationException;

import com.opengamma.strata.basics.StandardId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanServicingNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyReference;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.LoanContractEventType;
import com.syndloanhub.loansum.product.facility.Repayment;

public class LoanServicingNotificationExporter {

  // Convert ALL of the data associated with a loansum facility into FpML.
  static public LoanServicingNotification convert(Facility facility) throws DatatypeConfigurationException {
    LoanServicingNotification fpml = FpMLHelper.factory.createLoanServicingNotification();
    fpml.setFpmlVersion("5-11");
    fpml.setNoticeDate(LocalDate.now());
    fpml.setHeader(FpMLHelper.makeHeader());
    fpml.setIsGlobalOnly(true);

    Party agent = FpMLHelper.makeParty(facility.getAgent());
    fpml.getParty().add(agent);
    PartyReference agentReference = FpMLHelper.makePartyReference(agent);

    Party borrower = FpMLHelper.makeParty(facility.getBorrower());
    fpml.getParty().add(borrower);
    PartyReference borrowerReference = FpMLHelper.makePartyReference(borrower);

    FacilityIdentifier facilityId = FpMLHelper.factory.createFacilityIdentifier();
    fpml.getDealIdentifierOrDealSummaryAndFacilityIdentifier()
        .add(FpMLHelper.factory.createLoanServicingNotificationFacilityIdentifier(facilityId));
    facilityId.setId(FpMLHelper.makeID(facility.getId()));
    facilityId.setPartyReference(agentReference);

    for (StandardId id : facility.getIdentifiers()) {
      InstrumentId instrumentId = FpMLHelper.factory.createInstrumentId();
      instrumentId.setInstrumentIdScheme(id.getScheme());
      instrumentId.setValue(id.getValue());
      facilityId.getInstrumentId().add(instrumentId);
    }

    FacilityReference facilityRef = FpMLHelper.factory.createFacilityReference();
    facilityRef.setHref(facilityId);

    // Add contracts.

    for (com.syndloanhub.loansum.product.facility.LoanContract contract : facility.getContracts()) {
      LoanContract loanContract = LoanContractExporter.export(contract, facility);
      loanContract.setFacilityReference(facilityRef);
      fpml.getDealIdentifierOrDealSummaryAndFacilityIdentifier().add(
          FpMLHelper.factory.createLoanServicingNotificationContract(loanContract));

      for (com.syndloanhub.loansum.product.facility.LoanContractEvent event : contract.getEvents()) {
        switch (event.getType()) {
          case RepaymentEvent:
            fpml.getFacilityEventGroupOrLcEventGroupOrLoanContractEventGroup().add(
                FpMLHelper.factory.createRepayment(RepaymentExporter.convert((Repayment) event, loanContract)));
          default:
            break;
        }
      }
    }

    fpml.getDealIdentifierOrDealSummaryAndFacilityIdentifier().add(
        FpMLHelper.factory.createLoanServicingNotificationFacilitySummary(FacilitySummaryExporter.convert(facility)));

    return fpml;
  }
}
