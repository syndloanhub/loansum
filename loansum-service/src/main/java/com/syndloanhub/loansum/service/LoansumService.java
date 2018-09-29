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
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.joda.beans.Bean;
import org.joda.beans.ser.JodaBeanSer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.collect.result.Result;
import com.opengamma.strata.pricer.rate.ImmutableRatesProvider;
import com.opengamma.strata.pricer.rate.RatesProvider;
import com.syndloanhub.loansum.pricer.facility.prorated.ProratedLoanTradePricer;
import com.syndloanhub.loansum.product.facility.Commitment;
import com.syndloanhub.loansum.product.facility.Facility;
import com.syndloanhub.loansum.product.facility.Helper;
import com.syndloanhub.loansum.product.facility.LoanTrade;
import com.syndloanhub.loansum.product.facility.LoanTradeList;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTrade;
import com.syndloanhub.loansum.product.facility.prorated.ProratedLoanTradeList;

import javax.ws.rs.Path;

@Path("/")
public class LoansumService {
  private static final Logger log = LoggerFactory.getLogger(LoansumService.class);

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

  @POST
  @Path("/calculateCommitment")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Bean commitment(Result<Bean> bean) {
    if (bean.isSuccess()) {
      Facility loan = (Facility) bean.getValue();
      Commitment commitment = Helper.generateCommitment(loan.getFacilityType(), loan.getStartDate(),
          loan.getOriginalCommitmentAmount().getAmount(),
          loan.getContracts(), loan.getEvents().stream().collect(Collectors.toList()));
      return commitment;
    } else
      return bean.getFailure();
  }

}
