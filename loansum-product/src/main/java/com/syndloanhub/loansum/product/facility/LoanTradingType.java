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
 * A list that specifies whether a trade was executed during the syndication period or in the post-syndication market.
 */
public enum LoanTradingType {
  /**
   *  Trade is part of a syndication.
   */
  Primary,

  /**
   * Trade was performed in the secondary (non-syndication) market.
   */
  Secondary;
}
