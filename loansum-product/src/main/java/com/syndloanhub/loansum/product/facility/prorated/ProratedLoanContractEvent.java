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

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.syndloanhub.loansum.product.facility.LoanContractEventType;

/**
 * Interface for all prorated contract-level events.
 */
public interface ProratedLoanContractEvent {
  /**
   * @return effective date of event
   */
  LocalDate getEffectiveDate();

  /**
   * @return contract-level event type
   */
  LoanContractEventType getType();

  /**
   * @return currency and amount of event
   */
  CurrencyAmount getAmount();
}
