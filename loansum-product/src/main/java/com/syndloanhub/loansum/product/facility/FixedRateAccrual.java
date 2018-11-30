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

import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;

import static com.syndloanhub.loansum.product.facility.Helper.max;
import static com.syndloanhub.loansum.product.facility.Helper.tsget;

import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.joda.beans.ImmutableBean;
import org.joda.beans.MetaBean;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.date.DayCounts;
import com.opengamma.strata.basics.schedule.Frequency;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.product.ProductTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedFixedRateAccrual;

import org.joda.beans.Bean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.impl.direct.DirectMetaProperty;

/**
 * An implementation of an interest or fee accrual featuring a fixed cash rate and PIK spread. This is
 * a global view of the accrual; a prorated version may be produced via the prorate function based
 * on a specific trade. In addition, a sub-accrual structure may be constructed via the rebuild
 * function. For example, an interest contract with a repayment embedded may be rebuilt into
 * 2 sub-accruals implementing the pre and post-paydown accruals.
 */
@BeanDefinition
public final class FixedRateAccrual implements Accrual, ImmutableBean {

  /**
   * Prorate a global accrual into a share based on given trade.
   */
  @Override
  public ProratedFixedRateAccrual prorate(ProductTrade trade) {
    final LoanTrade loanTrade = (LoanTrade) trade;
    final double pctShare = tsget(loanTrade.getPctShare(), max(startDate, trade.getInfo().getTradeDate().get()));

    return ProratedFixedRateAccrual.builder()
        .accrualAmount(accrualAmount.multipliedBy(pctShare))
        .allInRate(allInRate)
        .dayCount(dayCount)
        .endDate(endDate)
        .paymentDate(paymentDate)
        .paymentFrequency(paymentFrequency)
        .pikSpread(pikSpread)
        .startDate(startDate)
        .paymentProjection(paymentProjection.multipliedBy(pctShare))
        .pikProjection(pikProjection.multipliedBy(pctShare))
        .build();
  }

  /**
   * Construct a modified instance of this accrual given the new period and amount.
   */
  @Override
  public Accrual rebuild(LocalDate startDate, LocalDate endDate, CurrencyAmount accrualAmount, LocalDate paymentDate) {
    return FixedRateAccrual.builder()
        .accrualAmount(accrualAmount)
        .allInRate(allInRate)
        .dayCount(dayCount)
        .startDate(startDate)
        .endDate(endDate)
        .paymentDate(paymentDate)
        .paymentFrequency(paymentFrequency)
        .paymentProjection(paymentProjection)
        .pikProjection(pikProjection)
        .pikSpread(pikSpread)
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
        FixedRateAccrual.builder()
            .accrualAmount(accrualAmount)
            .allInRate(allInRate)
            .dayCount(dayCount)
            .startDate(startDate)
            .endDate(endDate)
            .paymentFrequency(paymentFrequency)
            .pikSpread(0)
            .build(),
        FixedRateAccrual.builder()
            .accrualAmount(accrualAmount)
            .allInRate(0)
            .dayCount(dayCount)
            .startDate(startDate)
            .endDate(endDate)
            .paymentFrequency(paymentFrequency)
            .pikSpread(pikSpread)
            .build());
  }

  /**
   * The start date of the accrual.
   * <p>
   * Interest accrues from this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate startDate;

  /**
   * The end date of the accrual.
   * <p>
   * Interest accrues to this date.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate endDate;

  /**
   * The payment date, optional.
   */
  @PropertyDefinition(get = "optional")
  private final LocalDate paymentDate;

  /**
   * The cash rate of the accrual.
   * <p>
   * Cash interest "all-in" rate. Does NOT include PIK.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double allInRate;

  /**
   * The PIK rate of the accrual.
   * <p>
   * PIK interest.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
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
  @PropertyDefinition(validate = "notNull")
  private final DayCount dayCount;

  /**
   * Frequency of accrual period.
   */
  @PropertyDefinition(validate = "notNull")
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

  /**
   * Default common values, most contracts are not PIK-ing and are month ACT/360.
   * 
   * @param builder
   */
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder
        .pikSpread(0)
        .paymentFrequency(Frequency.P1M)
        .dayCount(DayCounts.ACT_360)
        .paymentDate(null);
  }

  /**
   * Validate that start date precedes end date and accrual amount if positive.
   */
  @ImmutableValidator
  private void validate() {
    ArgChecker.inOrderNotEqual(startDate, endDate, "startDate", "endDate");
    ArgChecker.notNegative(accrualAmount.getAmount(), "accrual amount must be positive");
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code FixedRateAccrual}.
   * @return the meta-bean, not null
   */
  public static FixedRateAccrual.Meta meta() {
    return FixedRateAccrual.Meta.INSTANCE;
  }

  static {
    MetaBean.register(FixedRateAccrual.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static FixedRateAccrual.Builder builder() {
    return new FixedRateAccrual.Builder();
  }

  private FixedRateAccrual(
      LocalDate startDate,
      LocalDate endDate,
      LocalDate paymentDate,
      double allInRate,
      double pikSpread,
      CurrencyAmount accrualAmount,
      DayCount dayCount,
      Frequency paymentFrequency,
      CurrencyAmount paymentProjection,
      CurrencyAmount pikProjection) {
    JodaBeanUtils.notNull(startDate, "startDate");
    JodaBeanUtils.notNull(endDate, "endDate");
    ArgChecker.notNegative(allInRate, "allInRate");
    ArgChecker.notNegative(pikSpread, "pikSpread");
    JodaBeanUtils.notNull(accrualAmount, "accrualAmount");
    JodaBeanUtils.notNull(dayCount, "dayCount");
    JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
    this.startDate = startDate;
    this.endDate = endDate;
    this.paymentDate = paymentDate;
    this.allInRate = allInRate;
    this.pikSpread = pikSpread;
    this.accrualAmount = accrualAmount;
    this.dayCount = dayCount;
    this.paymentFrequency = paymentFrequency;
    this.paymentProjection = paymentProjection;
    this.pikProjection = pikProjection;
    validate();
  }

  @Override
  public FixedRateAccrual.Meta metaBean() {
    return FixedRateAccrual.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the start date of the accrual.
   * <p>
   * Interest accrues from this date.
   * @return the value of the property, not null
   */
  public LocalDate getStartDate() {
    return startDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the end date of the accrual.
   * <p>
   * Interest accrues to this date.
   * @return the value of the property, not null
   */
  public LocalDate getEndDate() {
    return endDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the payment date, optional.
   * @return the optional value of the property, not null
   */
  public Optional<LocalDate> getPaymentDate() {
    return Optional.ofNullable(paymentDate);
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
   * @return the value of the property, not null
   */
  public DayCount getDayCount() {
    return dayCount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets frequency of accrual period.
   * @return the value of the property, not null
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
      FixedRateAccrual other = (FixedRateAccrual) obj;
      return JodaBeanUtils.equal(startDate, other.startDate) &&
          JodaBeanUtils.equal(endDate, other.endDate) &&
          JodaBeanUtils.equal(paymentDate, other.paymentDate) &&
          JodaBeanUtils.equal(allInRate, other.allInRate) &&
          JodaBeanUtils.equal(pikSpread, other.pikSpread) &&
          JodaBeanUtils.equal(accrualAmount, other.accrualAmount) &&
          JodaBeanUtils.equal(dayCount, other.dayCount) &&
          JodaBeanUtils.equal(paymentFrequency, other.paymentFrequency) &&
          JodaBeanUtils.equal(paymentProjection, other.paymentProjection) &&
          JodaBeanUtils.equal(pikProjection, other.pikProjection);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(startDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(endDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(allInRate);
    hash = hash * 31 + JodaBeanUtils.hashCode(pikSpread);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualAmount);
    hash = hash * 31 + JodaBeanUtils.hashCode(dayCount);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentFrequency);
    hash = hash * 31 + JodaBeanUtils.hashCode(paymentProjection);
    hash = hash * 31 + JodaBeanUtils.hashCode(pikProjection);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(352);
    buf.append("FixedRateAccrual{");
    buf.append("startDate").append('=').append(startDate).append(',').append(' ');
    buf.append("endDate").append('=').append(endDate).append(',').append(' ');
    buf.append("paymentDate").append('=').append(paymentDate).append(',').append(' ');
    buf.append("allInRate").append('=').append(allInRate).append(',').append(' ');
    buf.append("pikSpread").append('=').append(pikSpread).append(',').append(' ');
    buf.append("accrualAmount").append('=').append(accrualAmount).append(',').append(' ');
    buf.append("dayCount").append('=').append(dayCount).append(',').append(' ');
    buf.append("paymentFrequency").append('=').append(paymentFrequency).append(',').append(' ');
    buf.append("paymentProjection").append('=').append(paymentProjection).append(',').append(' ');
    buf.append("pikProjection").append('=').append(JodaBeanUtils.toString(pikProjection));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code FixedRateAccrual}.
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
        this, "startDate", FixedRateAccrual.class, LocalDate.class);
    /**
     * The meta-property for the {@code endDate} property.
     */
    private final MetaProperty<LocalDate> _endDate = DirectMetaProperty.ofImmutable(
        this, "endDate", FixedRateAccrual.class, LocalDate.class);
    /**
     * The meta-property for the {@code paymentDate} property.
     */
    private final MetaProperty<LocalDate> _paymentDate = DirectMetaProperty.ofImmutable(
        this, "paymentDate", FixedRateAccrual.class, LocalDate.class);
    /**
     * The meta-property for the {@code allInRate} property.
     */
    private final MetaProperty<Double> _allInRate = DirectMetaProperty.ofImmutable(
        this, "allInRate", FixedRateAccrual.class, Double.TYPE);
    /**
     * The meta-property for the {@code pikSpread} property.
     */
    private final MetaProperty<Double> _pikSpread = DirectMetaProperty.ofImmutable(
        this, "pikSpread", FixedRateAccrual.class, Double.TYPE);
    /**
     * The meta-property for the {@code accrualAmount} property.
     */
    private final MetaProperty<CurrencyAmount> _accrualAmount = DirectMetaProperty.ofImmutable(
        this, "accrualAmount", FixedRateAccrual.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code dayCount} property.
     */
    private final MetaProperty<DayCount> _dayCount = DirectMetaProperty.ofImmutable(
        this, "dayCount", FixedRateAccrual.class, DayCount.class);
    /**
     * The meta-property for the {@code paymentFrequency} property.
     */
    private final MetaProperty<Frequency> _paymentFrequency = DirectMetaProperty.ofImmutable(
        this, "paymentFrequency", FixedRateAccrual.class, Frequency.class);
    /**
     * The meta-property for the {@code paymentProjection} property.
     */
    private final MetaProperty<CurrencyAmount> _paymentProjection = DirectMetaProperty.ofImmutable(
        this, "paymentProjection", FixedRateAccrual.class, CurrencyAmount.class);
    /**
     * The meta-property for the {@code pikProjection} property.
     */
    private final MetaProperty<CurrencyAmount> _pikProjection = DirectMetaProperty.ofImmutable(
        this, "pikProjection", FixedRateAccrual.class, CurrencyAmount.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "startDate",
        "endDate",
        "paymentDate",
        "allInRate",
        "pikSpread",
        "accrualAmount",
        "dayCount",
        "paymentFrequency",
        "paymentProjection",
        "pikProjection");

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
        case -1540873516:  // paymentDate
          return _paymentDate;
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
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public FixedRateAccrual.Builder builder() {
      return new FixedRateAccrual.Builder();
    }

    @Override
    public Class<? extends FixedRateAccrual> beanType() {
      return FixedRateAccrual.class;
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
     * The meta-property for the {@code paymentDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> paymentDate() {
      return _paymentDate;
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

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return ((FixedRateAccrual) bean).getStartDate();
        case -1607727319:  // endDate
          return ((FixedRateAccrual) bean).getEndDate();
        case -1540873516:  // paymentDate
          return ((FixedRateAccrual) bean).paymentDate;
        case -724263770:  // allInRate
          return ((FixedRateAccrual) bean).getAllInRate();
        case 696818085:  // pikSpread
          return ((FixedRateAccrual) bean).getPikSpread();
        case -1672027417:  // accrualAmount
          return ((FixedRateAccrual) bean).getAccrualAmount();
        case 1905311443:  // dayCount
          return ((FixedRateAccrual) bean).getDayCount();
        case 863656438:  // paymentFrequency
          return ((FixedRateAccrual) bean).getPaymentFrequency();
        case 1204324597:  // paymentProjection
          return ((FixedRateAccrual) bean).getPaymentProjection();
        case -1104344703:  // pikProjection
          return ((FixedRateAccrual) bean).getPikProjection();
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
   * The bean-builder for {@code FixedRateAccrual}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<FixedRateAccrual> {

    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate paymentDate;
    private double allInRate;
    private double pikSpread;
    private CurrencyAmount accrualAmount;
    private DayCount dayCount;
    private Frequency paymentFrequency;
    private CurrencyAmount paymentProjection;
    private CurrencyAmount pikProjection;

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
    private Builder(FixedRateAccrual beanToCopy) {
      this.startDate = beanToCopy.getStartDate();
      this.endDate = beanToCopy.getEndDate();
      this.paymentDate = beanToCopy.paymentDate;
      this.allInRate = beanToCopy.getAllInRate();
      this.pikSpread = beanToCopy.getPikSpread();
      this.accrualAmount = beanToCopy.getAccrualAmount();
      this.dayCount = beanToCopy.getDayCount();
      this.paymentFrequency = beanToCopy.getPaymentFrequency();
      this.paymentProjection = beanToCopy.getPaymentProjection();
      this.pikProjection = beanToCopy.getPikProjection();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case -2129778896:  // startDate
          return startDate;
        case -1607727319:  // endDate
          return endDate;
        case -1540873516:  // paymentDate
          return paymentDate;
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
        case -1540873516:  // paymentDate
          this.paymentDate = (LocalDate) newValue;
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
    public FixedRateAccrual build() {
      preBuild(this);
      return new FixedRateAccrual(
          startDate,
          endDate,
          paymentDate,
          allInRate,
          pikSpread,
          accrualAmount,
          dayCount,
          paymentFrequency,
          paymentProjection,
          pikProjection);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the start date of the accrual.
     * <p>
     * Interest accrues from this date.
     * @param startDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder startDate(LocalDate startDate) {
      JodaBeanUtils.notNull(startDate, "startDate");
      this.startDate = startDate;
      return this;
    }

    /**
     * Sets the end date of the accrual.
     * <p>
     * Interest accrues to this date.
     * @param endDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder endDate(LocalDate endDate) {
      JodaBeanUtils.notNull(endDate, "endDate");
      this.endDate = endDate;
      return this;
    }

    /**
     * Sets the payment date, optional.
     * @param paymentDate  the new value
     * @return this, for chaining, not null
     */
    public Builder paymentDate(LocalDate paymentDate) {
      this.paymentDate = paymentDate;
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
      ArgChecker.notNegative(allInRate, "allInRate");
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
      ArgChecker.notNegative(pikSpread, "pikSpread");
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
     * @param dayCount  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder dayCount(DayCount dayCount) {
      JodaBeanUtils.notNull(dayCount, "dayCount");
      this.dayCount = dayCount;
      return this;
    }

    /**
     * Sets frequency of accrual period.
     * @param paymentFrequency  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder paymentFrequency(Frequency paymentFrequency) {
      JodaBeanUtils.notNull(paymentFrequency, "paymentFrequency");
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

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(352);
      buf.append("FixedRateAccrual.Builder{");
      buf.append("startDate").append('=').append(JodaBeanUtils.toString(startDate)).append(',').append(' ');
      buf.append("endDate").append('=').append(JodaBeanUtils.toString(endDate)).append(',').append(' ');
      buf.append("paymentDate").append('=').append(JodaBeanUtils.toString(paymentDate)).append(',').append(' ');
      buf.append("allInRate").append('=').append(JodaBeanUtils.toString(allInRate)).append(',').append(' ');
      buf.append("pikSpread").append('=').append(JodaBeanUtils.toString(pikSpread)).append(',').append(' ');
      buf.append("accrualAmount").append('=').append(JodaBeanUtils.toString(accrualAmount)).append(',').append(' ');
      buf.append("dayCount").append('=').append(JodaBeanUtils.toString(dayCount)).append(',').append(' ');
      buf.append("paymentFrequency").append('=').append(JodaBeanUtils.toString(paymentFrequency)).append(',').append(' ');
      buf.append("paymentProjection").append('=').append(JodaBeanUtils.toString(paymentProjection)).append(',').append(' ');
      buf.append("pikProjection").append('=').append(JodaBeanUtils.toString(pikProjection));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
