package com.syndloanhub.loansum.fpml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.AccrualTypeId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateAccrual;

public class FloatingAccrualExporter {
	static public FloatingRateAccrual export(com.syndloanhub.loansum.product.facility.FloatingRateAccrual accrual) {
		FloatingRateAccrual fpml = FpMLHelper.factory.createFloatingRateAccrual();
		fpml.setAllInRate(BigDecimal.valueOf(accrual.getAllInRate()));
		fpml.setBaseRate(BigDecimal.valueOf(accrual.getBaseRate()));
		fpml.setDayCountFraction(FpMLHelper.convert(accrual.getDayCount()));
		fpml.setDefaultSpread(BigDecimal.valueOf(accrual.getSpread()));
		fpml.setEndDate(accrual.getEndDate());
		fpml.setStartDate(accrual.getStartDate());
		fpml.setNumberOfDays(BigInteger.valueOf(accrual.getDays()));
		fpml.setPaymentFrequency(FpMLHelper.convert(accrual.getPaymentFrequency()));
		fpml.setFloatingRateIndex(FpMLHelper.convert(accrual.getIndex()));
		fpml.setPaymentProjection(FpMLHelper.convert(accrual.getPaymentProjection(),
				accrual.getPaymentDate().orElse(accrual.getEndDate())));
		// TODO: accrual types in facility and accruals
		AccrualTypeId accrualTypeId = FpMLHelper.factory.createAccrualTypeId();
		accrualTypeId.setAccrualTypeIdScheme(FpMLHelper.NA_SCHEME);
		accrualTypeId.setValue("N/A");
		fpml.setAccrualOptionId(accrualTypeId);
		if (accrual.getPikSpread() > 0)
			fpml.setPikSpread(BigDecimal.valueOf(accrual.getPikSpread()));
		// TODO: add rate fixing date
		fpml.setRateFixingDate(LocalDate.of(1900, 1, 1));
		fpml.setSpread(BigDecimal.valueOf(accrual.getSpread()));
		return fpml;
	}
}
