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

import com.opengamma.strata.product.TradeInfo;

public interface ProratedTrade {
  /**
   * Gets the standard trade information.
   * <p>
   * All trades contain this standard set of information.
   * 
   * @return the trade information
   */
  public abstract TradeInfo getInfo();

  /**
   * Gets the underlying product that was agreed when the trade occurred.
   * <p>
   * The product captures the contracted financial details of the trade.
   * 
   * @return the product
   */
  public abstract ProratedProduct getProduct();
}
