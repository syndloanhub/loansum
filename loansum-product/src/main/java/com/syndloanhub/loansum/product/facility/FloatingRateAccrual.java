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
import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.ProductTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedFloatingRateAccrual;

import static com.syndloanhub.loansum.product.facility.Helper.max;
import static com.syndloanhub.loansum.product.facility.Helper.tsget;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

/*
 * A set of attributes which define a floating-rate accrual, e.g. index and spread.
 */
@BeanDefinition
public final class FloatingRateAccrual implements Accrual, ImmutableBean {

  /**
   * An implementation of an interest or fee accrual featuring a floating cash rate and PIK spread. This is
   * a global view of the accrual; a prorated version may be produced via the prorate function based
   * on a specific trade. In addition, a sub-accrual structure may be constructed via the rebuild
   * function. For example, an interest contract with a repayment embedded may be rebuilt into
   * 2 sub-accruals implementing the pre and post-paydown accruals.
   */
  @Override
  public ProratedFloatingRateAccrual prorate(ProductTrade trade) {
    final LoanTrade loanTrade = (LoanTrade) trade;
    final double pctShare = tsget(loanTrade.getPctShare(), max(startDate, trade.getInfo().getTradeDate().get()));

    return ProratedFloatingRateAccrual.builder()
        .accrualAmount(accrualAmount.multipliedBy(pctShare))
        .allInRate(allInRate)
        .dayCount(dayCount)
        .endDate(endDate)
        .paymentFrequency(paymentFrequency)
        .pikSpread(pikSpread)
        .startDate(startDate)
        .paymentProjection(paymentProjection.multipliedBy(pctShare))
        .pikProjection(pikProjection.multipliedBy(pctShare))
        .baseRate(baseRate)
        .spread(spread)
        .index(index)
        .build();
  }

  /**
   * Construct a modified instance of this accrual given the new period and amount.
   */
  @Override
  public Accrual rebuild(LocalDate startDate, LocalDate endDate, CurrencyAmount accrualAmount) {
    return FloatingRateAccrual.builder()
        .accrualAmount(accrualAmount)
        .allInRate(allInRate)
        .dayCount(dayCount)
        .startDate(startDate)
        .endDate(endDate)
        .paymentFrequency(paymentFrequency)
        .paymentProjection(paymentProjection)
        .pikProjection(pikProjection)
        .pikSpread(pikSpread)
        .baseRate(baseRate)
        .index(index)
        .spread(spread)
        .accrualAmount(accrualAmount)
        .build();
  }

  /**
   * Split an accrual into cash and PIK sub-accruals. This is needed as a PIK accrual
   * will always span the full period but cash typically will commence on the actual
   * settlement date.
   */
  @Override
  public Pair<Accrual, Accrual> split() {
    return Pair.of(
        FloatingRateAccrual.builder()
            .accrualAmount(accrualAmount)
            .allInRate(allInRate)
            .dayCount(dayCount)
            .startDate(startDate)
            .endDate(endDate)
            .paymentFrequency(paymentFrequency)
            .pikSpread(0)
            .baseRate(baseRate)
            .index(index)
            .spread(spread)
            .build(),
        FloatingRateAccrual.builder()
            .accrualAmount(accrualAmount)
            .allInRate(0)
            .dayCount(dayCount)
            .startDate(startDate)
            .endDate(endDate)
            .paymentFrequency(paymentFrequency)
            .pikSpread(pikSpread)
            .baseRate(0)
            .index(index)
            .spread(0)
            .build());
  }

  /**
   * The start date of the accrual.
   * <p>
   * Interest accrues from this date.
   */
  @PropertyDefinition(validate = "")
  private final LocalDate startDate;

  /**
   * The end date of the accrual.
   * <p>
   * Interest accrues to this date.
   */
  @PropertyDefinition(validate = "")
  private final LocalDate endDate;

  /**
   * The cash rate of the accrual.
   * <p>
   * Cash interest "all-in" rate. Does NOT include PIK.
   */
  @PropertyDefinition(validate = "")
  private final double allInRate;

  /**
   * The PIK rate of the accrual.
   * <p>
   * PIK interest.
   */
  @PropertyDefinition(validate = "")
  private final double pikSpread;

  /**
   * The accrual notional amount and currency.
   */
  @PropertyDefinition(validate = "notNull")
  private final CurrencyAmount accrualAmount;

  /**
   * The day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   */
  @PropertyDefinition(validate = "")
  private final DayCount dayCount;

  /**
   * Frequency of accrual period.
   */
  @PropertyDefinition(validate = "")
  private final Frequency paymentFrequency;

  /**
   * Projected interest amount.
   * <p>
   * The global amount represents the principal amount, and must be non-negative.
   * The currency of the global amount is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "")
  private final CurrencyAmount paymentProjection;

  /**
   * Projected PIK amount.
   * <p>
   * The global amount represents the principal amount, and must be non-negative.
   * The currency of the global amount is specified by {@code currency}.
   */
  @PropertyDefinition(validate = "")
  private final CurrencyAmount pikProjection;

  /**
   * The type of index, e.g. 'USD-LIBOR-1M'.
   */
  @PropertyDefinition(validate = "notNull")
  private final RateIndex index;

  /**
   * The base rate, fixed based on the rate index.
   */
  @PropertyDefinition(validate = "")
  private final double baseRate;

  /**
   * The spread over the index.
   */
  @PropertyDefinition(validate = "")
  private final double spread;

  /**
   * Validation rules.
   */
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.notNegative(accrualAmount.getAmount(), "accrual amount must be positive");
  }

  /**
   * Calculate projected payment amounts.
   * 
   * @param builder
   */
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    final double yearFraction = builder.dayCount.yearFraction(builder.startDate, builder.endDate);

    builder.paymentProjection(CurrencyAmount.of(builder.accrualAmount.getCurrency(),
        builder.accrualAmount.getAmount() * builder.allInRate * yearFraction));
    builder.pikProjection(CurrencyAmount.of(builder.accrualAmount.getCurrency(),
        builder.accrualAmount.getAmount() * builder.pikSpread * yearFraction));
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FloatingRateAccrual}.
   * @return the meta-bean, not null
   */
  public static FloatingRateAccrual.Meta meta() {
    return FloatingRateAccrual.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FloatingRateAccrual.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FloatingRateAccrual.Builder builder() {
    return new FloatingRateAccrual.Builder();
  }

  private FloatingRateAccrual(
      LocalDate startDate,
      LocalDate endDate,
      double allInRate,
      double pikSpread,
      CurrencyAmount accrualAmount,
      DayCount dayCount,
      Frequency paymentFrequency,
      CurrencyAmount paymentProjection,
      CurrencyAmount pikProjection,
      RateIndex index,
      double baseRate,
      double spread) {
    JodaBeanUtils.notNull(accrualAmount, "accrualAmount");
    JodaBeanUtils.notNull(index, "index");
    this.startDate = startDate;
    this.endDate = endDate;
    this.allInRate = allInRate;
    this.pikSpread = pikSpread;
    this.accrualAmount = accrualAmount;
    this.dayCount = dayCount;
    this.paymentFrequency = paymentFrequency;
    this.paymentProjection = paymentProjection;
    this.pikProjection = pikProjection;
    this.index = index;
    this.baseRate = baseRate;
    this.spread = spread;
    validate();
  }

  @Override
  public FloatingRateAccrual.Meta metaBean() {
    return FloatingRateAccrual.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the accrual.
   * <p>
   * Interest accrues from this date.
   * @return the value of the property
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the accrual.
   * <p>
   * Interest accrues to this date.
   * @return the value of the property
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the cash rate of the accrual.
   * <p>
   * Cash interest "all-in" rate. Does NOT include PIK.
   * @return the value of the property
   */
  public double getAllInRate() {
    return allInRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the PIK rate of the accrual.
   * <p>
   * PIK interest.
   * @return the value of the property
   */
  public double getPikSpread() {
    return pikSpread;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the accrual notional amount and currency.
   * @return the value of the property, not null
   */
  public CurrencyAmount getAccrualAmount() {
    return accrualAmount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the day count convention.
   * <p>
   * This is used to convert dates to a numerical value.
   * @return the value of the property
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets frequency of accrual period.
   * @return the value of the property
   */
  public Frequency getPaymentFrequency() {
    return paymentFrequency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets projected interest amount.
   * <p>
   * The global amount represents the principal amount, and must be non-negative.
   * The currency of the global amount is specified by {@code currency}.
   * @return the value of the property
   */
  public CurrencyAmount getPaymentProjection() {
    return paymentProjection;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets projected PIK amount.
   * <p>
   * The global amount represents the principal amount, and must be non-negative.
   * The currency of the global amount is specified by {@code currency}.
   * @return the value of the property
   */
  public CurrencyAmount getPikProjection() {
    return pikProjection;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the type of index, e.g. 'USD-LIBOR-1M'.
   * @return the value of the property, not null
   */
  public RateIndex getIndex() {
    return index;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the base rate, fixed based on the rate index.
   * @return the value of the property
   */
  public double getBaseRate() {
    return baseRate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the spread over the index.
   * @return the value of the property
   */
  public double getSpread() {
    return spread;
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
      FloatingRateAccrual other = (FloatingRateAccrual) obj;
      return JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(allInRate, other.allInRate) &&
          JodaBeanUtils.equal(pikSpread, other.pikSpread) &&
          JodaBeanUtils.equal(accrualAmount, other.accrualAmount) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(paymentFrequency, other.paymentFrequency) &&
          JodaBeanUtils.equal(paymentProjection, other.paymentProjection) &&
          JodaBeanUtils.equal(pikProjection, other.pikProjection) &&
          JodaBeanUtils.equal(index, other.index) &&
          JodaBeanUtils.equal(baseRate, other.baseRate) &&
          JodaBeanUtils.equal(spread, other.spread);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(allInRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(pikSpread);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentProjection);
    hash = hash * 31 + JodaBeanUtils.hashCode(pikProjection);
    hash = hash * 31 + JodaBeanUtils.hashCode(index);
    hash = hash * 31 + JodaBeanUtils.hashCode(baseRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(spread);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(416);
    buf.append("FloatingRateAccrual{");
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("allInRate").append('=').append(allInRate).append(',').append(' ');
    buf.append("pikSpread").append('=').append(pikSpread).append(',').append(' ');
    buf.append("accrualAmount").append('=').append(accrualAmount).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("paymentFrequency").append('=').append(paymentFrequency).append(',').append(' ');
    buf.append("paymentProjection").append('=').append(paymentProjection).append(',').append(' ');
    buf.append("pikProjection").append('=').append(pikProjection).append(',').append(' ');
    buf.append("index").append('=').append(index).append(',').append(' ');
    buf.append("baseRate").append('=').append(baseRate).append(',').append(' ');
    buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FloatingRateAccrual}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code startDate} property.
     */
    private final MetaProperty<LocalDate> _startDate = DirectMetaProperty.ofImmutable(
        this, "startDate", FloatingRateAccrual.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> _endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", FloatingRateAccrual.class, LocalDate.class);
    /**
     * The meta-property for the {@code allInRate} property.
     */
    private final MetaProperty<Double> _allInRate = DirectMetaProperty.ofImmutable(
        this, "allInRate", FloatingRateAccrual.class, Double.TYPE);
    /**
     * The meta-property for the {@code pikSpread} property.
     */
    private final MetaProperty<Double> _pikSpread = DirectMetaProperty.ofImmutable(
        this, "pikSpread", FloatingRateAccrual.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualAmount} property.
     */
    private final MetaProperty<CurrencyAmount> _accrualAmount = DirectMetaProperty.ofImmutable(
        this, "accrualAmount", FloatingRateAccrual.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> _dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", FloatingRateAccrual.class, DayCount.class);
    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> _paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", FloatingRateAccrual.class, Frequency.class);
    /**
     * The meta-property for the {@code paymentProjection} property.
     */
    private final MetaProperty<CurrencyAmount> _paymentProjection = DirectMetaProperty.ofImmutable(
        this, "paymentProjection", FloatingRateAccrual.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code pikProjection} property.
     */
    private final MetaProperty<CurrencyAmount> _pikProjection = DirectMetaProperty.ofImmutable(
        this, "pikProjection", FloatingRateAccrual.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code index} property.
     */
    private final MetaProperty<RateIndex> _index = DirectMetaProperty.ofImmutable(
        this, "index", FloatingRateAccrual.class, RateIndex.class);
    /**
     * The meta-property for the {@code baseRate} property.
     */
    private final MetaProperty<Double> _baseRate = DirectMetaProperty.ofImmutable(
        this, "baseRate", FloatingRateAccrual.class, Double.TYPE);
    /**
     * The meta-property for the {@code spread} property.
     */
    private final MetaProperty<Double> _spread = DirectMetaProperty.ofImmutable(
        this, "spread", FloatingRateAccrual.class, Double.TYPE);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "allInRate",
        "pikSpread",
        "accrualAmount",
        "dayCount",
        "paymentFrequency",
        "paymentProjection",
        "pikProjection",
        "index",
        "baseRate",
        "spread");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return _startDate;
        case -1607727319:  // endDate
          return _endDate;
        case -724263770:  // allInRate
          return _allInRate;
        case 696818085:  // pikSpread
          return _pikSpread;
        case -1672027417:  // accrualAmount
          return _accrualAmount;
        case 1905311443:  // dayCount
          return _dayCount;
        case 863656438:  // paymentFrequency
          return _paymentFrequency;
        case 1204324597:  // paymentProjection
          return _paymentProjection;
        case -1104344703:  // pikProjection
          return _pikProjection;
        case 100346066:  // index
          return _index;
        case -1721567407:  // baseRate
          return _baseRate;
        case -895684237:  // spread
          return _spread;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FloatingRateAccrual.Builder builder() {
      return new FloatingRateAccrual.Builder();
    }

    @Override
    public Class<? extends FloatingRateAccrual> beanType() {
      return FloatingRateAccrual.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code startDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> startDate() {
      return _startDate;
    }

    /**
     * The meta-property for the {@code endDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> endDate() {
      return _endDate;
    }

    /**
     * The meta-property for the {@code allInRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> allInRate() {
      return _allInRate;
    }

    /**
     * The meta-property for the {@code pikSpread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> pikSpread() {
      return _pikSpread;
    }

    /**
     * The meta-property for the {@code accrualAmount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> accrualAmount() {
      return _accrualAmount;
    }

    /**
     * The meta-property for the {@code dayCount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<DayCount> dayCount() {
      return _dayCount;
    }

    /**
     * The meta-property for the {@code paymentFrequency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Frequency> paymentFrequency() {
      return _paymentFrequency;
    }

    /**
     * The meta-property for the {@code paymentProjection} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> paymentProjection() {
      return _paymentProjection;
    }

    /**
     * The meta-property for the {@code pikProjection} property.
     * @return the meta-property, not null
     */
    public MetaProperty<CurrencyAmount> pikProjection() {
      return _pikProjection;
    }

    /**
     * The meta-property for the {@code index} property.
     * @return the meta-property, not null
     */
    public MetaProperty<RateIndex> index() {
      return _index;
    }

    /**
     * The meta-property for the {@code baseRate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> baseRate() {
      return _baseRate;
    }

    /**
     * The meta-property for the {@code spread} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> spread() {
      return _spread;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((FloatingRateAccrual) bean).getStartDate();
        case -1607727319:  // endDate
          return ((FloatingRateAccrual) bean).getEndDate();
        case -724263770:  // allInRate
          return ((FloatingRateAccrual) bean).getAllInRate();
        case 696818085:  // pikSpread
          return ((FloatingRateAccrual) bean).getPikSpread();
        case -1672027417:  // accrualAmount
          return ((FloatingRateAccrual) bean).getAccrualAmount();
        case 1905311443:  // dayCount
          return ((FloatingRateAccrual) bean).getDayCount();
        case 863656438:  // paymentFrequency
          return ((FloatingRateAccrual) bean).getPaymentFrequency();
        case 1204324597:  // paymentProjection
          return ((FloatingRateAccrual) bean).getPaymentProjection();
        case -1104344703:  // pikProjection
          return ((FloatingRateAccrual) bean).getPikProjection();
        case 100346066:  // index
          return ((FloatingRateAccrual) bean).getIndex();
        case -1721567407:  // baseRate
          return ((FloatingRateAccrual) bean).getBaseRate();
        case -895684237:  // spread
          return ((FloatingRateAccrual) bean).getSpread();
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
   * The bean-builder for {@code FloatingRateAccrual}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FloatingRateAccrual> {

    private LocalDate startDate;
    private LocalDate endDate;
    private double allInRate;
    private double pikSpread;
    private CurrencyAmount accrualAmount;
    private DayCount dayCount;
    private Frequency paymentFrequency;
    private CurrencyAmount paymentProjection;
    private CurrencyAmount pikProjection;
    private RateIndex index;
    private double baseRate;
    private double spread;

    /**
     * Restricted constructor.
     */
    private Builder() {
    }

    /**
     * Restricted copy constructor.
     * @param beanToCopy  the bean to copy from, not null
     */
    private Builder(FloatingRateAccrual beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.allInRate = beanToCopy.getAllInRate();
      this.pikSpread = beanToCopy.getPikSpread();
      this.accrualAmount = beanToCopy.getAccrualAmount();
      this.dayCount = beanToCopy.getDayCount();
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
      this.paymentProjection = beanToCopy.getPaymentProjection();
      this.pikProjection = beanToCopy.getPikProjection();
      this.index = beanToCopy.getIndex();
      this.baseRate = beanToCopy.getBaseRate();
      this.spread = beanToCopy.getSpread();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -724263770:  // allInRate
          return allInRate;
        case 696818085:  // pikSpread
          return pikSpread;
        case -1672027417:  // accrualAmount
          return accrualAmount;
        case 1905311443:  // dayCount
          return dayCount;
        case 863656438:  // paymentFrequency
          return paymentFrequency;
        case 1204324597:  // paymentProjection
          return paymentProjection;
        case -1104344703:  // pikProjection
          return pikProjection;
        case 100346066:  // index
          return index;
        case -1721567407:  // baseRate
          return baseRate;
        case -895684237:  // spread
          return spread;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          this.startDate = (LocalDate) newValue;
          break;
        case -1607727319:  // endDate
          this.endDate = (LocalDate) newValue;
          break;
        case -724263770:  // allInRate
          this.allInRate = (Double) newValue;
          break;
        case 696818085:  // pikSpread
          this.pikSpread = (Double) newValue;
          break;
        case -1672027417:  // accrualAmount
          this.accrualAmount = (CurrencyAmount) newValue;
          break;
        case 1905311443:  // dayCount
          this.dayCount = (DayCount) newValue;
          break;
        case 863656438:  // paymentFrequency
          this.paymentFrequency = (Frequency) newValue;
          break;
        case 1204324597:  // paymentProjection
          this.paymentProjection = (CurrencyAmount) newValue;
          break;
        case -1104344703:  // pikProjection
          this.pikProjection = (CurrencyAmount) newValue;
          break;
        case 100346066:  // index
          this.index = (RateIndex) newValue;
          break;
        case -1721567407:  // baseRate
          this.baseRate = (Double) newValue;
          break;
        case -895684237:  // spread
          this.spread = (Double) newValue;
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
    public FloatingRateAccrual build() {
      preBuild(this);
      return new FloatingRateAccrual(
          startDate,
          endDate,
          allInRate,
          pikSpread,
          accrualAmount,
          dayCount,
          paymentFrequency,
          paymentProjection,
          pikProjection,
          index,
          baseRate,
          spread);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date of the accrual.
     * <p>
     * Interest accrues from this date.
     * @param startDate  the new value
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the accrual.
     * <p>
     * Interest accrues to this date.
     * @param endDate  the new value
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the cash rate of the accrual.
     * <p>
     * Cash interest "all-in" rate. Does NOT include PIK.
     * @param allInRate  the new value
     * @return this, for chaining, not null
     */
    public Builder allInRate(double allInRate) {
      this.allInRate = allInRate;
      return this;
    }

    /**
     * Sets the PIK rate of the accrual.
     * <p>
     * PIK interest.
     * @param pikSpread  the new value
     * @return this, for chaining, not null
     */
    public Builder pikSpread(double pikSpread) {
      this.pikSpread = pikSpread;
      return this;
    }

    /**
     * Sets the accrual notional amount and currency.
     * @param accrualAmount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder accrualAmount(CurrencyAmount accrualAmount) {
      JodaBeanUtils.notNull(accrualAmount, "accrualAmount");
      this.accrualAmount = accrualAmount;
      return this;
    }

    /**
     * Sets the day count convention.
     * <p>
     * This is used to convert dates to a numerical value.
     * @param dayCount  the new value
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets frequency of accrual period.
     * @param paymentFrequency  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      this.paymentFrequency = paymentFrequency;
      return this;
    }

    /**
     * Sets projected interest amount.
     * <p>
     * The global amount represents the principal amount, and must be non-negative.
     * The currency of the global amount is specified by {@code currency}.
     * @param paymentProjection  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentProjection(CurrencyAmount paymentProjection) {
      this.paymentProjection = paymentProjection;
      return this;
    }

    /**
     * Sets projected PIK amount.
     * <p>
     * The global amount represents the principal amount, and must be non-negative.
     * The currency of the global amount is specified by {@code currency}.
     * @param pikProjection  the new value
     * @return this, for chaining, not null
     */
    public Builder pikProjection(CurrencyAmount pikProjection) {
      this.pikProjection = pikProjection;
      return this;
    }

    /**
     * Sets the type of index, e.g. 'USD-LIBOR-1M'.
     * @param index  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder index(RateIndex index) {
      JodaBeanUtils.notNull(index, "index");
      this.index = index;
      return this;
    }

    /**
     * Sets the base rate, fixed based on the rate index.
     * @param baseRate  the new value
     * @return this, for chaining, not null
     */
    public Builder baseRate(double baseRate) {
      this.baseRate = baseRate;
      return this;
    }

    /**
     * Sets the spread over the index.
     * @param spread  the new value
     * @return this, for chaining, not null
     */
    public Builder spread(double spread) {
      this.spread = spread;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(416);
      buf.append("FloatingRateAccrual.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("allInRate").append('=').append(JodaBeanUtils.toString(allInRate)).append(',').append(' ');
      buf.append("pikSpread").append('=').append(JodaBeanUtils.toString(pikSpread)).append(',').append(' ');
      buf.append("accrualAmount").append('=').append(JodaBeanUtils.toString(accrualAmount)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("paymentProjection").append('=').append(JodaBeanUtils.toString(paymentProjection)).append(',').append(' ');
      buf.append("pikProjection").append('=').append(JodaBeanUtils.toString(pikProjection)).append(',').append(' ');
      buf.append("index").append('=').append(JodaBeanUtils.toString(index)).append(',').append(' ');
      buf.append("baseRate").append('=').append(JodaBeanUtils.toString(baseRate)).append(',').append(' ');
      buf.append("spread").append('=').append(JodaBeanUtils.toString(spread));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
