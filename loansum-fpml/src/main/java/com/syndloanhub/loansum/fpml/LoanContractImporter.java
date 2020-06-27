package com.syndloanhub.loansum.fpml;

import com.opengamma.strata.basics.StandardId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ContractId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.product.facility.Accrual;

public class LoanContractImporter {
  static public com.syndloanhub.loansum.product.facility.LoanContract convert(LoanContract fpmlContract) {
    ContractId contractId = fpmlContract.getContractId().get(0);
    Accrual accrual = null;

    if (fpmlContract.getFloatingRateAccrual() != null) {
      accrual = FloatingAccrualImporter.convert(fpmlContract.getFloatingRateAccrual());
    }

    return com.syndloanhub.loansum.product.facility.LoanContract.builder()
        .id(StandardId.of(contractId.getContractIdScheme(), contractId.getValue()))
        .accrual(accrual)
        .paymentDate(accrual.getPaymentDate().orElse(accrual.getEndDate()))
        .build();
  }
}
