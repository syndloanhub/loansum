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
 * Types of loan settlements, standard is without accrued interest.
 */
public enum LoanTradingAccrualSettlement {
  /**
   * Trades flat.
   */
  Flat,

  /**
   * Settled with Accrued Interest: Interest accrued during the settlement period is paid at settlement.
   */
  SettledWithAccrued,

  /**
   * / Settled without Accrued Interest: Interest accrued during for the settlement period is not paid at settlement.
   */
  SettledWithoutAccrued;
}
