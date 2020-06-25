package com.syndloanhub.loansum.fpml;

import java.math.BigDecimal;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateAccrual;

public class FloatingAccrualExporter {
	static public FloatingRateAccrual export(com.syndloanhub.loansum.product.facility.FloatingRateAccrual accrual) {
		FloatingRateAccrual fpml = FpMLHelper.factory.createFloatingRateAccrual();
		fpml.setAllInRate(accrual.getAllInRate());
		fpml.setBaseRate(accrual.getBaseRate());
		fpml.setDayCountFraction(FpMLHelper.convert(accrual.getDayCount()));
		fpml.setDefaultSpread(accrual.getSpread());
		fpml.setEndDate(accrual.getEndDate());
		fpml.setStartDate(accrual.getStartDate());
		//fpml.setNumberOfDays(accrual.getDays());
		return fpml;
	}
}
