package com.syndloanhub.loansum.fpml;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade;

public class LoanTradeExporter {
  static public LoanTrade convert(com.syndloanhub.loansum.product.facility.LoanTrade trade) {
    LoanTrade fpml = FpMLHelper.factory.createLoanTrade();
    fpml.setAccrualSettlementType(FpMLHelper.convert(trade.getAccrualSettlementType()));
    return fpml;
  }
}
