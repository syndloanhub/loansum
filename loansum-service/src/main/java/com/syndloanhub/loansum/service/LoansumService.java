/**
 * Copyright (c) 2018 SyndLoanHub, LLC and contributors
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License from within this distribution and at:
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package com.syndloanhub.loansum.service;

import javax.ws.rs.POST;

import java.time.LocalDate;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.joda.beans.Bean;
import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.syndloanhub.loansum.pricer.facility.prorated.ProratedLoanTradePricer;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.LoanTradeList;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTradeList;

import javax.ws.rs.Path;

@Path("/")
public class LoansumService {
  @POST
  @Path("/calculateCashflows")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Bean cashflow(Result<Bean> tradeList) {
    if (tradeList.isSuccess()) {
      ProratedLoanTradeList proratedTradeList = ((LoanTradeList) tradeList.getValue()).prorate(null);
      ProratedLoanTradePricer pricer = ProratedLoanTradePricer.DEFAULT;
      RatesProvider rates = ImmutableRatesProvider.builder(LocalDate.now()).build();

      return pricer.cashFlows(proratedTradeList, rates, true);
    } else
      return tradeList.getFailure();
  }

  @POST
  @Path("/calculateProceeds")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Bean proceeds(Result<Bean> trade) {
    if (trade.isSuccess()) {
      ProratedLoanTrade proratedTrade = ((LoanTrade) trade.getValue()).prorate(null);
      ProratedLoanTradePricer pricer = ProratedLoanTradePricer.DEFAULT;
      RatesProvider rates = ImmutableRatesProvider.builder(LocalDate.now()).build();

      return pricer.proceeds(proratedTrade, rates, true);
    } else
      return trade.getFailure();
  }

}
