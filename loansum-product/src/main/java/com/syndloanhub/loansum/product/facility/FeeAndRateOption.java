/**
 * Copyright (c) 2020 SyndLoanHub, LLC and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License from within this distribution and at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.syndloanhub.loansum.product.facility;

import java.time.LocalDate;
import java.util.Optional;

import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.date.DayCount;
import com.opengamma.strata.basics.index.RateIndex;
import com.opengamma.strata.basics.schedule.Frequency;

/**
 * Interface that all accrual option classes.
 */
public interface FeeAndRateOption {
	/**
	 * @return accrual option ID
	 */
	public abstract StandardId getId();

	/**
	 * @return option type
	 */
	public abstract FeeAndRateOptionType getOptionType();

	/**
	 * @return day count basis to be used for calculations
	 */
	public abstract DayCount getDayCount();

	/**
	 * @return accrual payment frequency
	 */
	public abstract Frequency getPaymentFrequency();

	/**
	 * @return total annual cash interest rate or spread over index
	 */
	public abstract double getRate();

	/**
	 * @return floating rate index
	 */
	public abstract Optional<RateIndex> getIndex();

	/**
	 * @return total annual pay-in-kind interest rate
	 */
	public abstract double getPikSpread();

	/**
	 * @return start date when option is valid
	 */
	public abstract LocalDate getStartDate();

	/**
	 * @return end date when option is valid
	 */
	public abstract LocalDate getEndDate();

	/**
	 * @return accrual option currency
	 */
	public abstract Currency getCurrency();
}
