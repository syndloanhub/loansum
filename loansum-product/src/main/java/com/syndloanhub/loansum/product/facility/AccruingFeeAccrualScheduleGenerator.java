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

import java.util.List;

/**
 * Interface defining generator for an accruing fee.
 */
public interface AccruingFeeAccrualScheduleGenerator {
  public abstract List<Accrual> generateAccrualSchedule(Facility loan, AccruingFee fee);
}
