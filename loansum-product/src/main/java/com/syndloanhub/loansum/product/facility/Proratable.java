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

import com.opengamma.strata.product.ProductTrade;

/**
 * A loan facility artifact containing global numerical values which can be adjusted down to
 * a specific share based on a specific trade.
 * <p>
 * A loan contract transaction is an example of a proratable type. Consider a global repayment
 * of 2MM on a 100MM facility. A participant assigned 50MM of that facility will have a 50%
 * share and thus the 2MM repayment would be prorated to 1MM.
 * 
 * @param <T>  the type of the prorated result
 */
public interface Proratable<T> {

  /**
   * Prorate a 100% allocation of a security or facility into a partial allocation
   * based on a specific trade.
   * 
   * @param trade buy or sell trade representing a full or partial allocation
   * @return prorated instance of type T
   */
  public abstract T prorate(ProductTrade trade);
}
