package com.syndloanhub.loansum.fpml;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilitySummary;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContractReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Repayment;

public class FacilitySummaryExporter {

  static public FacilitySummary convert(com.syndloanhub.loansum.product.facility.Facility facility) {
    FacilitySummary fpml = FpMLHelper.factory.createFacilitySummary();
    return fpml;
  }
}
