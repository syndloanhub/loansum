package com.syndloanhub.loansum.fpml;

import java.util.Random;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.AccrualTypeId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateOption;

public class FloatingRateOptionExporter {

  static public FloatingRateOption convert(com.syndloanhub.loansum.product.facility.FeeAndRateOption option) {
    FloatingRateOption fpml = FpMLHelper.factory.createFloatingRateOption();
    AccrualTypeId id = FpMLHelper.factory.createAccrualTypeId();
    id.setAccrualTypeIdScheme(FpMLHelper.OPTION_SCHEME);
    id.setValue("" + Math.abs(new Random().nextInt()));
    fpml.setAccrualOptionId(id);
    fpml.setSpread(option.getRate());
    fpml.setPikSpread(option.getPikSpread());
    fpml.setCurrency(FpMLHelper.exportCurrency(option.getCurrency()));
    fpml.setStartDate(option.getStartDate());
    fpml.setEndDate(option.getEndDate());
    fpml.setFloatingRateIndex(FpMLHelper.convert(option.getIndex().get()));
    fpml.setDayCountFraction(FpMLHelper.convert(option.getDayCount()));
    return fpml;
  }
}
