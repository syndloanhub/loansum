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

import static com.syndloanhub.loansum.product.facility.FacilityType.Term;
import static com.syndloanhub.loansum.product.facility.LoanTradingType.Secondary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;

import org.joda.beans.ImmutableBean;
import org.joda.beans.JodaBeanUtils;
import org.joda.beans.MetaProperty;
import org.joda.beans.impl.direct.DirectFieldsBeanBuilder;
import org.joda.beans.impl.direct.DirectMetaBean;
import org.joda.beans.impl.direct.DirectMetaPropertyMap;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.ArgChecker;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeriesBuilder;
import com.opengamma.strata.product.ProductTrade;
import com.opengamma.strata.product.TradeInfo;
import com.opengamma.strata.product.common.BuySell;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.NonNegativeMoney;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTrade;

import org.joda.beans.Bean;
import org.joda.beans.impl.direct.DirectMetaProperty;
import org.joda.beans.MetaBean;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.ImmutableDefaults;
import org.joda.beans.gen.ImmutablePreBuild;
import org.joda.beans.gen.ImmutableValidator;
import org.joda.beans.gen.PropertyDefinition;

/**
 * A loan trade.
 * <p>
 * A loan trade represents the purchase or sale of a loan facility. The prorating of a loan
 * trade accounts just for paydown-on-trade-date behavior.
 */
@BeanDefinition
public final class LoanTrade implements ProductTrade, Proratable<ProratedLoanTrade>,
    FpMLExportable<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade>, ImmutableBean {

  /**
   * Prorate trade and associated global facility with itself. This is mainly for prorating the 
   * facility but the trade amount itself may be adjusted if there was a repayment on
   * trade date that needs to be recognized.
   */
  @Override
  public ProratedLoanTrade prorate(ProductTrade trade) {
    assert (trade == null || trade == this);

    // Adjust trade amount if necessary due to trade date repayment or adjustment.

    double adjustedAmount = getAmount();
    final Facility loan = getProduct();
    final LocalDate tradeDate = getInfo().getTradeDate().get();

    if ((isPaydownOnTradeDate() || isAdjustmentOnTradeDate()) && tradeDate.isAfter(loan.getStartDate())) {
      double commitmentOnTrade = loan.getCommitmentAmount(tradeDate).getAmount();
      double commitmentBeforeTrade = loan.getCommitmentAmount(tradeDate.minusDays(1)).getAmount();

      if (Math.abs(commitmentBeforeTrade - commitmentOnTrade) > 0)
        adjustedAmount *= commitmentOnTrade / commitmentBeforeTrade;
    }

    // Build penultimate trade used to prorate penultimate loan.

    LoanTrade penultimateTrade = LoanTrade.builder()
        .accrualSettlementType(accrualSettlementType)
        .amount(adjustedAmount)
        .association(association)
        .averageLibor(averageLibor)
        .buyer(buyer)
        .seller(seller)
        .buySell(buySell)
        .commitmentReductionCreditFlag(commitmentReductionCreditFlag)
        .currency(currency)
        .delayedCompensationFlag(delayedCompensationFlag)
        .documentationType(documentationType)
        .expectedSettlementDate(expectedSettlementDate)
        .paydownOnTradeDate(false)
        .formOfPurchase(formOfPurchase)
        .info(info)
        .price(price)
        .product(loan)
        .tradeType(tradeType)
        .build();

    // Return final trade with prorated loan.

    return ProratedLoanTrade.builder()
        .accrualSettlementType(accrualSettlementType)
        .association(association)
        .buyer(buyer)
        .seller(seller)
        .formOfPurchase(formOfPurchase)
        .documentationType(documentationType)
        .commitmentReductionCreditFlag(commitmentReductionCreditFlag)
        .currency(currency)
        .paydownOnTradeDate(paydownOnTradeDate)
        .buySell(buySell)
        .amount(adjustedAmount)
        .originalAmount(amount)
        .price(price)
        .expectedSettlementDate(expectedSettlementDate)
        .delayedCompensationFlag(delayedCompensationFlag)
        .averageLibor(averageLibor)
        .tradeType(tradeType)
        .info(info)
        .product(loan.prorate(penultimateTrade))
        .pctShare(penultimateTrade.getPctShare())
        .build();
  }

  @Override
  public com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade export() throws DatatypeConfigurationException {
    ObjectFactory factory = new ObjectFactory();
    com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade fpml = factory.createLoanTrade();
    
    // LoanTrade-level attributes.
    fpml.setPrice(BigDecimal.valueOf(price * 100.0));
    
    ////
    fpml.setAccrualSettlementType(accrualSettlementType.export());
    fpml.setAmount(FpMLHelper.exportCurrencyAmount(CurrencyAmount.of(currency, amount)));
    //fpml.setBuyerAccountReference(value);
    fpml.setTradeDate(DatatypeFactory.newInstance().newXMLGregorianCalendar(info.getTradeDate().get().toString()));
    fpml.setFormOfPurchase(FpMLHelper.convert(formOfPurchase));

    return fpml;
  }

  /**
   * The additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   */
  @PropertyDefinition(overrideGet = true)
  private final TradeInfo info;

  /**
   * The loan product that was agreed when the trade occurred.
   */
  @PropertyDefinition(validate = "notNull", overrideGet = true)
  private final Facility product;

  /**
   * Whether the trade is 'Buy' or 'Sell'.
   */
  @PropertyDefinition(validate = "notNull")
  private final BuySell buySell;

  /**
   * Buy counter party
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId buyer;

  /**
   * Sell counter party
   */
  @PropertyDefinition(validate = "notNull")
  private final StandardId seller;

  /**
   * The traded commitment amount.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegativeOrZero")
  private final double amount;

  /**
   * Trade currency.
   */
  @PropertyDefinition(validate = "")
  private final Currency currency;

  /**
   * The clean price of the trade.
   */
  @PropertyDefinition(validate = "ArgChecker.notNegative")
  private final double price;

  /**
   * The expected (legal) settlement date of the trade.
   */
  @PropertyDefinition(validate = "notNull")
  private final LocalDate expectedSettlementDate;

  /**
   * Waive delayed compensation flag.
   */
  @PropertyDefinition(validate = "")
  private final boolean delayedCompensationFlag;

  /**
   * Governing loan association, e.g. LSTA, LMA
   */
  @PropertyDefinition(validate = "")
  private final LoanTradingAssoc association;

  /**
   * Form of purchase, e.g. assignment
   */
  @PropertyDefinition(validate = "")
  private final LoanTradingFormOfPurchase formOfPurchase;

  /**
   * Documentation type, e.g. par vs distressed
   */
  @PropertyDefinition(validate = "")
  private final LoanTradingDocType documentationType;

  /**
   * Trade type, e.g. secondary vs primary
   */
  @PropertyDefinition(validate = "")
  private final LoanTradingType tradeType;

  /**
   * A flag to indicate the dependency of a secondary market loan trade upon 
   * the closing of a primary market loan structuring and syndication process.
   */
  @PropertyDefinition(validate = "")
  private final boolean whenIssuedFlag;

  /**
   * Flag to waive CR credit.
   */
  @PropertyDefinition(validate = "")
  private final boolean commitmentReductionCreditFlag;

  /**
   * Flag to recognize repayment on trade date.
   */
  @PropertyDefinition(validate = "")
  private final boolean paydownOnTradeDate;

  /**
   * Flag to adjust amount for commitment adjustment occurring on trade date.
   */
  @PropertyDefinition(validate = "")
  private final boolean adjustmentOnTradeDate;

  /**
   * Settlement accrued interest treatment, e.g. SettlesWithoutAccrued.
   */
  @PropertyDefinition(validate = "")
  private final LoanTradingAccrualSettlement accrualSettlementType;

  /**
   * Average LIBOR fixing.
   */
  @PropertyDefinition(validate = "")
  private final double averageLibor;

  /**
   * Calculated series of prorated share of global facility.
   */
  @PropertyDefinition(validate = "")
  private final LocalDateDoubleTimeSeries pctShare;

  /**
   * Complete construction of LoanTrade percentage share series.
   * 
   * @param builder
   */
  @ImmutablePreBuild
  private static void preBuild(Builder builder) {
    LocalDateDoubleTimeSeriesBuilder pctShareBuilder = LocalDateDoubleTimeSeries.builder();
    LocalDate tradeDate = builder.info.getTradeDate().get();

    pctShareBuilder.put(tradeDate, builder.amount / builder.product.getCommitmentAmount(tradeDate).getAmount());

    if (builder.product.getFacilityType() == Term && builder.product.getEvents() != null) {
      for (FacilityEvent event : builder.product.getEvents()) {
        switch (event.getType()) {
          case CommitmentAdjustmentEvent:
            CommitmentAdjustment adjustment = (CommitmentAdjustment) event;

            if (!adjustment.getEffectiveDate().isBefore(tradeDate) && !adjustment.isPik() && adjustment.isRefusalAllowed()) {
              double lastPctShare = pctShareBuilder.build().getLatestValue();
              double lastCommitmentAmount =
                  builder.product.getCommitmentAmount(event.getEffectiveDate().minusDays(1)).getAmount();
              double newCommmitmentAmount = builder.product.getCommitmentAmount(event.getEffectiveDate()).getAmount();
              double newPctShare = lastPctShare * lastCommitmentAmount / newCommmitmentAmount;

              pctShareBuilder.put(event.getEffectiveDate(), newPctShare);
            }
            break;
        }
      }
    }

    builder.pctShare(pctShareBuilder.build());
  }

  /**
   * Defaulted values.
   * 
   * @param builder
   */
  @ImmutableDefaults
  private static void applyDefaults(Builder builder) {
    builder
        .info(TradeInfo.empty())
        .delayedCompensationFlag(true)
        .adjustmentOnTradeDate(false)
        .tradeType(Secondary)
        .whenIssuedFlag(false)
        .buyer(StandardId.of("cpty", "BUYER"))
        .seller(StandardId.of("cpty", "SELLER"));
  }

  /**
   * Trade validation.
   */
  @ImmutableValidator
  private void validate() {
    ArgChecker.isTrue(info.getTradeDate().isPresent(), "Facility trade must have a trade date");
    ArgChecker.isFalse(info.getTradeDate().get().isBefore(getProduct().getStartDate()),
        "Trade date cannot be before facility start date");
    ArgChecker.isFalse(info.getTradeDate().get().isAfter(getProduct().getMaturityDate()),
        "Trade date cannot be after facility maturity date");
    ArgChecker.inRangeInclusive(price, 0, 2, "Trade price must be between 0 and 2 (normalized)");
  }

  /* (non-Javadoc)
   * @see com.opengamma.strata.product.ProductTrade#withInfo(com.opengamma.strata.product.TradeInfo)
   */
  @Override
  public ProductTrade withInfo(TradeInfo info) {
    return builder()
        .accrualSettlementType(accrualSettlementType)
        .adjustmentOnTradeDate(adjustmentOnTradeDate)
        .amount(amount)
        .association(association)
        .averageLibor(averageLibor)
        .buyer(buyer)
        .buySell(buySell)
        .commitmentReductionCreditFlag(commitmentReductionCreditFlag)
        .currency(currency)
        .delayedCompensationFlag(delayedCompensationFlag)
        .documentationType(documentationType)
        .expectedSettlementDate(expectedSettlementDate)
        .formOfPurchase(formOfPurchase)
        .info(info)
        .paydownOnTradeDate(paydownOnTradeDate)
        .pctShare(pctShare)
        .price(price)
        .product(product)
        .seller(seller)
        .tradeType(tradeType)
        .whenIssuedFlag(whenIssuedFlag)
        .build();
  }

  //------------------------- AUTOGENERATED START -------------------------
  /**
   * The meta-bean for {@code LoanTrade}.
   * @return the meta-bean, not null
   */
  public static LoanTrade.Meta meta() {
    return LoanTrade.Meta.INSTANCE;
  }

  static {
    MetaBean.register(LoanTrade.Meta.INSTANCE);
  }

  /**
   * Returns a builder used to create an instance of the bean.
   * @return the builder, not null
   */
  public static LoanTrade.Builder builder() {
    return new LoanTrade.Builder();
  }

  private LoanTrade(
      TradeInfo info,
      Facility product,
      BuySell buySell,
      StandardId buyer,
      StandardId seller,
      double amount,
      Currency currency,
      double price,
      LocalDate expectedSettlementDate,
      boolean delayedCompensationFlag,
      LoanTradingAssoc association,
      LoanTradingFormOfPurchase formOfPurchase,
      LoanTradingDocType documentationType,
      LoanTradingType tradeType,
      boolean whenIssuedFlag,
      boolean commitmentReductionCreditFlag,
      boolean paydownOnTradeDate,
      boolean adjustmentOnTradeDate,
      LoanTradingAccrualSettlement accrualSettlementType,
      double averageLibor,
      LocalDateDoubleTimeSeries pctShare) {
    JodaBeanUtils.notNull(product, "product");
    JodaBeanUtils.notNull(buySell, "buySell");
    JodaBeanUtils.notNull(buyer, "buyer");
    JodaBeanUtils.notNull(seller, "seller");
    ArgChecker.notNegativeOrZero(amount, "amount");
    ArgChecker.notNegative(price, "price");
    JodaBeanUtils.notNull(expectedSettlementDate, "expectedSettlementDate");
    this.info = info;
    this.product = product;
    this.buySell = buySell;
    this.buyer = buyer;
    this.seller = seller;
    this.amount = amount;
    this.currency = currency;
    this.price = price;
    this.expectedSettlementDate = expectedSettlementDate;
    this.delayedCompensationFlag = delayedCompensationFlag;
    this.association = association;
    this.formOfPurchase = formOfPurchase;
    this.documentationType = documentationType;
    this.tradeType = tradeType;
    this.whenIssuedFlag = whenIssuedFlag;
    this.commitmentReductionCreditFlag = commitmentReductionCreditFlag;
    this.paydownOnTradeDate = paydownOnTradeDate;
    this.adjustmentOnTradeDate = adjustmentOnTradeDate;
    this.accrualSettlementType = accrualSettlementType;
    this.averageLibor = averageLibor;
    this.pctShare = pctShare;
    validate();
  }

  @Override
  public LoanTrade.Meta metaBean() {
    return LoanTrade.Meta.INSTANCE;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the additional trade information, defaulted to an empty instance.
   * <p>
   * This allows additional information to be attached to the trade.
   * @return the value of the property
   */
  @Override
  public TradeInfo getInfo() {
    return info;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the loan product that was agreed when the trade occurred.
   * @return the value of the property, not null
   */
  @Override
  public Facility getProduct() {
    return product;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets whether the trade is 'Buy' or 'Sell'.
   * @return the value of the property, not null
   */
  public BuySell getBuySell() {
    return buySell;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets buy counter party
   * @return the value of the property, not null
   */
  public StandardId getBuyer() {
    return buyer;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets sell counter party
   * @return the value of the property, not null
   */
  public StandardId getSeller() {
    return seller;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the traded commitment amount.
   * @return the value of the property
   */
  public double getAmount() {
    return amount;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets trade currency.
   * @return the value of the property
   */
  public Currency getCurrency() {
    return currency;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the clean price of the trade.
   * @return the value of the property
   */
  public double getPrice() {
    return price;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets the expected (legal) settlement date of the trade.
   * @return the value of the property, not null
   */
  public LocalDate getExpectedSettlementDate() {
    return expectedSettlementDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets waive delayed compensation flag.
   * @return the value of the property
   */
  public boolean isDelayedCompensationFlag() {
    return delayedCompensationFlag;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets governing loan association, e.g. LSTA, LMA
   * @return the value of the property
   */
  public LoanTradingAssoc getAssociation() {
    return association;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets form of purchase, e.g. assignment
   * @return the value of the property
   */
  public LoanTradingFormOfPurchase getFormOfPurchase() {
    return formOfPurchase;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets documentation type, e.g. par vs distressed
   * @return the value of the property
   */
  public LoanTradingDocType getDocumentationType() {
    return documentationType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets trade type, e.g. secondary vs primary
   * @return the value of the property
   */
  public LoanTradingType getTradeType() {
    return tradeType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets a flag to indicate the dependency of a secondary market loan trade upon
   * the closing of a primary market loan structuring and syndication process.
   * @return the value of the property
   */
  public boolean isWhenIssuedFlag() {
    return whenIssuedFlag;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets flag to waive CR credit.
   * @return the value of the property
   */
  public boolean isCommitmentReductionCreditFlag() {
    return commitmentReductionCreditFlag;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets flag to recognize repayment on trade date.
   * @return the value of the property
   */
  public boolean isPaydownOnTradeDate() {
    return paydownOnTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets flag to adjust amount for commitment adjustment occurring on trade date.
   * @return the value of the property
   */
  public boolean isAdjustmentOnTradeDate() {
    return adjustmentOnTradeDate;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets settlement accrued interest treatment, e.g. SettlesWithoutAccrued.
   * @return the value of the property
   */
  public LoanTradingAccrualSettlement getAccrualSettlementType() {
    return accrualSettlementType;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets average LIBOR fixing.
   * @return the value of the property
   */
  public double getAverageLibor() {
    return averageLibor;
  }

  //-----------------------------------------------------------------------
  /**
   * Gets calculated series of prorated share of global facility.
   * @return the value of the property
   */
  public LocalDateDoubleTimeSeries getPctShare() {
    return pctShare;
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
      LoanTrade other = (LoanTrade) obj;
      return JodaBeanUtils.equal(info, other.info) &&
          JodaBeanUtils.equal(product, other.product) &&
          JodaBeanUtils.equal(buySell, other.buySell) &&
          JodaBeanUtils.equal(buyer, other.buyer) &&
          JodaBeanUtils.equal(seller, other.seller) &&
          JodaBeanUtils.equal(amount, other.amount) &&
          JodaBeanUtils.equal(currency, other.currency) &&
          JodaBeanUtils.equal(price, other.price) &&
          JodaBeanUtils.equal(expectedSettlementDate, other.expectedSettlementDate) &&
          (delayedCompensationFlag == other.delayedCompensationFlag) &&
          JodaBeanUtils.equal(association, other.association) &&
          JodaBeanUtils.equal(formOfPurchase, other.formOfPurchase) &&
          JodaBeanUtils.equal(documentationType, other.documentationType) &&
          JodaBeanUtils.equal(tradeType, other.tradeType) &&
          (whenIssuedFlag == other.whenIssuedFlag) &&
          (commitmentReductionCreditFlag == other.commitmentReductionCreditFlag) &&
          (paydownOnTradeDate == other.paydownOnTradeDate) &&
          (adjustmentOnTradeDate == other.adjustmentOnTradeDate) &&
          JodaBeanUtils.equal(accrualSettlementType, other.accrualSettlementType) &&
          JodaBeanUtils.equal(averageLibor, other.averageLibor) &&
          JodaBeanUtils.equal(pctShare, other.pctShare);
    }
    return false;
  }

  @Override
  public int hashCode() {
    int hash = getClass().hashCode();
    hash = hash * 31 + JodaBeanUtils.hashCode(info);
    hash = hash * 31 + JodaBeanUtils.hashCode(product);
    hash = hash * 31 + JodaBeanUtils.hashCode(buySell);
    hash = hash * 31 + JodaBeanUtils.hashCode(buyer);
    hash = hash * 31 + JodaBeanUtils.hashCode(seller);
    hash = hash * 31 + JodaBeanUtils.hashCode(amount);
    hash = hash * 31 + JodaBeanUtils.hashCode(currency);
    hash = hash * 31 + JodaBeanUtils.hashCode(price);
    hash = hash * 31 + JodaBeanUtils.hashCode(expectedSettlementDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(delayedCompensationFlag);
    hash = hash * 31 + JodaBeanUtils.hashCode(association);
    hash = hash * 31 + JodaBeanUtils.hashCode(formOfPurchase);
    hash = hash * 31 + JodaBeanUtils.hashCode(documentationType);
    hash = hash * 31 + JodaBeanUtils.hashCode(tradeType);
    hash = hash * 31 + JodaBeanUtils.hashCode(whenIssuedFlag);
    hash = hash * 31 + JodaBeanUtils.hashCode(commitmentReductionCreditFlag);
    hash = hash * 31 + JodaBeanUtils.hashCode(paydownOnTradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(adjustmentOnTradeDate);
    hash = hash * 31 + JodaBeanUtils.hashCode(accrualSettlementType);
    hash = hash * 31 + JodaBeanUtils.hashCode(averageLibor);
    hash = hash * 31 + JodaBeanUtils.hashCode(pctShare);
    return hash;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder(704);
    buf.append("LoanTrade{");
    buf.append("info").append('=').append(info).append(',').append(' ');
    buf.append("product").append('=').append(product).append(',').append(' ');
    buf.append("buySell").append('=').append(buySell).append(',').append(' ');
    buf.append("buyer").append('=').append(buyer).append(',').append(' ');
    buf.append("seller").append('=').append(seller).append(',').append(' ');
    buf.append("amount").append('=').append(amount).append(',').append(' ');
    buf.append("currency").append('=').append(currency).append(',').append(' ');
    buf.append("price").append('=').append(price).append(',').append(' ');
    buf.append("expectedSettlementDate").append('=').append(expectedSettlementDate).append(',').append(' ');
    buf.append("delayedCompensationFlag").append('=').append(delayedCompensationFlag).append(',').append(' ');
    buf.append("association").append('=').append(association).append(',').append(' ');
    buf.append("formOfPurchase").append('=').append(formOfPurchase).append(',').append(' ');
    buf.append("documentationType").append('=').append(documentationType).append(',').append(' ');
    buf.append("tradeType").append('=').append(tradeType).append(',').append(' ');
    buf.append("whenIssuedFlag").append('=').append(whenIssuedFlag).append(',').append(' ');
    buf.append("commitmentReductionCreditFlag").append('=').append(commitmentReductionCreditFlag).append(',').append(' ');
    buf.append("paydownOnTradeDate").append('=').append(paydownOnTradeDate).append(',').append(' ');
    buf.append("adjustmentOnTradeDate").append('=').append(adjustmentOnTradeDate).append(',').append(' ');
    buf.append("accrualSettlementType").append('=').append(accrualSettlementType).append(',').append(' ');
    buf.append("averageLibor").append('=').append(averageLibor).append(',').append(' ');
    buf.append("pctShare").append('=').append(JodaBeanUtils.toString(pctShare));
    buf.append('}');
    return buf.toString();
  }

  //-----------------------------------------------------------------------
  /**
   * The meta-bean for {@code LoanTrade}.
   */
  public static final class Meta extends DirectMetaBean {
    /**
     * The singleton instance of the meta-bean.
     */
    static final Meta INSTANCE = new Meta();

    /**
     * The meta-property for the {@code info} property.
     */
    private final MetaProperty<TradeInfo> _info = DirectMetaProperty.ofImmutable(
        this, "info", LoanTrade.class, TradeInfo.class);
    /**
     * The meta-property for the {@code product} property.
     */
    private final MetaProperty<Facility> _product = DirectMetaProperty.ofImmutable(
        this, "product", LoanTrade.class, Facility.class);
    /**
     * The meta-property for the {@code buySell} property.
     */
    private final MetaProperty<BuySell> _buySell = DirectMetaProperty.ofImmutable(
        this, "buySell", LoanTrade.class, BuySell.class);
    /**
     * The meta-property for the {@code buyer} property.
     */
    private final MetaProperty<StandardId> _buyer = DirectMetaProperty.ofImmutable(
        this, "buyer", LoanTrade.class, StandardId.class);
    /**
     * The meta-property for the {@code seller} property.
     */
    private final MetaProperty<StandardId> _seller = DirectMetaProperty.ofImmutable(
        this, "seller", LoanTrade.class, StandardId.class);
    /**
     * The meta-property for the {@code amount} property.
     */
    private final MetaProperty<Double> _amount = DirectMetaProperty.ofImmutable(
        this, "amount", LoanTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code currency} property.
     */
    private final MetaProperty<Currency> _currency = DirectMetaProperty.ofImmutable(
        this, "currency", LoanTrade.class, Currency.class);
    /**
     * The meta-property for the {@code price} property.
     */
    private final MetaProperty<Double> _price = DirectMetaProperty.ofImmutable(
        this, "price", LoanTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code expectedSettlementDate} property.
     */
    private final MetaProperty<LocalDate> _expectedSettlementDate = DirectMetaProperty.ofImmutable(
        this, "expectedSettlementDate", LoanTrade.class, LocalDate.class);
    /**
     * The meta-property for the {@code delayedCompensationFlag} property.
     */
    private final MetaProperty<Boolean> _delayedCompensationFlag = DirectMetaProperty.ofImmutable(
        this, "delayedCompensationFlag", LoanTrade.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code association} property.
     */
    private final MetaProperty<LoanTradingAssoc> _association = DirectMetaProperty.ofImmutable(
        this, "association", LoanTrade.class, LoanTradingAssoc.class);
    /**
     * The meta-property for the {@code formOfPurchase} property.
     */
    private final MetaProperty<LoanTradingFormOfPurchase> _formOfPurchase = DirectMetaProperty.ofImmutable(
        this, "formOfPurchase", LoanTrade.class, LoanTradingFormOfPurchase.class);
    /**
     * The meta-property for the {@code documentationType} property.
     */
    private final MetaProperty<LoanTradingDocType> _documentationType = DirectMetaProperty.ofImmutable(
        this, "documentationType", LoanTrade.class, LoanTradingDocType.class);
    /**
     * The meta-property for the {@code tradeType} property.
     */
    private final MetaProperty<LoanTradingType> _tradeType = DirectMetaProperty.ofImmutable(
        this, "tradeType", LoanTrade.class, LoanTradingType.class);
    /**
     * The meta-property for the {@code whenIssuedFlag} property.
     */
    private final MetaProperty<Boolean> _whenIssuedFlag = DirectMetaProperty.ofImmutable(
        this, "whenIssuedFlag", LoanTrade.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code commitmentReductionCreditFlag} property.
     */
    private final MetaProperty<Boolean> _commitmentReductionCreditFlag = DirectMetaProperty.ofImmutable(
        this, "commitmentReductionCreditFlag", LoanTrade.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code paydownOnTradeDate} property.
     */
    private final MetaProperty<Boolean> _paydownOnTradeDate = DirectMetaProperty.ofImmutable(
        this, "paydownOnTradeDate", LoanTrade.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code adjustmentOnTradeDate} property.
     */
    private final MetaProperty<Boolean> _adjustmentOnTradeDate = DirectMetaProperty.ofImmutable(
        this, "adjustmentOnTradeDate", LoanTrade.class, Boolean.TYPE);
    /**
     * The meta-property for the {@code accrualSettlementType} property.
     */
    private final MetaProperty<LoanTradingAccrualSettlement> _accrualSettlementType = DirectMetaProperty.ofImmutable(
        this, "accrualSettlementType", LoanTrade.class, LoanTradingAccrualSettlement.class);
    /**
     * The meta-property for the {@code averageLibor} property.
     */
    private final MetaProperty<Double> _averageLibor = DirectMetaProperty.ofImmutable(
        this, "averageLibor", LoanTrade.class, Double.TYPE);
    /**
     * The meta-property for the {@code pctShare} property.
     */
    private final MetaProperty<LocalDateDoubleTimeSeries> _pctShare = DirectMetaProperty.ofImmutable(
        this, "pctShare", LoanTrade.class, LocalDateDoubleTimeSeries.class);
    /**
     * The meta-properties.
     */
    private final Map<String, MetaProperty<?>> _metaPropertyMap$ = new DirectMetaPropertyMap(
        this, null,
        "info",
        "product",
        "buySell",
        "buyer",
        "seller",
        "amount",
        "currency",
        "price",
        "expectedSettlementDate",
        "delayedCompensationFlag",
        "association",
        "formOfPurchase",
        "documentationType",
        "tradeType",
        "whenIssuedFlag",
        "commitmentReductionCreditFlag",
        "paydownOnTradeDate",
        "adjustmentOnTradeDate",
        "accrualSettlementType",
        "averageLibor",
        "pctShare");

    /**
     * Restricted constructor.
     */
    private Meta() {
    }

    @Override
    protected MetaProperty<?> metaPropertyGet(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return _info;
        case -309474065:  // product
          return _product;
        case 244977400:  // buySell
          return _buySell;
        case 94110131:  // buyer
          return _buyer;
        case -906014849:  // seller
          return _seller;
        case -1413853096:  // amount
          return _amount;
        case 575402001:  // currency
          return _currency;
        case 106934601:  // price
          return _price;
        case -775675569:  // expectedSettlementDate
          return _expectedSettlementDate;
        case 1658211432:  // delayedCompensationFlag
          return _delayedCompensationFlag;
        case -87499647:  // association
          return _association;
        case 492532700:  // formOfPurchase
          return _formOfPurchase;
        case 1128397076:  // documentationType
          return _documentationType;
        case 752919230:  // tradeType
          return _tradeType;
        case -1279664239:  // whenIssuedFlag
          return _whenIssuedFlag;
        case -1598421117:  // commitmentReductionCreditFlag
          return _commitmentReductionCreditFlag;
        case -223619991:  // paydownOnTradeDate
          return _paydownOnTradeDate;
        case -1728953242:  // adjustmentOnTradeDate
          return _adjustmentOnTradeDate;
        case 1552442258:  // accrualSettlementType
          return _accrualSettlementType;
        case 1222287115:  // averageLibor
          return _averageLibor;
        case -1304358018:  // pctShare
          return _pctShare;
      }
      return super.metaPropertyGet(propertyName);
    }

    @Override
    public LoanTrade.Builder builder() {
      return new LoanTrade.Builder();
    }

    @Override
    public Class<? extends LoanTrade> beanType() {
      return LoanTrade.class;
    }

    @Override
    public Map<String, MetaProperty<?>> metaPropertyMap() {
      return _metaPropertyMap$;
    }

    //-----------------------------------------------------------------------
    /**
     * The meta-property for the {@code info} property.
     * @return the meta-property, not null
     */
    public MetaProperty<TradeInfo> info() {
      return _info;
    }

    /**
     * The meta-property for the {@code product} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Facility> product() {
      return _product;
    }

    /**
     * The meta-property for the {@code buySell} property.
     * @return the meta-property, not null
     */
    public MetaProperty<BuySell> buySell() {
      return _buySell;
    }

    /**
     * The meta-property for the {@code buyer} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> buyer() {
      return _buyer;
    }

    /**
     * The meta-property for the {@code seller} property.
     * @return the meta-property, not null
     */
    public MetaProperty<StandardId> seller() {
      return _seller;
    }

    /**
     * The meta-property for the {@code amount} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> amount() {
      return _amount;
    }

    /**
     * The meta-property for the {@code currency} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Currency> currency() {
      return _currency;
    }

    /**
     * The meta-property for the {@code price} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> price() {
      return _price;
    }

    /**
     * The meta-property for the {@code expectedSettlementDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDate> expectedSettlementDate() {
      return _expectedSettlementDate;
    }

    /**
     * The meta-property for the {@code delayedCompensationFlag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> delayedCompensationFlag() {
      return _delayedCompensationFlag;
    }

    /**
     * The meta-property for the {@code association} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LoanTradingAssoc> association() {
      return _association;
    }

    /**
     * The meta-property for the {@code formOfPurchase} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LoanTradingFormOfPurchase> formOfPurchase() {
      return _formOfPurchase;
    }

    /**
     * The meta-property for the {@code documentationType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LoanTradingDocType> documentationType() {
      return _documentationType;
    }

    /**
     * The meta-property for the {@code tradeType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LoanTradingType> tradeType() {
      return _tradeType;
    }

    /**
     * The meta-property for the {@code whenIssuedFlag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> whenIssuedFlag() {
      return _whenIssuedFlag;
    }

    /**
     * The meta-property for the {@code commitmentReductionCreditFlag} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> commitmentReductionCreditFlag() {
      return _commitmentReductionCreditFlag;
    }

    /**
     * The meta-property for the {@code paydownOnTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> paydownOnTradeDate() {
      return _paydownOnTradeDate;
    }

    /**
     * The meta-property for the {@code adjustmentOnTradeDate} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Boolean> adjustmentOnTradeDate() {
      return _adjustmentOnTradeDate;
    }

    /**
     * The meta-property for the {@code accrualSettlementType} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LoanTradingAccrualSettlement> accrualSettlementType() {
      return _accrualSettlementType;
    }

    /**
     * The meta-property for the {@code averageLibor} property.
     * @return the meta-property, not null
     */
    public MetaProperty<Double> averageLibor() {
      return _averageLibor;
    }

    /**
     * The meta-property for the {@code pctShare} property.
     * @return the meta-property, not null
     */
    public MetaProperty<LocalDateDoubleTimeSeries> pctShare() {
      return _pctShare;
    }

    //-----------------------------------------------------------------------
    @Override
    protected Object propertyGet(Bean bean, String propertyName, boolean quiet) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return ((LoanTrade) bean).getInfo();
        case -309474065:  // product
          return ((LoanTrade) bean).getProduct();
        case 244977400:  // buySell
          return ((LoanTrade) bean).getBuySell();
        case 94110131:  // buyer
          return ((LoanTrade) bean).getBuyer();
        case -906014849:  // seller
          return ((LoanTrade) bean).getSeller();
        case -1413853096:  // amount
          return ((LoanTrade) bean).getAmount();
        case 575402001:  // currency
          return ((LoanTrade) bean).getCurrency();
        case 106934601:  // price
          return ((LoanTrade) bean).getPrice();
        case -775675569:  // expectedSettlementDate
          return ((LoanTrade) bean).getExpectedSettlementDate();
        case 1658211432:  // delayedCompensationFlag
          return ((LoanTrade) bean).isDelayedCompensationFlag();
        case -87499647:  // association
          return ((LoanTrade) bean).getAssociation();
        case 492532700:  // formOfPurchase
          return ((LoanTrade) bean).getFormOfPurchase();
        case 1128397076:  // documentationType
          return ((LoanTrade) bean).getDocumentationType();
        case 752919230:  // tradeType
          return ((LoanTrade) bean).getTradeType();
        case -1279664239:  // whenIssuedFlag
          return ((LoanTrade) bean).isWhenIssuedFlag();
        case -1598421117:  // commitmentReductionCreditFlag
          return ((LoanTrade) bean).isCommitmentReductionCreditFlag();
        case -223619991:  // paydownOnTradeDate
          return ((LoanTrade) bean).isPaydownOnTradeDate();
        case -1728953242:  // adjustmentOnTradeDate
          return ((LoanTrade) bean).isAdjustmentOnTradeDate();
        case 1552442258:  // accrualSettlementType
          return ((LoanTrade) bean).getAccrualSettlementType();
        case 1222287115:  // averageLibor
          return ((LoanTrade) bean).getAverageLibor();
        case -1304358018:  // pctShare
          return ((LoanTrade) bean).getPctShare();
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
   * The bean-builder for {@code LoanTrade}.
   */
  public static final class Builder extends DirectFieldsBeanBuilder<LoanTrade> {

    private TradeInfo info;
    private Facility product;
    private BuySell buySell;
    private StandardId buyer;
    private StandardId seller;
    private double amount;
    private Currency currency;
    private double price;
    private LocalDate expectedSettlementDate;
    private boolean delayedCompensationFlag;
    private LoanTradingAssoc association;
    private LoanTradingFormOfPurchase formOfPurchase;
    private LoanTradingDocType documentationType;
    private LoanTradingType tradeType;
    private boolean whenIssuedFlag;
    private boolean commitmentReductionCreditFlag;
    private boolean paydownOnTradeDate;
    private boolean adjustmentOnTradeDate;
    private LoanTradingAccrualSettlement accrualSettlementType;
    private double averageLibor;
    private LocalDateDoubleTimeSeries pctShare;

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
    private Builder(LoanTrade beanToCopy) {
      this.info = beanToCopy.getInfo();
      this.product = beanToCopy.getProduct();
      this.buySell = beanToCopy.getBuySell();
      this.buyer = beanToCopy.getBuyer();
      this.seller = beanToCopy.getSeller();
      this.amount = beanToCopy.getAmount();
      this.currency = beanToCopy.getCurrency();
      this.price = beanToCopy.getPrice();
      this.expectedSettlementDate = beanToCopy.getExpectedSettlementDate();
      this.delayedCompensationFlag = beanToCopy.isDelayedCompensationFlag();
      this.association = beanToCopy.getAssociation();
      this.formOfPurchase = beanToCopy.getFormOfPurchase();
      this.documentationType = beanToCopy.getDocumentationType();
      this.tradeType = beanToCopy.getTradeType();
      this.whenIssuedFlag = beanToCopy.isWhenIssuedFlag();
      this.commitmentReductionCreditFlag = beanToCopy.isCommitmentReductionCreditFlag();
      this.paydownOnTradeDate = beanToCopy.isPaydownOnTradeDate();
      this.adjustmentOnTradeDate = beanToCopy.isAdjustmentOnTradeDate();
      this.accrualSettlementType = beanToCopy.getAccrualSettlementType();
      this.averageLibor = beanToCopy.getAverageLibor();
      this.pctShare = beanToCopy.getPctShare();
    }

    //-----------------------------------------------------------------------
    @Override
    public Object get(String propertyName) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          return info;
        case -309474065:  // product
          return product;
        case 244977400:  // buySell
          return buySell;
        case 94110131:  // buyer
          return buyer;
        case -906014849:  // seller
          return seller;
        case -1413853096:  // amount
          return amount;
        case 575402001:  // currency
          return currency;
        case 106934601:  // price
          return price;
        case -775675569:  // expectedSettlementDate
          return expectedSettlementDate;
        case 1658211432:  // delayedCompensationFlag
          return delayedCompensationFlag;
        case -87499647:  // association
          return association;
        case 492532700:  // formOfPurchase
          return formOfPurchase;
        case 1128397076:  // documentationType
          return documentationType;
        case 752919230:  // tradeType
          return tradeType;
        case -1279664239:  // whenIssuedFlag
          return whenIssuedFlag;
        case -1598421117:  // commitmentReductionCreditFlag
          return commitmentReductionCreditFlag;
        case -223619991:  // paydownOnTradeDate
          return paydownOnTradeDate;
        case -1728953242:  // adjustmentOnTradeDate
          return adjustmentOnTradeDate;
        case 1552442258:  // accrualSettlementType
          return accrualSettlementType;
        case 1222287115:  // averageLibor
          return averageLibor;
        case -1304358018:  // pctShare
          return pctShare;
        default:
          throw new NoSuchElementException("Unknown property: " + propertyName);
      }
    }

    @Override
    public Builder set(String propertyName, Object newValue) {
      switch (propertyName.hashCode()) {
        case 3237038:  // info
          this.info = (TradeInfo) newValue;
          break;
        case -309474065:  // product
          this.product = (Facility) newValue;
          break;
        case 244977400:  // buySell
          this.buySell = (BuySell) newValue;
          break;
        case 94110131:  // buyer
          this.buyer = (StandardId) newValue;
          break;
        case -906014849:  // seller
          this.seller = (StandardId) newValue;
          break;
        case -1413853096:  // amount
          this.amount = (Double) newValue;
          break;
        case 575402001:  // currency
          this.currency = (Currency) newValue;
          break;
        case 106934601:  // price
          this.price = (Double) newValue;
          break;
        case -775675569:  // expectedSettlementDate
          this.expectedSettlementDate = (LocalDate) newValue;
          break;
        case 1658211432:  // delayedCompensationFlag
          this.delayedCompensationFlag = (Boolean) newValue;
          break;
        case -87499647:  // association
          this.association = (LoanTradingAssoc) newValue;
          break;
        case 492532700:  // formOfPurchase
          this.formOfPurchase = (LoanTradingFormOfPurchase) newValue;
          break;
        case 1128397076:  // documentationType
          this.documentationType = (LoanTradingDocType) newValue;
          break;
        case 752919230:  // tradeType
          this.tradeType = (LoanTradingType) newValue;
          break;
        case -1279664239:  // whenIssuedFlag
          this.whenIssuedFlag = (Boolean) newValue;
          break;
        case -1598421117:  // commitmentReductionCreditFlag
          this.commitmentReductionCreditFlag = (Boolean) newValue;
          break;
        case -223619991:  // paydownOnTradeDate
          this.paydownOnTradeDate = (Boolean) newValue;
          break;
        case -1728953242:  // adjustmentOnTradeDate
          this.adjustmentOnTradeDate = (Boolean) newValue;
          break;
        case 1552442258:  // accrualSettlementType
          this.accrualSettlementType = (LoanTradingAccrualSettlement) newValue;
          break;
        case 1222287115:  // averageLibor
          this.averageLibor = (Double) newValue;
          break;
        case -1304358018:  // pctShare
          this.pctShare = (LocalDateDoubleTimeSeries) newValue;
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
    public LoanTrade build() {
      preBuild(this);
      return new LoanTrade(
          info,
          product,
          buySell,
          buyer,
          seller,
          amount,
          currency,
          price,
          expectedSettlementDate,
          delayedCompensationFlag,
          association,
          formOfPurchase,
          documentationType,
          tradeType,
          whenIssuedFlag,
          commitmentReductionCreditFlag,
          paydownOnTradeDate,
          adjustmentOnTradeDate,
          accrualSettlementType,
          averageLibor,
          pctShare);
    }

    //-----------------------------------------------------------------------
    /**
     * Sets the additional trade information, defaulted to an empty instance.
     * <p>
     * This allows additional information to be attached to the trade.
     * @param info  the new value
     * @return this, for chaining, not null
     */
    public Builder info(TradeInfo info) {
      this.info = info;
      return this;
    }

    /**
     * Sets the loan product that was agreed when the trade occurred.
     * @param product  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder product(Facility product) {
      JodaBeanUtils.notNull(product, "product");
      this.product = product;
      return this;
    }

    /**
     * Sets whether the trade is 'Buy' or 'Sell'.
     * @param buySell  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buySell(BuySell buySell) {
      JodaBeanUtils.notNull(buySell, "buySell");
      this.buySell = buySell;
      return this;
    }

    /**
     * Sets buy counter party
     * @param buyer  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder buyer(StandardId buyer) {
      JodaBeanUtils.notNull(buyer, "buyer");
      this.buyer = buyer;
      return this;
    }

    /**
     * Sets sell counter party
     * @param seller  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder seller(StandardId seller) {
      JodaBeanUtils.notNull(seller, "seller");
      this.seller = seller;
      return this;
    }

    /**
     * Sets the traded commitment amount.
     * @param amount  the new value
     * @return this, for chaining, not null
     */
    public Builder amount(double amount) {
      ArgChecker.notNegativeOrZero(amount, "amount");
      this.amount = amount;
      return this;
    }

    /**
     * Sets trade currency.
     * @param currency  the new value
     * @return this, for chaining, not null
     */
    public Builder currency(Currency currency) {
      this.currency = currency;
      return this;
    }

    /**
     * Sets the clean price of the trade.
     * @param price  the new value
     * @return this, for chaining, not null
     */
    public Builder price(double price) {
      ArgChecker.notNegative(price, "price");
      this.price = price;
      return this;
    }

    /**
     * Sets the expected (legal) settlement date of the trade.
     * @param expectedSettlementDate  the new value, not null
     * @return this, for chaining, not null
     */
    public Builder expectedSettlementDate(LocalDate expectedSettlementDate) {
      JodaBeanUtils.notNull(expectedSettlementDate, "expectedSettlementDate");
      this.expectedSettlementDate = expectedSettlementDate;
      return this;
    }

    /**
     * Sets waive delayed compensation flag.
     * @param delayedCompensationFlag  the new value
     * @return this, for chaining, not null
     */
    public Builder delayedCompensationFlag(boolean delayedCompensationFlag) {
      this.delayedCompensationFlag = delayedCompensationFlag;
      return this;
    }

    /**
     * Sets governing loan association, e.g. LSTA, LMA
     * @param association  the new value
     * @return this, for chaining, not null
     */
    public Builder association(LoanTradingAssoc association) {
      this.association = association;
      return this;
    }

    /**
     * Sets form of purchase, e.g. assignment
     * @param formOfPurchase  the new value
     * @return this, for chaining, not null
     */
    public Builder formOfPurchase(LoanTradingFormOfPurchase formOfPurchase) {
      this.formOfPurchase = formOfPurchase;
      return this;
    }

    /**
     * Sets documentation type, e.g. par vs distressed
     * @param documentationType  the new value
     * @return this, for chaining, not null
     */
    public Builder documentationType(LoanTradingDocType documentationType) {
      this.documentationType = documentationType;
      return this;
    }

    /**
     * Sets trade type, e.g. secondary vs primary
     * @param tradeType  the new value
     * @return this, for chaining, not null
     */
    public Builder tradeType(LoanTradingType tradeType) {
      this.tradeType = tradeType;
      return this;
    }

    /**
     * Sets a flag to indicate the dependency of a secondary market loan trade upon
     * the closing of a primary market loan structuring and syndication process.
     * @param whenIssuedFlag  the new value
     * @return this, for chaining, not null
     */
    public Builder whenIssuedFlag(boolean whenIssuedFlag) {
      this.whenIssuedFlag = whenIssuedFlag;
      return this;
    }

    /**
     * Sets flag to waive CR credit.
     * @param commitmentReductionCreditFlag  the new value
     * @return this, for chaining, not null
     */
    public Builder commitmentReductionCreditFlag(boolean commitmentReductionCreditFlag) {
      this.commitmentReductionCreditFlag = commitmentReductionCreditFlag;
      return this;
    }

    /**
     * Sets flag to recognize repayment on trade date.
     * @param paydownOnTradeDate  the new value
     * @return this, for chaining, not null
     */
    public Builder paydownOnTradeDate(boolean paydownOnTradeDate) {
      this.paydownOnTradeDate = paydownOnTradeDate;
      return this;
    }

    /**
     * Sets flag to adjust amount for commitment adjustment occurring on trade date.
     * @param adjustmentOnTradeDate  the new value
     * @return this, for chaining, not null
     */
    public Builder adjustmentOnTradeDate(boolean adjustmentOnTradeDate) {
      this.adjustmentOnTradeDate = adjustmentOnTradeDate;
      return this;
    }

    /**
     * Sets settlement accrued interest treatment, e.g. SettlesWithoutAccrued.
     * @param accrualSettlementType  the new value
     * @return this, for chaining, not null
     */
    public Builder accrualSettlementType(LoanTradingAccrualSettlement accrualSettlementType) {
      this.accrualSettlementType = accrualSettlementType;
      return this;
    }

    /**
     * Sets average LIBOR fixing.
     * @param averageLibor  the new value
     * @return this, for chaining, not null
     */
    public Builder averageLibor(double averageLibor) {
      this.averageLibor = averageLibor;
      return this;
    }

    /**
     * Sets calculated series of prorated share of global facility.
     * @param pctShare  the new value
     * @return this, for chaining, not null
     */
    public Builder pctShare(LocalDateDoubleTimeSeries pctShare) {
      this.pctShare = pctShare;
      return this;
    }

    //-----------------------------------------------------------------------
    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder(704);
      buf.append("LoanTrade.Builder{");
      buf.append("info").append('=').append(JodaBeanUtils.toString(info)).append(',').append(' ');
      buf.append("product").append('=').append(JodaBeanUtils.toString(product)).append(',').append(' ');
      buf.append("buySell").append('=').append(JodaBeanUtils.toString(buySell)).append(',').append(' ');
      buf.append("buyer").append('=').append(JodaBeanUtils.toString(buyer)).append(',').append(' ');
      buf.append("seller").append('=').append(JodaBeanUtils.toString(seller)).append(',').append(' ');
      buf.append("amount").append('=').append(JodaBeanUtils.toString(amount)).append(',').append(' ');
      buf.append("currency").append('=').append(JodaBeanUtils.toString(currency)).append(',').append(' ');
      buf.append("price").append('=').append(JodaBeanUtils.toString(price)).append(',').append(' ');
      buf.append("expectedSettlementDate").append('=').append(JodaBeanUtils.toString(expectedSettlementDate)).append(',').append(' ');
      buf.append("delayedCompensationFlag").append('=').append(JodaBeanUtils.toString(delayedCompensationFlag)).append(',').append(' ');
      buf.append("association").append('=').append(JodaBeanUtils.toString(association)).append(',').append(' ');
      buf.append("formOfPurchase").append('=').append(JodaBeanUtils.toString(formOfPurchase)).append(',').append(' ');
      buf.append("documentationType").append('=').append(JodaBeanUtils.toString(documentationType)).append(',').append(' ');
      buf.append("tradeType").append('=').append(JodaBeanUtils.toString(tradeType)).append(',').append(' ');
      buf.append("whenIssuedFlag").append('=').append(JodaBeanUtils.toString(whenIssuedFlag)).append(',').append(' ');
      buf.append("commitmentReductionCreditFlag").append('=').append(JodaBeanUtils.toString(commitmentReductionCreditFlag)).append(',').append(' ');
      buf.append("paydownOnTradeDate").append('=').append(JodaBeanUtils.toString(paydownOnTradeDate)).append(',').append(' ');
      buf.append("adjustmentOnTradeDate").append('=').append(JodaBeanUtils.toString(adjustmentOnTradeDate)).append(',').append(' ');
      buf.append("accrualSettlementType").append('=').append(JodaBeanUtils.toString(accrualSettlementType)).append(',').append(' ');
      buf.append("averageLibor").append('=').append(JodaBeanUtils.toString(averageLibor)).append(',').append(' ');
      buf.append("pctShare").append('=').append(JodaBeanUtils.toString(pctShare));
      buf.append('}');
      return buf.toString();
    }

  }

  //-------------------------- AUTOGENERATED END --------------------------
}
