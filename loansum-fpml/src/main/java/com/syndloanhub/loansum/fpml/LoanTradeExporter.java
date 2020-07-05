package com.syndloanhub.loansum.fpml;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.BuySellEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.BuyerSellerAmounts;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTransferFee;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTransferFeePaidByEnum;

public class LoanTradeExporter {
  static public LoanTrade convert(com.syndloanhub.loansum.product.facility.LoanTrade trade) {
    LoanTrade fpml = FpMLHelper.factory.createLoanTrade();
    fpml.setId(FpMLHelper.nextID());
    fpml.setAccrualSettlementType(FpMLHelper.convert(trade.getAccrualSettlementType()));
    fpml.setIssuer(FpMLHelper.makeIssuerId(trade));
    fpml.setTradeId(FpMLHelper.makeTradeId(trade));
    fpml.setAmount(FpMLHelper.convertToNonNegativeMoney(CurrencyAmount.of(trade.getCurrency(), trade.getAmount())));
    fpml.setTradeDate(trade.getInfo().getTradeDate().get());
    fpml.setBuyerPartyReference(FpMLHelper.makePartyReference(trade.getBuyer()));
    fpml.setSellerPartyReference(FpMLHelper.makePartyReference(trade.getSeller()));
    fpml.setMarketType(FpMLHelper.convert(trade.getTradeType()));
    fpml.setWhenIssuedFlag(trade.isWhenIssuedFlag());
    fpml.setTradingAssociation(FpMLHelper.convert(trade.getAssociation()));
    fpml.setFormOfPurchase(FpMLHelper.convert(trade.getFormOfPurchase()));
    fpml.setRemittedBy(BuySellEnum.BUYER); // TODO: add to loansum
    fpml.setPrice(trade.getPrice());

    // TODO: add transfer fee to loansum
    LoanTransferFee transferFee = FpMLHelper.factory.createLoanTransferFee();
    transferFee.setPaidBy(LoanTransferFeePaidByEnum.SPLIT_FULL);
    transferFee.setTotalAmount(FpMLHelper.convertToNonNegativeMoney(CurrencyAmount.zero(trade.getCurrency())));

    /*
    BuyerSellerAmounts amounts = FpMLHelper.factory.createBuyerSellerAmounts();
    amounts.setBuyersAmount(FpMLHelper.convertToNonNegativeMoney(CurrencyAmount.zero(trade.getCurrency())));
    amounts.setSellersAmount(FpMLHelper.convertToNonNegativeMoney(CurrencyAmount.zero(trade.getCurrency())));
    transferFee.setTransferFeeAmounts(amounts);
*/
    fpml.setTransferFee(transferFee);
    fpml.setDocumentationType(FpMLHelper.convert(trade.getDocumentationType()));
    fpml.setDelayedCompensationFlag(true); // TODO: add to loansum
    fpml.setOtherFeesBenefactor(BuySellEnum.BUYER); // TODO: add to loansum
    return fpml;
  }
}
