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
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.IborIndex;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AbstractFacility;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AbstractLoanServicingEvent;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AccrualOptionBase;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AccrualTypeId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Adjustment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.AmountAdjustmentEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.BusinessEventIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.BuySellEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.CommitmentAdjustment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ContractId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.DayCountFraction;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.EventId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityCommitment;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityStatement;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FixedRateAccrual;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateAccrual;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateIndexLoan;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FloatingRateOption;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.IssuerId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContractReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanServicingNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeEvent;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingAccrualSettlementEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingAssocEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingDocTypeEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingFormOfPurchaseEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingPartyRole;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingTypeEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTransferFee;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTransferFeePaidByEnum;
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
import com.syndloanhub.loansum.product.facility.FeeAndRateOption;
import com.syndloanhub.loansum.product.facility.LoanContractEvent;
import com.syndloanhub.loansum.product.facility.LoanTradingAccrualSettlement;
import com.syndloanhub.loansum.product.facility.LoanTradingAssoc;
import com.syndloanhub.loansum.product.facility.LoanTradingDocType;
import com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase;
import com.syndloanhub.loansum.product.facility.LoanTradingType;

public final class FpMLHelper {
  public final static com.syndloanhub.loansum.fpml.v5_11.loansum.ObjectFactory loansumFactory =
      new com.syndloanhub.loansum.fpml.v5_11.loansum.ObjectFactory();

  private static final ObjectFactory factory = new ObjectFactory();

  private static final String MESSAGE_ID_SCHEME = "http://www.syndloanhub.com/messaging/id";
  private static final String SYNDLOANHUB_EIN = "0600447425";
  private static final String NJEIN_SCHEME = "https://www.njportal.com";
  private static final String NA_SCHEME = "http://www.syndloanhub.com/coding-scheme/na";
  private static final String EVENT_ID_SCHEME = "http://www.syndloanhub.com/coding-scheme/event-id";
  private static final String DCF_SCHEME = "http://www.fpml.org/coding-scheme/day-count-fraction-2-2";
  private static final String FRI_SCHEME = "http://www.fpml.org/coding-scheme/floating-rate-index-2-30";
  private static final String TP_ROLE_SCHEME = "http://www.fpml.org/coding-scheme/trading-party-role-1-0";

  //private static Map<StandardId, String> idMap = new HashMap<StandardId, String>();
  private static int nextId = 0;

  private static Map<StandardId, Party> partyMap = new HashMap<StandardId, Party>();

  private static int nextEventId = 0;

  private final static com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency convert(Currency value) {
    com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = factory.createCurrency();
    fpml.setValue(value.toString());
    return fpml;
  }

  private final static LoanTradingFormOfPurchaseEnum convert(LoanTradingFormOfPurchase fop) {
    switch (fop) {
      case Assignment:
        return LoanTradingFormOfPurchaseEnum.ASSIGNMENT;
      case Participation:
        return LoanTradingFormOfPurchaseEnum.PARTICIPATION;
      default:
        return LoanTradingFormOfPurchaseEnum.ASSIGNMENT;
    }
  }

  private final static XMLGregorianCalendar getXMLGregorianCalendarNow() throws DatatypeConfigurationException {
    GregorianCalendar gregorianCalendar = new GregorianCalendar();
    DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
    XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
    return now;
  }

  private final static RequestMessageHeader makeHeader() throws DatatypeConfigurationException {
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

  private final static StandardId convert(PartyReference ref) {
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

  private final static PartyReference makePartyReference(StandardId id) {
    Party party = makeParty(id);
    return makePartyReference(party);
  }

  private final static BusinessEventIdentifier makeBusinessEventIdentifier(PartyReference party) {
    BusinessEventIdentifier id = factory.createBusinessEventIdentifier();
    id.setPartyReference(party);
    id.setEventId(makeEventId());
    return id;
  }

  private final static EventId makeEventId() {
    EventId eid = factory.createEventId();
    eid.setEventIdScheme(EVENT_ID_SCHEME);
    eid.setValue(String.format("EID%08d", nextEventId++));
    return eid;
  }

  private final static void clearPartyMap() {
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

  private final static String nextID() {
    return String.format("ID%08d", nextId++);
  }

  private final static DayCountFraction convert(DayCount dc) {
    DayCountFraction dc2 = factory.createDayCountFraction();
    dc2.setDayCountFractionScheme(DCF_SCHEME);
    dc2.setValue(DayCount.extendedEnum().externalNames("FpML").reverseLookup(dc));
    return dc2;
  }

  private final static DayCount convert(DayCountFraction dc) {
    return DayCount.of(dc.getValue());
  }

  private final static Period convert(Frequency frequency) {
    Period period = factory.createPeriod();
    String s = frequency.toString();
    period.setPeriod(PeriodEnum.valueOf(s.substring(s.length() - 1)));
    period.setPeriodMultiplier(BigInteger.valueOf(Integer.valueOf(s.substring(1, s.length() - 1))));
    return period;
  }

  private final static Frequency convert(Period period) {
    return Frequency.parse("P" + period.getPeriodMultiplier() + period.getPeriod().toString());
  }

  private final static FloatingRateIndexLoan convert(RateIndex index) {
    FloatingRateIndexLoan fpmlIndex = factory.createFloatingRateIndexLoan();
    fpmlIndex.setFloatingRateIndexScheme(FRI_SCHEME);
    fpmlIndex.setValue(index.getName());
    return fpmlIndex;
  }

  private final static RateIndex convert(FloatingRateIndexLoan index) {
    return IborIndex.of(index.getValue());
  }

  private final static PaymentProjection convert(CurrencyAmount paymentProjection, LocalDate paymentDate) {
    PaymentProjection projection = factory.createPaymentProjection();
    projection.setNextPaymentDate(paymentDate);
    projection.setProjectedAmount(convert(paymentProjection));
    return projection;
  }

  private final static MoneyWithParticipantShare convert(CurrencyAmount amount) {
    MoneyWithParticipantShare fpml = factory.createMoneyWithParticipantShare();
    fpml.setAmount(amount.getAmount());
    fpml.setCurrency(convert(amount.getCurrency()));
    return fpml;
  }

  private final static NonNegativeMoney convertToNonNegativeMoney(CurrencyAmount amount) {
    NonNegativeMoney fpml = factory.createNonNegativeMoney();
    fpml.setCurrency(convert(amount.getCurrency()));
    fpml.setAmount(amount.getAmount());
    return fpml;
  }

  private final static LoanTradingAccrualSettlementEnum convert(LoanTradingAccrualSettlement accrualSettlementType) {
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

  private final static LoanTradingPartyRole makeTradingRole(String role) {
    LoanTradingPartyRole fpml = factory.createLoanTradingPartyRole();
    fpml.setTradingPartyRoleScheme(TP_ROLE_SCHEME);
    fpml.setValue(role);
    return fpml;
  }

  private final static IssuerId makeIssuerId(com.syndloanhub.loansum.product.facility.LoanTrade trade) {
    IssuerId id = factory.createIssuerId();
    id.setIssuerIdScheme(NA_SCHEME);
    id.setValue("" + Math.abs(new Random().nextInt()));
    return id;
  }

  private final static TradeId makeTradeId(com.syndloanhub.loansum.product.facility.LoanTrade trade) {
    TradeId id = factory.createTradeId();
    id.setTradeIdScheme(trade.getInfo().getId().get().getScheme());
    id.setValue(trade.getInfo().getId().get().getValue());
    return id;
  }

  private final static LoanTradingTypeEnum convert(LoanTradingType tradeType) {
    switch (tradeType) {
      case Secondary:
      default:
        return LoanTradingTypeEnum.SECONDARY;
      case Primary:
        return LoanTradingTypeEnum.PRIMARY;
    }
  }

  private final static LoanTradingAssocEnum convert(LoanTradingAssoc association) {
    switch (association) {
      case LMA:
        return LoanTradingAssocEnum.LMA;
      case LSTA:
      default:
        return LoanTradingAssocEnum.LSTA;
    }
  }

  private final static LoanTradingDocTypeEnum convert(LoanTradingDocType documentationType) {
    switch (documentationType) {
      case Par:
      default:
        return LoanTradingDocTypeEnum.PAR;
      case Distressed:
        return LoanTradingDocTypeEnum.DISTRESSED;
    }
  }

  public final static Facility convert(FacilityStatement fpmlFacility, LoanServicingNotification fpmlContracts) throws Exception {
    List<com.syndloanhub.loansum.product.facility.LoanContract> contracts =
        new ArrayList<com.syndloanhub.loansum.product.facility.LoanContract>();
    Map<StandardId, List<LoanContractEvent>> eventMap = new HashMap<StandardId, List<LoanContractEvent>>();
    List<com.syndloanhub.loansum.product.facility.FacilityEvent> events =
        new ArrayList<com.syndloanhub.loansum.product.facility.FacilityEvent>();
    List<JAXBElement<? extends AbstractLoanServicingEvent>> fpmlEvents =
        fpmlContracts.getFacilityEventGroupOrLcEventGroupOrLoanContractEventGroup();

    for (JAXBElement<? extends AbstractLoanServicingEvent> event : fpmlEvents) {
      if (event.getDeclaredType() == Repayment.class) {
        Repayment repayment = (Repayment) event.getValue();
        LoanContract contract = (LoanContract) repayment.getLoanContractReference().getHref();
        StandardId contractId = convert(contract.getContractId().get(0));

        if (!eventMap.containsKey(contractId)) {
          eventMap.put(contractId, new ArrayList<LoanContractEvent>());
        }
        eventMap.get(contractId).add(convert(repayment));
      } else if (event.getDeclaredType() == CommitmentAdjustment.class) {
        events.add(convert((CommitmentAdjustment) event.getValue()));
      }
    }

    for (JAXBElement<? extends Serializable> item : fpmlContracts.getDealIdentifierOrDealSummaryAndFacilityIdentifier()) {
      Object object = item.getValue();
      if (object.getClass() == LoanContract.class) {
        contracts.add(convert((LoanContract) object, eventMap));
      }
    }

    AbstractFacility loan = fpmlFacility.getFacilityGroup().getValue();
    FacilityType facilityType = FacilityType.Term;

    if (loan.getClass() == TermLoan.class)
      facilityType = FacilityType.Term;

    List<StandardId> identifiers = new ArrayList<StandardId>();
    for (InstrumentId id : loan.getInstrumentId()) {
      identifiers.add(convert(id));
    }

    List<FeeAndRateOption> options = new ArrayList<FeeAndRateOption>();

    for (AccrualOptionBase option : loan.getFixedRateOptionOrFloatingRateOptionOrLcOption()) {
      if (option instanceof FloatingRateOption) {
        options.add(convert((FloatingRateOption) option));
      } else
        throw new Exception();
    }

    return Facility.builder()
        .agent(convert(loan.getAgentPartyReference()))
        .borrower(convert(loan.getBorrowerPartyReference()))
        .contracts(contracts)
        .events(events)
        .facilityType(facilityType)
        .id(identifiers.get(0))
        .identifiers(identifiers)
        .maturityDate(loan.getMaturityDate())
        .originalCommitmentAmount(convert(loan.getOriginalCommitment()))
        .startDate(loan.getStartDate())
        .options(options)
        .build();
  }

  private static CurrencyAmount convert(Adjustment adjustment) {
    switch (adjustment.getAdjustmentType()) {
      case INCREASE:
      default:
        return CurrencyAmount.of(adjustment.getAmount().getCurrency().getValue(), adjustment.getAmount().getAmount());
      case DECREASE:
        return CurrencyAmount.of(adjustment.getAmount().getCurrency().getValue(), adjustment.getAmount().getAmount()).negated();
    }
  }

  private static com.syndloanhub.loansum.product.facility.LoanContract convert(LoanContract contract,
      Map<StandardId, List<LoanContractEvent>> eventMap) {
    Accrual accrual = null;

    if (contract.getFixedRateAccrual() != null)
      accrual = convert(contract.getFixedRateAccrual(), contract);
    else if (contract.getFloatingRateAccrual() != null)
      accrual = convert(contract.getFloatingRateAccrual(), contract);

    StandardId contractId = convert(contract.getContractId().get(0));
    List<LoanContractEvent> contractEvents =
        eventMap.containsKey(contractId) ? eventMap.get(contractId) : new ArrayList<LoanContractEvent>();

    return com.syndloanhub.loansum.product.facility.LoanContract.builder()
        .accrual(accrual)
        .paymentDate(contract.getMaturityDate()) // TODO: payment date
        .id(contractId)
        .events(contractEvents)
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
        .paymentFrequency(convert(floatingRateAccrual.getPaymentFrequency()))
        .startDate(floatingRateAccrual.getStartDate())
        .baseRate(floatingRateAccrual.getBaseRate())
        .spread(floatingRateAccrual.getSpread())
        .pikSpread(floatingRateAccrual.getPikSpread() == null ? 0 : floatingRateAccrual.getPikSpread())
        .index(convert(floatingRateAccrual.getFloatingRateIndex()))
        .build();
  }

  private static StandardId convert(InstrumentId instrumentId) {
    return StandardId.of(instrumentId.getInstrumentIdScheme(), instrumentId.getValue());
  }

  static private com.syndloanhub.loansum.product.facility.Repayment convert(Repayment event) {
    // TODO: interest on paydown?
    return com.syndloanhub.loansum.product.facility.Repayment.builder()
        .effectiveDate(event.getEffectiveDate())
        .amount(FpMLHelper.convert(event.getAmount()))
        .price(event.getPrice())
        .build();
  }

  private final static CurrencyAmount convert(MoneyWithParticipantShare amount) {
    return CurrencyAmount.of(FpMLHelper.convert(amount.getCurrency()), amount.getAmount());
  }

  private static Currency convert(com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency currency) {
    return Currency.of(currency.getValue());
  }

  static private CommitmentAdjustment convert(com.syndloanhub.loansum.product.facility.CommitmentAdjustment event) {
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

  static private com.syndloanhub.loansum.product.facility.CommitmentAdjustment convert(CommitmentAdjustment fpml) {
    return com.syndloanhub.loansum.product.facility.CommitmentAdjustment.builder()
        .effectiveDate(fpml.getEffectiveDate())
        .pik(fpml.isPik())
        .refusalAllowed(fpml.isRefusalAllowed())
        .amount(convert(fpml.getAdjustment()))
        .build();
  }

  static private FixedRateAccrual convert(com.syndloanhub.loansum.product.facility.FixedRateAccrual accrual) {
    FixedRateAccrual fpml = FpMLHelper.factory.createFixedRateAccrual();
    return fpml;
  }

  static private FloatingRateAccrual convert(com.syndloanhub.loansum.product.facility.FloatingRateAccrual accrual) {
    FloatingRateAccrual fpml = FpMLHelper.factory.createFloatingRateAccrual();
    fpml.setAllInRate(accrual.getAllInRate());
    fpml.setBaseRate(accrual.getBaseRate());
    fpml.setDayCountFraction(FpMLHelper.convert(accrual.getDayCount()));
    fpml.setDefaultSpread(accrual.getSpread());
    fpml.setEndDate(accrual.getEndDate());
    fpml.setStartDate(accrual.getStartDate());
    fpml.setNumberOfDays(BigInteger.valueOf(accrual.getDays()));
    fpml.setPaymentFrequency(FpMLHelper.convert(accrual.getPaymentFrequency()));
    fpml.setFloatingRateIndex(FpMLHelper.convert(accrual.getIndex()));
    fpml.setPaymentProjection(FpMLHelper.convert(accrual.getPaymentProjection(),
        accrual.getPaymentDate().orElse(accrual.getEndDate())));
    // TODO: accrual types in facility and accruals
    AccrualTypeId accrualTypeId = FpMLHelper.factory.createAccrualTypeId();
    accrualTypeId.setAccrualTypeIdScheme(FpMLHelper.NA_SCHEME);
    accrualTypeId.setValue("N/A");
    fpml.setAccrualOptionId(accrualTypeId);
    if (accrual.getPikSpread() > 0)
      fpml.setPikSpread(accrual.getPikSpread());
    // TODO: add rate fixing date
    fpml.setRateFixingDate(LocalDate.of(1900, 1, 1));
    fpml.setSpread(accrual.getSpread());
    return fpml;
  }

  static private TermLoan convert(com.syndloanhub.loansum.product.facility.Facility facility)
      throws DatatypeConfigurationException {
    TermLoan fpml = FpMLHelper.factory.createTermLoan();

    for (StandardId id : facility.getIdentifiers()) {
      InstrumentId instrumentId = FpMLHelper.factory.createInstrumentId();
      instrumentId.setInstrumentIdScheme(id.getScheme());
      instrumentId.setValue(id.getValue());
      fpml.getInstrumentId().add(instrumentId);
    }

    fpml.setAgentPartyReference(FpMLHelper.makePartyReference(facility.getAgent()));
    fpml.setBorrowerPartyReference(FpMLHelper.makePartyReference(facility.getBorrower()));
    fpml.setOriginalCommitment(FpMLHelper.convert(facility.getOriginalCommitmentAmount()));
    fpml.setPartyReference(fpml.getAgentPartyReference());
    fpml.setStartDate(facility.getStartDate());
    fpml.setMaturityDate(facility.getMaturityDate());

    FacilityCommitment commitment = FpMLHelper.factory.createFacilityCommitment();
    commitment.setTotalCommitmentAmount(FpMLHelper.convert(facility.getCommitmentAmount(LocalDate.now())));

    fpml.setCurrentCommitment(commitment);

    for (FeeAndRateOption option : facility.getOptions()) {
      switch (option.getOptionType()) {
        case FloatingRate:
          fpml.getFixedRateOptionOrFloatingRateOptionOrLcOption().add(convert(option));
          break;
        default:
          break;
      }
    }

    return fpml;
  }

  static private LoanTrade convert(com.syndloanhub.loansum.product.facility.LoanTrade trade) {
    LoanTrade fpml = factory.createLoanTrade();
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

  static public LoanTradeNotification convert(com.syndloanhub.loansum.product.facility.LoanTrade trade,
      LoanTradeNotification selector)
      throws DatatypeConfigurationException {
    FpMLHelper.clearPartyMap();

    LoanTradeNotification fpml = FpMLHelper.factory.createLoanTradeNotification();
    fpml.setTrade(FpMLHelper.convert(trade));
    fpml.setHeader(FpMLHelper.makeHeader());
    fpml.setFpmlVersion("5-11");
    fpml.setNoticeDate(LocalDate.now());
    fpml.setIsCorrection(false);
    fpml.setPartyReference(FpMLHelper.makePartyReference(trade.getProduct().getAgent()));
    fpml.setRole(FpMLHelper.makeTradingRole("Agent"));

    LoanTradeEvent event = FpMLHelper.factory.createLoanTradeEvent();
    event.getEventIdentifier().add(FpMLHelper.makeBusinessEventIdentifier(
        FpMLHelper.makePartyReference(trade.getProduct().getAgent())));
    LoanTradeReference ref = FpMLHelper.factory.createLoanTradeReference();
    ref.setHref(fpml.getTrade());
    event.setLoanTradeReference(ref);
    fpml.setLoanTradeEventGroup(FpMLHelper.factory.createLoanTrade(event));

    fpml.getParty().add((Party) fpml.getPartyReference().getHref());
    fpml.getParty().add((Party) fpml.getTrade().getBuyerPartyReference().getHref());
    fpml.getParty().add((Party) fpml.getTrade().getSellerPartyReference().getHref());

    FacilityIdentifier facid = FpMLHelper.factory.createFacilityIdentifier();
    facid.setId(FpMLHelper.nextID());
    facid.setPartyReference(fpml.getPartyReference());

    for (StandardId id : trade.getProduct().getIdentifiers()) {
      InstrumentId instrumentId = FpMLHelper.factory.createInstrumentId();
      instrumentId.setInstrumentIdScheme(id.getScheme());
      instrumentId.setValue(id.getValue());
      facid.getInstrumentId().add(instrumentId);
    }

    fpml.setFacilityIdentifier(facid);

    FacilityReference facref = FpMLHelper.factory.createFacilityReference();
    facref.setHref(facid);
    fpml.getTrade().setFacilityReference(facref);

    return fpml;
  }

  static public LoanServicingNotification convert(Facility facility, LoanServicingNotification selector)
      throws DatatypeConfigurationException {
    FpMLHelper.clearPartyMap();

    LoanServicingNotification fpml = FpMLHelper.factory.createLoanServicingNotification();
    fpml.setFpmlVersion("5-11");
    fpml.setNoticeDate(LocalDate.now());
    fpml.setHeader(FpMLHelper.makeHeader());
    fpml.setIsGlobalOnly(true);

    PartyReference agentReference = FpMLHelper.makePartyReference(facility.getAgent());
    PartyReference borrowerReference = FpMLHelper.makePartyReference(facility.getBorrower());

    FacilityIdentifier facilityId = FpMLHelper.factory.createFacilityIdentifier();
    fpml.getDealIdentifierOrDealSummaryAndFacilityIdentifier()
        .add(FpMLHelper.factory.createLoanServicingNotificationFacilityIdentifier(facilityId));
    facilityId.setId(FpMLHelper.nextID());
    facilityId.setPartyReference(agentReference);

    for (StandardId id : facility.getIdentifiers()) {
      InstrumentId instrumentId = FpMLHelper.factory.createInstrumentId();
      instrumentId.setInstrumentIdScheme(id.getScheme());
      instrumentId.setValue(id.getValue());
      facilityId.getInstrumentId().add(instrumentId);
    }

    FacilityReference facilityRef = FpMLHelper.factory.createFacilityReference();
    facilityRef.setHref(facilityId);

    // Add contracts.

    for (com.syndloanhub.loansum.product.facility.LoanContract contract : facility.getContracts()) {
      LoanContract loanContract = convert(contract, facility);
      loanContract.setFacilityReference(facilityRef);
      fpml.getDealIdentifierOrDealSummaryAndFacilityIdentifier().add(
          FpMLHelper.factory.createLoanServicingNotificationContract(loanContract));

      for (com.syndloanhub.loansum.product.facility.LoanContractEvent event : contract.getEvents()) {
        switch (event.getType()) {
          case RepaymentEvent:
            fpml.getFacilityEventGroupOrLcEventGroupOrLoanContractEventGroup().add(
                FpMLHelper.factory
                    .createRepayment(convert((com.syndloanhub.loansum.product.facility.Repayment) event, loanContract)));
          default:
            break;
        }
      }
    }

    fpml.getDealIdentifierOrDealSummaryAndFacilityIdentifier().add(
        FpMLHelper.factory.createLoanServicingNotificationFacilitySummary(convert(facility)));

    fpml.getParty().add((Party) agentReference.getHref());
    fpml.getParty().add((Party) borrowerReference.getHref());

    for (com.syndloanhub.loansum.product.facility.FacilityEvent event : facility.getEvents()) {
      switch (event.getType()) {
        case CommitmentAdjustmentEvent:
          CommitmentAdjustment adj = convert((com.syndloanhub.loansum.product.facility.CommitmentAdjustment) event);
          adj.getEventIdentifier().add(FpMLHelper.makeBusinessEventIdentifier(agentReference));
          adj.setFacilityReference(facilityRef);
          FacilityCommitment commitment = FpMLHelper.factory.createFacilityCommitment();
          commitment.setTotalCommitmentAmount(FpMLHelper.convert(facility.getCommitmentAmount(event.getEffectiveDate())));
          adj.setFacilityCommitment(commitment);
          fpml.getFacilityEventGroupOrLcEventGroupOrLoanContractEventGroup().add(
              FpMLHelper.factory.createCommitmentAdjustment(adj));
          break;
        default:
          throw new DatatypeConfigurationException();
      }
    }

    return fpml;
  }

  static private Repayment convert(com.syndloanhub.loansum.product.facility.Repayment event, LoanContract contract) {
    Repayment fpml = FpMLHelper.factory.createRepayment();
    fpml.setAmount(FpMLHelper.convert(event.getAmount()));
    fpml.setPrice(event.getPrice());
    fpml.setEffectiveDate(event.getEffectiveDate());
    fpml.getEventIdentifier().add(FpMLHelper.makeBusinessEventIdentifier(contract.getPartyReference()));
    LoanContractReference ref = FpMLHelper.factory.createLoanContractReference();
    ref.setHref(contract);
    fpml.setLoanContractReference(ref);
    return fpml;
  }

  static private LoanContract convert(com.syndloanhub.loansum.product.facility.LoanContract contract,
      Facility facility) {
    LoanContract fpml = FpMLHelper.factory.createLoanContract();
    fpml.setAmount(FpMLHelper.convert(contract.getAccrual().getAccrualAmount()));
    fpml.setMaturityDate(contract.getPaymentDate()); // TODO: payment date?
    fpml.setEffectiveDate(contract.getAccrual().getStartDate());
    fpml.setId(FpMLHelper.nextID());
    fpml.setBorrowerPartyReference(FpMLHelper.makePartyReference(facility.getBorrower()));
    fpml.setPartyReference(FpMLHelper.makePartyReference(facility.getAgent()));

    ContractId contractId = FpMLHelper.factory.createContractId();
    contractId.setContractIdScheme(contract.getId().getScheme());
    contractId.setValue(contract.getId().getValue());
    fpml.getContractId().add(contractId);

    switch (contract.getAccrual().getAccrualType()) {
      case Fixed:
        fpml.setFixedRateAccrual(FpMLHelper
            .convert((com.syndloanhub.loansum.product.facility.FixedRateAccrual) contract.getAccrual()));
        break;
      case Floating:
        fpml.setFloatingRateAccrual(FpMLHelper
            .convert((com.syndloanhub.loansum.product.facility.FloatingRateAccrual) contract.getAccrual()));
        break;
      default:
        break;
    }

    return fpml;
  }

  static private FloatingRateOption convert(com.syndloanhub.loansum.product.facility.FeeAndRateOption option) {
    FloatingRateOption fpml = FpMLHelper.factory.createFloatingRateOption();
    AccrualTypeId id = FpMLHelper.factory.createAccrualTypeId();
    id.setAccrualTypeIdScheme(option.getId().getScheme());
    id.setValue(option.getId().getValue());
    fpml.setAccrualOptionId(id);
    fpml.setSpread(option.getRate());
    fpml.setPikSpread(option.getPikSpread());
    fpml.setCurrency(FpMLHelper.convert(option.getCurrency()));
    fpml.setStartDate(option.getStartDate());
    fpml.setEndDate(option.getEndDate());
    fpml.setFloatingRateIndex(FpMLHelper.convert(option.getIndex().get()));
    fpml.setDayCountFraction(FpMLHelper.convert(option.getDayCount()));
    fpml.setPaymentFrequency(convert(option.getPaymentFrequency()));
    return fpml;
  }

  static private com.syndloanhub.loansum.product.facility.FloatingRateOption convert(FloatingRateOption option) {
    StandardId id = StandardId.of(option.getAccrualOptionId().getAccrualTypeIdScheme(), option.getAccrualOptionId().getValue());
    return com.syndloanhub.loansum.product.facility.FloatingRateOption.builder()
        .currency(convert(option.getCurrency()))
        .dayCount(convert(option.getDayCountFraction()))
        .endDate(option.getEndDate())
        .id(id)
        .index(convert(option.getFloatingRateIndex()))
        .paymentFrequency(convert(option.getPaymentFrequency()))
        .pikSpread(option.getPikSpread())
        .rate(option.getSpread())
        .startDate(option.getStartDate())
        .build();
  }

  static public FacilityStatement convert(com.syndloanhub.loansum.product.facility.Facility facility, FacilityStatement selector)
      throws DatatypeConfigurationException {
    FpMLHelper.clearPartyMap();

    FacilityStatement fpml = FpMLHelper.factory.createFacilityStatement();
    fpml.setFpmlVersion("5-11");
    fpml.setHeader(FpMLHelper.makeHeader());
    fpml.setStatementDate(LocalDate.now());
    fpml.setIsCorrection(false);

    switch (facility.getFacilityType()) {
      case Term:
        fpml.setFacilityGroup(FpMLHelper.factory.createTermLoan(FpMLHelper.convert(facility)));
        break;
      default:
        break;
    }

    fpml.getParty().add((Party) fpml.getFacilityGroup().getValue().getAgentPartyReference().getHref());
    fpml.getParty().add((Party) fpml.getFacilityGroup().getValue().getBorrowerPartyReference().getHref());

    return fpml;
  }

}
