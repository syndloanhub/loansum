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

import static com.syndloanhub.loansum.product.facility.LoanContractEventType.RepaymentEvent;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.collect.timeseries.LocalDateDoubleTimeSeries;
import com.opengamma.strata.collect.tuple.Pair;

/**
 * A class containing basic static helper functions.
 */
public final class Helper {
  private static final Logger log = LoggerFactory.getLogger(Helper.class);
  public final static double EPSILON_1 = 0.1;

  /**
   * Return the larger of two dates
   * 
   * @param d1
   * @param d2
   * @return max of dates d1 and d2
   */
  public final static LocalDate max(LocalDate d1, LocalDate d2) {
    return d2.isAfter(d1) ? d2 : d1;
  }

  /**
   * Return the smaller of two dates
   * @param d1
   * @param d2
   * @return min or dates d1 and d2
   */
  public final static LocalDate min(LocalDate d1, LocalDate d2) {
    return d2.isBefore(d1) ? d2 : d1;
  }

  /**
   * Return the intersection between two date intervals or null if there is
   * no intersection.
   * 
   * @param i1 first interval
   * @param i2 second interval
   * @return intersection between two date intervals i1 and i2
   */
  public final static Pair<LocalDate, LocalDate> intersection(
      Pair<LocalDate, LocalDate> i1, Pair<LocalDate, LocalDate> i2) {
    if (i1.getFirst().compareTo(i1.getSecond()) > 0 || i1.getFirst().isAfter(i1.getSecond()) ||
        i2.getFirst().isAfter(i2.getSecond()))
      return null;

    Pair<LocalDate, LocalDate> pred = i1, succ = i2;

    if (i1.getFirst().isAfter(i2.getFirst())) {
      pred = i2;
      succ = i1;
    }

    if (!succ.getFirst().isBefore(pred.getSecond()))
      return null;

    return Pair.of(max(pred.getFirst(), succ.getFirst()), min(pred.getSecond(), succ.getSecond()));
  }

  /**
   * Return true if date is within the interval exclusive of interval end date
   * 
   * @param date
   * @param interval
   * @return true if date intersects the interval else false
   */
  public final static boolean intersects(LocalDate date, Pair<LocalDate, LocalDate> interval) {
    Pair<LocalDate, LocalDate> normalized = interval;

    if (!interval.getFirst().isBefore(interval.getSecond()))
      normalized = Pair.of(interval.getSecond(), interval.getFirst());

    return !(date.isBefore(normalized.getFirst()) || date.isEqual(normalized.getSecond()) ||
        date.isAfter(normalized.getSecond()));
  }

  /**
   * Get the value of a time-series as of date
   * 
   * @param ts the time series
   * @param date the date
   * @return the value of the (sparse) time series as of date
   */
  public final static double tsget(LocalDateDoubleTimeSeries ts, LocalDate date) {
    Object[] dates = ts.dates().toArray();
    double[] values = ts.values().toArray();
    int i = Arrays.binarySearch(dates, date);

    return i >= 0 ? values[i] : values[-i - 2];
  }

  /**
   * Generate set of accruals given two intersecting intervals.
   * 
   * @param first
   * @param second
   * @return set of intervals formed from two intersecting intervals
   */
  public final static List<Pair<LocalDate, LocalDate>> accruals(Pair<LocalDate, LocalDate> first,
      Pair<LocalDate, LocalDate> second) {
    Set<LocalDate> dates = new HashSet<LocalDate>();

    dates.add(first.getFirst());
    dates.add(first.getSecond());
    dates.add(second.getFirst());
    dates.add(second.getSecond());

    List<LocalDate> sorted = dates.stream().sorted().collect(Collectors.toList());
    List<Pair<LocalDate, LocalDate>> periods = new ArrayList<Pair<LocalDate, LocalDate>>();

    for (int i = 1; i < sorted.size(); i++)
      periods.add(Pair.of(sorted.get(i - 1), sorted.get(i)));

    return periods;
  }

  /**
   * Given a single loan contract, generate an equivalent set of sub-accruals from
   * the single contract accrual and repayment events.
   * 
   * @param contract
   * @return list of equivalent sub-accruals
   */
  public final static List<Accrual> generateContractAccrualSchedule(LoanContract contract) {
    List<Accrual> accrualSchedule = new ArrayList<Accrual>();

    // Filter and sort repayment contract events.
    List<Repayment> repayments = contract.getEvents()
        .stream()
        .filter(event -> event.getType() == RepaymentEvent)
        .map(event -> (Repayment) event)
        .sorted(Comparator.comparing(Repayment::getEffectiveDate))
        .collect(Collectors.toList());

    // The accrual list will have a second element only if an accrual is 
    // mixed cash and PIK, split since accrual periods may be different across 
    // the settlement period.
    List<Accrual> accruals = Arrays.asList(contract.getAccrual());

    if (contract.getAccrual().getAllInRate() > 0 && contract.getAccrual().getPikSpread() > 0) {
      Pair<Accrual, Accrual> cashPik = contract.getAccrual().split();
      accruals = Arrays.asList(cashPik.getFirst(), cashPik.getSecond());
    }

    for (Accrual accrual : accruals) {
      // If no repayments, add accrual as is.
      if (repayments.isEmpty() || (repayments.size() == 1 && repayments.get(0).getEffectiveDate().isEqual(accrual.getEndDate())))
        accrualSchedule.add(accrual);
      else {
        final Currency currency = accrual.getAccrualAmount().getCurrency();
        LocalDate rollingStartDate = accrual.getStartDate();
        double rollingAmount = accrual.getAccrualAmount().getAmount();

        // Partition the repayments by interest-on-paydown flag.
        Map<Boolean, List<Repayment>> partitionedRepayments = repayments
            .stream()
            .collect(Collectors.partitioningBy(repayment -> repayment.isInterestOnPaydown()));

        // First pass handles repayments with interest to get accrual explains correct.
        for (Repayment repayment : partitionedRepayments.get(true)) {
          accrualSchedule.add(accrual.rebuild(accrual.getStartDate(), repayment.getEffectiveDate(),
              repayment.getAmount()));
          rollingAmount -= repayment.getAmount().getAmount();
        }

        // Second pass handles remaining repayments.
        for (Repayment repayment : partitionedRepayments.get(false)) {
          accrualSchedule.add(accrual.rebuild(rollingStartDate, repayment.getEffectiveDate(),
              CurrencyAmount.of(currency, rollingAmount)));
          rollingStartDate = repayment.getEffectiveDate();
          rollingAmount -= repayment.getAmount().getAmount();
        }

        // Handle last sub-accrual if residual.
        if (rollingStartDate.isBefore(accrual.getEndDate()) && rollingAmount > 0)
          accrualSchedule.add(accrual.rebuild(rollingStartDate, accrual.getEndDate(),
              CurrencyAmount.of(currency, rollingAmount)));
      }
    }

    return accrualSchedule;
  }

  /**
   * Default generator for fee accrual schedule generation: just return given accrual.
   * 
   * @param loan the facility
   * @param fee some fee accrual
   * @return list containing the given accrual
   */
  public final static List<Accrual> generateDefaultFeeAccrualSchedule(Facility loan, AccruingFee fee) {
    return Arrays.asList(fee.getAccrual());
  }

  /**
   * Generate commitment fee accrual schedule for each change in unfunded amount.
   * 
   * @param loan the facility
   * @param fee commitment fee accrual
   * @return list of commitment fee accruals for each unfunded change
   */
  public final static List<Accrual> generateCommitmentFeeAccrualSchedule(Facility loan, AccruingFee fee) {
    List<Accrual> accrualSchedule = new ArrayList<Accrual>();

    final Accrual accrual = fee.getAccrual();
    if (!accrual.getAccrualAmount().equals(loan.getUnfundedAmount(accrual.getStartDate())))
      log.warn(
          "assumed commitment fee " + fee.getId() + " amount " + accrual.getAccrualAmount() + " does not equal unfunded amount " +
              loan.getUnfundedAmount(accrual.getStartDate()) + " as of start date " + accrual.getStartDate());

    Set<LocalDate> dates = new HashSet<LocalDate>();

    dates.addAll(loan.getEvents()
        .stream()
        .map(event -> event.getEffectiveDate())
        .filter(date -> date.isAfter(accrual.getStartDate()) && date.isBefore(accrual.getEndDate()))
        .collect(Collectors.toList()));

    for (LoanContract contract : loan.getContracts())
      dates.addAll(contract.getEvents()
          .stream()
          .map(event -> event.getEffectiveDate())
          .filter(date -> date.isAfter(accrual.getStartDate()) && date.isBefore(accrual.getEndDate()))
          .collect(Collectors.toList()));

    if (dates.isEmpty())
      accrualSchedule.add(accrual);
    else {
      LocalDate startDate = accrual.getStartDate();
      for (LocalDate endDate : dates) {
        accrualSchedule.add(accrual.rebuild(startDate, endDate, CurrencyAmount.of(accrual.getAccrualAmount().getCurrency(),
            loan.getUnfundedAmount(startDate).getAmount())));
        startDate = endDate;
      }

      if (loan.getUnfundedAmount(startDate).getAmount() > EPSILON_1)
        accrualSchedule
            .add(accrual.rebuild(startDate, accrual.getEndDate(), CurrencyAmount.of(accrual.getAccrualAmount().getCurrency(),
                loan.getUnfundedAmount(startDate).getAmount())));
    }

    return accrualSchedule;
  }
}
