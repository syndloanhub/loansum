package com.syndloanhub.loansum.pricer.facility.prorated;

import static com.syndloanhub.loansum.product.facility.CashFlowType.Interest;
import static com.syndloanhub.loansum.product.facility.Helper.EPSILON_1;
import static com.syndloanhub.loansum.product.facility.Helper.intersection;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

import org.joda.beans.ser.JodaBeanSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.collect.tuple.Pair;
import com.opengamma.strata.market.explain.ExplainKey;
import com.opengamma.strata.market.explain.ExplainMap;
import com.opengamma.strata.market.explain.ExplainMapBuilder;
import com.syndloanhub.loansum.product.facility.CashFlowType;
import com.syndloanhub.loansum.product.facility.Helper;
import com.syndloanhub.loansum.product.facility.prorated.ProratedAccrual;

/**
 * Facility-specific explain keys and supporting functions.
 */
final public class Explain {
  private static final Logger log = LoggerFactory.getLogger(Explain.class);

  public static final ExplainKey<List<ExplainMap>> CASHFLOW = ExplainKey.of("CashFlow");
  public static final ExplainKey<StandardId> FEE = ExplainKey.of("Fee");
  public static final ExplainKey<CurrencyAmount> GLOBAL_FORECAST_VALUE = ExplainKey.of("GlobalForecastValue");
  public static final ExplainKey<CurrencyAmount> GLOBAL_NOTIONAL = ExplainKey.of("Global Not");
  public static final ExplainKey<Double> SHARE_AMOUNT = ExplainKey.of("Share Amt");
  public static final ExplainKey<CurrencyAmount> GLOBAL_AMOUNT = ExplainKey.of("Global Amt");
  public static final ExplainKey<DayCount> DAY_COUNT = ExplainKey.of("Day Cnt");
  public static final ExplainKey<Integer> DAYS = ExplainKey.of("Days");
  public static final ExplainKey<Double> DIY = ExplainKey.of("DIY");
  public static final ExplainKey<LocalDate> START_DATE = ExplainKey.of("Start");
  public static final ExplainKey<LocalDate> END_DATE = ExplainKey.of("End");
  public static final ExplainKey<LocalDate> CASHFLOW_DATE = ExplainKey.of("Flow Dt");
  public static final ExplainKey<Double> SHARE_NOTIONAL = ExplainKey.of("Share Not");
  public static final ExplainKey<Double> ALLIN_RATE = ExplainKey.of("All-in Rt");
  public static final ExplainKey<Double> PCT_SHARE = ExplainKey.of("Pct Share");
  public static final ExplainKey<Double> AVG_LIBOR = ExplainKey.of("Avg LIBOR");
  public static final ExplainKey<Double> PRICE = ExplainKey.of("Price");
  public static final ExplainKey<String> FORMULA = ExplainKey.of("Formula");
  public static final ExplainKey<Double> FREE_PIK = ExplainKey.of("Sett Per PIK");
  public static final ExplainKey<Double> EXP_SETT_PX = ExplainKey.of("Exp Sett Px");
  public static final ExplainKey<Double> ACT_SETT_PX = ExplainKey.of("Act Sett Px");
  public static final ExplainKey<LocalDate> EXP_SETT_DT = ExplainKey.of("Exp Sett Dt");
  public static final ExplainKey<LocalDate> ACT_SETT_DT = ExplainKey.of("Act Sett Dt");
  public static final ExplainKey<Double> TRD_DT_FUNDED = ExplainKey.of("Trd Dt Funded");
  public static final ExplainKey<Double> SETT_DT_FUNDED = ExplainKey.of("Sett Dt Funded");
  public static final ExplainKey<Double> TRD_DT_REPAY = ExplainKey.of("Trd Dt Repay");
  public static final ExplainKey<Double> FUNDED_AMOUNT = ExplainKey.of("Funded Amt");
  public static final ExplainKey<Double> UNFUNDED_AMOUNT = ExplainKey.of("Unfunded Amt");
  public static final ExplainKey<Double> PAYDOWN_CREDIT = ExplainKey.of("Paydown Crdt");
  public static final ExplainKey<Double> UNFUNDED_CREDIT = ExplainKey.of("Unfunded Crdt");

  /**
   * Helper function to add explain map list for an accrual which is optionally attached to corresponding
   * cash flow annotation.
   * 
   * @param builder ExplainMap builder
   * @param accrual interest, fee, PIK, etc. accrual to be explained
   * @param pik true if PIK accrual
   */
  protected static void explainAccrual(ExplainMapBuilder builder, ProratedAccrual accrual, boolean pik) {
    final int days = accrual.getDayCount().days(accrual.getStartDate(), accrual.getEndDate());
    final double yearFraction = accrual.getDayCount().yearFraction(accrual.getStartDate(), accrual.getEndDate());
    final double diy = days / yearFraction;
    final double rate = pik ? accrual.getPikSpread() : accrual.getAllInRate();
    final CurrencyAmount amount = pik ? accrual.getPikProjection() : accrual.getPaymentProjection();

    builder = builder.openListEntry(CASHFLOW);
    builder.put(SHARE_AMOUNT, amount.getAmount());
    builder.put(START_DATE, accrual.getStartDate());
    builder.put(END_DATE, accrual.getEndDate());
    builder.put(DAYS, days);
    builder.put(DIY, diy);
    builder.put(DAY_COUNT, accrual.getDayCount());
    builder.put(ALLIN_RATE, rate);
    builder.put(SHARE_NOTIONAL, accrual.getAccrualAmount().getAmount());
    builder.put(FORMULA,
        SHARE_NOTIONAL.getName() + " x " + ALLIN_RATE.getName() + " x " + DAYS.getName() + " / " + DIY.getName());
    builder.closeListEntry(CASHFLOW);
  }

  /**
   * Merge two cash flow explain maps.
   * 
   * @param newOptExpl
   * @param existingOptExpl
   * @param add
   * @param cashFlowType 
   * @return merged map
   */
  protected static Optional<ExplainMap> mergeExplains(Optional<ExplainMap> newOptExpl, Optional<ExplainMap> existingOptExpl,
      boolean add,
      CashFlowType cashFlowType) {
    log.debug("enter merge explains, add=" + add);

    if (!newOptExpl.isPresent() || !existingOptExpl.isPresent()) {
      log.debug("missing explains, returning empty");
      return Optional.empty();
    }

    ExplainMap newExpl = newOptExpl.get();
    ExplainMap existingExpl = existingOptExpl.get();

    if (newExpl.getMap().size() == 0 || existingExpl.getMap().size() == 0) {
      log.debug("empty explains, returning empty");
      return Optional.empty();
    }

    log.debug("new explain list\n" + JodaBeanSer.PRETTY.jsonWriter().write(newExpl));
    log.debug("\nexisting explain list\n" + JodaBeanSer.PRETTY.jsonWriter().write(existingExpl));

    List<ExplainMap> mergedExplains = new ArrayList<ExplainMap>();

    if (cashFlowType == Interest) {
      // Attempt to merge each new explain against each existing explain.
      for (ListIterator<ExplainMap> newit = newExpl.get(CASHFLOW).get().listIterator(); newit.hasNext();) {
        ExplainMap newex = newit.next();

        for (ListIterator<ExplainMap> existingit = existingExpl.get(CASHFLOW).get().listIterator(); existingit.hasNext();) {
          ExplainMap existingex = existingit.next();

          log.debug("new explain\n" + JodaBeanSer.PRETTY.jsonWriter().write(newex));
          log.debug("\nexisting explain\n" + JodaBeanSer.PRETTY.jsonWriter().write(existingex));

          Optional<List<ExplainMap>> merged = mergeInterestExplains(newex, existingex, add);

          if (merged.isPresent()) {
            mergedExplains.addAll(merged.get());
            newit.remove();
            existingit.remove();
            break;
          }
        }
      }

      // Add all unmerged explains left in the maps.
      mergedExplains.addAll(newExpl.get(CASHFLOW).get());
      mergedExplains.addAll(existingExpl.get(CASHFLOW).get());

      log.debug("merged explain list before removing overlaps\n" +
          JodaBeanSer.PRETTY.jsonWriter().write(ExplainMap.builder().put(CASHFLOW, mergedExplains).build()));

      // Compress any overlaps in the merged explains.
      boolean overlaps = true;

      while (overlaps) {
        overlaps = false;

        for (int i = 0; i < mergedExplains.size() && !overlaps; i++) {
          for (int j = 0; j < mergedExplains.size() && !overlaps; j++) {
            if (i != j) {
              ExplainMap first = mergedExplains.get(i);
              ExplainMap second = mergedExplains.get(j);

              Pair<LocalDate, LocalDate> firstPeriod = Pair.of(first.get(START_DATE).get(), first.get(END_DATE).get());
              Pair<LocalDate, LocalDate> secondPeriod = Pair.of(second.get(START_DATE).get(), second.get(END_DATE).get());
              Pair<LocalDate, LocalDate> overlap = intersection(firstPeriod, secondPeriod);

              if (overlap != null) {
                overlaps = true;
                mergedExplains.removeAll(Arrays.asList(first, second));
                mergedExplains.addAll(mergeInterestExplains(first, second, true).orElse(new ArrayList<ExplainMap>()));
              }
            }
          }
        }
      }
    }

    log.debug("merged explain list after removing overlaps\n" +
        JodaBeanSer.PRETTY.jsonWriter().write(ExplainMap.builder().put(CASHFLOW, mergedExplains).build()));

    return Optional.of(ExplainMap.builder().put(CASHFLOW, mergedExplains).build());
  }

  /**
   * Merge two interest cash flow explains.
   * 
   * @param first
   * @param second
   * @param add
   * @return
   */
  protected static Optional<List<ExplainMap>> mergeInterestExplains(ExplainMap first, ExplainMap second, boolean add) {
    log.debug("enter mergeInterestExplains");

    List<ExplainMap> merged = new ArrayList<ExplainMap>();

    log.debug("first interest explain" + JodaBeanSer.PRETTY.jsonWriter().write(first));
    log.debug("second interest explain\n" + JodaBeanSer.PRETTY.jsonWriter().write(second));

    Pair<LocalDate, LocalDate> firstPeriod = Pair.of(first.get(START_DATE).get(), first.get(END_DATE).get());
    Pair<LocalDate, LocalDate> secondPeriod = Pair.of(second.get(START_DATE).get(), second.get(END_DATE).get());
    Pair<LocalDate, LocalDate> overlap = intersection(firstPeriod, secondPeriod);

    log.debug("overlap: " + overlap);

    if (overlap == null)
      return Optional.empty();

    double firstShareAmount = first.get(SHARE_AMOUNT).get();
    double firstShareNotional = first.get(SHARE_NOTIONAL).get();
    double secondShareAmount = second.get(SHARE_AMOUNT).get();
    double secondShareNotional = second.get(SHARE_NOTIONAL).get();

    DayCount dayCount = first.get(DAY_COUNT).get();

    // For each unique accrual period generated from overlapping explain periods, create
    // net explain for each period.
    for (Pair<LocalDate, LocalDate> accrualPeriod : Helper.accruals(firstPeriod, secondPeriod)) {
      log.debug("merging accrualPeriod " + accrualPeriod.getFirst() + " - " + accrualPeriod.getSecond());

      double aggregateShareAmount = 0;
      double aggregateShareNotional = 0;

      // Merge with first explain.
      overlap = intersection(accrualPeriod, firstPeriod);
      log.debug("first overlap: " + overlap);

      if (overlap != null) {
        double factor = (double) dayCount.days(overlap.getFirst(), overlap.getSecond()) /
            (double) dayCount.days(firstPeriod.getFirst(), firstPeriod.getSecond());

        if (add) {
          aggregateShareAmount += firstShareAmount * factor;
          aggregateShareNotional += firstShareNotional;
          log.debug("adding share " + firstShareAmount * factor + " firstShareAmount=" + firstShareAmount + " factor=" +
              dayCount.days(overlap.getFirst(), overlap.getSecond()) + " / " +
              dayCount.days(firstPeriod.getFirst(), firstPeriod.getSecond()));
        } else {
          aggregateShareAmount -= firstShareAmount * factor;
          aggregateShareNotional -= firstShareNotional;
          log.debug("subtracting share " + firstShareAmount * factor + " firstShareAmount=" + firstShareAmount + " factor=" +
              dayCount.days(overlap.getFirst(), overlap.getSecond()) + " / " +
              dayCount.days(firstPeriod.getFirst(), firstPeriod.getSecond()));
        }

        log.debug("after merging with first: share=" + aggregateShareAmount + " notional=" + aggregateShareNotional);
      }

      // Merge with second explain.
      overlap = intersection(accrualPeriod, secondPeriod);

      log.debug("second overlap: " + overlap);

      if (overlap != null) {
        double factor = (double) dayCount.days(overlap.getFirst(), overlap.getSecond()) /
            (double) dayCount.days(secondPeriod.getFirst(), secondPeriod.getSecond());

        aggregateShareAmount += secondShareAmount * factor;
        aggregateShareNotional += secondShareNotional;

        log.debug("adding share " + secondShareAmount * factor + " secondShareAmount=" + secondShareAmount + " factor=" +
            dayCount.days(overlap.getFirst(), overlap.getSecond()) + " / " +
            dayCount.days(secondPeriod.getFirst(), secondPeriod.getSecond()));
      }

      log.debug("aggregateShareAmount: " + aggregateShareAmount);

      // If we have a net share amount, create and add merged explain.
      if (Math.abs(aggregateShareAmount) > EPSILON_1) {
        int days = dayCount.days(accrualPeriod.getFirst(), accrualPeriod.getSecond());
        double yearFraction = dayCount.yearFraction(accrualPeriod.getFirst(), accrualPeriod.getSecond());
        double diy = days / yearFraction;

        ExplainMapBuilder explainsBuilder = ExplainMap.builder();
        explainsBuilder.put(SHARE_AMOUNT, aggregateShareAmount);
        explainsBuilder.put(START_DATE, accrualPeriod.getFirst());
        explainsBuilder.put(END_DATE, accrualPeriod.getSecond());
        explainsBuilder.put(DAYS, days);
        explainsBuilder.put(DIY, diy);
        explainsBuilder.put(DAY_COUNT, dayCount);
        explainsBuilder.put(ALLIN_RATE, first.get(ALLIN_RATE).get());
        explainsBuilder.put(SHARE_NOTIONAL, aggregateShareNotional);
        merged.add(explainsBuilder.build());

        log.debug("merged explain" + JodaBeanSer.PRETTY.jsonWriter().write(explainsBuilder.build()));
      } else
        log.debug("net zero");
    }

    return Optional.of(merged);
  }

}
