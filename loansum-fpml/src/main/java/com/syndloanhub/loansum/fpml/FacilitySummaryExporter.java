package com.syndloanhub.loansum.fpml;

import java.time.LocalDate;

import com.opengamma.strata.basics.StandardId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityCommitment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilitySummary;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;

public class FacilitySummaryExporter {

  static public FacilitySummary convert(com.syndloanhub.loansum.product.facility.Facility facility) {
    FacilitySummary fpml = FpMLHelper.factory.createFacilitySummary();

    for (StandardId id : facility.getIdentifiers()) {
      InstrumentId instrumentId = FpMLHelper.factory.createInstrumentId();
      instrumentId.setInstrumentIdScheme(id.getScheme());
      instrumentId.setValue(id.getValue());
      fpml.getInstrumentId().add(instrumentId);
    }

    fpml.setAgentPartyReference(FpMLHelper.makePartyReference(facility.getAgent()));
    fpml.setBorrowerPartyReference(FpMLHelper.makePartyReference(facility.getBorrower()));
    fpml.setOriginalCommitment(FpMLHelper.convert(facility.getOriginalCommitmentAmount()));
    fpml.setPartyReference(fpml.getAgentPartyReference());
    fpml.setStartDate(facility.getStartDate());
    fpml.setMaturityDate(facility.getMaturityDate());

    FacilityCommitment commitment = FpMLHelper.factory.createFacilityCommitment();
    commitment.setTotalCommitmentAmount(FpMLHelper.convert(facility.getCommitmentAmount(LocalDate.now())));

    fpml.setCurrentCommitment(commitment);

    return fpml;
  }
}
