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

/**
 * Types of supported contract events, e.g. borrow or principal repayment.
 */
public enum LoanContractEventType {
  /**
   * A borrow, currently must be on a delayed-draw or revolver on the start date
   * of a contract.
   */
  BorrowingEvent,

  /**
   * Principal repayment, may occur any time within the contract start and end date.
   */
  RepaymentEvent;
}
