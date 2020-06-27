package com.syndloanhub.loansum.fpml.test;

import static com.opengamma.strata.product.common.BuySell.BUY;
import static com.syndloanhub.loansum.product.facility.FacilityType.Term;
import static com.syndloanhub.loansum.product.facility.LoanTradingAccrualSettlement.SettledWithoutAccrued;
import static com.syndloanhub.loansum.product.facility.LoanTradingAssoc.LSTA;
import static com.syndloanhub.loansum.product.facility.LoanTradingDocType.Par;
import static com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase.Assignment;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
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
import com.syndloanhub.loansum.fpml.FpMLHelper;
import com.syndloanhub.loansum.fpml.LoanContractExporter;
import com.syndloanhub.loansum.fpml.LoanServicingNotificationExporter;
import com.syndloanhub.loansum.fpml.OutstandingContractsStatementExporter;
import com.syndloanhub.loansum.fpml.ProratedLoanContractExporter;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityContractIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanServicingNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.OutstandingContractsStatement;
import com.syndloanhub.loansum.fpml.v5_11.util.FpMLNamespacePrefixMapper;
import com.syndloanhub.loansum.product.facility.CommitmentAdjustment;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.FacilityEvent;
import com.syndloanhub.loansum.product.facility.FloatingRateAccrual;
import com.syndloanhub.loansum.product.facility.LoanContract;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.Repayment;

class FpMLTest {
  private static final Logger log = LoggerFactory.getLogger(FpMLTest.class);

  private static final String CUSIP_SCHEME = "http://www.fpml.org/coding-scheme/external/instrument-id-CUSIP";
  private static final String BBG_SCHEME = "http://www.fpml.org/coding-scheme/external/instrument-id-Bloomberg";
  private static final String PTY_SCHEME = "http://www.fpml.org/coding-scheme/external/iso9362";
  private static final String NA_SCHEME = "http://www.fpml.org/coding-scheme/external/na";

  private Marshaller createMarshaller(JAXBContext context) throws JAXBException {
    FpMLNamespacePrefixMapper mapper = new FpMLNamespacePrefixMapper();
    mapper.addMapping("http://www.fpml.org/FpML-5/confirmation", "fpml");
    mapper.addMapping("http://www.w3.org/2000/09/xmldsig#", "ds");

    Marshaller marshaller = context.createMarshaller();

    marshaller.setProperty("jaxb.formatted.output", Boolean.TRUE);
    marshaller.setProperty("jaxb.fragment", Boolean.TRUE);
    marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
        "http://www.fpml.org/FpML-5/confirmation https://loansum.org/schemas/fpml/5_11/confirmation/fpml-loan-5-11.xsd");
    marshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", mapper);
    marshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
        "http://www.fpml.org/FpML-5/confirmation https://loansum.org/schemas/fpml/5_11/confirmation/fpml-loan-5-11.xsd");

    return marshaller;
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test_termLoan_1() throws IOException, JAXBException, DatatypeConfigurationException {
    final Repayment REPAYMENT_1 = Repayment.builder().effectiveDate(LocalDate.of(2017, 3, 31))
        .amount(CurrencyAmount.of(Currency.USD, 4050000)).build();
    final Repayment REPAYMENT_2 = Repayment.builder().effectiveDate(LocalDate.of(2017, 6, 30))
        .amount(CurrencyAmount.of(Currency.USD, 4558012.17)).build();

    final LoanContract CONTRACT_1 = LoanContract.builder().id(StandardId.of(NA_SCHEME, "1"))
        .accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 1, 24))
            .endDate(LocalDate.of(2017, 3, 16)).dayCount(DayCounts.ACT_360).allInRate(4.50283 / 100)
            .baseRate(1.2583 / 100).spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M"))
            .paymentFrequency(Frequency.P3M).accrualAmount(CurrencyAmount.of(Currency.USD, 1598500000))
            .build())
        .paymentDate(LocalDate.of(2017, 3, 16)).build();
    final LoanContract CONTRACT_2 = LoanContract.builder().id(StandardId.of(NA_SCHEME, "2"))
        .accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 3, 16))
            .endDate(LocalDate.of(2017, 4, 20)).dayCount(DayCounts.ACT_360).allInRate(4.38733 / 100.0)
            .baseRate(1.13733 / 100).spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M"))
            .paymentFrequency(Frequency.P3M).accrualAmount(CurrencyAmount.of(Currency.USD, 1598500000))
            .build())
        .paymentDate(LocalDate.of(2017, 4, 26)).events(REPAYMENT_1).build();
    final LoanContract CONTRACT_3 = LoanContract.builder().id(StandardId.of(NA_SCHEME, "3"))
        .accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 4, 20))
            .endDate(LocalDate.of(2017, 4, 26)).allInRate(4.38733 / 100.0).baseRate(1.13733 / 100)
            .spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M")).paymentFrequency(Frequency.P3M)
            .dayCount(DayCounts.ACT_360).accrualAmount(CurrencyAmount.of(Currency.USD, 1794450000)).build())
        .paymentDate(LocalDate.of(2017, 4, 26)).build();
    final LoanContract CONTRACT_4 = LoanContract.builder().id(StandardId.of(NA_SCHEME, "4"))
        .accrual(FloatingRateAccrual.builder().startDate(LocalDate.of(2017, 4, 26))
            .endDate(LocalDate.of(2017, 7, 26)).dayCount(DayCounts.ACT_360).allInRate(4.4165 / 100)
            .baseRate(1.1665 / 100).spread(3.25 / 100).index(IborIndex.of("USD-LIBOR-3M"))
            .paymentFrequency(Frequency.P3M).accrualAmount(CurrencyAmount.of(Currency.USD, 1794450000))
            .build())
        .paymentDate(LocalDate.of(2017, 7, 26)).events(REPAYMENT_2).build();

    final FacilityEvent ADJUSTMENT_1 = CommitmentAdjustment.builder().effectiveDate(LocalDate.of(2017, 4, 20))
        .amount(CurrencyAmount.of(Currency.USD, 200000000)).build();

    final Facility LOAN = Facility.builder().id(StandardId.of(CUSIP_SCHEME, "012345678"))
        .agent(StandardId.of(PTY_SCHEME, "AGENT")).borrower(StandardId.of(PTY_SCHEME, "BORROWER"))
        .startDate(LocalDate.of(2017, 1, 24)).maturityDate(LocalDate.of(2022, 8, 14))
        .contracts(Arrays.asList(CONTRACT_1, CONTRACT_2, CONTRACT_3, CONTRACT_4)).events(ADJUSTMENT_1)
        .facilityType(Term).originalCommitmentAmount(CurrencyAmount.of(Currency.USD, 1598500000))
        .identifiers(
            Arrays.asList(StandardId.of(CUSIP_SCHEME, "012345678"), StandardId.of(BBG_SCHEME, "012345678")))
        .build();

    final TradeInfo TRADE_INFO = TradeInfo.builder().id(StandardId.of(NA_SCHEME, "126838"))
        .tradeDate(LocalDate.of(2017, 3, 21)).settlementDate(LocalDate.of(2017, 4, 10)).build();

    LoanTrade LOAN_TRADE = LoanTrade.builder().product(LOAN).info(TRADE_INFO)
        .buyer(StandardId.of(PTY_SCHEME, "987654")).seller(StandardId.of(PTY_SCHEME, "76543")).amount(3000000)
        .price(101.125 / 100).expectedSettlementDate(LocalDate.of(2017, 3, 30)).averageLibor(0.9834 / 100)
        .buySell(BUY).accrualSettlementType(SettledWithoutAccrued).association(LSTA)
        .commitmentReductionCreditFlag(true).currency(Currency.USD).delayedCompensationFlag(true)
        .documentationType(Par).formOfPurchase(Assignment).paydownOnTradeDate(false).build();

    final LocalDate effectiveDate = LocalDate.of(2017, 5, 1);

    ObjectFactory factory = new ObjectFactory();
    JAXBElement<LoanServicingNotification> je = factory.createLoanServicingNotification(
        LoanServicingNotificationExporter.convert(LOAN));
    JAXBContext context = JAXBContext.newInstance(LoanServicingNotification.class);
    Marshaller marshaller = createMarshaller(context);

    StringWriter sw = new StringWriter();
    marshaller.marshal(je, sw);

    log.debug("facility: \n" + sw.toString());
    
    /*
    ObjectFactory factory = new ObjectFactory();
    JAXBElement<OutstandingContractsStatement> je = factory.createOutstandingContractsStatement(
        OutstandingContractsStatementExporter.export(effectiveDate, LOAN, true));
    JAXBContext context = JAXBContext.newInstance(OutstandingContractsStatement.class);
    Marshaller marshaller = createMarshaller(context);

    StringWriter sw = new StringWriter();
    marshaller.marshal(je, sw);

    log.debug("contracts: \n" + sw.toString());

    Unmarshaller unmarshaller = context.createUnmarshaller();

    je = (JAXBElement<OutstandingContractsStatement>) unmarshaller.unmarshal(new StringReader(sw.toString()));

    List<LoanContract> contracts = new ArrayList<LoanContract>();

    for (FacilityContractIdentifier contractOrLC : je.getValue().getLoanContractOrLetterOfCredit()) {
      if (contractOrLC.getClass() == com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract.class) {
        com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract contract =
            (com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract) contractOrLC;
      }
    }
  }
*/
  }
}
