package com.syndloanhub.loansum.fpml;

import java.math.BigDecimal;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.FixedRateAccrual;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MoneyWithParticipantShare;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyReference;
import com.syndloanhub.loansum.product.facility.Accrual;
import com.syndloanhub.loansum.product.facility.Facility;

public class LoanContractExporter {
	static public LoanContract export(com.syndloanhub.loansum.product.facility.LoanContract contract,
			Facility facility) {
		LoanContract fpml = FpMLHelper.factory.createLoanContract();

		MoneyWithParticipantShare amount = new MoneyWithParticipantShare();
		amount.setAmount(contract.getAccrual().getAccrualAmount().getAmount());
		amount.setShareAmount(amount.getAmount());
		fpml.setAmount(amount);

		fpml.setMaturityDate(contract.getAccrual().getEndDate());
		fpml.setEffectiveDate(contract.getAccrual().getStartDate());
		fpml.setId(FpMLHelper.makeID(contract.getId().toString()));

		Party borrower = FpMLHelper.factory.createParty();
		borrower.setId(FpMLHelper.makeID(facility.getBorrower().toString()));

		PartyId borrowerId = FpMLHelper.factory.createPartyId();
		borrowerId.setPartyIdScheme(facility.getBorrower().getScheme());
		borrowerId.setValue(facility.getBorrower().getValue());
		borrower.getPartyId().add(borrowerId);

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

		return fpml;
	}
}
