package com.syndloanhub.loansum.fpml;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeNotification;

public class LoanTradeNotificationExporter {
  static public LoanTradeNotification convert(com.syndloanhub.loansum.product.facility.LoanTrade trade) {
    LoanTradeNotification fpml = FpMLHelper.factory.createLoanTradeNotification();
    fpml.setTrade(LoanTradeExporter.convert(trade));
    return fpml;
  }
}
