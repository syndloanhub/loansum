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
 * Enumeration of loan cash flow types.
 */
public enum CashFlowType {
  /**
   * Contract interest.
   */
  Interest,

  /**
   * Contract PIK interest capitalization.
   */
  PikInterest,

  /**
   * Principal repayment.
   */
  Repayment,

  /**
   * Cash borrowing.
   */
  Borrowing,

  /**
   * Delayed settlement interest compensation.
   */
  DelayedCompensation,

  /**
   * Settlement price of funded amount.
   */
  CostOfFunded,

  /**
   * Trade away from par unfunded benefit.
   */
  BenefitOfUnfunded,

  /**
   * Delayed settlement funding compensation.
   */
  CostOfCarry,

  /**
   * Benefit of repayments during settlement period.
   */
  EconomicBenefit,

  /**
   * LC fee.
   */
  LetterOfCreditFee,

  /**
   * Misc fee, e.g. commitment fee.
   */
  Fee;
}
