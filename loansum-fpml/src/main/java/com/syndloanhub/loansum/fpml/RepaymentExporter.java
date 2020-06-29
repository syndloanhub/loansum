package com.syndloanhub.loansum.fpml;

import java.math.BigDecimal;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContractReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Repayment;

public class RepaymentExporter {

  static public Repayment convert(com.syndloanhub.loansum.product.facility.Repayment event, LoanContract contract) {
    Repayment fpml = FpMLHelper.factory.createRepayment();
    fpml.setAmount(FpMLHelper.convert(event.getAmount()));
    fpml.setPrice(BigDecimal.valueOf(event.getPrice()));
    fpml.setEffectiveDate(event.getEffectiveDate());
    fpml.getEventIdentifier().add(FpMLHelper.makeBusinessEventIdentifier(contract.getPartyReference().getHref()));
    LoanContractReference ref = FpMLHelper.factory.createLoanContractReference();
    ref.setHref(contract);
    fpml.setLoanContractReference(ref);
    return fpml;
  }
}
