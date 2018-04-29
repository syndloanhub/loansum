/**
 * Copyright (c) 2018 SyndLoanHub, LLC and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License from within this distribution and at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.syndloanhub.loansum.product.facility.prorated;

import static com.syndloanhub.loansum.product.facility.Helper.intersects;
import static com.syndloanhub.loansum.product.facility.Helper.tsget;
import static com.syndloanhub.loansum.product.facility.LoanContractEventType.RepaymentEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;
import com.syndloanhub.loansum.product.facility.FacilityType;

import org.joda.beans.Bean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

/**
 * A loan prorated based on a specific trade.
 */
@BeanDefinition
public final class ProratedFacility implements ProratedProduct, ImmutableBean {

  /**
   * Return the prorated funded amount as of a specific date by
   * simply summing the prorated amount of each active contract on that date,
   * accounting for any repayments.
   * 
   * @param date to return funded amount.
   * @return total amount of all active contracts as of date
   */
  public double getFundedAmount(LocalDate date) {
    double fundedAmount = 0;

    for (ProratedLoanContract contract : contracts) {
      if (intersects(date, Pair.of(contract.getAccrual().getStartDate(), contract.getAccrual().getEndDate()))) {
        fundedAmount += contract.getAccrual().getAccrualAmount().getAmount();

        if (contract.getEvents() != null) {
          for (ProratedLoanContractEvent event : contract.getEvents()) {
            if (event.getType() == RepaymentEvent && !event.getEffectiveDate().isAfter(date)) {
              fundedAmount -= event.getAmount().getAmount();
            }
          }
        }
      }
    }

    return fundedAmount;
  }

  /**
   * Return the prorated unfunded amount as of a specific date.
   * 
   * @param date to return funded amount.
   * @return total commitment minus total amount of all active contracts as of date
   */
  public double getUnfundedAmount(LocalDate date) {
    return getCommitmentAmount(date) - getFundedAmount(date);
  }

  /**
   * Return the global commitment amount as of a specific date
   * 
   * @param date to return commitment amount.
   * @return commitment amount
   */
  public double getCommitmentAmount(LocalDate date) {
    return tsget(commitment, date);
  }

  /**
   * Unique loan identifier.
   * <p>
   * A public (e.g. LXID) or internal id which uniquely identifies a loan facility.
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
   * The funding date of this facility.
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
   * The primary currency.
   * <p>
   * This is the currency of the loan and the currency that payment is made in.
   */
  @PropertyDefinition(validate = "notNull")
  private final Currency currency;

  /**
   * Given total commitment schedule for this loan.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDateDoubleTimeSeries commitment;

  /**
   * The interest paying contracts of the facility.
   * <p>
   * A loan facility may have of zero or more contracts.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends ProratedLoanContract>")
  private final ImmutableList<ProratedLoanContract> contracts;

  /**
   * The accruing fees of the facility.
   * <p>
   * A loan facility may have of zero or more accruing fees.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends ProratedAccruingFee>")
  private final ImmutableList<ProratedAccruingFee> fees;

  /**
   * Events (CommitmentAdjustment) associated with this loan.
   * <p>
   * A loan may have of zero or more events.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends ProratedLoanEvent>")
  private final ImmutableList<ProratedLoanEvent> events;

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ProratedFacility}.
   * @return the meta-bean, not null
   */
  public static ProratedFacility.Meta meta() {
    return ProratedFacility.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ProratedFacility.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ProratedFacility.Builder builder() {
    return new ProratedFacility.Builder();
  }

  private ProratedFacility(
      StandardId id,
      StandardId borrower,
      StandardId agent,
      FacilityType facilityType,
      List<? extends StandardId> identifiers,
      LocalDate startDate,
      LocalDate maturityDate,
      Currency currency,
      LocalDateDoubleTimeSeries commitment,
      List<? extends ProratedLoanContract> contracts,
      List<? extends ProratedAccruingFee> fees,
      List<? extends ProratedLoanEvent> events) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(borrower, "borrower");
    JodaBeanUtils.notNull(agent, "agent");
    JodaBeanUtils.notNull(facilityType, "facilityType");
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(maturityDate, "maturityDate");
    JodaBeanUtils.notNull(currency, "currency");
    JodaBeanUtils.notNull(commitment, "commitment");
    this.id = id;
    this.borrower = borrower;
    this.agent = agent;
    this.facilityType = facilityType;
    this.identifiers = (identifiers != null ? ImmutableList.copyOf(identifiers) : null);
    this.startDate = startDate;
    this.maturityDate = maturityDate;
    this.currency = currency;
    this.commitment = commitment;
    this.contracts = (contracts != null ? ImmutableList.copyOf(contracts) : null);
    this.fees = (fees != null ? ImmutableList.copyOf(fees) : null);
    this.events = (events != null ? ImmutableList.copyOf(events) : null);
  }

  @Override
  public ProratedFacility.Meta metaBean() {
    return ProratedFacility.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets unique loan identifier.
   * <p>
   * A public (e.g. LXID) or internal id which uniquely identifies a loan facility.
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
   * Gets the funding date of this facility.
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
   * Gets the primary currency.
   * <p>
   * This is the currency of the loan and the currency that payment is made in.
   * @return the value of the property, not null
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets given total commitment schedule for this loan.
   * @return the value of the property, not null
   */
  public LocalDateDoubleTimeSeries getCommitment() {
    return commitment;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the interest paying contracts of the facility.
   * <p>
   * A loan facility may have of zero or more contracts.
   * @return the value of the property
   */
  public ImmutableList<ProratedLoanContract> getContracts() {
    return contracts;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accruing fees of the facility.
   * <p>
   * A loan facility may have of zero or more accruing fees.
   * @return the value of the property
   */
  public ImmutableList<ProratedAccruingFee> getFees() {
    return fees;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets events (CommitmentAdjustment) associated with this loan.
   * <p>
   * A loan may have of zero or more events.
   * @return the value of the property
   */
  public ImmutableList<ProratedLoanEvent> getEvents() {
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
      ProratedFacility other = (ProratedFacility) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(borrower, other.borrower) &&
          JodaBeanUtils.equal(agent, other.agent) &&
          JodaBeanUtils.equal(facilityType, other.facilityType) &&
          JodaBeanUtils.equal(identifiers, other.identifiers) &&
          JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(maturityDate, other.maturityDate) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(commitment, other.commitment) &&
          JodaBeanUtils.equal(contracts, other.contracts) &&
          JodaBeanUtils.equal(fees, other.fees) &&
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
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(maturityDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(commitment);
    hash = hash * 31 + JodaBeanUtils.hashCode(contracts);
    hash = hash * 31 + JodaBeanUtils.hashCode(fees);
    hash = hash * 31 + JodaBeanUtils.hashCode(events);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(416);
    buf.append("ProratedFacility{");
    buf.append("id").append('=').append(id).append(',').append(' ');
    buf.append("borrower").append('=').append(borrower).append(',').append(' ');
    buf.append("agent").append('=').append(agent).append(',').append(' ');
    buf.append("facilityType").append('=').append(facilityType).append(',').append(' ');
    buf.append("identifiers").append('=').append(identifiers).append(',').append(' ');
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("maturityDate").append('=').append(maturityDate).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("commitment").append('=').append(commitment).append(',').append(' ');
    buf.append("contracts").append('=').append(contracts).append(',').append(' ');
    buf.append("fees").append('=').append(fees).append(',').append(' ');
    buf.append("events").append('=').append(JodaBeanUtils.toString(events));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ProratedFacility}.
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
        this, "id", ProratedFacility.class, StandardId.class);
    /**
     * The meta-property for the {@code borrower} property.
     */
    private final MetaProperty<StandardId> _borrower = DirectMetaProperty.ofImmutable(
        this, "borrower", ProratedFacility.class, StandardId.class);
    /**
     * The meta-property for the {@code agent} property.
     */
    private final MetaProperty<StandardId> _agent = DirectMetaProperty.ofImmutable(
        this, "agent", ProratedFacility.class, StandardId.class);
    /**
     * The meta-property for the {@code facilityType} property.
     */
    private final MetaProperty<FacilityType> _facilityType = DirectMetaProperty.ofImmutable(
        this, "facilityType", ProratedFacility.class, FacilityType.class);
    /**
     * The meta-property for the {@code identifiers} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<StandardId>> _identifiers = DirectMetaProperty.ofImmutable(
        this, "identifiers", ProratedFacility.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> _startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", ProratedFacility.class, LocalDate.class);
    /**
     * The meta-property for the {@code maturityDate} property.
     */
    private final MetaProperty<LocalDate> _maturityDate = DirectMetaProperty.ofImmutable(
        this, "maturityDate", ProratedFacility.class, LocalDate.class);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> _currency = DirectMetaProperty.ofImmutable(
        this, "currency", ProratedFacility.class, Currency.class);
    /**
     * The meta-property for the {@code commitment} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> _commitment = DirectMetaProperty.ofImmutable(
        this, "commitment", ProratedFacility.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-property for the {@code contracts} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ProratedLoanContract>> _contracts = DirectMetaProperty.ofImmutable(
        this, "contracts", ProratedFacility.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code fees} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ProratedAccruingFee>> _fees = DirectMetaProperty.ofImmutable(
        this, "fees", ProratedFacility.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code events} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ProratedLoanEvent>> _events = DirectMetaProperty.ofImmutable(
        this, "events", ProratedFacility.class, (Class) ImmutableList.class);
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
        "startDate",
        "maturityDate",
        "currency",
        "commitment",
        "contracts",
        "fees",
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
        case -2129778896:  // startDate
          return _startDate;
        case -414641441:  // maturityDate
          return _maturityDate;
        case 575402001:  // currency
          return _currency;
        case 1019005717:  // commitment
          return _commitment;
        case -395505247:  // contracts
          return _contracts;
        case 3138989:  // fees
          return _fees;
        case -1291329255:  // events
          return _events;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ProratedFacility.Builder builder() {
      return new ProratedFacility.Builder();
    }

    @Override
    public Class<? extends ProratedFacility> beanType() {
      return ProratedFacility.class;
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
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return _currency;
    }

    /**
     * The meta-property for the {@code commitment} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> commitment() {
      return _commitment;
    }

    /**
     * The meta-property for the {@code contracts} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ProratedLoanContract>> contracts() {
      return _contracts;
    }

    /**
     * The meta-property for the {@code fees} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ProratedAccruingFee>> fees() {
      return _fees;
    }

    /**
     * The meta-property for the {@code events} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ProratedLoanEvent>> events() {
      return _events;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((ProratedFacility) bean).getId();
        case 2097810786:  // borrower
          return ((ProratedFacility) bean).getBorrower();
        case 92750597:  // agent
          return ((ProratedFacility) bean).getAgent();
        case 370698365:  // facilityType
          return ((ProratedFacility) bean).getFacilityType();
        case 1368189162:  // identifiers
          return ((ProratedFacility) bean).getIdentifiers();
        case -2129778896:  // startDate
          return ((ProratedFacility) bean).getStartDate();
        case -414641441:  // maturityDate
          return ((ProratedFacility) bean).getMaturityDate();
        case 575402001:  // currency
          return ((ProratedFacility) bean).getCurrency();
        case 1019005717:  // commitment
          return ((ProratedFacility) bean).getCommitment();
        case -395505247:  // contracts
          return ((ProratedFacility) bean).getContracts();
        case 3138989:  // fees
          return ((ProratedFacility) bean).getFees();
        case -1291329255:  // events
          return ((ProratedFacility) bean).getEvents();
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
   * The bean-builder for {@code ProratedFacility}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ProratedFacility> {

    private StandardId id;
    private StandardId borrower;
    private StandardId agent;
    private FacilityType facilityType;
    private List<? extends StandardId> identifiers;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private Currency currency;
    private LocalDateDoubleTimeSeries commitment;
    private List<? extends ProratedLoanContract> contracts;
    private List<? extends ProratedAccruingFee> fees;
    private List<? extends ProratedLoanEvent> events;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ProratedFacility beanToCopy) {
      this.id = beanToCopy.getId();
      this.borrower = beanToCopy.getBorrower();
      this.agent = beanToCopy.getAgent();
      this.facilityType = beanToCopy.getFacilityType();
      this.identifiers = beanToCopy.getIdentifiers();
      this.startDate = beanToCopy.getStartDate();
      this.maturityDate = beanToCopy.getMaturityDate();
      this.currency = beanToCopy.getCurrency();
      this.commitment = beanToCopy.getCommitment();
      this.contracts = beanToCopy.getContracts();
      this.fees = beanToCopy.getFees();
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
        case -2129778896:  // startDate
          return startDate;
        case -414641441:  // maturityDate
          return maturityDate;
        case 575402001:  // currency
          return currency;
        case 1019005717:  // commitment
          return commitment;
        case -395505247:  // contracts
          return contracts;
        case 3138989:  // fees
          return fees;
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
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -414641441:  // maturityDate
          this.maturityDate = (LocalDate) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 1019005717:  // commitment
          this.commitment = (LocalDateDoubleTimeSeries) newValue;
          break;
        case -395505247:  // contracts
          this.contracts = (List<? extends ProratedLoanContract>) newValue;
          break;
        case 3138989:  // fees
          this.fees = (List<? extends ProratedAccruingFee>) newValue;
          break;
        case -1291329255:  // events
          this.events = (List<? extends ProratedLoanEvent>) newValue;
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
    public ProratedFacility build() {
      return new ProratedFacility(
          id,
          borrower,
          agent,
          facilityType,
          identifiers,
          startDate,
          maturityDate,
          currency,
          commitment,
          contracts,
          fees,
          events);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets unique loan identifier.
     * <p>
     * A public (e.g. LXID) or internal id which uniquely identifies a loan facility.
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
     * Sets the funding date of this facility.
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
     * Sets the primary currency.
     * <p>
     * This is the currency of the loan and the currency that payment is made in.
     * @param currency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      JodaBeanUtils.notNull(currency, "currency");
      this.currency = currency;
      return this;
    }

    /**
     * Sets given total commitment schedule for this loan.
     * @param commitment  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder commitment(LocalDateDoubleTimeSeries commitment) {
      JodaBeanUtils.notNull(commitment, "commitment");
      this.commitment = commitment;
      return this;
    }

    /**
     * Sets the interest paying contracts of the facility.
     * <p>
     * A loan facility may have of zero or more contracts.
     * @param contracts  the new value
     * @return this, for chaining, not null
     */
    public Builder contracts(List<? extends ProratedLoanContract> contracts) {
      this.contracts = contracts;
      return this;
    }

    /**
     * Sets the {@code contracts} property in the builder
     * from an array of objects.
     * @param contracts  the new value
     * @return this, for chaining, not null
     */
    public Builder contracts(ProratedLoanContract... contracts) {
      return contracts(ImmutableList.copyOf(contracts));
    }

    /**
     * Sets the accruing fees of the facility.
     * <p>
     * A loan facility may have of zero or more accruing fees.
     * @param fees  the new value
     * @return this, for chaining, not null
     */
    public Builder fees(List<? extends ProratedAccruingFee> fees) {
      this.fees = fees;
      return this;
    }

    /**
     * Sets the {@code fees} property in the builder
     * from an array of objects.
     * @param fees  the new value
     * @return this, for chaining, not null
     */
    public Builder fees(ProratedAccruingFee... fees) {
      return fees(ImmutableList.copyOf(fees));
    }

    /**
     * Sets events (CommitmentAdjustment) associated with this loan.
     * <p>
     * A loan may have of zero or more events.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(List<? extends ProratedLoanEvent> events) {
      this.events = events;
      return this;
    }

    /**
     * Sets the {@code events} property in the builder
     * from an array of objects.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(ProratedLoanEvent... events) {
      return events(ImmutableList.copyOf(events));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(416);
      buf.append("ProratedFacility.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("borrower").append('=').append(JodaBeanUtils.toString(borrower)).append(',').append(' ');
      buf.append("agent").append('=').append(JodaBeanUtils.toString(agent)).append(',').append(' ');
      buf.append("facilityType").append('=').append(JodaBeanUtils.toString(facilityType)).append(',').append(' ');
      buf.append("identifiers").append('=').append(JodaBeanUtils.toString(identifiers)).append(',').append(' ');
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("maturityDate").append('=').append(JodaBeanUtils.toString(maturityDate)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("commitment").append('=').append(JodaBeanUtils.toString(commitment)).append(',').append(' ');
      buf.append("contracts").append('=').append(JodaBeanUtils.toString(contracts)).append(',').append(' ');
      buf.append("fees").append('=').append(JodaBeanUtils.toString(fees)).append(',').append(' ');
      buf.append("events").append('=').append(JodaBeanUtils.toString(events));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
