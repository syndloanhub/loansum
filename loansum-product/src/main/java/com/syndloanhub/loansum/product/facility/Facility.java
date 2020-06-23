/**
 * Copyright (c) 2018 SyndLoanHub, LLC and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License from within this distribution and at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.syndloanhub.loansum.product.facility;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.Bean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;

import static com.syndloanhub.loansum.product.facility.FacilityType.Term;
import static com.syndloanhub.loansum.product.facility.LoanContractEventType.RepaymentEvent;
import static com.syndloanhub.loansum.product.facility.Helper.generateCommitmentSchedule;
import static com.syndloanhub.loansum.product.facility.Helper.intersects;
import static com.syndloanhub.loansum.product.facility.Helper.tsget;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.Product;
import com.opengamma.strata.product.ProductTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedFacility;

/**
 * A loan facility, e.g. revolver, term, delayed draw, or letter of credit.
 * <p>
 * A loan facility, or tranche, has terms defined under a credit agreement. Cash
 * flows related to a loan facility may be one-off fees or payments that accrue
 * over time.
 * <p>
 * The amounts defined by a loan facility are so-called global amounts which may
 * then be prorated based on a participant's share, defined by a loan trade.
 */
@BeanDefinition
public final class Facility implements Product, Proratable<ProratedFacility>, ImmutableBean {

  /**
   * Return the global funded amount as of a specific date by simply summing the
   * global amount of each active contract on that date, accounting for any
   * repayments.
   * 
   * @param date to return funded amount.
   * @return total amount of all active contracts as of date
   */
  public CurrencyAmount getFundedAmount(LocalDate date) {
    if (facilityType == Term)
      return getCommitmentAmount(date);

    CurrencyAmount fundedAmount = CurrencyAmount.zero(originalCommitmentAmount.getCurrency());

    for (LoanContract contract : contracts) {
      if (intersects(date, Pair.of(contract.getAccrual().getStartDate(), contract.getAccrual().getEndDate()))) {
        fundedAmount = fundedAmount.plus(contract.getAccrual().getAccrualAmount());

        if (contract.getEvents() != null) {
          for (LoanContractEvent event : contract.getEvents()) {
            if (event.getType() == RepaymentEvent && !event.getEffectiveDate().isAfter(date)) {
              fundedAmount = fundedAmount.minus(event.getAmount());
            }
          }
        }
      }
    }

    return fundedAmount;
  }

  /**
   * Return the global totalCommitmentSchedule amount as of a specific date
   * 
   * @param date to return totalCommitmentSchedule amount.
   * @return totalCommitmentSchedule amount
   */
  public CurrencyAmount getCommitmentAmount(LocalDate date) {
    return CurrencyAmount.of(originalCommitmentAmount.getCurrency(), tsget(totalCommitmentSchedule, date));
  }

  /**
   * Return the global unfunded amount as of a specific date
   * 
   * @param date to return unfunded amount.
   * @return totalCommitmentSchedule amount
   */
  public CurrencyAmount getUnfundedAmount(LocalDate date) {
    return getCommitmentAmount(date).minus(getFundedAmount(date)).minus(getUndrawnLCAmount(date));
  }

  /**
   * Return the global undrawn LC amount as of a specific date
   * 
   * @param date to return undrawn LC amount.
   * @return undrawn LC amount
   */
  public CurrencyAmount getUndrawnLCAmount(LocalDate date) {
    // TODO:
    return CurrencyAmount.zero(originalCommitmentAmount.getCurrency());
  }

  @Override
  public ProratedFacility prorate(ProductTrade trade) {
    LoanTrade loanTrade = (LoanTrade) trade;

    double[] values = totalCommitmentSchedule.values().toArray();
    Object[] dates = totalCommitmentSchedule.dates().toArray();

    LocalDateDoubleTimeSeriesBuilder builder = LocalDateDoubleTimeSeries.builder();

    for (int i = 0; i < dates.length; ++i) {
      LocalDate date = (LocalDate) dates[i];

      if (!date.isBefore(trade.getInfo().getTradeDate().get()))
        builder.put((LocalDate) dates[i], values[i] * tsget(loanTrade.getPctShare(), (LocalDate) dates[i]));
    }

    builder.put(trade.getInfo().getTradeDate().get(), loanTrade.getAmount());

    return ProratedFacility.builder().id(id).identifiers(identifiers).agent(agent).borrower(borrower)
        .contracts(contracts.stream().map(contract -> contract.prorate(trade)).collect(Collectors.toList()))
        .fees(fees.stream().map(fee -> fee.prorate(trade)).collect(Collectors.toList())).startDate(startDate)
        .maturityDate(maturityDate).facilityType(facilityType).commitment(builder.build())
        .events(events.stream().map(event -> event.prorate(trade)).collect(Collectors.toList()))
        .currency(originalCommitmentAmount.getCurrency()).build();
  }

  /**
   * Unique loan identifier.
   * <p>
   * A public (e.g. LXID) or internal id which uniquely identifies a loan
   * facility.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId id;

  /**
   * The borrower identifier
   * <p>
   * An identifier used to specify the borrower of the facility.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId borrower;

  /**
   * The agent identifier, optional.
   * <p>
   * An identifier used to specify the agent of the facility.
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId agent;

  /**
   * The type of this facility.
   */
  @PropertyDefinition(validate = "notNull")
  private final FacilityType facilityType;

  /**
   * The identifiers of this facility.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends StandardId>")
  private final ImmutableList<StandardId> identifiers;

  /**
   * The total totalCommitmentSchedule amount against a facility.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount originalCommitmentAmount;

  /**
   * The start date of this facility.
   * <p>
   * Funds may be drawn from this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;

  /**
   * The maturity date of this facility.
   * <p>
   * Date on which final principal and interest is repaid.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate maturityDate;

  /**
   * The interest paying contracts of he facility.
   * <p>
   * A loan facility may have of zero or more contracts.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends LoanContract>")
  private final ImmutableList<LoanContract> contracts;

  /**
   * The accruing fees of the facility.
   * <p>
   * A loan facility may have of zero or more accruing fees.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends AccruingFee>")
  private final ImmutableList<AccruingFee> fees;

  /**
   * Given or generated total commitment schedule for this loan in the loan
   * currency.
   */
  @PropertyDefinition(validate = "")
  private final LocalDateDoubleTimeSeries totalCommitmentSchedule;

  /**
   * Events (CommitmentAdjustment) associated with this loan.
   * <p>
   * A loan may have of zero or more events.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends FacilityEvent>")
  private final ImmutableList<FacilityEvent> events;

  /**
   * Facility-level validation rules.
   */
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, maturityDate, "startDate", "maturityDate");
    ArgChecker.isTrue(originalCommitmentAmount.getAmount() >= 0 || !totalCommitmentSchedule.isEmpty(),
        "original totalCommitmentSchedule amount must be positive or totalCommitmentSchedule schedule provided");

    for (FacilityEvent event : events) {
      ArgChecker.inOrderNotEqual(startDate, event.getEffectiveDate(), "startDate", "effectiveDate");
      ArgChecker.inOrderNotEqual(event.getEffectiveDate(), maturityDate, "effectiveDate", "maturityDate");
    }

    ArgChecker.isFalse(facilityType == Term && contracts == null || contracts.size() == 0,
        "A term loan must have at least one contract");

    for (LoanContract contract : contracts) {
      ArgChecker.isFalse(contract.getAccrual().getStartDate().isBefore(startDate),
          "Contract " + contract.getId() + " start date " + contract.getAccrual().getStartDate()
              + " is prior to facility start date " + startDate);
      ArgChecker.isFalse(contract.getAccrual().getEndDate().isAfter(maturityDate),
          "Contract " + contract.getId() + " end date " + contract.getAccrual().getEndDate()
              + " is after facility maturity date " + maturityDate);
      ArgChecker.isFalse(contract.getPaymentDate().isAfter(maturityDate),
          "Contract " + contract.getId() + " payment date " + contract.getPaymentDate()
              + " is after facility maturity date " + maturityDate);

      // TODO: figure out how to apply below, maybe as an option.
      /*
       * ArgChecker.isFalse(facilityType == Term &&
       * Math.abs(getFundedAmount(contract.getAccrual().getStartDate()).getAmount() -
       * getCommitmentAmount(contract.getAccrual().getStartDate()).getAmount()) >
       * EPSILON_1, "Funded amount " +
       * getFundedAmount(contract.getAccrual().getStartDate()) + " as of " +
       * contract.getAccrual().getStartDate() +
       * " not equal to totalCommitmentSchedule amount " +
       * getCommitmentAmount(contract.getAccrual().getStartDate()));
       */
    }

  }

  /**
   * Default values, empty events, contracts, and fees.
   * 
   * @param builder
   */
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder.totalCommitmentSchedule(LocalDateDoubleTimeSeries.builder().build())
        .events(new ArrayList<FacilityEvent>()).contracts(new ArrayList<LoanContract>())
        .fees(new ArrayList<AccruingFee>());
  }

  /**
   * Complete construction of Facility, in particular building commitment schedule
   * if incomplete.
   * 
   * @param builder
   */
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    if (builder.totalCommitmentSchedule == null || builder.totalCommitmentSchedule.isEmpty()) {
      builder.totalCommitmentSchedule(generateCommitmentSchedule(builder.facilityType, builder.startDate,
          builder.originalCommitmentAmount.getAmount(),
          builder.contracts.stream().collect(Collectors.toList()),
          builder.events.stream().collect(Collectors.toList())));
    }
  }

  /**
   * TODO: collect set of currencies from accruals. For now loans must be
   * single-currrency.
   * 
   * @see com.opengamma.strata.product.Product#allCurrencies()
   */
  @Override
  public ImmutableSet<Currency> allCurrencies() {
    return ImmutableSet.of(originalCommitmentAmount.getCurrency());
  }
  /*
   * @Override public OutstandingContractsStatement
   * export(OutstandingContractsStatement fpml) throws
   * DatatypeConfigurationException { fpml.setFpmlVersion("5-11");
   * fpml.setHeader(FpMLHelper.getHeader(factory));
   * fpml.setStatementDate(LocalDate.now()); return fpml; }
   */

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code Facility}.
   * @return the meta-bean, not null
   */
  public static Facility.Meta meta() {
    return Facility.Meta.INSTANCE;
  }

  static {
    MetaBean.register(Facility.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static Facility.Builder builder() {
    return new Facility.Builder();
  }

  private Facility(
      StandardId id,
      StandardId borrower,
      StandardId agent,
      FacilityType facilityType,
      List<? extends StandardId> identifiers,
      CurrencyAmount originalCommitmentAmount,
      LocalDate startDate,
      LocalDate maturityDate,
      List<? extends LoanContract> contracts,
      List<? extends AccruingFee> fees,
      LocalDateDoubleTimeSeries totalCommitmentSchedule,
      List<? extends FacilityEvent> events) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(borrower, "borrower");
    JodaBeanUtils.notNull(agent, "agent");
    JodaBeanUtils.notNull(facilityType, "facilityType");
    JodaBeanUtils.notNull(originalCommitmentAmount, "originalCommitmentAmount");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(maturityDate, "maturityDate");
    this.id = id;
    this.borrower = borrower;
    this.agent = agent;
    this.facilityType = facilityType;
    this.identifiers = (identifiers != null ? ImmutableList.copyOf(identifiers) : null);
    this.originalCommitmentAmount = originalCommitmentAmount;
    this.startDate = startDate;
    this.maturityDate = maturityDate;
    this.contracts = (contracts != null ? ImmutableList.copyOf(contracts) : null);
    this.fees = (fees != null ? ImmutableList.copyOf(fees) : null);
    this.totalCommitmentSchedule = totalCommitmentSchedule;
    this.events = (events != null ? ImmutableList.copyOf(events) : null);
    validate();
  }

  @Override
  public Facility.Meta metaBean() {
    return Facility.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets unique loan identifier.
   * <p>
   * A public (e.g. LXID) or internal id which uniquely identifies a loan
   * facility.
   * @return the value of the property, not null
   */
  public StandardId getId() {
    return id;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the borrower identifier
   * <p>
   * An identifier used to specify the borrower of the facility.
   * @return the value of the property, not null
   */
  public StandardId getBorrower() {
    return borrower;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the agent identifier, optional.
   * <p>
   * An identifier used to specify the agent of the facility.
   * @return the value of the property, not null
   */
  public StandardId getAgent() {
    return agent;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of this facility.
   * @return the value of the property, not null
   */
  public FacilityType getFacilityType() {
    return facilityType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the identifiers of this facility.
   * @return the value of the property
   */
  public ImmutableList<StandardId> getIdentifiers() {
    return identifiers;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the total totalCommitmentSchedule amount against a facility.
   * @return the value of the property, not null
   */
  public CurrencyAmount getOriginalCommitmentAmount() {
    return originalCommitmentAmount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of this facility.
   * <p>
   * Funds may be drawn from this date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the maturity date of this facility.
   * <p>
   * Date on which final principal and interest is repaid.
   * @return the value of the property, not null
   */
  public LocalDate getMaturityDate() {
    return maturityDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interest paying contracts of he facility.
   * <p>
   * A loan facility may have of zero or more contracts.
   * @return the value of the property
   */
  public ImmutableList<LoanContract> getContracts() {
    return contracts;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accruing fees of the facility.
   * <p>
   * A loan facility may have of zero or more accruing fees.
   * @return the value of the property
   */
  public ImmutableList<AccruingFee> getFees() {
    return fees;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets given or generated total commitment schedule for this loan in the loan
   * currency.
   * @return the value of the property
   */
  public LocalDateDoubleTimeSeries getTotalCommitmentSchedule() {
    return totalCommitmentSchedule;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets events (CommitmentAdjustment) associated with this loan.
   * <p>
   * A loan may have of zero or more events.
   * @return the value of the property
   */
  public ImmutableList<FacilityEvent> getEvents() {
    return events;
  }

  //-----------------------------------------------------------------------
  /**
   * Returns a builder that allows this bean to be mutated.
   * @return the mutable builder, not null
   */
  public Builder toBuilder() {
    return new Builder(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj != null && obj.getClass() == this.getClass()) {
      Facility other = (Facility) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(borrower, other.borrower) &&
          JodaBeanUtils.equal(agent, other.agent) &&
          JodaBeanUtils.equal(facilityType, other.facilityType) &&
          JodaBeanUtils.equal(identifiers, other.identifiers) &&
          JodaBeanUtils.equal(originalCommitmentAmount, other.originalCommitmentAmount) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(maturityDate, other.maturityDate) &&
          JodaBeanUtils.equal(contracts, other.contracts) &&
          JodaBeanUtils.equal(fees, other.fees) &&
          JodaBeanUtils.equal(totalCommitmentSchedule, other.totalCommitmentSchedule) &&
          JodaBeanUtils.equal(events, other.events);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(borrower);
    hash = hash * 31 + JodaBeanUtils.hashCode(agent);
    hash = hash * 31 + JodaBeanUtils.hashCode(facilityType);
    hash = hash * 31 + JodaBeanUtils.hashCode(identifiers);
    hash = hash * 31 + JodaBeanUtils.hashCode(originalCommitmentAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(maturityDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(contracts);
    hash = hash * 31 + JodaBeanUtils.hashCode(fees);
    hash = hash * 31 + JodaBeanUtils.hashCode(totalCommitmentSchedule);
    hash = hash * 31 + JodaBeanUtils.hashCode(events);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(416);
    buf.append("Facility{");
    buf.append("id").append('=').append(id).append(',').append(' ');
    buf.append("borrower").append('=').append(borrower).append(',').append(' ');
    buf.append("agent").append('=').append(agent).append(',').append(' ');
    buf.append("facilityType").append('=').append(facilityType).append(',').append(' ');
    buf.append("identifiers").append('=').append(identifiers).append(',').append(' ');
    buf.append("originalCommitmentAmount").append('=').append(originalCommitmentAmount).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("maturityDate").append('=').append(maturityDate).append(',').append(' ');
    buf.append("contracts").append('=').append(contracts).append(',').append(' ');
    buf.append("fees").append('=').append(fees).append(',').append(' ');
    buf.append("totalCommitmentSchedule").append('=').append(totalCommitmentSchedule).append(',').append(' ');
    buf.append("events").append('=').append(JodaBeanUtils.toString(events));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code Facility}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code id} property.
     */
    private final MetaProperty<StandardId> _id = DirectMetaProperty.ofImmutable(
        this, "id", Facility.class, StandardId.class);
    /**
     * The meta-property for the {@code borrower} property.
     */
    private final MetaProperty<StandardId> _borrower = DirectMetaProperty.ofImmutable(
        this, "borrower", Facility.class, StandardId.class);
    /**
     * The meta-property for the {@code agent} property.
     */
    private final MetaProperty<StandardId> _agent = DirectMetaProperty.ofImmutable(
        this, "agent", Facility.class, StandardId.class);
    /**
     * The meta-property for the {@code facilityType} property.
     */
    private final MetaProperty<FacilityType> _facilityType = DirectMetaProperty.ofImmutable(
        this, "facilityType", Facility.class, FacilityType.class);
    /**
     * The meta-property for the {@code identifiers} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<StandardId>> _identifiers = DirectMetaProperty.ofImmutable(
        this, "identifiers", Facility.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code originalCommitmentAmount} property.
     */
    private final MetaProperty<CurrencyAmount> _originalCommitmentAmount = DirectMetaProperty.ofImmutable(
        this, "originalCommitmentAmount", Facility.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> _startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", Facility.class, LocalDate.class);
    /**
     * The meta-property for the {@code maturityDate} property.
     */
    private final MetaProperty<LocalDate> _maturityDate = DirectMetaProperty.ofImmutable(
        this, "maturityDate", Facility.class, LocalDate.class);
    /**
     * The meta-property for the {@code contracts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<LoanContract>> _contracts = DirectMetaProperty.ofImmutable(
        this, "contracts", Facility.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code fees} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<AccruingFee>> _fees = DirectMetaProperty.ofImmutable(
        this, "fees", Facility.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code totalCommitmentSchedule} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> _totalCommitmentSchedule = DirectMetaProperty.ofImmutable(
        this, "totalCommitmentSchedule", Facility.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code events} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<FacilityEvent>> _events = DirectMetaProperty.ofImmutable(
        this, "events", Facility.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "borrower",
        "agent",
        "facilityType",
        "identifiers",
        "originalCommitmentAmount",
        "startDate",
        "maturityDate",
        "contracts",
        "fees",
        "totalCommitmentSchedule",
        "events");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return _id;
        case 2097810786:  // borrower
          return _borrower;
        case 92750597:  // agent
          return _agent;
        case 370698365:  // facilityType
          return _facilityType;
        case 1368189162:  // identifiers
          return _identifiers;
        case 1529720766:  // originalCommitmentAmount
          return _originalCommitmentAmount;
        case -2129778896:  // startDate
          return _startDate;
        case -414641441:  // maturityDate
          return _maturityDate;
        case -395505247:  // contracts
          return _contracts;
        case 3138989:  // fees
          return _fees;
        case -1503941840:  // totalCommitmentSchedule
          return _totalCommitmentSchedule;
        case -1291329255:  // events
          return _events;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public Facility.Builder builder() {
      return new Facility.Builder();
    }

    @Override
    public Class<? extends Facility> beanType() {
      return Facility.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code id} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> id() {
      return _id;
    }

    /**
     * The meta-property for the {@code borrower} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> borrower() {
      return _borrower;
    }

    /**
     * The meta-property for the {@code agent} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> agent() {
      return _agent;
    }

    /**
     * The meta-property for the {@code facilityType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<FacilityType> facilityType() {
      return _facilityType;
    }

    /**
     * The meta-property for the {@code identifiers} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<StandardId>> identifiers() {
      return _identifiers;
    }

    /**
     * The meta-property for the {@code originalCommitmentAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> originalCommitmentAmount() {
      return _originalCommitmentAmount;
    }

    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return _startDate;
    }

    /**
     * The meta-property for the {@code maturityDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> maturityDate() {
      return _maturityDate;
    }

    /**
     * The meta-property for the {@code contracts} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<LoanContract>> contracts() {
      return _contracts;
    }

    /**
     * The meta-property for the {@code fees} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<AccruingFee>> fees() {
      return _fees;
    }

    /**
     * The meta-property for the {@code totalCommitmentSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> totalCommitmentSchedule() {
      return _totalCommitmentSchedule;
    }

    /**
     * The meta-property for the {@code events} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<FacilityEvent>> events() {
      return _events;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((Facility) bean).getId();
        case 2097810786:  // borrower
          return ((Facility) bean).getBorrower();
        case 92750597:  // agent
          return ((Facility) bean).getAgent();
        case 370698365:  // facilityType
          return ((Facility) bean).getFacilityType();
        case 1368189162:  // identifiers
          return ((Facility) bean).getIdentifiers();
        case 1529720766:  // originalCommitmentAmount
          return ((Facility) bean).getOriginalCommitmentAmount();
        case -2129778896:  // startDate
          return ((Facility) bean).getStartDate();
        case -414641441:  // maturityDate
          return ((Facility) bean).getMaturityDate();
        case -395505247:  // contracts
          return ((Facility) bean).getContracts();
        case 3138989:  // fees
          return ((Facility) bean).getFees();
        case -1503941840:  // totalCommitmentSchedule
          return ((Facility) bean).getTotalCommitmentSchedule();
        case -1291329255:  // events
          return ((Facility) bean).getEvents();
      }
      return super.propertyGet(bean, propertyName, quiet);
    }

    @Override
    protected void propertySet(Bean bean, String propertyName, Object newValue, boolean quiet) {
      metaProperty(propertyName);
      if (quiet) {
        return;
      }
      throw new UnsupportedOperationException("Property cannot be written: " + propertyName);
    }

  }

  //-----------------------------------------------------------------------
  /**
   * The bean-builder for {@code Facility}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<Facility> {

    private StandardId id;
    private StandardId borrower;
    private StandardId agent;
    private FacilityType facilityType;
    private List<? extends StandardId> identifiers;
    private CurrencyAmount originalCommitmentAmount;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private List<? extends LoanContract> contracts;
    private List<? extends AccruingFee> fees;
    private LocalDateDoubleTimeSeries totalCommitmentSchedule;
    private List<? extends FacilityEvent> events;

    /**
     * Restricted constructor.
     */
    private Builder() {
      applyDefaults(this);
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(Facility beanToCopy) {
      this.id = beanToCopy.getId();
      this.borrower = beanToCopy.getBorrower();
      this.agent = beanToCopy.getAgent();
      this.facilityType = beanToCopy.getFacilityType();
      this.identifiers = beanToCopy.getIdentifiers();
      this.originalCommitmentAmount = beanToCopy.getOriginalCommitmentAmount();
      this.startDate = beanToCopy.getStartDate();
      this.maturityDate = beanToCopy.getMaturityDate();
      this.contracts = beanToCopy.getContracts();
      this.fees = beanToCopy.getFees();
      this.totalCommitmentSchedule = beanToCopy.getTotalCommitmentSchedule();
      this.events = beanToCopy.getEvents();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case 2097810786:  // borrower
          return borrower;
        case 92750597:  // agent
          return agent;
        case 370698365:  // facilityType
          return facilityType;
        case 1368189162:  // identifiers
          return identifiers;
        case 1529720766:  // originalCommitmentAmount
          return originalCommitmentAmount;
        case -2129778896:  // startDate
          return startDate;
        case -414641441:  // maturityDate
          return maturityDate;
        case -395505247:  // contracts
          return contracts;
        case 3138989:  // fees
          return fees;
        case -1503941840:  // totalCommitmentSchedule
          return totalCommitmentSchedule;
        case -1291329255:  // events
          return events;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          this.id = (StandardId) newValue;
          break;
        case 2097810786:  // borrower
          this.borrower = (StandardId) newValue;
          break;
        case 92750597:  // agent
          this.agent = (StandardId) newValue;
          break;
        case 370698365:  // facilityType
          this.facilityType = (FacilityType) newValue;
          break;
        case 1368189162:  // identifiers
          this.identifiers = (List<? extends StandardId>) newValue;
          break;
        case 1529720766:  // originalCommitmentAmount
          this.originalCommitmentAmount = (CurrencyAmount) newValue;
          break;
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -414641441:  // maturityDate
          this.maturityDate = (LocalDate) newValue;
          break;
        case -395505247:  // contracts
          this.contracts = (List<? extends LoanContract>) newValue;
          break;
        case 3138989:  // fees
          this.fees = (List<? extends AccruingFee>) newValue;
          break;
        case -1503941840:  // totalCommitmentSchedule
          this.totalCommitmentSchedule = (LocalDateDoubleTimeSeries) newValue;
          break;
        case -1291329255:  // events
          this.events = (List<? extends FacilityEvent>) newValue;
          break;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
      return this;
    }

    @Override
    public Builder set(MetaProperty<?> property, Object value) {
      super.set(property, value);
      return this;
    }

    @Override
    public Facility build() {
      preBuild(this);
      return new Facility(
          id,
          borrower,
          agent,
          facilityType,
          identifiers,
          originalCommitmentAmount,
          startDate,
          maturityDate,
          contracts,
          fees,
          totalCommitmentSchedule,
          events);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets unique loan identifier.
     * <p>
     * A public (e.g. LXID) or internal id which uniquely identifies a loan
     * facility.
     * @param id  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder id(StandardId id) {
      JodaBeanUtils.notNull(id, "id");
      this.id = id;
      return this;
    }

    /**
     * Sets the borrower identifier
     * <p>
     * An identifier used to specify the borrower of the facility.
     * @param borrower  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder borrower(StandardId borrower) {
      JodaBeanUtils.notNull(borrower, "borrower");
      this.borrower = borrower;
      return this;
    }

    /**
     * Sets the agent identifier, optional.
     * <p>
     * An identifier used to specify the agent of the facility.
     * @param agent  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder agent(StandardId agent) {
      JodaBeanUtils.notNull(agent, "agent");
      this.agent = agent;
      return this;
    }

    /**
     * Sets the type of this facility.
     * @param facilityType  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder facilityType(FacilityType facilityType) {
      JodaBeanUtils.notNull(facilityType, "facilityType");
      this.facilityType = facilityType;
      return this;
    }

    /**
     * Sets the identifiers of this facility.
     * @param identifiers  the new value
     * @return this, for chaining, not null
     */
    public Builder identifiers(List<? extends StandardId> identifiers) {
      this.identifiers = identifiers;
      return this;
    }

    /**
     * Sets the {@code identifiers} property in the builder
     * from an array of objects.
     * @param identifiers  the new value
     * @return this, for chaining, not null
     */
    public Builder identifiers(StandardId... identifiers) {
      return identifiers(ImmutableList.copyOf(identifiers));
    }

    /**
     * Sets the total totalCommitmentSchedule amount against a facility.
     * @param originalCommitmentAmount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder originalCommitmentAmount(CurrencyAmount originalCommitmentAmount) {
      JodaBeanUtils.notNull(originalCommitmentAmount, "originalCommitmentAmount");
      this.originalCommitmentAmount = originalCommitmentAmount;
      return this;
    }

    /**
     * Sets the start date of this facility.
     * <p>
     * Funds may be drawn from this date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the maturity date of this facility.
     * <p>
     * Date on which final principal and interest is repaid.
     * @param maturityDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder maturityDate(LocalDate maturityDate) {
      JodaBeanUtils.notNull(maturityDate, "maturityDate");
      this.maturityDate = maturityDate;
      return this;
    }

    /**
     * Sets the interest paying contracts of he facility.
     * <p>
     * A loan facility may have of zero or more contracts.
     * @param contracts  the new value
     * @return this, for chaining, not null
     */
    public Builder contracts(List<? extends LoanContract> contracts) {
      this.contracts = contracts;
      return this;
    }

    /**
     * Sets the {@code contracts} property in the builder
     * from an array of objects.
     * @param contracts  the new value
     * @return this, for chaining, not null
     */
    public Builder contracts(LoanContract... contracts) {
      return contracts(ImmutableList.copyOf(contracts));
    }

    /**
     * Sets the accruing fees of the facility.
     * <p>
     * A loan facility may have of zero or more accruing fees.
     * @param fees  the new value
     * @return this, for chaining, not null
     */
    public Builder fees(List<? extends AccruingFee> fees) {
      this.fees = fees;
      return this;
    }

    /**
     * Sets the {@code fees} property in the builder
     * from an array of objects.
     * @param fees  the new value
     * @return this, for chaining, not null
     */
    public Builder fees(AccruingFee... fees) {
      return fees(ImmutableList.copyOf(fees));
    }

    /**
     * Sets given or generated total commitment schedule for this loan in the loan
     * currency.
     * @param totalCommitmentSchedule  the new value
     * @return this, for chaining, not null
     */
    public Builder totalCommitmentSchedule(LocalDateDoubleTimeSeries totalCommitmentSchedule) {
      this.totalCommitmentSchedule = totalCommitmentSchedule;
      return this;
    }

    /**
     * Sets events (CommitmentAdjustment) associated with this loan.
     * <p>
     * A loan may have of zero or more events.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(List<? extends FacilityEvent> events) {
      this.events = events;
      return this;
    }

    /**
     * Sets the {@code events} property in the builder
     * from an array of objects.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(FacilityEvent... events) {
      return events(ImmutableList.copyOf(events));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(416);
      buf.append("Facility.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("borrower").append('=').append(JodaBeanUtils.toString(borrower)).append(',').append(' ');
      buf.append("agent").append('=').append(JodaBeanUtils.toString(agent)).append(',').append(' ');
      buf.append("facilityType").append('=').append(JodaBeanUtils.toString(facilityType)).append(',').append(' ');
      buf.append("identifiers").append('=').append(JodaBeanUtils.toString(identifiers)).append(',').append(' ');
      buf.append("originalCommitmentAmount").append('=').append(JodaBeanUtils.toString(originalCommitmentAmount)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("maturityDate").append('=').append(JodaBeanUtils.toString(maturityDate)).append(',').append(' ');
      buf.append("contracts").append('=').append(JodaBeanUtils.toString(contracts)).append(',').append(' ');
      buf.append("fees").append('=').append(JodaBeanUtils.toString(fees)).append(',').append(' ');
      buf.append("totalCommitmentSchedule").append('=').append(JodaBeanUtils.toString(totalCommitmentSchedule)).append(',').append(' ');
      buf.append("events").append('=').append(JodaBeanUtils.toString(events));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
