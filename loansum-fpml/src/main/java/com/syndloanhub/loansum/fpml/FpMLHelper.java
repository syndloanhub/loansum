package com.syndloanhub.loansum.fpml;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AbstractFacility;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AbstractLoanServicingEvent;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.BusinessEventIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ContractId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.DayCountFraction;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.EventId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityStatement;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FixedRateAccrual;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateAccrual;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateIndexLoan;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.IssuerId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanServicingNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingAccrualSettlementEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingAssocEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingDocTypeEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingFormOfPurchaseEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingPartyRole;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingTypeEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MessageAddress;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MessageId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MoneyWithParticipantShare;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.NonNegativeMoney;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PaymentProjection;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Period;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PeriodEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Repayment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.RequestMessageHeader;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.TermLoan;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.TradeId;
import com.syndloanhub.loansum.product.facility.Accrual;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.FacilityType;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.LoanTradingAccrualSettlement;
import com.syndloanhub.loansum.product.facility.LoanTradingAssoc;
import com.syndloanhub.loansum.product.facility.LoanTradingDocType;
import com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase;
import com.syndloanhub.loansum.product.facility.LoanTradingType;

public final class FpMLHelper {
  public static final Logger log = LoggerFactory.getLogger(FpMLHelper.class);
  public static final ObjectFactory factory = new ObjectFactory();
  public static final com.syndloanhub.loansum.fpml.v5_11.loansum.ObjectFactory loansumFactory =
      new com.syndloanhub.loansum.fpml.v5_11.loansum.ObjectFactory();
  public static final String MESSAGE_ID_SCHEME = "http://www.syndloanhub.com/messaging/id";
  public static final String MESSAGE_ADDRESS_SCHEME = "http://www.syndloanhub.com/messaging/address";
  public static final String OPTION_SCHEME = "http://www.syndloanhub.com/messaging/borrowingoptionids";
  public static final String SYNDLOANHUB_EIN = "0600447425";
  public static final String NJEIN_SCHEME = "https://www.njportal.com";
  public static final String CUSIP_SCHEME = "http://www.fpml.org/coding-scheme/external/instrument-id-CUSIP";
  public static final String BBG_SCHEME = "http://www.fpml.org/coding-scheme/external/instrument-id-Bloomberg";
  public static final String PTY_SCHEME = "http://www.fpml.org/coding-scheme/external/iso9362";
  public static final String NA_SCHEME = "http://www.syndloanhub.com/coding-scheme/na";
  public static final String EVENT_ID_SCHEME = "http://www.syndloanhub.com/coding-scheme/event-id";
  public static final String DCF_SCHEME = "http://www.fpml.org/coding-scheme/day-count-fraction-2-2";
  public static final String FRI_SCHEME = "http://www.fpml.org/coding-scheme/floating-rate-index-2-30";
  public static final String CCY_SCHEME = "http://www.fpml.org/coding-scheme/currency-1-0";
  public static final String TP_ROLE_SCHEME = "http://www.fpml.org/coding-scheme/trading-party-role-1-0";

  //private static Map<StandardId, String> idMap = new HashMap<StandardId, String>();
  private static int nextId = 0;

  private static Map<StandardId, Party> partyMap = new HashMap<StandardId, Party>();

  private static int nextEventId = 0;

  public final static com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency convert(Currency value) {
    com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = factory.createCurrency();
    fpml.setValue(value.toString());
    return fpml;
  }

  public final static JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency> exportCurrencyElement(
      Currency value) {
    com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = convert(value);
    return new JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency>(
        new QName(value.getClass().getSimpleName()),
        com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency.class, fpml);
  }

  public final static NonNegativeMoney exportCurrencyAmount(CurrencyAmount value) {
    NonNegativeMoney fpml = factory.createNonNegativeMoney();
    fpml.setCurrency(convert(value.getCurrency()));
    fpml.setAmount(value.getAmount());
    return fpml;
  }

  public final static JAXBElement<NonNegativeMoney> exportCurrencyAmountElement(CurrencyAmount value) {
    final NonNegativeMoney export = exportCurrencyAmount(value);
    final XmlType xmlType = export.getClass().getAnnotation(XmlType.class);
    final JAXBElement<NonNegativeMoney> element = new JAXBElement<NonNegativeMoney>(new QName(xmlType.name()),
        NonNegativeMoney.class, exportCurrencyAmount(value));

    return element;
  }

  public final static JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade> createLoanTradeElement(
      com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade export) {
    final XmlType xmlType = export.getClass().getAnnotation(XmlType.class);
    final JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade> element =
        new JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade>(
            new QName(xmlType.name()), com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade.class, export);

    return element;
  }

  public final static LoanTradingFormOfPurchaseEnum convert(LoanTradingFormOfPurchase fop) {
    switch (fop) {
      case Assignment:
        return LoanTradingFormOfPurchaseEnum.ASSIGNMENT;
      case Participation:
        return LoanTradingFormOfPurchaseEnum.PARTICIPATION;
      default:
        return LoanTradingFormOfPurchaseEnum.ASSIGNMENT;
    }
  }

  public final static XMLGregorianCalendar getXMLGregorianCalendarNow() throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
    XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
    return now;
  }

  public final static RequestMessageHeader makeHeader() throws DatatypeConfigurationException {
    RequestMessageHeader header = factory.createRequestMessageHeader();

    MessageId id = factory.createMessageId();
    id.setMessageIdScheme(MESSAGE_ID_SCHEME);
    id.setValue("" + Math.abs(new Random().nextInt()));
    header.setMessageId(id);

    MessageAddress address = factory.createMessageAddress();
    address.setMessageAddressScheme(NJEIN_SCHEME);
    address.setValue(SYNDLOANHUB_EIN);
    header.setSentBy(address);

    header.setCreationTimestamp(getXMLGregorianCalendarNow());

    return header;
  }

  public final static StandardId convert(PartyReference ref) {
    Party party = (Party) ref.getHref();
    PartyId partyId = party.getPartyId().get(0);
    StandardId id = StandardId.of(partyId.getPartyIdScheme(), partyId.getValue());
    return id;
  }

  private final static PartyReference makePartyReference(Party party) {
    PartyReference ref = factory.createPartyReference();
    ref.setHref(party);
    return ref;
  }

  public final static PartyReference makePartyReference(StandardId id) {
    Party party = makeParty(id);
    return makePartyReference(party);
  }

  public final static BusinessEventIdentifier makeBusinessEventIdentifier(PartyReference party) {
    BusinessEventIdentifier id = factory.createBusinessEventIdentifier();
    id.setPartyReference(party);
    id.setEventId(makeEventId());
    return id;
  }

  public final static EventId makeEventId() {
    EventId eid = factory.createEventId();
    eid.setEventIdScheme(EVENT_ID_SCHEME);
    eid.setValue(String.format("EID%08d", nextEventId++));
    return eid;
  }

  public final static void clearPartyMap() {
    partyMap.clear();
  }

  private final static Party makeParty(StandardId id) {
    if (!partyMap.containsKey(id)) {
      Party party = factory.createParty();
      party.setId(nextID());

      PartyId partyId = factory.createPartyId();
      partyId.setPartyIdScheme(id.getScheme());
      partyId.setValue(id.getValue());

      party.getPartyId().add(partyId);
      partyMap.put(id, party);
    }

    return partyMap.get(id);
  }

  public final static String nextID() {
    return String.format("ID%08d", nextId++);
  }

  public final static DayCountFraction convert(DayCount dc) {
    DayCountFraction dc2 = factory.createDayCountFraction();
    dc2.setDayCountFractionScheme(DCF_SCHEME);
    dc2.setValue(DayCount.extendedEnum().externalNames("FpML").reverseLookup(dc));
    return dc2;
  }

  public final static DayCount convert(DayCountFraction dc) {
    return DayCount.of(dc.getValue());
  }

  public static Period convert(Frequency frequency) {
    Period period = factory.createPeriod();
    String s = frequency.toString();
    period.setPeriod(PeriodEnum.valueOf(s.substring(s.length() - 1)));
    period.setPeriodMultiplier(BigInteger.valueOf(Integer.valueOf(s.substring(1, s.length() - 1))));
    return period;
  }

  public static Frequency convert(Period period) {
    return Frequency.parse("P" + period.getPeriodMultiplier() + period.getPeriod().toString());
  }

  public static FloatingRateIndexLoan convert(RateIndex index) {
    FloatingRateIndexLoan fpmlIndex = factory.createFloatingRateIndexLoan();
    fpmlIndex.setFloatingRateIndexScheme(FRI_SCHEME);
    fpmlIndex.setValue(index.getName());
    return fpmlIndex;
  }

  public static RateIndex convert(FloatingRateIndexLoan index) {
    return IborIndex.of(index.getValue());
  }

  public static PaymentProjection convert(CurrencyAmount paymentProjection, LocalDate paymentDate) {
    PaymentProjection projection = factory.createPaymentProjection();
    projection.setNextPaymentDate(paymentDate);
    projection.setProjectedAmount(convert(paymentProjection));
    return projection;
  }

  public static MoneyWithParticipantShare convert(CurrencyAmount amount) {
    MoneyWithParticipantShare fpml = factory.createMoneyWithParticipantShare();
    fpml.setAmount(amount.getAmount());
    fpml.setCurrency(convert(amount.getCurrency()));
    return fpml;
  }

  public static NonNegativeMoney convertToNonNegativeMoney(CurrencyAmount amount) {
    NonNegativeMoney fpml = factory.createNonNegativeMoney();
    fpml.setCurrency(convert(amount.getCurrency()));
    fpml.setAmount(amount.getAmount());
    return fpml;
  }

  public static LoanTradingAccrualSettlementEnum convert(LoanTradingAccrualSettlement accrualSettlementType) {
    LoanTradingAccrualSettlementEnum accrualSettlementEnum = LoanTradingAccrualSettlementEnum.SETTLED_WITHOUT_ACCRUED;
    switch (accrualSettlementType) {
      case Flat:
        accrualSettlementEnum = LoanTradingAccrualSettlementEnum.FLAT;
        break;
      case SettledWithAccrued:
        accrualSettlementEnum = LoanTradingAccrualSettlementEnum.SETTLED_WITH_ACCRUED;
        break;
      case SettledWithoutAccrued:
        accrualSettlementEnum = LoanTradingAccrualSettlementEnum.SETTLED_WITHOUT_ACCRUED;
        break;
    }
    return accrualSettlementEnum;
  }

  public static LoanTradingPartyRole makeTradingRole(String role) {
    LoanTradingPartyRole fpml = factory.createLoanTradingPartyRole();
    fpml.setTradingPartyRoleScheme(TP_ROLE_SCHEME);
    fpml.setValue(role);
    return fpml;
  }

  public static IssuerId makeIssuerId(LoanTrade trade) {
    IssuerId id = factory.createIssuerId();
    id.setIssuerIdScheme(NA_SCHEME);
    id.setValue("" + Math.abs(new Random().nextInt()));
    return id;
  }

  public static TradeId makeTradeId(LoanTrade trade) {
    TradeId id = factory.createTradeId();
    id.setTradeIdScheme(trade.getInfo().getId().get().getScheme());
    id.setValue(trade.getInfo().getId().get().getValue());
    return id;
  }

  public static LoanTradingTypeEnum convert(LoanTradingType tradeType) {
    switch (tradeType) {
      case Secondary:
      default:
        return LoanTradingTypeEnum.SECONDARY;
      case Primary:
        return LoanTradingTypeEnum.PRIMARY;
    }
  }

  public static LoanTradingAssocEnum convert(LoanTradingAssoc association) {
    switch (association) {
      case LMA:
        return LoanTradingAssocEnum.LMA;
      case LSTA:
      default:
        return LoanTradingAssocEnum.LSTA;
    }
  }

  public static LoanTradingDocTypeEnum convert(LoanTradingDocType documentationType) {
    switch (documentationType) {
      case Par:
      default:
        return LoanTradingDocTypeEnum.PAR;
      case Distressed:
        return LoanTradingDocTypeEnum.DISTRESSED;
    }
  }

  public static Facility convert(FacilityStatement fpmlFacility, LoanServicingNotification fpmlContracts) {
    List<com.syndloanhub.loansum.product.facility.Repayment> repayments =
        new ArrayList<com.syndloanhub.loansum.product.facility.Repayment>();
    List<com.syndloanhub.loansum.product.facility.LoanContract> contracts =
        new ArrayList<com.syndloanhub.loansum.product.facility.LoanContract>();
    List<com.syndloanhub.loansum.product.facility.FacilityEvent> events =
        new ArrayList<com.syndloanhub.loansum.product.facility.FacilityEvent>();

    List<JAXBElement<? extends AbstractLoanServicingEvent>> fpmlEvents =
        fpmlContracts.getFacilityEventGroupOrLcEventGroupOrLoanContractEventGroup();

    for (JAXBElement<? extends AbstractLoanServicingEvent> event : fpmlEvents) {
      if (event.getDeclaredType() == Repayment.class) {
        repayments.add(FpMLHelper.convert((Repayment) event.getValue()));
      }
    }

    for (JAXBElement<? extends Serializable> item : fpmlContracts.getDealIdentifierOrDealSummaryAndFacilityIdentifier()) {
      Object object = item.getValue();
      if (object.getClass() == LoanContract.class) {
        contracts.add(convert((LoanContract) object));
      }
    }

    AbstractFacility loan = fpmlFacility.getFacilityGroup().getValue();
    FacilityType facilityType = FacilityType.Term;

    if (loan.getClass() == TermLoan.class)
      facilityType = FacilityType.Term;

    List<StandardId> identifiers = new ArrayList<StandardId>();

    return Facility.builder()
        .agent(convert(loan.getAgentPartyReference()))
        .borrower(convert(loan.getBorrowerPartyReference()))
        .contracts(contracts)
        .events(events)
        .facilityType(facilityType)
        .id(convert(loan.getInstrumentId().get(0)))
        .identifiers(identifiers)
        .maturityDate(loan.getMaturityDate())
        .originalCommitmentAmount(convert(loan.getOriginalCommitment()))
        .startDate(loan.getStartDate())
        .build();
  }

  private static com.syndloanhub.loansum.product.facility.LoanContract convert(LoanContract contract) {
    Accrual accrual = null;

    if (contract.getFixedRateAccrual() != null)
      accrual = convert(contract.getFixedRateAccrual(), contract);
    else if (contract.getFloatingRateAccrual() != null)
      accrual = convert(contract.getFloatingRateAccrual(), contract);

    return com.syndloanhub.loansum.product.facility.LoanContract.builder()
        .accrual(accrual)
        .paymentDate(accrual.getPaymentDate().orElse(accrual.getEndDate()))
        .id(convert(contract.getContractId().get(0)))
        .build();
  }

  private static StandardId convert(ContractId contractId) {
    return StandardId.of(contractId.getContractIdScheme(), contractId.getValue());
  }

  private static Accrual convert(FixedRateAccrual fixedRateAccrual, LoanContract contract) {
    return com.syndloanhub.loansum.product.facility.FixedRateAccrual.builder()
        .accrualAmount(convert(contract.getAmount()))
        .allInRate(fixedRateAccrual.getAllInRate())
        .dayCount(convert(fixedRateAccrual.getDayCountFraction()))
        .endDate(fixedRateAccrual.getEndDate())
        .paymentDate(fixedRateAccrual.getEndDate())
        .paymentFrequency(convert(fixedRateAccrual.getPaymentFrequency()))
        .startDate(fixedRateAccrual.getStartDate())
        .build();
  }

  private static Accrual convert(FloatingRateAccrual floatingRateAccrual, LoanContract contract) {
    return com.syndloanhub.loansum.product.facility.FloatingRateAccrual.builder()
        .accrualAmount(convert(contract.getAmount()))
        .allInRate(floatingRateAccrual.getAllInRate())
        .dayCount(convert(floatingRateAccrual.getDayCountFraction()))
        .endDate(floatingRateAccrual.getEndDate())
        .paymentDate(floatingRateAccrual.getEndDate())
        .paymentFrequency(convert(floatingRateAccrual.getPaymentFrequency()))
        .startDate(floatingRateAccrual.getStartDate())
        .baseRate(floatingRateAccrual.getBaseRate())
        .index(convert(floatingRateAccrual.getFloatingRateIndex()))
        .build();
  }

  private static StandardId convert(InstrumentId instrumentId) {
    return StandardId.of(instrumentId.getInstrumentIdScheme(), instrumentId.getValue());
  }

  static public com.syndloanhub.loansum.product.facility.Repayment convert(Repayment event) {
    // TODO: interest on paydown?
    return com.syndloanhub.loansum.product.facility.Repayment.builder()
        .effectiveDate(event.getEffectiveDate())
        .amount(FpMLHelper.convert(event.getAmount()))
        .price(event.getPrice())
        .build();
  }

  public static CurrencyAmount convert(MoneyWithParticipantShare amount) {
    return CurrencyAmount.of(FpMLHelper.convert(amount.getCurrency()), amount.getAmount());
  }

  private static Currency convert(com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency currency) {
    return Currency.of(currency.getValue());
  }

}
