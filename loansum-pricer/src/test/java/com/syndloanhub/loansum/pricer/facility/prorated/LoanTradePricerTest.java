/**
 * 
 */
package com.syndloanhub.loansum.pricer.facility.prorated;

import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.syndloanhub.loansum.product.facility.FacilityType.Term;
import static com.syndloanhub.loansum.product.facility.LoanTradingAccrualSettlement.SettledWithoutAccrued;
import static com.syndloanhub.loansum.product.facility.LoanTradingAssoc.LSTA;
import static com.syndloanhub.loansum.product.facility.LoanTradingDocType.Par;
import static com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase.Assignment;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Optional;

import org.joda.beans.ser.JodaBeanSer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.opengamma.strata.product.TradeInfo;
import com.syndloanhub.loansum.product.facility.AnnotatedCashFlows;
import com.syndloanhub.loansum.product.facility.Borrowing;
import com.syndloanhub.loansum.product.facility.CommitmentAdjustment;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.FacilityEvent;
import com.syndloanhub.loansum.product.facility.FacilityType;
import com.syndloanhub.loansum.product.facility.FixedRateAccrual;
import com.syndloanhub.loansum.product.facility.FixedRateOption;
import com.syndloanhub.loansum.product.facility.FloatingRateAccrual;
import com.syndloanhub.loansum.product.facility.FloatingRateOption;
import com.syndloanhub.loansum.product.facility.LoanContract;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.Repayment;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTrade;

/**
 * @author jsiss
 *
 */
class LoanTradePricerTest {

  /**
   * @throws java.lang.Exception
   */
  @BeforeAll
  static void setUpBeforeClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterAll
  static void tearDownAfterClass() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @BeforeEach
  void setUp() throws Exception {
  }

  /**
   * @throws java.lang.Exception
   */
  @AfterEach
  void tearDown() throws Exception {
  }

  @Test
  public void test_termLoan_1() throws IOException {
    final Repayment REPAYMENT_1 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 3, 31))
        .amount(CurrencyAmount.of(Currency.USD, 4050000)).build();
    final Repayment REPAYMENT_2 = Repayment.builder()
        .effectiveDate(LocalDate.of(2017, 6, 30))
        .amount(CurrencyAmount.of(Currency.USD, 4558012.17)).build();

    final FloatingRateOption OPTION = FloatingRateOption.builder()
        .id(StandardId.of("oid", "Floating Rate Option 1"))
        .startDate(LocalDate.of(2017, 1, 24))
        .endDate(LocalDate.of(2022, 8, 14))
        .dayCount(DayCounts.ACT_360)
        .rate(3.25 / 100)
        .index(IborIndex.of("USD-LIBOR-3M"))
        .paymentFrequency(Frequency.P3M)
        .currency(Currency.USD)
        .build();

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
                .option(OPTION)
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
                .option(OPTION)
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
                .option(OPTION)
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
                .option(OPTION)
                .build())
        .paymentDate(LocalDate.of(2017, 7, 26)).events(REPAYMENT_2)
        .build();

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder()
        .effectiveDate(LocalDate.of(2017, 4, 20))
        .amount(CurrencyAmount.of(Currency.USD, 200000000)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "Evilcorp, TL A"))
        .agent(StandardId.of("cpty", "Ortland"))
        .borrower(StandardId.of("cpty", "Evilcorp, LLC"))
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
        .options(OPTION)
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .id(StandardId.of("trade", "126838"))
        .tradeDate(LocalDate.of(2017, 3, 21))
        .settlementDate(LocalDate.of(2017, 4, 10)).build();

    LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "SyndLoanHub"))
        .seller(StandardId.of("cpty", "CLO Group")).amount(3000000)
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

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);

    cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE, PROV, false);

    cfFileName = "src/test/resources/aliantcf_ne.json";

    expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(
        new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

  @Test
  public void test_revolvingLoan_1() throws IOException {
    final FixedRateOption OPTION = FixedRateOption.builder()
        .id(StandardId.of("oid", "Fixed Rate Option 1"))
        .startDate(LocalDate.of(2016, 7, 28))
        .endDate(LocalDate.of(2017, 3, 24))
        .dayCount(DayCounts.ACT_360)
        .rate(3.5 / 100)
        .paymentFrequency(Frequency.P1M)
        .currency(Currency.USD)
        .build();

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
                .option(OPTION)
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
                .option(OPTION)
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
                .option(OPTION)
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
                .option(OPTION)
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
                .option(OPTION)
                .build())
        .paymentDate(LocalDate.of(2016, 11, 17)).build();

    final Facility LOAN = Facility
        .builder()
        .id(StandardId.of("lid", "Morehead RC"))
        .agent(StandardId.of("cpty", "Organ Stanley"))
        .borrower(StandardId.of("cpty", "Morehead Inc."))
        .startDate(LocalDate.of(2016, 7, 28))
        .maturityDate(LocalDate.of(2017, 3, 24))
        .contracts(
            Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3,
                CONTRACT_4, CONTRACT_5))
        .originalCommitmentAmount(
            CurrencyAmount.of(Currency.USD, 300000000))
        .facilityType(FacilityType.Revolving)
        .options(OPTION)
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder()
        .id(StandardId.of("trade", "98082"))
        .tradeDate(LocalDate.of(2016, 8, 1))
        .settlementDate(LocalDate.of(2016, 8, 1)).build();

    final LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN)
        .info(TRADE_INFO).buyer(StandardId.of("cpty", "The Cash Store"))
        .seller(StandardId.of("cpty", "United Trust")).amount(300000000)
        .price(100.0 / 100)
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

    AnnotatedCashFlows expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY
        .jsonReader().read(new FileReader(cfFileName));
    assertEquals(cashFlows, expected);

    cashFlows = PRICER.cashFlows(PRORATED_LOAN_TRADE, PROV, false);
    cfFileName = "src/test/resources/stoneridgecf_ne.json";

    expected = (AnnotatedCashFlows) JodaBeanSer.PRETTY.jsonReader().read(
        new FileReader(cfFileName));
    assertEquals(cashFlows, expected);
  }

}
