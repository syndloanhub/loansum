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
 * Type of loan facility (aka tranche). 
 */
public enum FacilityType {
  /**
   * Term loan, funded upfront.
   */
  Term,

  /**
   * Revolving facility, commitment amount remains constant, borrower draws and repays over time.
   */
  Revolving,

  /**
   * Term loan funded during the draw period.
   */
  DelayedDraw,

  /**
   * Facility containing only letters-of-credit.
   */
  LetterOfCredit;
}
