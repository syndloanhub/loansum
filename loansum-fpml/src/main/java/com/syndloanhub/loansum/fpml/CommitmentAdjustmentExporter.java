package com.syndloanhub.loansum.fpml;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.Adjustment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AmountAdjustmentEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.CommitmentAdjustment;

public class CommitmentAdjustmentExporter {

  static public CommitmentAdjustment convert(com.syndloanhub.loansum.product.facility.CommitmentAdjustment event) {
    CommitmentAdjustment fpml = FpMLHelper.factory.createCommitmentAdjustment();
    fpml.setEffectiveDate(event.getEffectiveDate());
    fpml.setRefusalAllowed(event.isRefusalAllowed());
    fpml.setScheduled(false); // TODO:
    fpml.setPik(event.isPik());
    Adjustment adj = FpMLHelper.factory.createAdjustment();
    if (event.getAmount().isPositive()) {
      adj.setAdjustmentType(AmountAdjustmentEnum.INCREASE);
      adj.setAmount(FpMLHelper.convert(event.getAmount()));
    } else {
      adj.setAdjustmentType(AmountAdjustmentEnum.DECREASE);
      adj.setAmount(FpMLHelper.convert(event.getAmount().negated()));
    }
    fpml.setAdjustment(adj);
    return fpml;
  }

}
