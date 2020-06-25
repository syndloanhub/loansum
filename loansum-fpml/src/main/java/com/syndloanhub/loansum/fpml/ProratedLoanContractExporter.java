package com.syndloanhub.loansum.fpml;

import java.math.BigDecimal;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MoneyWithParticipantShare;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanContract;

public class ProratedLoanContractExporter {
	static public LoanContract export(com.syndloanhub.loansum.product.facility.LoanContract contract, LoanTrade trade) {
		ObjectFactory factory = new ObjectFactory();
		LoanContract fpml = factory.createLoanContract();
		
		ProratedLoanContract proratedContract = contract.prorate(trade);

		MoneyWithParticipantShare amount = new MoneyWithParticipantShare();
		amount.setAmount(contract.getAccrual().getAccrualAmount().getAmount());
		amount.setShareAmount(proratedContract.getAccrual().getAccrualAmount().getAmount());
		fpml.setAmount(amount);

		fpml.setMaturityDate(contract.getAccrual().getEndDate());

		return fpml;
	}
}
