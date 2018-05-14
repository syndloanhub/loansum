
/**
 * Copyright (c) 2018 SyndLoanHub, LLC and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License from within this distribution and at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.syndloanhub.loansum.pricer.facility.prorated;

import static com.syndloanhub.loansum.pricer.facility.prorated.Explain.*;
import static com.syndloanhub.loansum.product.facility.CashFlowType.BenefitOfUnfunded;
import static com.syndloanhub.loansum.product.facility.CashFlowType.Borrowing;
import static com.syndloanhub.loansum.product.facility.CashFlowType.CostOfCarry;
import static com.syndloanhub.loansum.product.facility.CashFlowType.CostOfFunded;
import static com.syndloanhub.loansum.product.facility.CashFlowType.DelayedCompensation;
import static com.syndloanhub.loansum.product.facility.CashFlowType.EconomicBenefit;
import static com.syndloanhub.loansum.product.facility.CashFlowType.Fee;
import static com.syndloanhub.loansum.product.facility.CashFlowType.Interest;
import static com.syndloanhub.loansum.product.facility.CashFlowType.PikInterest;
import static com.syndloanhub.loansum.product.facility.Helper.EPSILON_1;
import static com.syndloanhub.loansum.product.facility.Helper.intersection;
import static com.syndloanhub.loansum.product.facility.Helper.intersects;
import static com.syndloanhub.loansum.product.facility.Helper.max;
import static com.syndloanhub.loansum.product.facility.Helper.tsget;
import static com.syndloanhub.loansum.product.facility.FacilityEventType.CommitmentAdjustmentEvent;
import static com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase.Participation;
import static com.syndloanhub.loansum.product.facility.LoanTradingType.Secondary;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.amount.CashFlow;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.syndloanhub.loansum.product.facility.AnnotatedCashFlow;
import com.syndloanhub.loansum.product.facility.AnnotatedCashFlows;
import com.syndloanhub.loansum.product.facility.CashFlowAnnotations;
import com.syndloanhub.loansum.product.facility.CashFlowType;
import com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase;
import com.syndloanhub.loansum.product.facility.LoanTradingType;
import com.syndloanhub.loansum.product.facility.prorated.ProratedAccrual;
import com.syndloanhub.loansum.product.facility.prorated.ProratedAccruingFee;
import com.syndloanhub.loansum.product.facility.prorated.ProratedCommitmentAdjustment;
import com.syndloanhub.loansum.product.facility.prorated.ProratedFacility;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanContract;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanContractEvent;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanEvent;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTradeList;

/**
 * The methods associated to the pricing of a loan trade.
 * <p>
 * This provides the ability to price {@link ProratedLoanTrade}.
 */
public class ProratedLoanTradePricer {
  @SuppressWarnings("unused")
  private static final Logger log = LoggerFactory.getLogger(ProratedLoanTradePricer.class);

  /**
   * Default implementation.
   */
  public static final ProratedLoanTradePricer DEFAULT = new ProratedLoanTradePricer();

  /**
   * Creates an instance.
   */
  public ProratedLoanTradePricer() {
  }

  /**
   * Return true if settlement is delayed and delayed closing fees are in effect.
   * 
   * @param trade loan trade
   * @param provider rates environment
   * @return true if settlement delayed
   */
  private boolean isSettlementDelayed(ProratedLoanTrade trade, RatesProvider provider) {
    TradeInfo info = trade.getInfo();

    return trade.getTradeType() == Secondary && trade.isDelayedCompensationFlag() && info.getSettlementDate().isPresent() &&
        info.getSettlementDate().get().isAfter(trade.getExpectedSettlementDate());
  }

  /**
   * Return the present value given a clean price. This amount is the sum of the proceeds of an offsetting trade settling on 
   * valuation date plus any accrued interest. If the trade is unsettled, then we net with the expected proceeds of the trade itself.
   * 
   * @param trade
   * @param provider
   * @param cleanPrice
   * @param explainBuilder
   * @return
   */
  public CurrencyAmount presentValueFromCleanPrice(ProratedLoanTrade trade, RatesProvider provider, double cleanPrice,
      Optional<ExplainMapBuilder> explainBuilder) {
    CurrencyAmount pv = CurrencyAmount.zero(trade.getProduct().getCurrency());
    final TradeInfo info = TradeInfo.builder()
        .settlementDate(provider.getValuationDate())
        .tradeDate(provider.getValuationDate())
        .build();
    final ProratedFacility facility = trade.getProduct();
    ProratedLoanTrade offsettingTrade = ProratedLoanTrade.builder()
        .accrualSettlementType(trade.getAccrualSettlementType())
        .amount(
            facility.getCommitmentAmount(provider.getValuationDate()) * tsget(trade.getPctShare(), provider.getValuationDate()))
        .association(trade.getAssociation())
        .averageLibor(trade.getAverageLibor())
        .buyer(trade.getSeller())
        .buySell(trade.getBuySell().isBuy() ? BuySell.SELL : BuySell.BUY)
        .commitmentReductionCreditFlag(trade.isCommitmentReductionCreditFlag())
        .currency(trade.getCurrency())
        .delayedCompensationFlag(trade.isDelayedCompensationFlag())
        .documentationType(trade.getDocumentationType())
        .expectedSettlementDate(provider.getValuationDate())
        .formOfPurchase(LoanTradingFormOfPurchase.Assignment)
        .info(info)
        .paydownOnTradeDate(false)
        .pctShare(trade.getPctShare())
        .price(cleanPrice)
        .product(trade.getProduct())
        .seller(trade.getBuyer())
        .tradeType(LoanTradingType.Secondary)
        .whenIssuedFlag(false)
        .build();
    final CurrencyAmount proceedsFromOffsettingTrade =
        purchasePrice(offsettingTrade, provider, provider.getValuationDate());
    final CurrencyAmount accruedInterest = accruedInterest(trade, provider);

    log.info("offsetting trade proceeds: " + proceedsFromOffsettingTrade);
    log.info("accrued interest: " + accruedInterest);
    pv = proceedsFromOffsettingTrade.plus(accruedInterest);

    return pv;
  }

  /**
   * Calculates the total accrued interest.
   * <p>
   * The total accrued interest as of valuation date.
   * 
   * @param trade  the trade
   * @param provider  the rates provider
   * @return total accrued interest
   */
  public CurrencyAmount accruedInterest(ProratedLoanTrade trade, RatesProvider provider) {
    ImmutableList<ProratedLoanContract> contracts = trade.getProduct().getContracts();
    CurrencyAmount accruedInterest = CurrencyAmount.of(trade.getProduct().getCurrency(), 0);

    for (ProratedLoanContract contract : contracts) {
      CurrencyAmount contractAccruedInterest = accruedInterest(contract, trade, provider);
      accruedInterest = accruedInterest.plus(contractAccruedInterest);
    }

    return accruedInterest;
  }

  /**
   * Calculate the cost of funded amount of a loan trade.
   * 
   * @param trade
   * @param provider
   * @return cost of funded in settlement currency
   */
  public CurrencyAmount costOfFunded(ProratedLoanTrade trade, RatesProvider provider, LocalDate settle,
      Optional<ExplainMapBuilder> explains) {
    CurrencyAmount costOfFunded = CurrencyAmount.zero(trade.getProduct().getCurrency());
    TradeInfo info = trade.getInfo();

    if (info.getSettlementDate().isPresent()) {
      double funded = trade.getProduct().getFundedAmount(settle);
      double cost = funded * trade.getPrice();
      double PIK = 0;

      // "PIKs travel for free"
      for (ProratedLoanContract contract : trade.getProduct().getContracts()) {
        if (contract.getAccrual().getPikSpread() > 0 && !info.getTradeDate().get().isAfter(contract.getAccrual().getEndDate()) &&
            !settle.isBefore(contract.getAccrual().getEndDate())) {
          PIK += contract.getAccrual().getDayCount().yearFraction(contract.getAccrual().getStartDate(),
              contract.getAccrual().getEndDate()) *
              contract.getAccrual().getPikSpread() * contract.getAccrual().getAccrualAmount().getAmount() * trade.getPrice();
        }
      }

      costOfFunded = CurrencyAmount.of(trade.getProduct().getCurrency(), cost - PIK);

      if (explains.isPresent()) {
        ExplainMapBuilder explainsBuilder = explains.get();
        explainsBuilder = explainsBuilder.openListEntry(CASHFLOW);
        explainsBuilder.put(SHARE_AMOUNT, costOfFunded.getAmount());
        explainsBuilder.put(SHARE_NOTIONAL, funded);
        explainsBuilder.put(PRICE, trade.getPrice());
        explainsBuilder.put(FREE_PIK, PIK);
        explainsBuilder.put(FORMULA, SHARE_NOTIONAL.getName() + " x " + PRICE.getName() + " - " + FREE_PIK.getName());
        explainsBuilder = explainsBuilder.closeListEntry(CASHFLOW);
      }
    }

    return costOfFunded;
  }

  /**
   * Calculate the benefit of non-par trade of unfunded amount.
   * 
   * @param trade
   * @param provider
   * @return benefit of unfunded in settlement currency
   */
  public CurrencyAmount benefitOfUnfunded(ProratedLoanTrade trade, RatesProvider provider, LocalDate settle) {
    CurrencyAmount benefitOfUnfunded = CurrencyAmount.zero(trade.getProduct().getCurrency());
    TradeInfo info = trade.getInfo();

    if (info.getSettlementDate().isPresent()) {
      double unfunded = trade.getProduct().getUnfundedAmount(settle);
      double cost = unfunded * (1 - trade.getPrice());

      benefitOfUnfunded = CurrencyAmount.of(trade.getProduct().getCurrency(), cost);
    }

    return benefitOfUnfunded;
  }

  /**
   * Calculate cost of carry delayed closing fee
   * 
   * @param trade
   * @param provider
   * @return cost of carry in settlement currency
   */
  public CurrencyAmount costOfCarry(ProratedLoanTrade trade, RatesProvider provider, Optional<ExplainMapBuilder> explains) {
    CurrencyAmount costOfCarry = CurrencyAmount.zero(trade.getProduct().getCurrency());
    TradeInfo info = trade.getInfo();

    if (isSettlementDelayed(trade, provider)) {
      CurrencyAmount expectedSettlePrice = purchasePrice(trade, provider, trade.getExpectedSettlementDate());
      CurrencyAmount actualSettlePrice = purchasePrice(trade, provider, info.getSettlementDate().get());
      double pctChange =
          Math.abs(expectedSettlePrice.minus(actualSettlePrice).getAmount()) / Math.abs(expectedSettlePrice.getAmount());

      if (pctChange <= 0.25) {
        costOfCarry = costOfCarry.plus(expectedSettlePrice.getAmount() * trade.getAverageLibor() *
            DayCounts.ACT_360.yearFraction(trade.getExpectedSettlementDate(), info.getSettlementDate().get()));

        if (explains.isPresent()) {
          ExplainMapBuilder explainsBuilder = explains.get();
          explainsBuilder = explainsBuilder.openListEntry(CASHFLOW);
          explainsBuilder.put(SHARE_AMOUNT, costOfCarry.getAmount());
          explainsBuilder.put(START_DATE, trade.getExpectedSettlementDate());
          explainsBuilder.put(END_DATE, info.getSettlementDate().get());
          explainsBuilder.put(DAY_COUNT, DayCounts.ACT_360);
          explainsBuilder.put(DAYS, DayCounts.ACT_360.days(trade.getExpectedSettlementDate(), info.getSettlementDate().get()));
          explainsBuilder.put(DIY, 360.0);
          explainsBuilder.put(AVG_LIBOR, trade.getAverageLibor());
          explainsBuilder.put(EXP_SETT_PX, expectedSettlePrice.getAmount());
          explainsBuilder.put(FORMULA,
              EXP_SETT_PX.getName() + " x " + AVG_LIBOR.getName() + " x " + DAYS.getName() + " / " + DIY.getName());
          explainsBuilder = explainsBuilder.closeListEntry(CASHFLOW);
        }
      } else {
        double averageLibor = trade.getAverageLibor();
        LocalDate startDate = trade.getExpectedSettlementDate();
        LocalDateDoubleTimeSeries commitment =
            trade.getProduct().getCommitment().subSeries(startDate, info.getSettlementDate().get());
        Iterator<LocalDate> it = commitment.dates().iterator();

        while (it.hasNext()) {
          LocalDate endDate = it.next();
          double purchasePrice = purchasePrice(trade, provider, startDate).getAmount();
          double amount = purchasePrice * averageLibor * DayCounts.ACT_360.yearFraction(startDate, endDate);

          if (explains.isPresent()) {
            ExplainMapBuilder explainsBuilder = explains.get();
            explainsBuilder = explainsBuilder.openListEntry(CASHFLOW);
            explainsBuilder.put(SHARE_AMOUNT, amount);
            explainsBuilder.put(START_DATE, startDate);
            explainsBuilder.put(END_DATE, endDate);
            explainsBuilder.put(DAY_COUNT, DayCounts.ACT_360);
            explainsBuilder.put(DAYS, DayCounts.ACT_360.days(startDate, endDate));
            explainsBuilder.put(DIY, 360.0);
            explainsBuilder.put(AVG_LIBOR, averageLibor);
            explainsBuilder.put(EXP_SETT_PX, purchasePrice);
            explainsBuilder.put(FORMULA,
                EXP_SETT_PX.getName() + " x " + AVG_LIBOR.getName() + " x " + DAYS.getName() + " / " + DIY.getName());
            explainsBuilder = explainsBuilder.closeListEntry(CASHFLOW);
          }

          costOfCarry = costOfCarry.plus(amount);
          startDate = endDate;
        }

        LocalDate endDate = info.getSettlementDate().get();
        double purchasePrice = purchasePrice(trade, provider, startDate).getAmount();
        double amount = purchasePrice * averageLibor * DayCounts.ACT_360.yearFraction(startDate, endDate);

        if (explains.isPresent()) {
          ExplainMapBuilder explainsBuilder = explains.get();
          explainsBuilder = explainsBuilder.openListEntry(CASHFLOW);
          explainsBuilder.put(SHARE_AMOUNT, amount);
          explainsBuilder.put(START_DATE, startDate);
          explainsBuilder.put(END_DATE, endDate);
          explainsBuilder.put(DAY_COUNT, DayCounts.ACT_360);
          explainsBuilder.put(DAYS, DayCounts.ACT_360.days(startDate, endDate));
          explainsBuilder.put(DIY, 360.0);
          explainsBuilder.put(AVG_LIBOR, averageLibor);
          explainsBuilder.put(EXP_SETT_PX, purchasePrice);
          explainsBuilder.put(FORMULA,
              EXP_SETT_PX.getName() + " x " + AVG_LIBOR.getName() + " x " + DAYS.getName() + " / " + DIY.getName());
          explainsBuilder = explainsBuilder.closeListEntry(CASHFLOW);
        }

        costOfCarry = costOfCarry.plus(amount);
      }
    }

    return costOfCarry;
  }

  /**
   * Calculate economic benefit of commitment reductions during settlement period
   * 
   * @param trade
   * @param provider
   * @return economic benefit in settlement currency
   */
  public CurrencyAmount economicBenefit(ProratedLoanTrade trade, RatesProvider provider, LocalDate settle,
      Optional<ExplainMapBuilder> explains) {
    CurrencyAmount economicBenefit = CurrencyAmount.zero(trade.getProduct().getCurrency());
    TradeInfo info = trade.getInfo();

    if (info.getSettlementDate().isPresent()) {
      double fundedAsOfTradeDate = trade.getProduct().getFundedAmount(info.getTradeDate().get());
      double fundedAsOfSettleDate = trade.getProduct().getFundedAmount(settle);
      double tradeDateRepayments = trade.getOriginalAmount() - trade.getAmount();
      double fundedChange = (fundedAsOfTradeDate - fundedAsOfSettleDate) + tradeDateRepayments;

      if (fundedChange > 0) {
        economicBenefit = economicBenefit.plus(fundedChange * (1.0 - trade.getPrice()));

        if (explains.isPresent()) {
          ExplainMapBuilder explainsBuilder = explains.get();
          explainsBuilder = explainsBuilder.openListEntry(CASHFLOW);
          explainsBuilder.put(SHARE_AMOUNT, economicBenefit.getAmount());
          explainsBuilder.put(PRICE, trade.getPrice());
          explainsBuilder.put(TRD_DT_FUNDED, fundedAsOfTradeDate);
          explainsBuilder.put(SETT_DT_FUNDED, fundedAsOfSettleDate);
          explainsBuilder.put(TRD_DT_REPAY, tradeDateRepayments);
          explainsBuilder.put(FORMULA,
              "(" + TRD_DT_FUNDED.getName() + " - " + SETT_DT_FUNDED.getName() + " + " + TRD_DT_REPAY.getName() + ") x (1 - " +
                  PRICE.getName() + ")");
          explainsBuilder = explainsBuilder.closeListEntry(CASHFLOW);
        }
      }
    }

    return economicBenefit;
  }

  /**
   * Calculate accrued contract interest as of valuation date
   * 
   * @param contract
   * @param trade
   * @param provider
   * @return
   */
  public CurrencyAmount accruedInterest(ProratedLoanContract contract, ProratedLoanTrade trade, RatesProvider provider) {
    TradeInfo info = trade.getInfo();
    CurrencyAmount zero = CurrencyAmount.zero(contract.getAccrual().getAccrualAmount().getCurrency());

    if (!info.getSettlementDate().isPresent())
      return zero;

    if (!intersects(provider.getValuationDate(), Pair.of(contract.getAccrual().getStartDate(),
        max(contract.getAccrual().getEndDate(), contract.getPaymentDate()))))
      return zero;

    Pair<LocalDate, LocalDate> accrualPeriod = intersection(
        Pair.of(info.getSettlementDate().get(), provider.getValuationDate()),
        Pair.of(contract.getAccrual().getStartDate(), contract.getAccrual().getEndDate()));

    double accruedInterest =
        contract.getAccrual().getDayCount().yearFraction(accrualPeriod.getFirst(), accrualPeriod.getSecond()) *
            contract.getAccrual().getAllInRate() * contract.getAccrual().getAccrualAmount().getAmount();

    return CurrencyAmount.of(contract.getAccrual().getAccrualAmount().getCurrency(), accruedInterest);
  }

  /**
   * Calculate delayed compensation of a loan trade.
   * 
   * @param trade
   * @param provider
   * @return
   */
  public CurrencyAmount delayedCompensation(ProratedLoanTrade trade, RatesProvider provider) {
    CurrencyAmount delayedCompensation = CurrencyAmount.zero(trade.getProduct().getCurrency());

    if (isSettlementDelayed(trade, provider)) {
      ImmutableList<ProratedLoanContract> contracts = trade.getProduct().getContracts();

      for (ProratedLoanContract contract : contracts) {
        for (ProratedAccrual accrual : contract.getAccrualSchedule()) {
          delayedCompensation =
              delayedCompensation.plus(delayedCompensation(accrual, contract, trade, provider, Optional.empty()));
        }
      }
    }

    return delayedCompensation;
  }

  /**
   * Calculate delayed compensation on a specific accrual.
   * 
   * @param accrual
   * @param contract
   * @param trade
   * @param provider
   * @param explainBuilder
   * @return
   */
  public CurrencyAmount delayedCompensation(ProratedAccrual accrual, ProratedLoanContract contract, ProratedLoanTrade trade,
      RatesProvider provider,
      Optional<ExplainMapBuilder> explainBuilder) {
    TradeInfo info = trade.getInfo();
    CurrencyAmount delayedCompensation = CurrencyAmount.zero(contract.getAccrual().getAccrualAmount().getCurrency());

    if (isSettlementDelayed(trade, provider)) {
      Pair<LocalDate, LocalDate> accrualPeriod = intersection(
          Pair.of(trade.getExpectedSettlementDate(), info.getSettlementDate().get()),
          Pair.of(accrual.getStartDate(), accrual.getEndDate()));

      if (accrualPeriod != null) {
        delayedCompensation =
            delayedCompensation
                .plus(accrual.getDayCount().yearFraction(accrualPeriod.getFirst(), accrualPeriod.getSecond()) *
                    accrual.getAllInRate() * accrual.getAccrualAmount().getAmount());

        if (explainBuilder.isPresent()) {
          final int days = accrual.getDayCount().days(accrualPeriod.getFirst(), accrualPeriod.getSecond());
          final double yearFraction =
              accrual.getDayCount().yearFraction(accrualPeriod.getFirst(), accrualPeriod.getSecond());
          final double diy = days / yearFraction;

          ExplainMapBuilder explainMapBuilder = explainBuilder.get();
          explainMapBuilder = explainMapBuilder.openListEntry(CASHFLOW);
          explainMapBuilder.put(SHARE_AMOUNT, delayedCompensation.getAmount());
          explainMapBuilder.put(START_DATE, accrualPeriod.getFirst());
          explainMapBuilder.put(END_DATE, accrualPeriod.getSecond());
          explainMapBuilder.put(DAYS, days);
          explainMapBuilder.put(DIY, diy);
          explainMapBuilder.put(DAY_COUNT, accrual.getDayCount());
          explainMapBuilder.put(ALLIN_RATE, accrual.getAllInRate());
          explainMapBuilder.put(SHARE_NOTIONAL, accrual.getAccrualAmount().getAmount());
          explainMapBuilder.put(FORMULA, SHARE_NOTIONAL.getName() + " x " + ALLIN_RATE.getName() + " x " +
              DAYS.getName() + " / " + DIY.getName());
          explainMapBuilder = explainMapBuilder.closeListEntry(CASHFLOW);
        }
      }
    }

    return delayedCompensation;
  }

  /**
   * Calculate the core purchase price of a trade.
   * 
   * @param trade
   * @param provider
   * @param settle
   * @return
   */
  public CurrencyAmount purchasePrice(ProratedLoanTrade trade, RatesProvider provider, LocalDate settle) {
    return costOfFunded(trade, provider, settle, Optional.empty())
        .minus(benefitOfUnfunded(trade, provider, settle))
        .minus(economicBenefit(trade, provider, settle, Optional.empty()));
  }

  /**
   * Determine if two cash flows are mergeable. Criteria are:
   * - cash flow types match (e.g. both interest payments)
   * - source is undefined or matches (e.g. same contract)
   * - currency matches
   * - payers and receivers match or are inverses
   * 
   * @param first
   * @param second
   * @return true if cash flows can be merged else false
   */
  private static boolean mergeable(AnnotatedCashFlow first, AnnotatedCashFlow second) {
    final boolean likeType = first.getAnnotation().getType().equals(second.getAnnotation().getType());
    final boolean likeCurrency =
        first.getCashFlow().getForecastValue().getCurrency().equals(second.getCashFlow().getForecastValue().getCurrency());
    final boolean likeSource = first.getAnnotation().getSource() != null && second.getAnnotation().getSource() != null &&
        first.getAnnotation().getSource().equals(second.getAnnotation().getSource());
    final boolean likeCounterparties =
        first.getAnnotation().getPayingCounterparty().equals(second.getAnnotation().getPayingCounterparty()) &&
            first.getAnnotation().getReceivingCounterparty().equals(second.getAnnotation().getReceivingCounterparty());
    final boolean reversedCounterparties =
        first.getAnnotation().getPayingCounterparty().equals(second.getAnnotation().getReceivingCounterparty()) &&
            first.getAnnotation().getReceivingCounterparty().equals(second.getAnnotation().getPayingCounterparty());

    return likeType && likeCurrency && likeSource && (likeCounterparties || reversedCounterparties);
  }

  /**
   * Determine if two cash flows can be summed vs netted based on whether the payer and
   * receiver are the same for each flow.
   * 
   * @param first
   * @param second
   * @return true if cash flows can be summed else false.
   */
  private static boolean addable(AnnotatedCashFlow first, AnnotatedCashFlow second) {
    return first.getAnnotation().getPayingCounterparty().equals(second.getAnnotation().getPayingCounterparty()) &&
        first.getAnnotation().getReceivingCounterparty().equals(second.getAnnotation().getReceivingCounterparty());
  }

  /**
   * Merge an annotated cash flow into a list of merged cash flows.
   * 
   * @param cashFlow
   * @param merged
   */
  private void merge(AnnotatedCashFlow cashFlow, Map<LocalDate, List<AnnotatedCashFlow>> merged) {
    if (!merged.containsKey(cashFlow.getCashFlow().getPaymentDate()))
      merged.put(cashFlow.getCashFlow().getPaymentDate(), new ArrayList<AnnotatedCashFlow>(Arrays.asList(cashFlow)));
    else {
      List<AnnotatedCashFlow> mergedCashFlows = merged.get(cashFlow.getCashFlow().getPaymentDate());

      for (ListIterator<AnnotatedCashFlow> it = mergedCashFlows.listIterator(); it.hasNext();) {
        AnnotatedCashFlow flow = it.next();

        if (mergeable(flow, cashFlow)) {
          if (addable(flow, cashFlow)) {
            CashFlowAnnotations mergedAnnotation = CashFlowAnnotations.builder()
                .source(flow.getAnnotation().getSource())
                .type(flow.getAnnotation().getType())
                .payingCounterparty(flow.getAnnotation().getPayingCounterparty())
                .receivingCounterparty(flow.getAnnotation().getReceivingCounterparty())
                .uncertain(flow.getAnnotation().isUncertain() || cashFlow.getAnnotation().isUncertain())
                .explains(mergeExplains(cashFlow.getAnnotation().getExplains(), flow.getAnnotation().getExplains(), true,
                    cashFlow.getAnnotation().getType()).orElse(null))
                .build();
            CashFlow mergedFlow = CashFlow.ofForecastValue(flow.getCashFlow().getPaymentDate(),
                flow.getCashFlow().getForecastValue().plus(cashFlow.getCashFlow().getForecastValue()), 1);

            it.set(AnnotatedCashFlow.builder()
                .annotation(mergedAnnotation)
                .cashFlow(mergedFlow)
                .build());
          } else {
            CurrencyAmount net = flow.getCashFlow().getForecastValue().minus(cashFlow.getCashFlow().getForecastValue());
            CashFlowAnnotations mergedAnnotations;

            if (Math.abs(net.getAmount()) > EPSILON_1) {
              if (net.getAmount() > 0)
                mergedAnnotations = CashFlowAnnotations.builder()
                    .source(flow.getAnnotation().getSource())
                    .type(flow.getAnnotation().getType())
                    .payingCounterparty(flow.getAnnotation().getPayingCounterparty())
                    .receivingCounterparty(flow.getAnnotation().getReceivingCounterparty())
                    .uncertain(flow.getAnnotation().isUncertain() || cashFlow.getAnnotation().isUncertain())
                    .explains(mergeExplains(cashFlow.getAnnotation().getExplains(), flow.getAnnotation().getExplains(), false,
                        cashFlow.getAnnotation().getType()).orElse(null))
                    .build();
              else {
                net = net.positive();
                mergedAnnotations = CashFlowAnnotations.builder()
                    .type(flow.getAnnotation().getType())
                    .source(flow.getAnnotation().getSource())
                    .payingCounterparty(cashFlow.getAnnotation().getPayingCounterparty())
                    .receivingCounterparty(cashFlow.getAnnotation().getReceivingCounterparty())
                    .uncertain(flow.getAnnotation().isUncertain() || cashFlow.getAnnotation().isUncertain())
                    .explains(mergeExplains(cashFlow.getAnnotation().getExplains(), flow.getAnnotation().getExplains(), false,
                        cashFlow.getAnnotation().getType()).orElse(null))
                    .build();
              }

              CashFlow mergedFlow = CashFlow.ofForecastValue(flow.getCashFlow().getPaymentDate(), net, 1);

              it.set(AnnotatedCashFlow.builder()
                  .annotation(mergedAnnotations)
                  .cashFlow(mergedFlow)
                  .build());
            } else
              it.remove();
          }

          return;
        }
      }

      mergedCashFlows.add(cashFlow);
    }
  }

  /**
   * Merge a list of cash flows into a list of merged cash flows.
   * 
   * @param tradeCashFlows
   * @param merged
   */
  private void merge(AnnotatedCashFlows tradeCashFlows, Map<LocalDate, List<AnnotatedCashFlow>> merged) {
    for (AnnotatedCashFlow cashFlow : tradeCashFlows.getCashFlows())
      merge(cashFlow, merged);
  }

  /**
   * Generate cash flows from a given collection of trades.
   * 
   * @param trades
   * @param provider
   * @param explain
   * @return
   */
  public AnnotatedCashFlows cashFlows(ProratedLoanTradeList trades, RatesProvider provider, boolean explain) {
    Map<LocalDate, List<AnnotatedCashFlow>> merged = new HashMap<LocalDate, List<AnnotatedCashFlow>>();

    for (ProratedLoanTrade trade : trades.getTrades())
      merge(cashFlows(trade, provider, explain), merged);

    ImmutableList.Builder<AnnotatedCashFlow> builder = ImmutableList.builder();

    merged.forEach((k, v) -> {
      builder.addAll(v);
    });

    return AnnotatedCashFlows.builder()
        .cashFlows(builder.build())
        .build();
  }

  /**
   * Calculate the set of cash flows exchanged on settlement date.
   * 
   * @param trade
   * @param provider
   * @param explain
   * @return
   */
  public AnnotatedCashFlows proceeds(ProratedLoanTrade trade, RatesProvider provider, boolean explain) {
    return AnnotatedCashFlows.builder().cashFlows(cashFlows(trade, provider, explain).getCashFlows()
        .stream()
        .filter(cashFlow -> cashFlow.getCashFlow().getPaymentDate().isEqual(trade.getInfo().getSettlementDate().get()))
        .collect(Collectors.toList()))
        .build();
  }

  /**
   * Calculates the past and future cash flows of a loan.
   * <p>
   * Each realized or expected cash flow is added to the result.
   * 
   * @param trade  the trade 
   * @param provider  the rates provider
   * @param explain optional explains builder
   * @return the cash flows
   */
  public AnnotatedCashFlows cashFlows(ProratedLoanTrade trade, RatesProvider provider, boolean explain) {
    ImmutableList.Builder<AnnotatedCashFlow> builder = ImmutableList.builder();
    TradeInfo info = trade.getInfo();

    if (info.getSettlementDate().isPresent()) {
      for (ProratedLoanContract contract : trade.getProduct().getContracts())
        builder.addAll(cashFlows(contract, trade, provider, explain));

      for (ProratedAccruingFee fee : trade.getProduct().getFees())
        builder.addAll(cashFlows(fee, trade, provider, explain));

      for (ProratedLoanEvent event : trade.getProduct().getEvents()) {
        if (event.getType() == CommitmentAdjustmentEvent) {
          ProratedCommitmentAdjustment adjustment = (ProratedCommitmentAdjustment) event;
          if (!adjustment.isRefusalAllowed()) {
            StandardId payingCounterparty = trade.getProduct().getAgent();
            StandardId receivingCounterparty = trade.getBuyer();

            if (trade.getFormOfPurchase() == Participation)
              payingCounterparty = trade.getSeller();

            if (trade.getBuySell().isSell()) {
              payingCounterparty = trade.getSeller();
              receivingCounterparty =
                  trade.getFormOfPurchase() == Participation ? trade.getBuyer() : trade.getProduct().getAgent();
            }

            builder.add(AnnotatedCashFlow.builder()
                .cashFlow(CashFlow.ofForecastValue(adjustment.getEffectiveDate(), adjustment.getAmount(), 1.0))
                .annotation(CashFlowAnnotations.builder()
                    .uncertain(false)
                    .payingCounterparty(payingCounterparty)
                    .receivingCounterparty(receivingCounterparty)
                    .type(Borrowing)
                    .build())
                .build());
          }
        }
      }

      Optional<ExplainMapBuilder> explainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();
      CurrencyAmount costOfFunded = costOfFunded(trade, provider, info.getSettlementDate().get(), explainBuilder);

      if (costOfFunded.getAmount() > EPSILON_1) {
        AnnotatedCashFlow cashFlow = AnnotatedCashFlow.builder()
            .cashFlow(CashFlow.ofForecastValue(info.getSettlementDate().get(), costOfFunded, 1.0))
            .annotation(CashFlowAnnotations.builder()
                .uncertain(false)
                .payingCounterparty(trade.getBuyer())
                .receivingCounterparty(trade.getSeller())
                .type(CostOfFunded)
                .explains(explainBuilder.orElse(ExplainMap.builder()).build())
                .build())
            .build();
        builder.add(cashFlow);
      }

      CurrencyAmount benefitOfUnfunded = benefitOfUnfunded(trade, provider, info.getSettlementDate().get());

      if (Math.abs(benefitOfUnfunded.getAmount()) > EPSILON_1) {
        builder.add(AnnotatedCashFlow.builder()
            .cashFlow(CashFlow.ofForecastValue(info.getSettlementDate().get(),
                benefitOfUnfunded.getAmount() < 0 ? benefitOfUnfunded.negated() : benefitOfUnfunded, 1.0))
            .annotation(CashFlowAnnotations.builder()
                .uncertain(false)
                .payingCounterparty(benefitOfUnfunded.getAmount() < 0 ? trade.getSeller() : trade.getBuyer())
                .receivingCounterparty(benefitOfUnfunded.getAmount() < 0 ? trade.getBuyer() : trade.getSeller())
                .type(BenefitOfUnfunded)
                .build())
            .build());
      }

      explainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();
      CurrencyAmount costOfCarry = costOfCarry(trade, provider, explainBuilder);

      if (costOfCarry.getAmount() > EPSILON_1)
        builder.add(AnnotatedCashFlow.builder()
            .cashFlow(CashFlow.ofForecastValue(info.getSettlementDate().get(), costOfCarry, 1.0))
            .annotation(CashFlowAnnotations.builder()
                .uncertain(false)
                .payingCounterparty(trade.getBuyer())
                .receivingCounterparty(trade.getSeller())
                .type(CostOfCarry)
                .explains(explainBuilder.orElse(ExplainMap.builder()).build())
                .build())
            .build());

      explainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();
      CurrencyAmount economicBenefit = economicBenefit(trade, provider, info.getSettlementDate().get(), explainBuilder);

      if (Math.abs(economicBenefit.getAmount()) > EPSILON_1)
        builder.add(AnnotatedCashFlow.builder()
            .cashFlow(CashFlow.ofForecastValue(info.getSettlementDate().get(),
                economicBenefit.getAmount() < 0 ? economicBenefit.negated() : economicBenefit, 1.0))
            .annotation(CashFlowAnnotations.builder()
                .uncertain(false)
                .payingCounterparty(economicBenefit.getAmount() < 0 ? trade.getBuyer() : trade.getSeller())
                .receivingCounterparty(economicBenefit.getAmount() < 0 ? trade.getSeller() : trade.getBuyer())
                .type(EconomicBenefit)
                .explains(explainBuilder.orElse(ExplainMap.builder()).build())
                .build())
            .build());
    }

    return AnnotatedCashFlows.builder()
        .cashFlows(builder.build())
        .build();
  }

  /**
   * Generate all of the cash flows associated with a single loan contract share.
   * 
   * @param contract
   * @param trade
   * @param provider
   * @param explain
   * @return
   */
  private ImmutableList<AnnotatedCashFlow> cashFlows(ProratedLoanContract contract, ProratedLoanTrade trade,
      RatesProvider provider, boolean explain) {
    ImmutableList.Builder<AnnotatedCashFlow> builder = ImmutableList.builder();
    TradeInfo info = trade.getInfo();
    StandardId payingCounterparty = trade.getProduct().getAgent();
    StandardId receivingCounterparty = trade.getBuyer();
    Currency currency = contract.getAccrual().getAccrualAmount().getCurrency();

    if (trade.getFormOfPurchase() == Participation)
      payingCounterparty = trade.getSeller();

    if (trade.getBuySell().isSell()) {
      payingCounterparty = trade.getSeller();
      receivingCounterparty = trade.getFormOfPurchase() == Participation ? trade.getBuyer() : trade.getProduct().getAgent();
    }

    // First, produce interest cash flows. There are 3 possible interest cash flows that a
    // contract might produce: delayed compensation, cash interest, and PIK interest.

    CurrencyAmount delayedCompensation = CurrencyAmount.zero(currency);
    Optional<ExplainMapBuilder> dcExplainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();

    CurrencyAmount interest = CurrencyAmount.zero(currency);
    Optional<ExplainMapBuilder> interestExplainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();

    CurrencyAmount pik = CurrencyAmount.zero(currency);
    Optional<ExplainMapBuilder> pikExplainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();

    for (ProratedAccrual accrual : contract.getAccrualSchedule()) {
      CurrencyAmount cashProjection = accrual.getPaymentProjection();

      if (cashProjection.getAmount() > EPSILON_1) {
        boolean isDelayedCompensation = accrual.getStartDate().isBefore(info.getSettlementDate().get());

        if (isDelayedCompensation)
          delayedCompensation = delayedCompensation.plus(cashProjection);
        else
          interest = interest.plus(cashProjection);

        if (explain)
          explainAccrual(isDelayedCompensation ? dcExplainBuilder.get() : interestExplainBuilder.get(), accrual, false);
      }

      CurrencyAmount pikProjection = accrual.getPikProjection();

      if (pikProjection.getAmount() > EPSILON_1) {
        pik = pik.plus(pikProjection);

        if (explain)
          explainAccrual(pikExplainBuilder.get(), accrual, true);
      }
    }

    if (delayedCompensation.getAmount() > EPSILON_1)
      builder.add(AnnotatedCashFlow.builder()
          .cashFlow(CashFlow.ofForecastValue(info.getSettlementDate().get(), currency, delayedCompensation.getAmount(), 1))
          .annotation(CashFlowAnnotations.builder()
              .source(contract.getId())
              .uncertain(false)
              .payingCounterparty(trade.getSeller())
              .receivingCounterparty(trade.getBuyer())
              .type(DelayedCompensation)
              .explains(dcExplainBuilder.orElse(ExplainMap.builder()).build())
              .build())
          .build());

    if (interest.getAmount() > EPSILON_1)
      builder.add(AnnotatedCashFlow.builder()
          .cashFlow(CashFlow.ofForecastValue(contract.getPaymentDate(), currency, interest.getAmount(), 1))
          .annotation(CashFlowAnnotations.builder()
              .source(contract.getId())
              //.source(trade.getId())
              .uncertain(contract.getAccrual().getEndDate().isAfter(provider.getValuationDate()))
              .payingCounterparty(payingCounterparty)
              .receivingCounterparty(receivingCounterparty)
              .type(Interest)
              .explains(interestExplainBuilder.orElse(ExplainMap.builder()).build())
              .build())
          .build());

    if (pik.getAmount() > EPSILON_1)
      builder.add(AnnotatedCashFlow.builder()
          .cashFlow(CashFlow.ofForecastValue(contract.getPaymentDate(), currency, pik.getAmount(), 1))
          .annotation(CashFlowAnnotations.builder()
              .source(contract.getId())
              .uncertain(contract.getAccrual().getEndDate().isAfter(provider.getValuationDate()))
              .payingCounterparty(payingCounterparty)
              .receivingCounterparty(receivingCounterparty)
              .type(PikInterest)
              .explains(pikExplainBuilder.orElse(ExplainMap.builder()).build())
              .build())
          .build());

    // Next, produce cash flows for each contract event.

    for (ProratedLoanContractEvent event : contract.getEvents()
        .stream()
        .filter(event -> event.getEffectiveDate().isAfter(info.getSettlementDate().get()))
        .collect(Collectors.toList())) {
      switch (event.getType()) {
        case BorrowingEvent:
          builder.add(AnnotatedCashFlow.builder()
              .annotation(CashFlowAnnotations.builder()
                  .source(contract.getId())
                  .uncertain(false)
                  .payingCounterparty(receivingCounterparty)
                  .receivingCounterparty(payingCounterparty)
                  .type(CashFlowType.Borrowing)
                  .build())
              .cashFlow(CashFlow.ofForecastValue(event.getEffectiveDate(), currency, event.getAmount().getAmount(), 1))
              .build());
          break;
        case RepaymentEvent:
          builder.add(AnnotatedCashFlow.builder()
              .annotation(CashFlowAnnotations.builder()
                  .source(contract.getId())
                  .uncertain(false)
                  .payingCounterparty(payingCounterparty)
                  .receivingCounterparty(receivingCounterparty)
                  .type(CashFlowType.Repayment)
                  .build())
              .cashFlow(CashFlow.ofForecastValue(event.getEffectiveDate(), currency, event.getAmount().getAmount(), 1))
              .build());
          break;
      }
    }

    return builder.build();
  }

  /**
   * Calculates the past and future cash flows of an accruing fee.
   * <p>
   * Each realized or expected fee cash flow is added to the result.
   * 
   * @param fee
   * @param trade
   * @param provider
   * @param explain
   * @return
   */
  public ImmutableList<AnnotatedCashFlow> cashFlows(ProratedAccruingFee fee, ProratedLoanTrade trade,
      RatesProvider provider, boolean explain) {
    ImmutableList.Builder<AnnotatedCashFlow> builder = ImmutableList.builder();
    StandardId payingCounterparty = trade.getProduct().getAgent();
    StandardId receivingCounterparty = trade.getBuyer();
    Currency currency = fee.getAccrual().getAccrualAmount().getCurrency();

    if (trade.getFormOfPurchase() == Participation)
      payingCounterparty = trade.getSeller();

    if (trade.getBuySell().isSell()) {
      payingCounterparty = trade.getSeller();
      receivingCounterparty = trade.getFormOfPurchase() == Participation ? trade.getBuyer() : trade.getProduct().getAgent();
    }

    CurrencyAmount sum = CurrencyAmount.zero(currency);
    Optional<ExplainMapBuilder> explainBuilder = explain ? Optional.of(ExplainMap.builder()) : Optional.empty();

    for (ProratedAccrual accrual : fee.getAccrualSchedule()) {
      CurrencyAmount cashProjection = accrual.getPaymentProjection();

      if (cashProjection.getAmount() > EPSILON_1)
        sum = sum.plus(accrual.getPaymentProjection());

      if (explain)
        explainAccrual(explainBuilder.get(), accrual, false);
    }

    if (sum.getAmount() > EPSILON_1)
      builder.add(AnnotatedCashFlow.builder()
          .cashFlow(CashFlow.ofForecastValue(fee.getPaymentDate(), currency, sum.getAmount(), 1))
          .annotation(CashFlowAnnotations.builder()
              .source(fee.getId())
              .uncertain(fee.getAccrual().getEndDate().isAfter(provider.getValuationDate()))
              .payingCounterparty(payingCounterparty)
              .receivingCounterparty(receivingCounterparty)
              .type(Fee)
              .explains(explainBuilder.orElse(ExplainMap.builder()).build())
              .build())
          .build());

    return builder.build();
  }

}
