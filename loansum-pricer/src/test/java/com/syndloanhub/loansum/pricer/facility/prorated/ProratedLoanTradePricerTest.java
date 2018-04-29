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

import static org.testng.Assert.assertEquals;
import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.opengamma.strata.product.common.BuySell.SELL;
import static com.syndloanhub.loansum.product.facility.FacilityType.Revolving;
import static com.syndloanhub.loansum.product.facility.FacilityType.Term;
import static com.syndloanhub.loansum.product.facility.LoanTradingAccrualSettlement.SettledWithoutAccrued;
import static com.syndloanhub.loansum.product.facility.LoanTradingDocType.Par;
import static com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase.Assignment;
import static com.syndloanhub.loansum.product.facility.LoanTradingType.Primary;
import static com.syndloanhub.loansum.product.facility.LoanTradingAssoc.LSTA;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.joda.beans.ser.JodaBeanSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.syndloanhub.loansum.pricer.facility.prorated.ProratedLoanTradePricer;
import com.syndloanhub.loansum.product.facility.AccruingFee;
import com.syndloanhub.loansum.product.facility.AnnotatedCashFlows;
import com.syndloanhub.loansum.product.facility.Borrowing;
import com.syndloanhub.loansum.product.facility.CommitmentAdjustment;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.FixedRateAccrual;
import com.syndloanhub.loansum.product.facility.FloatingRateAccrual;
import com.syndloanhub.loansum.product.facility.LoanContract;
import com.syndloanhub.loansum.product.facility.FacilityEvent;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.Repayment;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTradeList;

/**
 * Tests {@link ProratedLoanTradePricer}.
 */
@Test
public class ProratedLoanTradePricerTest {
  private static final Logger log = LoggerFactory
      .getLogger(ProratedLoanTradePricerTest.class);
  private static boolean regenerate = false;

  public void test_termLoan_1() throws IOException {
    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 31))
        .amount(CurrencyAmount.of(Currency.USD, 4050000)).build();
    final Repayment REPAYMENT_2 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 30))
        .amount(CurrencyAmount.of(Currency.USD, 4558012.17)).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 1, 24))
                .endDate(LocalDate.of(2017, 3, 16))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.50283 / 100)
                .baseRate(1.2583 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1598500000))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 16)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 16))
                .endDate(LocalDate.of(2017, 4, 20))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.38733 / 100.0)
                .baseRate(1.13733 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1598500000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).events(REPAYMENT_1)
        .build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 20))
                .endDate(LocalDate.of(2017, 4, 26))
                .allInRate(4.38733 / 100.0)
                .baseRate(1.13733 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1794450000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 26))
                .endDate(LocalDate.of(2017, 7, 26))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.4165 / 100)
                .baseRate(1.1665 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1794450000))
                .build())
        .paymentDate(LocalDate.of(2017, 7, 26)).events(REPAYMENT_2)
        .build();

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 4, 20))
        .amount(CurrencyAmount.of(Currency.USD, 200000000)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN1"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2017, 1, 24))
        .maturityDate(LocalDate.of(2022, 8, 14))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4))
        .events(ADJUSTMENT_1)
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 1598500000))
        .identifiers(
            Arrays.asList(StandardId.of("LXID", "LX123456"),
                StandardId.of("CUSIP", "012345678"),
                StandardId.of("BLOOMBERGID", "BB12345678")))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 3, 21))
        .settlementDate(LocalDate.of(2017, 4, 10)).build();

    LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(3000000)
        .price(101.125 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 3, 30))
        .averageLibor(0.9834 / 100).buySell(BUY)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 7, 14)).build();
    final LocalDate settle = PRORATED_LOAN_TRADE.getInfo()
        .getSettlementDate().get();

    assertEquals(PRICER.delayedCompensation(PRORATED_LOAN_TRADE, PROV)
        .getAmount(), 4012.46, 1E-2);
    assertEquals(
        PRICER.costOfFunded(PRORATED_LOAN_TRADE, PROV, settle,
            Optional.empty()).getAmount(),
        3026063.61, 1E-2);
    assertEquals(
        PRICER.costOfCarry(PRORATED_LOAN_TRADE, PROV, Optional.empty())
            .getAmount(),
        911.59, 1E-2);
    assertEquals(
        PRICER.economicBenefit(PRORATED_LOAN_TRADE, PROV, settle,
            Optional.empty()).getAmount(),
        -85.51, 1E-2);

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE,
        PROV, true);

    String cfFileName = "src/test/resources/aliantcf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);

    cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE, PROV, false);

    cfFileName = "src/test/resources/aliantcf_ne.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(
        new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_revolvingLoan_1() throws IOException {
    final Borrowing BORROW_1 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2016, 8, 1))
        .amount(CurrencyAmount.of(Currency.USD, 225000000)).build();
    final Borrowing BORROW_2 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2016, 8, 4))
        .amount(CurrencyAmount.of(Currency.USD, 75000000)).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 1))
                .endDate(LocalDate.of(2016, 8, 17))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.4939 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        225000000))
                .build())
        .paymentDate(LocalDate.of(2016, 8, 17)).events(BORROW_1)
        .build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 4))
                .endDate(LocalDate.of(2016, 8, 17))
                .allInRate(3.4939 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        75000000))
                .build())
        .paymentDate(LocalDate.of(2016, 8, 17)).events(BORROW_2)
        .build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 17))
                .endDate(LocalDate.of(2016, 9, 19))
                .allInRate(3.50744 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        300000000))
                .build())
        .paymentDate(LocalDate.of(2016, 9, 19)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 9, 19))
                .endDate(LocalDate.of(2016, 10, 17))
                .allInRate(3.53094 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        300000000))
                .build())
        .paymentDate(LocalDate.of(2016, 10, 17)).build();
    final LoanContract CONTRACT_5 = LoanContract
        .builder()
        .id(StandardId.of("contract", "5"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 10, 17))
                .endDate(LocalDate.of(2016, 11, 17))
                .allInRate(3.53456 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        300000000))
                .build())
        .paymentDate(LocalDate.of(2016, 11, 17)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN2"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2016, 7, 28))
        .maturityDate(LocalDate.of(2017, 3, 24))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4, CONTRACT_5))
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 300000000))
        .facilityType(Revolving).build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2016, 8, 1))
        .settlementDate(LocalDate.of(2016, 8, 1)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(300000000)
        .price(100 / 100)
        .expectedSettlementDate(TRADE_INFO.getSettlementDate().get())
        .buySell(BUY).accrualSettlementType(SettledWithoutAccrued)
        .association(LSTA).commitmentReductionCreditFlag(true)
        .currency(Currency.USD).delayedCompensationFlag(true)
        .documentationType(Par).formOfPurchase(Assignment)
        .paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2016, 8, 2)).build();

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE,
        PROV, true);
    String cfFileName = "src/test/resources/stoneridgecf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);

    cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE, PROV, false);
    cfFileName = "src/test/resources/stoneridgecf_ne.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(
        new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_revolvingLoan_2() throws IOException {
    final Borrowing BORROW_1 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2016, 8, 1))
        .amount(CurrencyAmount.of(Currency.USD, 225000000)).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 1))
                .endDate(LocalDate.of(2016, 8, 17))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.4939 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        225000000))
                .build())
        .paymentDate(LocalDate.of(2016, 8, 17)).events(BORROW_1)
        .build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 17))
                .endDate(LocalDate.of(2016, 9, 19))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.50744 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        225000000))
                .build())
        .paymentDate(LocalDate.of(2016, 9, 19)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 9, 19))
                .endDate(LocalDate.of(2016, 10, 17))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.53094 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        225000000))
                .build())
        .paymentDate(LocalDate.of(2016, 10, 17)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 10, 17))
                .endDate(LocalDate.of(2016, 11, 17))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.53456 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        225000000))
                .build())
        .paymentDate(LocalDate.of(2016, 11, 17)).build();

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2016, 8, 17))
        .amount(CurrencyAmount.of(Currency.USD, -75000000)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN3"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2016, 7, 28))
        .maturityDate(LocalDate.of(2017, 3, 24))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4))
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 300000000))
        .facilityType(Revolving).events(ADJUSTMENT_1).build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2016, 8, 1))
        .settlementDate(LocalDate.of(2016, 8, 1)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(300000000)
        .price(100 / 100)
        .expectedSettlementDate(TRADE_INFO.getSettlementDate().get())
        .buySell(BUY).accrualSettlementType(SettledWithoutAccrued)
        .association(LSTA).commitmentReductionCreditFlag(true)
        .currency(Currency.USD).delayedCompensationFlag(true)
        .documentationType(Par).formOfPurchase(Assignment)
        .paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2016, 8, 2)).build();

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);
    final String cfFileName = "src/test/resources/stoneridgecf2.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_revolvingLoan_3() throws IOException {
    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .dayCount(DayCounts.ACT_360)
                .startDate(LocalDate.of(2017, 1, 1))
                .endDate(LocalDate.of(2017, 2, 1))
                .allInRate(10.0 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        50000000))
                .build())
        .paymentDate(LocalDate.of(2017, 2, 1)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 2, 1))
                .endDate(LocalDate.of(2017, 3, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(10.0 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        50000000))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 1)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 1))
                .endDate(LocalDate.of(2017, 4, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(10.0 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        50000000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 1)).build();

    final LocalDateDoubleTimeSeries COMMITMENT = LocalDateDoubleTimeSeries
        .builder().put(LocalDate.of(2017, 1, 1), 50000000)
        .put(LocalDate.of(2017, 3, 1), 100000000).build();

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 1))
        .amount(CurrencyAmount.of(Currency.USD, 50000000)).build();

    final Facility LOAN = Facility.builder()
        .id(StandardId.of("lid", "LOAN4"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2016, 1, 1))
        .maturityDate(LocalDate.of(2018, 1, 1))
        .contracts(Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3))
        .totalCommitmentSchedule(COMMITMENT)
        .originalCommitmentAmount(CurrencyAmount.zero(Currency.USD))
        .facilityType(Revolving).events(ADJUSTMENT_1).build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 1, 1))
        .settlementDate(LocalDate.of(2017, 1, 1)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(50000000)
        .price(100 / 100)
        .expectedSettlementDate(TRADE_INFO.getSettlementDate().get())
        .buySell(BUY).accrualSettlementType(SettledWithoutAccrued)
        .association(LSTA).commitmentReductionCreditFlag(true)
        .currency(Currency.USD).delayedCompensationFlag(true)
        .documentationType(Par).formOfPurchase(Assignment)
        .paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 5, 1)).build();

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);

    final String cfFileName = "src/test/resources/stoneridgecf3.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_termLoan_2() throws IOException {
    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 1, 3))
                .endDate(LocalDate.of(2017, 2, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(0)
                .pikSpread(15.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        15806946.89))
                .build())
        .paymentDate(LocalDate.of(2017, 2, 1)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 2, 1))
                .endDate(LocalDate.of(2017, 3, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(0)
                .pikSpread(15.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        15997947.50))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 1)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 1))
                .endDate(LocalDate.of(2017, 4, 3))
                .dayCount(DayCounts.ACT_360)
                .allInRate(0)
                .pikSpread(15.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        16184590.22))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 3)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 3))
                .endDate(LocalDate.of(2017, 5, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(0)
                .pikSpread(15.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        16407128.33))
                .build())
        .paymentDate(LocalDate.of(2017, 5, 1)).build();
    final LoanContract CONTRACT_5 = LoanContract
        .builder()
        .id(StandardId.of("contract", "5"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 1))
                .endDate(LocalDate.of(2017, 6, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(0)
                .pikSpread(15.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        16598544.83))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 1)).build();
    final LoanContract CONTRACT_6 = LoanContract
        .builder()
        .id(StandardId.of("contract", "6"))
        .accrual(
            FixedRateAccrual
                .builder()
                .dayCount(DayCounts.ACT_360)
                .startDate(LocalDate.of(2017, 6, 1))
                .endDate(LocalDate.of(2017, 7, 3))
                .allInRate(15.0 / 100.0)
                .pikSpread(0.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        16598544.83 + 214397.87))
                .build())
        .paymentDate(LocalDate.of(2017, 7, 3))
        .build();

    List<LoanContract> contracts = Arrays.asList(CONTRACT_1, CONTRACT_2,
        CONTRACT_3, CONTRACT_4, CONTRACT_5, CONTRACT_6);

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN5"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2014, 4, 8))
        .maturityDate(LocalDate.of(2017, 9, 29))
        .contracts(contracts)
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 15806946.89))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 1, 3))
        .settlementDate(LocalDate.of(2017, 1, 3)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(15806946.89)
        .price(100.0 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 1, 3)).buySell(BUY)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 7, 14)).build();

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);

    final String cfFileName = "src/test/resources/dorocf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  /**
   * Mixed cash and PIK contracts, PIK across settlement period.
   * 
   * @throws IOException
   */
  public void test_termLoan_3() throws IOException {
    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 5, 23))
                .endDate(LocalDate.of(2016, 6, 23))
                .dayCount(DayCounts.ACT_360)
                .allInRate(2.0 / 100.0)
                .pikSpread(4.5 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        500000000.0))
                .build())
        .paymentDate(LocalDate.of(2016, 6, 23)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 6, 23))
                .endDate(LocalDate.of(2016, 7, 25))
                .dayCount(DayCounts.ACT_360)
                .allInRate(2.0 / 100.0)
                .pikSpread(4.5 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        501937500.0))
                .build())
        .paymentDate(LocalDate.of(2016, 7, 25)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 7, 25))
                .endDate(LocalDate.of(2016, 8, 25))
                .dayCount(DayCounts.ACT_360)
                .allInRate(2.0 / 100.0)
                .pikSpread(4.5 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        503945250.0))
                .build())
        .paymentDate(LocalDate.of(2016, 8, 25)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 25))
                .endDate(LocalDate.of(2016, 9, 26))
                .allInRate(2.0 / 100.0)
                .pikSpread(4.5 / 100.0)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        505898037.84))
                .build())
        .paymentDate(LocalDate.of(2016, 9, 26)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN6"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2016, 5, 23))
        .maturityDate(LocalDate.of(2021, 5, 23))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4))
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 500000000))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2016, 7, 28))
        .settlementDate(LocalDate.of(2016, 9, 8)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(1296133.536)
        .price(70.0 / 100)
        .expectedSettlementDate(LocalDate.of(2016, 8, 8)).buySell(SELL)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false)
        .averageLibor(0.51638 / 100).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2016, 10, 15)).build();

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);

    final String cfFileName = "src/test/resources/areteccf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  /**
   * Repayment on trade date, delayed settlement.
   * 
   * @throws IOException
   */
  public void test_termLoan_4() throws IOException {
    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2016, 12, 22))
        .interestOnPaydown(true)
        .amount(CurrencyAmount.of(Currency.USD, 3242033.29))
        .price(100.0 / 100).build();
    final Repayment REPAYMENT_2 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 24))
        .interestOnPaydown(true)
        .amount(CurrencyAmount.of(Currency.USD, 3223683.88))
        .price(100 / 100).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 10, 31))
                .endDate(LocalDate.of(2016, 11, 30))
                .allInRate(4.0 / 100.0)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1205313733.68))
                .build())
        .paymentDate(LocalDate.of(2016, 11, 30)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 11, 30))
                .endDate(LocalDate.of(2016, 12, 30))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1205313733.68))
                .build())
        .paymentDate(LocalDate.of(2016, 12, 30)).events(REPAYMENT_1)
        .build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 12, 30))
                .endDate(LocalDate.of(2017, 1, 31))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1202071700.39))
                .build())
        .paymentDate(LocalDate.of(2017, 1, 31)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 1, 31))
                .endDate(LocalDate.of(2017, 2, 28))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1202071700.39))
                .build())
        .paymentDate(LocalDate.of(2017, 2, 28)).build();
    final LoanContract CONTRACT_5 = LoanContract
        .builder()
        .id(StandardId.of("contract", "5"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 2, 28))
                .endDate(LocalDate.of(2017, 3, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1202071700.39))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 15)).build();
    final LoanContract CONTRACT_6 = LoanContract
        .builder()
        .id(StandardId.of("contract", "6"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 15))
                .endDate(LocalDate.of(2017, 4, 17))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.0 / 100.0)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1202071700.39))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 17)).events(REPAYMENT_2)
        .build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN7"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2013, 5, 29))
        .maturityDate(LocalDate.of(2020, 3, 1))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4, CONTRACT_5, CONTRACT_6))
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 1205313733.68))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2016, 12, 22))
        .settlementDate(LocalDate.of(2017, 1, 5)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().buySell(SELL)
        .product(LOAN).info(TRADE_INFO)
        .buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(1500000)
        .price(100.625 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 1, 4))
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(true)
        .averageLibor(0.77167 / 100).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 8, 18)).build();
    final LocalDate settle = PRORATED_LOAN_TRADE.getInfo()
        .getSettlementDate().get();

    assertEquals(PRICER.delayedCompensation(PRORATED_LOAN_TRADE, PROV)
        .getAmount(), 166.22, 1E-2);
    assertEquals(
        PRICER.costOfFunded(PRORATED_LOAN_TRADE, PROV, settle,
            Optional.empty()).getAmount(),
        1505315.10, 1E-2);
    assertEquals(
        PRICER.costOfCarry(PRORATED_LOAN_TRADE, PROV, Optional.empty())
            .getAmount(),
        32.27, 1E-2);
    assertEquals(
        PRICER.economicBenefit(PRORATED_LOAN_TRADE, PROV, settle,
            Optional.empty()).getAmount(),
        -25.22, 1E-2);

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);

    final String cfFileName = "src/test/resources/uvncf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  /**
   * Active revolver.
   * 
   * @throws IOException
   */
  public void test_revolvingLoan_4() throws IOException {
    final Borrowing BORROWING_1 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 4, 26))
        .amount(CurrencyAmount.of(Currency.USD, 4374449.60)).build();
    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 5, 4))
        .amount(CurrencyAmount.of(Currency.USD, 4374449.60))
        .interestOnPaydown(false).build();
    final Borrowing BORROWING_2 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 5, 3))
        .amount(CurrencyAmount.of(Currency.USD, 6060652.0)).build();
    final Borrowing BORROWING_3 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 5, 10))
        .amount(CurrencyAmount.of(Currency.USD, 8367273.6)).build();
    final Repayment REPAYMENT_2 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 5, 16))
        .amount(CurrencyAmount.of(Currency.USD, 14427925.6)).build();
    final Borrowing BORROWING_4 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 5, 17))
        .amount(CurrencyAmount.of(Currency.USD, 7189261)).build();
    final Repayment REPAYMENT_3 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 6))
        .amount(CurrencyAmount.of(Currency.USD, 7189261)).build();
    final Borrowing BORROWING_5 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 5, 24))
        .amount(CurrencyAmount.of(Currency.USD, 8665667)).build();
    final Repayment REPAYMENT_4 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 6))
        .amount(CurrencyAmount.of(Currency.USD, 8665667)).build();
    final Borrowing BORROWING_6 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 5, 31))
        .amount(CurrencyAmount.of(Currency.USD, 7639240)).build();
    final Repayment REPAYMENT_5 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 6))
        .amount(CurrencyAmount.of(Currency.USD, 7639240)).build();
    final Borrowing BORROWING_7 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 6, 7))
        .amount(CurrencyAmount.of(Currency.USD, 5935653)).build();
    final Borrowing BORROWING_8 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2017, 6, 14))
        .amount(CurrencyAmount.of(Currency.USD, 8614583.8)).build();

    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 26))
                .endDate(LocalDate.of(2017, 5, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49222 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        4374449.6))
                .build())
        .paymentDate(LocalDate.of(2017, 5, 15))
        .events(Arrays.asList(BORROWING_1, REPAYMENT_1)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 3))
                .endDate(LocalDate.of(2017, 5, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49278 / 100)
                .accrualAmount(
                    CurrencyAmount
                        .of(Currency.USD, 6060652))
                .build())
        .paymentDate(LocalDate.of(2017, 5, 15)).events(BORROWING_2)
        .build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 10))
                .endDate(LocalDate.of(2017, 5, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.48856 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        8367273.6))
                .build())
        .paymentDate(LocalDate.of(2017, 5, 15)).events(BORROWING_3)
        .build();
    final LoanContract CONTRACT_5 = LoanContract
        .builder()
        .id(StandardId.of("contract", "5"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 15))
                .endDate(LocalDate.of(2017, 6, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49244 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        14427925.6))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 15)).events(REPAYMENT_2)
        .build();
    final LoanContract CONTRACT_6 = LoanContract
        .builder()
        .id(StandardId.of("contract", "6"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 17))
                .endDate(LocalDate.of(2017, 6, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49244 / 100)
                .accrualAmount(
                    CurrencyAmount
                        .of(Currency.USD, 7189261))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 15))
        .events(Arrays.asList(BORROWING_4, REPAYMENT_3)).build();
    final LoanContract CONTRACT_7 = LoanContract
        .builder()
        .id(StandardId.of("contract", "7"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 24))
                .endDate(LocalDate.of(2017, 6, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49244 / 100)
                .accrualAmount(
                    CurrencyAmount
                        .of(Currency.USD, 8665667))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 15))
        .events(Arrays.asList(BORROWING_5, REPAYMENT_4)).build();
    final LoanContract CONTRACT_8 = LoanContract
        .builder()
        .id(StandardId.of("contract", "8"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 5, 31))
                .endDate(LocalDate.of(2017, 6, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49244 / 100)
                .accrualAmount(
                    CurrencyAmount
                        .of(Currency.USD, 7639240))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 15))
        .events(Arrays.asList(BORROWING_6, REPAYMENT_5)).build();
    final LoanContract CONTRACT_9 = LoanContract
        .builder()
        .id(StandardId.of("contract", "9"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 6, 7))
                .endDate(LocalDate.of(2017, 6, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49244 / 100)
                .accrualAmount(
                    CurrencyAmount
                        .of(Currency.USD, 5935653))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 15)).events(BORROWING_7)
        .build();
    final LoanContract CONTRACT_10 = LoanContract
        .builder()
        .id(StandardId.of("contract", "10"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 6, 14))
                .endDate(LocalDate.of(2017, 6, 15))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49244 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        8614583.8))
                .build())
        .paymentDate(LocalDate.of(2017, 6, 15)).events(BORROWING_8)
        .build();

    List<LoanContract> contracts = new ArrayList<LoanContract>();
    contracts.add(CONTRACT_2);
    contracts.add(CONTRACT_3);
    contracts.add(CONTRACT_4);
    contracts.add(CONTRACT_5);
    contracts.add(CONTRACT_6);
    contracts.add(CONTRACT_7);
    contracts.add(CONTRACT_8);
    contracts.add(CONTRACT_9);
    contracts.add(CONTRACT_10);

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN8"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2017, 2, 7))
        .maturityDate(LocalDate.of(2018, 3, 3))
        .contracts(contracts)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 25000000))
        .facilityType(Revolving).build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 2, 7))
        .settlementDate(LocalDate.of(2017, 3, 3)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(25000000)
        .price(100 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 3, 23)).buySell(BUY)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 9, 1)).build();

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE,
        PROV, true);

    final String cfFileName = "src/test/resources/clgecf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_revolvingLoan_5() throws IOException {
    final Borrowing BORROW_1 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2016, 8, 1))
        .amount(CurrencyAmount.of(Currency.USD, 225000000)).build();
    final Borrowing BORROW_2 = Borrowing.builder()
        .effectiveDate(LocalDate.of(2016, 8, 4))
        .amount(CurrencyAmount.of(Currency.USD, 75000000)).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 1))
                .endDate(LocalDate.of(2016, 8, 17))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.4939 / 100)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        225000000))
                .build())
        .paymentDate(LocalDate.of(2016, 8, 17)).events(BORROW_1)
        .build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 4))
                .endDate(LocalDate.of(2016, 8, 17))
                .allInRate(3.4939 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        75000000))
                .build())
        .paymentDate(LocalDate.of(2016, 8, 17)).events(BORROW_2)
        .build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 8, 17))
                .endDate(LocalDate.of(2016, 9, 19))
                .allInRate(3.50744 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        300000000))
                .build())
        .paymentDate(LocalDate.of(2016, 9, 19)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 9, 19))
                .endDate(LocalDate.of(2016, 10, 17))
                .allInRate(3.53094 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        300000000))
                .build())
        .paymentDate(LocalDate.of(2016, 10, 17)).build();
    final LoanContract CONTRACT_5 = LoanContract
        .builder()
        .id(StandardId.of("contract", "5"))
        .accrual(
            FixedRateAccrual
                .builder()
                .startDate(LocalDate.of(2016, 10, 17))
                .endDate(LocalDate.of(2016, 11, 17))
                .allInRate(3.53456 / 100)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        300000000))
                .build())
        .paymentDate(LocalDate.of(2016, 11, 17)).build();

    List<LoanContract> contracts = new ArrayList<LoanContract>();
    contracts.add(CONTRACT_1);
    contracts.add(CONTRACT_2);
    contracts.add(CONTRACT_3);
    contracts.add(CONTRACT_4);
    contracts.add(CONTRACT_5);

    final AccruingFee FEE_1 = AccruingFee
        .builder()
        .accrual(
            FixedRateAccrual
                .builder()
                .allInRate(.5 / 100)
                .dayCount(DayCounts.ACT_360)
                .startDate(LocalDate.of(2016, 8, 1))
                .endDate(LocalDate.of(2016, 8, 17))
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        75000000))
                .build())
        .id(StandardId.of("fee", "1")).type("commitment fee")
        .paymentDate(LocalDate.of(2016, 8, 17)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN9"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2016, 7, 28))
        .maturityDate(LocalDate.of(2017, 3, 24))
        .contracts(contracts)
        .fees(FEE_1)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 500000000))
        .facilityType(Revolving).build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2016, 8, 1))
        .settlementDate(LocalDate.of(2016, 8, 1)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(500000000)
        .price(100 / 100)
        .expectedSettlementDate(TRADE_INFO.getSettlementDate().get())
        .buySell(BUY).accrualSettlementType(SettledWithoutAccrued)
        .association(LSTA).commitmentReductionCreditFlag(true)
        .currency(Currency.USD).delayedCompensationFlag(true)
        .documentationType(Par).formOfPurchase(Assignment)
        .paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2016, 8, 2)).build();

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE,
        PROV, true);

    String cfFileName = "src/test/resources/comfeecf.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);

    cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE, PROV, false);

    cfFileName = "src/test/resources/comfeecf_ne.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(
        new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  /**
   * Purchase price change of > 25% during settlement period.
   * 
   * @throws IOException
   */
  public void test_termLoan_6() throws IOException {
    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 1))
        .amount(CurrencyAmount.of(Currency.USD, 650000000)).build();

    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 4, 10))
        .amount(CurrencyAmount.of(Currency.USD, 570075000))
        .interestOnPaydown(false).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 1, 19))
                .endDate(LocalDate.of(2017, 2, 21))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.5 / 100)
                .baseRate(1.0 / 100)
                .spread(2.5 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1350000000.0))
                .build())
        .paymentDate(LocalDate.of(2017, 2, 21)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 2, 21))
                .endDate(LocalDate.of(2017, 3, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.5 / 100)
                .baseRate(1.0 / 100)
                .spread(2.5 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1350000000.0))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 21)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 1))
                .endDate(LocalDate.of(2017, 3, 21))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.5 / 100)
                .baseRate(1.0 / 100)
                .spread(2.5 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        2000000000.0))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 21)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 21))
                .endDate(LocalDate.of(2017, 4, 21))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.5 / 100)
                .baseRate(1.0 / 100)
                .spread(2.5 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        2000000000.0))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 21)).events(REPAYMENT_1)
        .build();
    final LoanContract CONTRACT_5 = LoanContract
        .builder()
        .id(StandardId.of("contract", "5"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 21))
                .endDate(LocalDate.of(2017, 5, 22))
                .dayCount(DayCounts.ACT_360)
                .allInRate(3.5 / 100)
                .baseRate(1.0 / 100)
                .spread(2.5 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1429925000.0))
                .build())
        .paymentDate(LocalDate.of(2017, 5, 22)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2017, 1, 19))
        .maturityDate(LocalDate.of(2024, 1, 19))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4, CONTRACT_5))
        .events(ADJUSTMENT_1)
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 1350000000))
        .identifiers(
            Arrays.asList(StandardId.of("LXID", "LX158900"),
                StandardId.of("CUSIP", "012345678"),
                StandardId.of("BLOOMBERGID", "BB12345678")))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 2, 2))
        .settlementDate(LocalDate.of(2017, 4, 28)).build();

    LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(500000)
        .price(101.01 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 2, 13))
        .averageLibor(0.90819 / 100).buySell(SELL)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 7, 14)).build();

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);
    final String cfFileName = "src/test/resources/zayo.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  /**
   * Prorata commitment adjustment upsize causes borrowing.
   * 
   * @throws IOException
   */
  public void test_termLoan_5() throws IOException {
    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 31))
        .amount(CurrencyAmount.of(Currency.USD, 4050000)).build();
    final Repayment REPAYMENT_2 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 30))
        .amount(CurrencyAmount.of(Currency.USD, 4558012.17)).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 1, 24))
                .endDate(LocalDate.of(2017, 3, 16))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.50283 / 100)
                .baseRate(1.2583 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1598500000))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 16)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 16))
                .endDate(LocalDate.of(2017, 4, 20))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.38733 / 100.0)
                .baseRate(1.13733 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1598500000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).events(REPAYMENT_1)
        .build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 20))
                .endDate(LocalDate.of(2017, 4, 26))
                .allInRate(4.38733 / 100.0)
                .baseRate(1.13733 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1794450000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 26))
                .endDate(LocalDate.of(2017, 7, 26))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.4165 / 100)
                .baseRate(1.1665 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1794450000))
                .build())
        .paymentDate(LocalDate.of(2017, 7, 26)).events(REPAYMENT_2)
        .build();

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 4, 20))
        .amount(CurrencyAmount.of(Currency.USD, 200000000))
        .refusalAllowed(false).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN1"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2017, 1, 24))
        .maturityDate(LocalDate.of(2022, 8, 14))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4))
        .events(ADJUSTMENT_1)
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 1598500000))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 3, 21))
        .settlementDate(LocalDate.of(2017, 4, 10)).build();

    LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(3000000)
        .price(101.125 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 3, 30))
        .averageLibor(0.9834 / 100).buySell(BUY)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false).build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 7, 14)).build();
    final LocalDate settle = PRORATED_LOAN_TRADE.getInfo()
        .getSettlementDate().get();

    assertEquals(PRICER.delayedCompensation(PRORATED_LOAN_TRADE, PROV)
        .getAmount(), 4012.46, 1E-2);
    assertEquals(
        PRICER.costOfFunded(PRORATED_LOAN_TRADE, PROV, settle,
            Optional.empty()).getAmount(),
        3026063.61, 1E-2);
    assertEquals(
        PRICER.costOfCarry(PRORATED_LOAN_TRADE, PROV, Optional.empty())
            .getAmount(),
        911.59, 1E-2);
    assertEquals(
        PRICER.economicBenefit(PRORATED_LOAN_TRADE, PROV, settle,
            Optional.empty()).getAmount(),
        -85.51, 1E-2);

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(
        PRORATED_LOAN_TRADE, PROV, true);

    final String cfFileName = "src/test/resources/aliantcf2.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_portfolio_1() throws IOException {
    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 31))
        .amount(CurrencyAmount.of(Currency.USD, 4050000)).build();
    final Repayment REPAYMENT_2 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 30))
        .amount(CurrencyAmount.of(Currency.USD, 4558012.17)).build();

    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 1, 24))
                .endDate(LocalDate.of(2017, 3, 16))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.50283 / 100)
                .baseRate(1.2583 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1598500000))
                .build())
        .paymentDate(LocalDate.of(2017, 3, 16)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 3, 16))
                .endDate(LocalDate.of(2017, 4, 20))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.38733 / 100.0)
                .baseRate(1.13733 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1598500000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).events(REPAYMENT_1)
        .build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 20))
                .endDate(LocalDate.of(2017, 4, 26))
                .allInRate(4.38733 / 100.0)
                .baseRate(1.13733 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .dayCount(DayCounts.ACT_360)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1794450000))
                .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 4, 26))
                .endDate(LocalDate.of(2017, 7, 26))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.4165 / 100)
                .baseRate(1.1665 / 100)
                .spread(3.25 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        1794450000))
                .build())
        .paymentDate(LocalDate.of(2017, 7, 26)).events(REPAYMENT_2)
        .build();

    List<LoanContract> contracts = new ArrayList<LoanContract>();
    contracts.add(CONTRACT_1);
    contracts.add(CONTRACT_2);
    contracts.add(CONTRACT_3);
    contracts.add(CONTRACT_4);

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 4, 20))
        .amount(CurrencyAmount.of(Currency.USD, 200000000)).build();
    List<FacilityEvent> events = new ArrayList<FacilityEvent>();
    events.add(ADJUSTMENT_1);

    List<StandardId> identifiers = new ArrayList<StandardId>();
    identifiers.add(StandardId.of("LXID", "LX123456"));
    identifiers.add(StandardId.of("CUSIP", "012345678"));
    identifiers.add(StandardId.of("BLOOMBERGID", "BB12345678"));

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN1"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2017, 1, 24))
        .maturityDate(LocalDate.of(2022, 8, 14))
        .contracts(contracts)
        .events(ADJUSTMENT_1)
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 1598500000))
        .identifiers(identifiers).build();

    final TradeInfo BUY_TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 4, 20))
        .settlementDate(LocalDate.of(2017, 5, 11)).build();

    LoanTrade BUY_LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(BUY_TRADE_INFO).buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER")).amount(500000)
        .price(100.0 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 5, 1)).buySell(BUY)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(false).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false)
        .adjustmentOnTradeDate(true).tradeType(Primary).build();

    final TradeInfo SELL_TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2017, 5, 30))
        .settlementDate(LocalDate.of(2017, 6, 9)).build();

    LoanTrade SELL_LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(SELL_TRADE_INFO).buyer(StandardId.of("cpty", "SELLER"))
        .seller(StandardId.of("cpty", "BUYER")).amount(500000)
        .price(100.375 / 100)
        .expectedSettlementDate(LocalDate.of(2017, 6, 8)).buySell(SELL)
        .accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(false).currency(Currency.USD)
        .delayedCompensationFlag(true).documentationType(Par)
        .formOfPurchase(Assignment).paydownOnTradeDate(false)
        .adjustmentOnTradeDate(false).averageLibor(1.08867 / 100.0)
        .build();

    final ProratedLoanTrade PRORATED_BUY_LOAN_TRADE = BUY_LOAN_TRADE
        .prorate(null);
    final ProratedLoanTrade PRORATED_SELL_LOAN_TRADE = SELL_LOAN_TRADE
        .prorate(null);

    List<ProratedLoanTrade> tradeList = new ArrayList<ProratedLoanTrade>();
    tradeList.add(PRORATED_BUY_LOAN_TRADE);
    tradeList.add(PRORATED_SELL_LOAN_TRADE);

    ProratedLoanTradeList trades = ProratedLoanTradeList.builder()
        .trades(tradeList).build();

    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.now()).build();

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(trades, PROV, false);
    String cfFileName = "src/test/resources/aliantbuysell_ne.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);

    cashFlows = PRICER.cashFlows(trades, PROV, true);
    cfFileName = "src/test/resources/aliantbuysell.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(
        new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_portfolio_2() throws IOException {
    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2018, 2, 6))
                .endDate(LocalDate.of(2018, 3, 8))
                .dayCount(DayCounts.ACT_360)
                .allInRate(5.57926 / 100)
                .baseRate(1.57926 / 100)
                .spread(4.0 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        260000000))
                .build())
        .paymentDate(LocalDate.of(2018, 3, 8)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2018, 3, 8))
                .endDate(LocalDate.of(2018, 4, 9))
                .dayCount(DayCounts.ACT_360)
                .allInRate(5.71131 / 100)
                .baseRate(1.57926 / 100)
                .spread(4.0 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        260000000))
                .build())
        .paymentDate(LocalDate.of(2018, 4, 9)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2018, 4, 9))
                .endDate(LocalDate.of(2018, 5, 9))
                .dayCount(DayCounts.ACT_360)
                .allInRate(5.89519 / 100)
                .baseRate(1.89519 / 100)
                .spread(4.0 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        260000000))
                .build())
        .paymentDate(LocalDate.of(2018, 5, 9)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2018, 2, 6))
        .maturityDate(LocalDate.of(2025, 2, 10))
        .contracts(Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3))
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 260000000))
        .build();

    Function<String, List<String>> mapToItem = (line) -> {
      return Arrays.asList(line.split(","));
    };

    List<List<String>> inputList = new ArrayList<List<String>>();
    File inputF = new File("src/test/resources/VIST_TRADES.csv");
    InputStream inputFS = new FileInputStream(inputF);
    BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));

    inputList = br.lines().skip(1).map(mapToItem)
        .collect(Collectors.toList());
    br.close();

    List<ProratedLoanTrade> tradeList = new ArrayList<ProratedLoanTrade>();

    final int MAX_TRADES = 10000;
    int tradeCount = 0;

    for (List<String> sl : inputList) {
      if (tradeCount++ > MAX_TRADES)
        break;

      String dealNumber = sl.get(0);
      String child = sl.get(1);
      String tid = dealNumber + "." + child;
      LocalDate tradeDate = LocalDate.parse(sl.get(2));
      LocalDate expectedSettle = LocalDate.parse(sl.get(3));
      LocalDate actualSettle = LocalDate.parse(sl.get(4));
      double price = Double.parseDouble(sl.get(5));
      double amount = Double.parseDouble(sl.get(6));

      final TradeInfo TRADE_INFO = TradeInfo.builder()
          .tradeDate(tradeDate)
          .settlementDate(actualSettle)
          .id(StandardId.of("trade", tid))
          .build();

      LoanTrade LOAN_TRADE = LoanTrade
          .builder()
          .product(LOAN)
          .info(TRADE_INFO)
          .buyer(amount < 0 ? StandardId.of("cpty", "BUYER") : StandardId.of("cpty", "SELF"))
          .seller(amount > 0 ? StandardId.of("cpty", "SELLER") : StandardId.of("cpty", "SELF"))
          .amount(Math.abs(amount)).price(price / 100)
          .expectedSettlementDate(expectedSettle)
          .buySell(amount > 0 ? BUY : SELL)
          .accrualSettlementType(SettledWithoutAccrued)
          .association(LSTA).commitmentReductionCreditFlag(true)
          .currency(Currency.USD).delayedCompensationFlag(true)
          .documentationType(Par).formOfPurchase(Assignment)
          .paydownOnTradeDate(false).build();

      tradeList.add(LOAN_TRADE.prorate(null));
    }

    ProratedLoanTradeList trades = ProratedLoanTradeList.builder()
        .trades(tradeList).build();

    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2017, 5, 30)).build();

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(trades, PROV, true);
    String cfFileName = "src/test/resources/vist.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  public void test_portfolio_3() throws IOException {
    final LoanContract CONTRACT_1 = LoanContract
        .builder()
        .id(StandardId.of("contract", "1"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 11, 2))
                .endDate(LocalDate.of(2017, 12, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4. / 100)
                .baseRate(1. / 100)
                .spread(3.0 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        700000000))
                .build())
        .paymentDate(LocalDate.of(2017, 12, 1)).build();
    final LoanContract CONTRACT_2 = LoanContract
        .builder()
        .id(StandardId.of("contract", "2"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2017, 12, 1))
                .endDate(LocalDate.of(2018, 3, 1))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.49 / 100)
                .baseRate(1.49 / 100)
                .spread(3.0 / 100)
                .index(IborIndex.of("USD-LIBOR-3M"))
                .paymentFrequency(Frequency.P3M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        800000000))
                .build())
        .paymentDate(LocalDate.of(2018, 3, 1)).build();
    final LoanContract CONTRACT_3 = LoanContract
        .builder()
        .id(StandardId.of("contract", "3"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2018, 3, 1))
                .endDate(LocalDate.of(2018, 4, 3))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.66418 / 100)
                .baseRate(1.66418 / 100)
                .spread(3.0 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        800000000))
                .build())
        .paymentDate(LocalDate.of(2018, 4, 3))
        .events(Repayment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 2000000))
            .effectiveDate(LocalDate.of(2018, 3, 29)).build())
        .build();
    final LoanContract CONTRACT_4 = LoanContract
        .builder()
        .id(StandardId.of("contract", "4"))
        .accrual(
            FloatingRateAccrual
                .builder()
                .startDate(LocalDate.of(2018, 4, 3))
                .endDate(LocalDate.of(2018, 5, 3))
                .dayCount(DayCounts.ACT_360)
                .allInRate(4.88688 / 100)
                .baseRate(1.88688 / 100)
                .spread(3.0 / 100)
                .index(IborIndex.of("USD-LIBOR-1M"))
                .paymentFrequency(Frequency.P1M)
                .accrualAmount(
                    CurrencyAmount.of(Currency.USD,
                        798000000))
                .build())
        .paymentDate(LocalDate.of(2018, 5, 3)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "LOAN"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2017, 11, 2))
        .maturityDate(LocalDate.of(2024, 11, 2))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4))
        .facilityType(Term)
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 700000000))
        .events(CommitmentAdjustment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 100000000))
            .effectiveDate(LocalDate.of(2017, 12, 1)).build())
        .build();

    Function<String, List<String>> mapToItem = (line) -> {
      return Arrays.asList(line.split(","));
    };

    List<List<String>> inputList = new ArrayList<List<String>>();
    File inputF = new File("src/test/resources/SUPVIS_TRADES.csv");
    InputStream inputFS = new FileInputStream(inputF);
    BufferedReader br = new BufferedReader(new InputStreamReader(inputFS));

    inputList = br.lines().skip(1).map(mapToItem)
        .collect(Collectors.toList());
    br.close();

    List<ProratedLoanTrade> tradeList = new ArrayList<ProratedLoanTrade>();

    final int MAX_TRADES = 10000;
    int tradeCount = 0;

    for (List<String> sl : inputList) {
      if (tradeCount++ > MAX_TRADES)
        break;

      String dealNumber = sl.get(0);
      String child = sl.get(1);
      String tid = dealNumber + "." + child;
      LocalDate tradeDate = LocalDate.parse(sl.get(2));
      LocalDate expectedSettle = LocalDate.parse(sl.get(3));
      LocalDate actualSettle = LocalDate.parse(sl.get(4));
      double price = Double.parseDouble(sl.get(5));
      double amount = Double.parseDouble(sl.get(6));

      final TradeInfo TRADE_INFO = TradeInfo.builder()
          .tradeDate(tradeDate)
          .settlementDate(actualSettle)
          .id(StandardId.of("trade", tid))
          .build();

      LoanTrade LOAN_TRADE = LoanTrade
          .builder()
          .product(LOAN)
          .info(TRADE_INFO)
          .buyer(amount < 0 ? StandardId.of("cpty", "BUYER") : StandardId.of("cpty", "SELF"))
          .seller(amount > 0 ? StandardId.of("cpty", "SELLER") : StandardId.of("cpty", "SELF"))
          .amount(Math.abs(amount)).price(price / 100)
          .expectedSettlementDate(expectedSettle)
          .buySell(amount > 0 ? BUY : SELL)
          .accrualSettlementType(SettledWithoutAccrued)
          .association(LSTA).commitmentReductionCreditFlag(true)
          .currency(Currency.USD).delayedCompensationFlag(true)
          .documentationType(Par).formOfPurchase(Assignment)
          .paydownOnTradeDate(false).build();

      tradeList.add(LOAN_TRADE.prorate(null));
    }

    ProratedLoanTradeList trades = ProratedLoanTradeList.builder()
        .trades(tradeList).build();

    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(
        LocalDate.of(2018, 4, 16)).build();

    AnnotatedCashFlows cashFlows = PRICER.cashFlows(trades, PROV, true);
    String cfFileName = "src/test/resources/supvis.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  /**
   * Multiple non-pro rata adjustments, paydown-on-trade.
   * @throws IOException
   */
  public void test_termLoan_7() throws IOException {
    final LoanContract CONTRACT_9 = LoanContract.builder()
        .id(StandardId.of("contract", "9"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2016, 8, 29))
            .endDate(LocalDate.of(2016, 9, 30))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.75 / 100)
            .baseRate(1. / 100)
            .spread(4.75 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 238200000))
            .build())
        .paymentDate(LocalDate.of(2016, 9, 30))
        .events(Repayment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 595500))
            .effectiveDate(LocalDate.of(2016, 9, 30))
            .build())
        .build();
    final LoanContract CONTRACT_10 = LoanContract.builder()
        .id(StandardId.of("contract", "10"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2016, 9, 30))
            .endDate(LocalDate.of(2016, 10, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.75 / 100)
            .baseRate(1. / 100)
            .spread(4.75 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 237604500))
            .build())
        .paymentDate(LocalDate.of(2016, 10, 31))
        .build();
    final LoanContract CONTRACT_11 = LoanContract.builder()
        .id(StandardId.of("contract", "11"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2016, 10, 31))
            .endDate(LocalDate.of(2016, 11, 30))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.75 / 100)
            .baseRate(1. / 100)
            .spread(4.75 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 237604500))
            .build())
        .paymentDate(LocalDate.of(2016, 11, 30))
        .build();
    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2016, 11, 30))
        .amount(CurrencyAmount.of(Currency.USD, 44887500))
        .build();
    final LoanContract CONTRACT_12 = LoanContract.builder()
        .id(StandardId.of("contract", "12"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2016, 11, 30))
            .endDate(LocalDate.of(2016, 12, 30))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.75 / 100)
            .baseRate(1. / 100)
            .spread(4.75 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 282492000))
            .build())
        .paymentDate(LocalDate.of(2016, 12, 30))
        .events(Repayment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 717000))
            .effectiveDate(LocalDate.of(2016, 12, 30))
            .build())
        .build();
    final LoanContract CONTRACT_13 = LoanContract.builder()
        .id(StandardId.of("contract", "13"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2016, 12, 30))
            .endDate(LocalDate.of(2017, 1, 3))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.75 / 100)
            .baseRate(1. / 100)
            .spread(4.75 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281775000))
            .build())
        .paymentDate(LocalDate.of(2017, 1, 3))
        .build();
    final LoanContract CONTRACT_14 = LoanContract.builder()
        .id(StandardId.of("contract", "14"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 1, 3))
            .endDate(LocalDate.of(2017, 1, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.25 / 100)
            .baseRate(1. / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281775000))
            .build())
        .paymentDate(LocalDate.of(2017, 1, 31))
        .build();
    final LoanContract CONTRACT_15 = LoanContract.builder()
        .id(StandardId.of("contract", "15"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 1, 31))
            .endDate(LocalDate.of(2017, 2, 28))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.25 / 100)
            .baseRate(1. / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281775000))
            .build())
        .paymentDate(LocalDate.of(2017, 2, 28))
        .build();
    final LoanContract CONTRACT_16 = LoanContract.builder()
        .id(StandardId.of("contract", "16"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 2, 28))
            .endDate(LocalDate.of(2017, 3, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.25 / 100)
            .baseRate(1. / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281775000))
            .build())
        .paymentDate(LocalDate.of(2017, 3, 31))
        .events(Repayment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 712500))
            .effectiveDate(LocalDate.of(2017, 3, 31))
            .build())
        .build();
    final LoanContract CONTRACT_17 = LoanContract.builder()
        .id(StandardId.of("contract", "17"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 3, 31))
            .endDate(LocalDate.of(2017, 4, 28))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.25 / 100)
            .baseRate(1. / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281062500))
            .build())
        .paymentDate(LocalDate.of(2017, 4, 28))
        .build();
    final LoanContract CONTRACT_18 = LoanContract.builder()
        .id(StandardId.of("contract", "18"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 4, 28))
            .endDate(LocalDate.of(2017, 5, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.25 / 100)
            .baseRate(1. / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281062500))
            .build())
        .paymentDate(LocalDate.of(2017, 5, 31))
        .build();
    final LoanContract CONTRACT_19 = LoanContract.builder()
        .id(StandardId.of("contract", "19"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 5, 31))
            .endDate(LocalDate.of(2017, 6, 30))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.30 / 100)
            .baseRate(1.05 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 281062500))
            .build())
        .paymentDate(LocalDate.of(2017, 6, 30))
        .events(Repayment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 712500))
            .effectiveDate(LocalDate.of(2017, 6, 30))
            .build())
        .build();
    final LoanContract CONTRACT_20 = LoanContract.builder()
        .id(StandardId.of("contract", "20"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 6, 30))
            .endDate(LocalDate.of(2017, 7, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.48 / 100)
            .baseRate(1.23 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 280350000))
            .build())
        .paymentDate(LocalDate.of(2017, 7, 31))
        .build();
    final LoanContract CONTRACT_21 = LoanContract.builder()
        .id(StandardId.of("contract", "21"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 7, 31))
            .endDate(LocalDate.of(2017, 8, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.49 / 100)
            .baseRate(1.24 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 280350000))
            .build())
        .paymentDate(LocalDate.of(2017, 8, 31))
        .build();
    final LoanContract CONTRACT_22 = LoanContract.builder()
        .id(StandardId.of("contract", "22"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 8, 31))
            .endDate(LocalDate.of(2017, 9, 29))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.49 / 100)
            .baseRate(1.24 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 280350000))
            .build())
        .paymentDate(LocalDate.of(2017, 9, 29))
        .events(Repayment.builder()
            .amount(CurrencyAmount.of(Currency.USD, 712500))
            .effectiveDate(LocalDate.of(2017, 9, 29))
            .build())
        .build();
    final LoanContract CONTRACT_23 = LoanContract.builder()
        .id(StandardId.of("contract", "23"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 9, 29))
            .endDate(LocalDate.of(2017, 10, 2))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.49 / 100)
            .baseRate(1.24 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 279637500))
            .build())
        .paymentDate(LocalDate.of(2017, 10, 31))
        .build();
    final FacilityEvent ADJUSTMENT_2 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 10, 2))
        .amount(CurrencyAmount.of(Currency.USD, 90000000))
        .build();
    final LoanContract CONTRACT_24 = LoanContract.builder()
        .id(StandardId.of("contract", "24"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 10, 2))
            .endDate(LocalDate.of(2017, 10, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.49 / 100)
            .baseRate(1.24 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 90000000))
            .build())
        .paymentDate(LocalDate.of(2017, 10, 31))
        .build();
    final LoanContract CONTRACT_25 = LoanContract.builder()
        .id(StandardId.of("contract", "25"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 10, 2))
            .endDate(LocalDate.of(2017, 10, 31))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.49 / 100)
            .baseRate(1.24 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 279637500))
            .build())
        .paymentDate(LocalDate.of(2017, 10, 31))
        .build();
    final LoanContract CONTRACT_26 = LoanContract.builder()
        .id(StandardId.of("contract", "26"))
        .accrual(FloatingRateAccrual.builder()
            .startDate(LocalDate.of(2017, 10, 31))
            .endDate(LocalDate.of(2017, 11, 30))
            .dayCount(DayCounts.ACT_360)
            .allInRate(5.5 / 100)
            .baseRate(1.25 / 100)
            .spread(4.25 / 100)
            .index(IborIndex.of("USD-LIBOR-1M"))
            .paymentFrequency(Frequency.P1M)
            .accrualAmount(CurrencyAmount.of(Currency.USD, 369637500))
            .build())
        .paymentDate(LocalDate.of(2017, 11, 30))
        .build();

    final Facility LOAN = Facility.builder()
        .id(StandardId.of("lid", "LOAN1"))
        .agent(StandardId.of("cpty", "AGENT"))
        .borrower(StandardId.of("cpty", "BORROWER"))
        .startDate(LocalDate.of(2016, 8, 29))
        .maturityDate(LocalDate.of(2022, 8, 4))
        .contracts(
            Arrays.asList(CONTRACT_9, CONTRACT_10, CONTRACT_11, CONTRACT_12, CONTRACT_13, CONTRACT_14, CONTRACT_15, CONTRACT_16,
                CONTRACT_17, CONTRACT_18, CONTRACT_19, CONTRACT_20, CONTRACT_21, CONTRACT_22, CONTRACT_23, CONTRACT_24,
                CONTRACT_25,
                CONTRACT_26))
        .events(Arrays.asList(ADJUSTMENT_1, ADJUSTMENT_2))
        .facilityType(Term)
        .originalCommitmentAmount(CurrencyAmount.of(Currency.USD, 238200000))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .tradeDate(LocalDate.of(2016, 9, 30))
        .settlementDate(LocalDate.of(2017, 1, 24))
        .build();

    LoanTrade LOAN_TRADE = LoanTrade.builder()
        .product(LOAN)
        .info(TRADE_INFO)
        .buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER"))
        .amount(2500000)
        .price(101.5 / 100)
        .expectedSettlementDate(LocalDate.of(2016, 10, 12))
        .averageLibor(0.63771 / 100)
        .buySell(SELL)
        .accrualSettlementType(SettledWithoutAccrued)
        .association(LSTA)
        .commitmentReductionCreditFlag(true)
        .currency(Currency.USD)
        .delayedCompensationFlag(true)
        .documentationType(Par)
        .formOfPurchase(Assignment)
        .paydownOnTradeDate(true)
        .build();

    final ProratedLoanTrade PRORATED_LOAN_TRADE = LOAN_TRADE.prorate(null);
    final ProratedLoanTradePricer PRICER = ProratedLoanTradePricer.DEFAULT;
    final RatesProvider PROV = ImmutableRatesProvider.builder(LocalDate.of(2018, 4, 24)).build();

    final AnnotatedCashFlows cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE, PROV, true);

    final String cfFileName = "src/test/resources/mocean.json";

    if (regenerate) {
      try (FileWriter writer = new FileWriter(cfFileName)) {
        writer.write(JodaBeanSer.PRETTY.jsonWriter().write(cashFlows));
        log.warn("regenerated " + cfFileName);
      }
    }

    final AnnotatedCashFlows expected =
        (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

}
