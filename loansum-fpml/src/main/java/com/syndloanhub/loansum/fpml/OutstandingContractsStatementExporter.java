package com.syndloanhub.loansum.fpml;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.DatatypeConfigurationException;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.tuple.Pair;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.OutstandingContractsStatement;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyReference;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.Helper;

public class OutstandingContractsStatementExporter {
	static public OutstandingContractsStatement export(LocalDate effectiveDate, Facility facility, boolean allContracts)
			throws DatatypeConfigurationException {
		ObjectFactory factory = new ObjectFactory();
		OutstandingContractsStatement fpml = factory.createOutstandingContractsStatement();
		fpml.setFpmlVersion("5-11");
		fpml.setStatementDate(effectiveDate);
		fpml.setHeader(FpMLHelper.getHeader(factory));

		FacilityIdentifier facilityId = factory.createFacilityIdentifier();
		facilityId.setId(FpMLHelper.makeID(facility.getId().toString()));

		Party agent = factory.createParty();
		agent.setId(FpMLHelper.makeID(facility.getAgent().toString()));

		PartyId agentId = factory.createPartyId();
		agentId.setPartyIdScheme(facility.getAgent().getScheme());
		agentId.setValue(facility.getAgent().getValue());
		agent.getPartyId().add(agentId);

		PartyReference agentReference = factory.createPartyReference();
		agentReference.setHref(agent);

		facilityId.setPartyReference(agentReference);

		fpml.setFacilityIdentifier(facilityId);
		fpml.getParty().add(agent);

		for (StandardId id : facility.getIdentifiers()) {
			InstrumentId instrumentId = factory.createInstrumentId();
			instrumentId.setInstrumentIdScheme(id.getScheme());
			instrumentId.setValue(id.getValue());
			facilityId.getInstrumentId().add(instrumentId);
		}

		for (com.syndloanhub.loansum.product.facility.LoanContract contract : facility.getContracts()) {
			if (allContracts || Helper.intersects(effectiveDate,
					Pair.of(contract.getAccrual().getStartDate(), contract.getAccrual().getEndDate()))) {
				LoanContract loanContract = LoanContractExporter.export(contract, facility);
				fpml.getLoanContractOrLetterOfCredit().add(loanContract);
				fpml.getParty().add((Party) loanContract.getBorrowerPartyReference().getHref());
			}
		}

		return fpml;
	}
}
