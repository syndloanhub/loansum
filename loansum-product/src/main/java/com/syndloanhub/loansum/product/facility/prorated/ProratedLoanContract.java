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

import org.joda.beans.ImmutableBean;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.Bean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;

/**
 * A loan contract prorated against a specific trade.
 */
@BeanDefinition
public final class ProratedLoanContract implements ImmutableBean {

  /**
   * The internal id of this contract
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId id;

  /**
   * Contract accrual, fixed or floating.
   */
  @PropertyDefinition(validate = "notNull")
  private final ProratedAccrual accrual;

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
  @PropertyDefinition(validate = "", builderType = "List<? extends ProratedLoanContractEvent>")
  private final ImmutableList<ProratedLoanContractEvent> events;

  /**
   * Normalized set of sub-accruals based on contract-level events.
   */
  @PropertyDefinition(validate = "", builderType = "List<? extends ProratedAccrual>")
  private final ImmutableList<ProratedAccrual> accrualSchedule;

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code ProratedLoanContract}.
   * @return the meta-bean, not null
   */
  public static ProratedLoanContract.Meta meta() {
    return ProratedLoanContract.Meta.INSTANCE;
  }

  static {
    MetaBean.register(ProratedLoanContract.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static ProratedLoanContract.Builder builder() {
    return new ProratedLoanContract.Builder();
  }

  private ProratedLoanContract(
      StandardId id,
      ProratedAccrual accrual,
      LocalDate paymentDate,
      List<? extends ProratedLoanContractEvent> events,
      List<? extends ProratedAccrual> accrualSchedule) {
    JodaBeanUtils.notNull(id, "id");
    JodaBeanUtils.notNull(accrual, "accrual");
    JodaBeanUtils.notNull(paymentDate, "paymentDate");
    this.id = id;
    this.accrual = accrual;
    this.paymentDate = paymentDate;
    this.events = (events != null ? ImmutableList.copyOf(events) : null);
    this.accrualSchedule = (accrualSchedule != null ? ImmutableList.copyOf(accrualSchedule) : null);
  }

  @Override
  public ProratedLoanContract.Meta metaBean() {
    return ProratedLoanContract.Meta.INSTANCE;
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
  public ProratedAccrual getAccrual() {
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
  public ImmutableList<ProratedLoanContractEvent> getEvents() {
    return events;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets normalized set of sub-accruals based on contract-level events.
   * @return the value of the property
   */
  public ImmutableList<ProratedAccrual> getAccrualSchedule() {
    return accrualSchedule;
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
      ProratedLoanContract other = (ProratedLoanContract) obj;
      return JodaBeanUtils.equal(id, other.id) &&
          JodaBeanUtils.equal(accrual, other.accrual) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(events, other.events) &&
          JodaBeanUtils.equal(accrualSchedule, other.accrualSchedule);
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
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSchedule);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(192);
    buf.append("ProratedLoanContract{");
    buf.append("id").append('=').append(id).append(',').append(' ');
    buf.append("accrual").append('=').append(accrual).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("events").append('=').append(events).append(',').append(' ');
    buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code ProratedLoanContract}.
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
        this, "id", ProratedLoanContract.class, StandardId.class);
    /**
     * The meta-property for the {@code accrual} property.
     */
    private final MetaProperty<ProratedAccrual> _accrual = DirectMetaProperty.ofImmutable(
        this, "accrual", ProratedLoanContract.class, ProratedAccrual.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> _paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", ProratedLoanContract.class, LocalDate.class);
    /**
     * The meta-property for the {@code events} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ProratedLoanContractEvent>> _events = DirectMetaProperty.ofImmutable(
        this, "events", ProratedLoanContract.class, (Class) ImmutableList.class);
    /**
     * The meta-property for the {@code accrualSchedule} property.
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    private final MetaProperty<ImmutableList<ProratedAccrual>> _accrualSchedule = DirectMetaProperty.ofImmutable(
        this, "accrualSchedule", ProratedLoanContract.class, (Class) ImmutableList.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "id",
        "accrual",
        "paymentDate",
        "events",
        "accrualSchedule");

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
        case 304659814:  // accrualSchedule
          return _accrualSchedule;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public ProratedLoanContract.Builder builder() {
      return new ProratedLoanContract.Builder();
    }

    @Override
    public Class<? extends ProratedLoanContract> beanType() {
      return ProratedLoanContract.class;
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
    public MetaProperty<ProratedAccrual> accrual() {
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
    public MetaProperty<ImmutableList<ProratedLoanContractEvent>> events() {
      return _events;
    }

    /**
     * The meta-property for the {@code accrualSchedule} property.
     * @return the meta-property, not null
     */
    public MetaProperty<ImmutableList<ProratedAccrual>> accrualSchedule() {
      return _accrualSchedule;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3355:  // id
          return ((ProratedLoanContract) bean).getId();
        case -1177229905:  // accrual
          return ((ProratedLoanContract) bean).getAccrual();
        case -1540873516:  // paymentDate
          return ((ProratedLoanContract) bean).getPaymentDate();
        case -1291329255:  // events
          return ((ProratedLoanContract) bean).getEvents();
        case 304659814:  // accrualSchedule
          return ((ProratedLoanContract) bean).getAccrualSchedule();
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
   * The bean-builder for {@code ProratedLoanContract}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<ProratedLoanContract> {

    private StandardId id;
    private ProratedAccrual accrual;
    private LocalDate paymentDate;
    private List<? extends ProratedLoanContractEvent> events;
    private List<? extends ProratedAccrual> accrualSchedule;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(ProratedLoanContract beanToCopy) {
      this.id = beanToCopy.getId();
      this.accrual = beanToCopy.getAccrual();
      this.paymentDate = beanToCopy.getPaymentDate();
      this.events = beanToCopy.getEvents();
      this.accrualSchedule = beanToCopy.getAccrualSchedule();
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
        case 304659814:  // accrualSchedule
          return accrualSchedule;
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
          this.accrual = (ProratedAccrual) newValue;
          break;
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
          break;
        case -1291329255:  // events
          this.events = (List<? extends ProratedLoanContractEvent>) newValue;
          break;
        case 304659814:  // accrualSchedule
          this.accrualSchedule = (List<? extends ProratedAccrual>) newValue;
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
    public ProratedLoanContract build() {
      return new ProratedLoanContract(
          id,
          accrual,
          paymentDate,
          events,
          accrualSchedule);
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
    public Builder accrual(ProratedAccrual accrual) {
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
    public Builder events(List<? extends ProratedLoanContractEvent> events) {
      this.events = events;
      return this;
    }

    /**
     * Sets the {@code events} property in the builder
     * from an array of objects.
     * @param events  the new value
     * @return this, for chaining, not null
     */
    public Builder events(ProratedLoanContractEvent... events) {
      return events(ImmutableList.copyOf(events));
    }

    /**
     * Sets normalized set of sub-accruals based on contract-level events.
     * @param accrualSchedule  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualSchedule(List<? extends ProratedAccrual> accrualSchedule) {
      this.accrualSchedule = accrualSchedule;
      return this;
    }

    /**
     * Sets the {@code accrualSchedule} property in the builder
     * from an array of objects.
     * @param accrualSchedule  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualSchedule(ProratedAccrual... accrualSchedule) {
      return accrualSchedule(ImmutableList.copyOf(accrualSchedule));
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(192);
      buf.append("ProratedLoanContract.Builder{");
      buf.append("id").append('=').append(JodaBeanUtils.toString(id)).append(',').append(' ');
      buf.append("accrual").append('=').append(JodaBeanUtils.toString(accrual)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("events").append('=').append(JodaBeanUtils.toString(events)).append(',').append(' ');
      buf.append("accrualSchedule").append('=').append(JodaBeanUtils.toString(accrualSchedule));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
