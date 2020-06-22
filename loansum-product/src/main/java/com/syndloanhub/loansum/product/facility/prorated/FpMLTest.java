package com.syndloanhub.loansum.product.facility.prorated;

import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.syndloanhub.loansum.product.facility.FacilityType.Term;
import static com.syndloanhub.loansum.product.facility.FpMLHelper.exportCurrencyAmountElement;
import static com.syndloanhub.loansum.product.facility.FpMLHelper.exportCurrencyElement;
import static com.syndloanhub.loansum.product.facility.LoanTradingAccrualSettlement.SettledWithoutAccrued;
import static com.syndloanhub.loansum.product.facility.LoanTradingAssoc.LSTA;
import static com.syndloanhub.loansum.product.facility.LoanTradingDocType.Par;
import static com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase.Assignment;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.product.TradeInfo;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.NonNegativeMoney;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.OutstandingContractsStatement;
import com.syndloanhub.loansum.fpml.v5_11.util.FpMLNamespacePrefixMapper;
import com.syndloanhub.loansum.product.facility.CommitmentAdjustment;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.FacilityEvent;
import com.syndloanhub.loansum.product.facility.FloatingRateAccrual;
import com.syndloanhub.loansum.product.facility.LoanContract;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.OutstandingContractsStatementExporter;
import com.syndloanhub.loansum.product.facility.Repayment;

class FpMLTest {
	private static final Logger log = LoggerFactory.getLogger(FpMLTest.class);

	private Marshaller createMarshaller(JAXBContext context) throws JAXBException {
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);

		FpMLNamespacePrefixMapper mapper = new FpMLNamespacePrefixMapper();
		mapper.addMapping("http://www.fpml.org/FpML-5/confirmation", "fpml");
		mapper.addMapping("http://www.w3.org/2000/09/xmldsig#", "ds");

		marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", mapper);

		return marshaller;
	}

	@Test
	public void test_termLoan_1() throws IOException, JAXBException, DatatypeConfigurationException {
		final Repayment REPAYMENT_1 = Repayment.builder().effectiveDate(LocalDate.of(2017, 3, 31))
				.amount(CurrencyAmount.of(Currency.USD, 4050000)).build();
		final Repayment REPAYMENT_2 = Repayment.builder().effectiveDate(LocalDate.of(2017, 6, 30))
				.amount(CurrencyAmount.of(Currency.USD, 4558012.17)).build();

		final LoanContract CONTRACT_1 = LoanContract.builder().id(StandardId.of("contract", "1"))
				.accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 1, 24))
						.endDate(LocalDate.of(2017, 3, 16)).dayCount(DayCounts.ACT_360).allInRate(4.50283 / 100)
						.baseRate(1.2583 / 100).spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M"))
						.paymentFrequency(Frequency.P3M).accrualAmount(CurrencyAmount.of(Currency.USD, 1598500000))
						.build())
				.paymentDate(LocalDate.of(2017, 3, 16)).build();
		final LoanContract CONTRACT_2 = LoanContract.builder().id(StandardId.of("contract", "2"))
				.accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 3, 16))
						.endDate(LocalDate.of(2017, 4, 20)).dayCount(DayCounts.ACT_360).allInRate(4.38733 / 100.0)
						.baseRate(1.13733 / 100).spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M"))
						.paymentFrequency(Frequency.P3M).accrualAmount(CurrencyAmount.of(Currency.USD, 1598500000))
						.build())
				.paymentDate(LocalDate.of(2017, 4, 26)).events(REPAYMENT_1).build();
		final LoanContract CONTRACT_3 = LoanContract.builder().id(StandardId.of("contract", "3"))
				.accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 4, 20))
						.endDate(LocalDate.of(2017, 4, 26)).allInRate(4.38733 / 100.0).baseRate(1.13733 / 100)
						.spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M")).paymentFrequency(Frequency.P3M)
						.dayCount(DayCounts.ACT_360).accrualAmount(CurrencyAmount.of(Currency.USD, 1794450000)).build())
				.paymentDate(LocalDate.of(2017, 4, 26)).build();
		final LoanContract CONTRACT_4 = LoanContract.builder().id(StandardId.of("contract", "4"))
				.accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 4, 26))
						.endDate(LocalDate.of(2017, 7, 26)).dayCount(DayCounts.ACT_360).allInRate(4.4165 / 100)
						.baseRate(1.1665 / 100).spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M"))
						.paymentFrequency(Frequency.P3M).accrualAmount(CurrencyAmount.of(Currency.USD, 1794450000))
						.build())
				.paymentDate(LocalDate.of(2017, 7, 26)).events(REPAYMENT_2).build();

		final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder().effectiveDate(LocalDate.of(2017, 4, 20))
				.amount(CurrencyAmount.of(Currency.USD, 200000000)).build();

		final Facility LOAN = Facility.builder().id(StandardId.of("lid", "Evilcorp, TL A"))
				.agent(StandardId.of("cpty", "Ortland")).borrower(StandardId.of("cpty", "Evilcorp, LLC"))
				.startDate(LocalDate.of(2017, 1, 24)).maturityDate(LocalDate.of(2022, 8, 14))
				.contracts(Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3, CONTRACT_4)).events(ADJUSTMENT_1)
				.facilityType(Term).originalCommitmentAmount(CurrencyAmount.of(Currency.USD, 1598500000))
				.identifiers(Arrays.asList(StandardId.of("LXID", "LX123456"), StandardId.of("CUSIP", "012345678"),
						StandardId.of("BLOOMBERGID", "BB12345678")))
				.build();

		final TradeInfo TRADE_INFO = TradeInfo.builder().id(StandardId.of("trade", "126838"))
				.tradeDate(LocalDate.of(2017, 3, 21)).settlementDate(LocalDate.of(2017, 4, 10)).build();

		LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN).info(TRADE_INFO)
				.buyer(StandardId.of("cpty", "SyndLoanHub")).seller(StandardId.of("cpty", "CLO Group")).amount(3000000)
				.price(101.125 / 100).expectedSettlementDate(LocalDate.of(2017, 3, 30)).averageLibor(0.9834 / 100)
				.buySell(BUY).accrualSettlementType(SettledWithoutAccrued).association(LSTA)
				.commitmentReductionCreditFlag(true).currency(Currency.USD).delayedCompensationFlag(true)
				.documentationType(Par).formOfPurchase(Assignment).paydownOnTradeDate(false).build();

		JAXBElement<OutstandingContractsStatement> e2 = new OutstandingContractsStatementExporter(LOAN).exportElement();
		JAXBContext context = JAXBContext.newInstance(OutstandingContractsStatement.class);
		Marshaller marshaller = createMarshaller(context);
		marshaller.marshal(e2, System.out);
		
		/*
		log.info("JAXB! 2");
		JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency> element = exportCurrencyElement(
				LOAN_TRADE.getCurrency());
		JAXBContext context = JAXBContext.newInstance(com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency.class);
		Marshaller marshaller = createMarshaller(context);
		marshaller.marshal(element, System.out);

		JAXBElement<NonNegativeMoney> e = exportCurrencyAmountElement(CONTRACT_1.getAccrual().getAccrualAmount());
		context = JAXBContext.newInstance(NonNegativeMoney.class);
		marshaller = createMarshaller(context);
		marshaller.marshal(e, System.out);

		JAXBElement<LoanTradeNotification> e2 = LOAN_TRADE.exportElement();
		context = JAXBContext.newInstance(LoanTradeNotification.class);
		marshaller = createMarshaller(context);
		marshaller.marshal(e2, System.out);
		*/
	}

}
