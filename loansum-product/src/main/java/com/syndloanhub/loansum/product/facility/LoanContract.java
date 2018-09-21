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

import static com.syndloanhub.loansum.product.facility.Helper.intersection;
import static com.syndloanhub.loansum.product.facility.Helper.generateContractAccrualSchedule;
import java.time.LocalDate;

import org.joda.beans.ImmutableBean;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.stream.Collectors;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.google.common.collect.ImmutableList;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.TradeInfo;

import com.syndloanhub.loansum.product.facility.prorated.ProratedAccrual;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanContract;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanContractEvent;

import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;

/**
 * A loan contract.
 * <p>
 * A loan contract defines specific interest accrual terms (start date,
 * end date, rate, day count convention) over a specific borrowed amount.
 * <p> * Over the course of a contract, multiple repayments of the contract amount
 * may occur. A borrowing always results in a new contract.
 * <p>
 * This is the standard representation but for usage within the calculation framework,
 * a contracted must be expanded into canonical form and prorated against a single trade.
 */
@BeanDefinition
public final class LoanContract implements ImmutableBean, Proratable<ProratedLoanContract> {

  /**
   * Given a loan trade, create a prorated version of this contract by converting global quantities 
   * into share amounts.
   */
  @Override
  public ProratedLoanContract prorate(ProductTrade trade) {
    assert (trade instanceof LoanTrade);

    final LoanTrade loanTrade = (LoanTrade) trade;
    final TradeInfo info = trade.getInfo();
    final LocalDate tradeDate = info.getTradeDate().get();
    final LocalDate actualSettlementDate = info.getSettlementDate().get();
    final boolean delayedSettlement =
        loanTrade.isDelayedCompensationFlag() && actualSettlementDate.isAfter(loanTrade.getExpectedSettlementDate());

    /*
     * Expand accrual into sub-accruals based on contract events
     * and then prorate accruals.
     */
    List<ProratedAccrual> proratedAccrualSchedule = new ArrayList<ProratedAccrual>();

    for (Accrual accrual : generateContractAccrualSchedule(this)) {
      Pair<LocalDate, LocalDate> accrualPeriod = Pair.of(accrual.getStartDate(), accrual.getEndDate());
      boolean isPikAccrual = accrual.getPikSpread() > 0;
      boolean accrualProrated = false;

      // If delayed settlement and accrual spans the settlement period then
      // create separate accruals for delayed compensation and interest.
      if (delayedSettlement && !isPikAccrual) {
        Pair<LocalDate, LocalDate> settlementPeriod =
            Pair.of(loanTrade.getExpectedSettlementDate(), actualSettlementDate);
        Pair<LocalDate, LocalDate> intersection = intersection(accrualPeriod, settlementPeriod);

        if (intersection != null) {
          proratedAccrualSchedule.add(accrual.rebuild(intersection.getFirst(), intersection.getSecond(),
              accrual.getAccrualAmount(), false).prorate(trade));

          if (accrual.getEndDate().isAfter(actualSettlementDate))
            proratedAccrualSchedule.add(accrual.rebuild(actualSettlementDate,
                accrual.getEndDate(), accrual.getAccrualAmount(), false).prorate(trade));
          accrualProrated = true;
        }
      }

      if (!accrualProrated) {
        if (accrual.getEndDate().isAfter(actualSettlementDate)) {
          if (accrual.getStartDate().isBefore(actualSettlementDate) && !isPikAccrual)
            accrual =
                accrual.rebuild(actualSettlementDate, accrual.getEndDate(), accrual.getAccrualAmount(), false);

          proratedAccrualSchedule.add(accrual.prorate(trade));
        }
      }
    }

    // Prorate contract events.
    List<ProratedLoanContractEvent> proratedEvents = new ArrayList<ProratedLoanContractEvent>();

    if (tradeDate.isBefore(accrual.getEndDate())) {
      proratedEvents = events
          .stream()
          .map(event -> event.prorate(trade))
          .collect(Collectors.toList());
    }

    // Return prorated contract.
    return ProratedLoanContract.builder()
        .id(id)
        .accrualSchedule(proratedAccrualSchedule)
        .accrual(accrual.prorate(trade))
        .paymentDate(paymentDate)
        .events(proratedEvents)
        .build();
  }

  /**
   * The internal id of this contract
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId id;

  /**
   * Contract accrual, fixed or floating.
   */
  @PropertyDefinition(validate = "notNull")
  private final Accrual accrual;

  /**
   * The payment date of the contract.
   * <p>
   * Interest pays on this date. Usually identical to end date 
   * unless end date is a holiday. Payment date is given, not calculated.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate paymentDate;

  /**
   * Events (Borrowing, Repayment) associated with this contract.
   * <p>
   * A loan contract may have of zero or more events.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends LoanContractEvent>")
  private final ImmutableList<LoanContractEvent> events;

  /**
   * Defaulted values: empty event list, random contract id.
   * @param builder
   */
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder
        .id(StandardId.of("contract", Integer.toString(new Random().nextInt(100000))))
        .events(new ArrayList<LoanContractEvent>());
  }

  /**
   * Validation rules.
   */
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderOrEqual(accrual.getEndDate(), paymentDate, "endDate", "paymentDate");

    if (events != null) {
      double totalRepayment = 0;

      for (LoanContractEvent event : events) {
        ArgChecker.isTrue(!event.getEffectiveDate().isBefore(accrual.getStartDate()),
            "Contract " + id + " " + event.getEffectiveDate() + " event prior to start date " + accrual.getStartDate());
        ArgChecker.isTrue(!event.getEffectiveDate().isAfter(accrual.getEndDate()),
            "Contract events must be on or before end date");

        switch (event.getType()) {
          case BorrowingEvent:
            ArgChecker.isTrue(event.getEffectiveDate().equals(accrual.getStartDate()),
                "Borrows only allowed on contract stat date");
            break;
          case RepaymentEvent:
            totalRepayment += event.getAmount().getAmount();
            break;
        }
      }

      ArgChecker.isTrue(accrual.getAccrualAmount().getAmount() - totalRepayment > -0.001,
          "Contract " + id + ": total repayments " + totalRepayment +
              " must be equal or less than contract amount " + accrual.getAccrualAmount());
    }
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code LoanContract}.
   * @return the meta-bean, not null
   */
  public static LoanContract.Meta meta() {
    return LoanContract.Meta.INSTANCE;
  }

  static {
    MetaBean.register(LoanContract.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static LoanContract.Builder builder() {
    return new LoanContract.Builder();
  }

  private LoanContract(
      StandardId id,
      Accrual accrual,
      LocalDate paymentDate,
      List<? extends LoanContractEvent> events) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(accrual, "accrual");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    this.id = id;
    this.accrual = accrual;
    this.paymentDate = paymentDate;
    this.events = (events != null ? ImmutableList.copyOf(events) : null);
    validate();
  }

  @Override
  public LoanContract.Meta metaBean() {
    return LoanContract.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the internal id of this contract
   * @return the value of the property, not null
   */
  public StandardId getId() {
    return id;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets contract accrual, fixed or floating.
   * @return the value of the property, not null
   */
  public Accrual getAccrual() {
    return accrual;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment date of the contract.
   * <p>
   * Interest pays on this date. Usually identical to end date
   * unless end date is a holiday. Payment date is given, not calculated.
   * @return the value of the property, not null
   */
  public LocalDate getPaymentDate() {
    return paymentDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets events (Borrowing, Repayment) associated with this contract.
   * <p>
   * A loan contract may have of zero or more events.
   * @return the value of the property
   */
  public ImmutableList<LoanContractEvent> getEvents() {
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
      LoanContract other = (LoanContract) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(accrual, other.accrual) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(events, other.events);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(id);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrual);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(events);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(160);
    buf.append("LoanContract{");
    buf.append("id").append('=').append(id).append(',').append(' ');
    buf.append("accrual").append('=').append(accrual).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("events").append('=').append(JodaBeanUtils.toString(events));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code LoanContract}.
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
        this, "id", LoanContract.class, StandardId.class);
    /**
     * The meta-property for the {@code accrual} property.
     */
    private final MetaProperty<Accrual> _accrual = DirectMetaProperty.ofImmutable(
        this, "accrual", LoanContract.class, Accrual.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> _paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", LoanContract.class, LocalDate.class);
    /**
     * The meta-property for the {@code events} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<LoanContractEvent>> _events = DirectMetaProperty.ofImmutable(
        this, "events", LoanContract.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "accrual",
        "paymentDate",
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
        case -1177229905:  // accrual
          return _accrual;
        case -1540873516:  // paymentDate
          return _paymentDate;
        case -1291329255:  // events
          return _events;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public LoanContract.Builder builder() {
      return new LoanContract.Builder();
    }

    @Override
    public Class<? extends LoanContract> beanType() {
      return LoanContract.class;
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
     * The meta-property for the {@code accrual} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Accrual> accrual() {
      return _accrual;
    }

    /**
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return _paymentDate;
    }

    /**
     * The meta-property for the {@code events} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<LoanContractEvent>> events() {
      return _events;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((LoanContract) bean).getId();
        case -1177229905:  // accrual
          return ((LoanContract) bean).getAccrual();
        case -1540873516:  // paymentDate
          return ((LoanContract) bean).getPaymentDate();
        case -1291329255:  // events
          return ((LoanContract) bean).getEvents();
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
   * The bean-builder for {@code LoanContract}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<LoanContract> {

    private StandardId id;
    private Accrual accrual;
    private LocalDate paymentDate;
    private List<? extends LoanContractEvent> events;

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
    private Builder(LoanContract beanToCopy) {
      this.id = beanToCopy.getId();
      this.accrual = beanToCopy.getAccrual();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.events = beanToCopy.getEvents();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return id;
        case -1177229905:  // accrual
          return accrual;
        case -1540873516:  // paymentDate
          return paymentDate;
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
        case -1177229905:  // accrual
          this.accrual = (Accrual) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -1291329255:  // events
          this.events = (List<? extends LoanContractEvent>) newValue;
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
    public LoanContract build() {
      return new LoanContract(
          id,
          accrual,
          paymentDate,
          events);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the internal id of this contract
     * @param id  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder id(StandardId id) {
      JodaBeanUtils.notNull(id, "id");
      this.id = id;
      return this;
    }

    /**
     * Sets contract accrual, fixed or floating.
     * @param accrual  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrual(Accrual accrual) {
      JodaBeanUtils.notNull(accrual, "accrual");
      this.accrual = accrual;
      return this;
    }

    /**
     * Sets the payment date of the contract.
     * <p>
     * Interest pays on this date. Usually identical to end date
     * unless end date is a holiday. Payment date is given, not calculated.
     * @param paymentDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      JodaBeanUtils.notNull(paymentDate, "paymentDate");
      this.paymentDate = paymentDate;
      return this;
    }

    /**
     * Sets events (Borrowing, Repayment) associated with this contract.
     * <p>
     * A loan contract may have of zero or more events.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(List<? extends LoanContractEvent> events) {
      this.events = events;
      return this;
    }

    /**
     * Sets the {@code events} property in the builder
     * from an array of objects.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(LoanContractEvent... events) {
      return events(ImmutableList.copyOf(events));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(160);
      buf.append("LoanContract.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("accrual").append('=').append(JodaBeanUtils.toString(accrual)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("events").append('=').append(JodaBeanUtils.toString(events));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
