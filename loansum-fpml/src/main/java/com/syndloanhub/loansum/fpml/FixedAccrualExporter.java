package com.syndloanhub.loansum.fpml;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.FixedRateAccrual;

public class FixedAccrualExporter {
	static public FixedRateAccrual export(com.syndloanhub.loansum.product.facility.FixedRateAccrual accrual) {
		FixedRateAccrual fpml = FpMLHelper.factory.createFixedRateAccrual();
		return fpml;
	}
}
