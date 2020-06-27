package com.syndloanhub.loansum.fpml;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.ContractId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyReference;
import com.syndloanhub.loansum.product.facility.Facility;

public class LoanContractExporter {
	static public LoanContract export(com.syndloanhub.loansum.product.facility.LoanContract contract,
			Facility facility) {
		LoanContract fpml = FpMLHelper.factory.createLoanContract();
		fpml.setAmount(FpMLHelper.convert(contract.getAccrual().getAccrualAmount()));
		fpml.setMaturityDate(contract.getAccrual().getEndDate());
		fpml.setEffectiveDate(contract.getAccrual().getStartDate());
		fpml.setId(FpMLHelper.makeID(contract.getId()));
		
		ContractId contractId = FpMLHelper.factory.createContractId();
		contractId.setContractIdScheme(contract.getId().getScheme());
		contractId.setValue(contract.getId().getValue());
		fpml.getContractId().add(contractId);
		
		Party borrower = FpMLHelper.makeParty(facility.getBorrower());

		PartyReference borrowerReference = FpMLHelper.factory.createPartyReference();
		borrowerReference.setHref(borrower);

		fpml.setBorrowerPartyReference(borrowerReference);

		switch (contract.getAccrual().getAccrualType()) {
		case Fixed:
			fpml.setFixedRateAccrual(FixedAccrualExporter
					.export((com.syndloanhub.loansum.product.facility.FixedRateAccrual) contract.getAccrual()));
			break;
		case Floating:
			fpml.setFloatingRateAccrual(FloatingAccrualExporter
					.export((com.syndloanhub.loansum.product.facility.FloatingRateAccrual) contract.getAccrual()));
			break;
		default:
			break;
		}
		
		Party agent = FpMLHelper.makeParty(facility.getAgent());

		PartyReference agentReference = FpMLHelper.factory.createPartyReference();
		agentReference.setHref(agent);

		fpml.setPartyReference(agentReference);

		return fpml;
	}
}
