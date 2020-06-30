package com.syndloanhub.loansum.fpml;

import java.time.LocalDate;

import javax.xml.datatype.DatatypeConfigurationException;

import com.opengamma.strata.basics.StandardId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityCommitment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.TermLoan;

import com.syndloanhub.loansum.product.facility.FeeAndRateOption;
import com.syndloanhub.loansum.product.facility.FeeAndRateOptionType;

public class TermLoanExporter {

  static public TermLoan convert(com.syndloanhub.loansum.product.facility.Facility facility)
      throws DatatypeConfigurationException {
    TermLoan fpml = FpMLHelper.factory.createTermLoan();

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

    for (FeeAndRateOption option : facility.getOptions()) {
      switch (option.getOptionType()) {
        case FloatingRate:
          fpml.getFixedRateOptionOrFloatingRateOptionOrLcOption().add(FloatingRateOptionExporter.convert(option));
          break;
        default:
          break;
      }
    }

    return fpml;
  }
}
