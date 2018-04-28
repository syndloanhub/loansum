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

import java.time.LocalDate;

import com.opengamma.strata.basics.currency.CurrencyAmount;

import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanContractEvent;

/**
 * Interface implemented by all contract-level events.
 */
public interface LoanContractEvent extends Proratable<ProratedLoanContractEvent> {
  /**
   * @return event effective date
   */
  LocalDate getEffectiveDate();

  /**
   * @return event type, e.g. borrowing or repayment
   */
  LoanContractEventType getType();

  /**
   * @return amount of event and currency
   */
  CurrencyAmount getAmount();
}
