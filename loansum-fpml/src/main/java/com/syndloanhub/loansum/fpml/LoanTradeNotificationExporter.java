package com.syndloanhub.loansum.fpml;

import java.time.LocalDate;
import java.util.Random;

import javax.xml.datatype.DatatypeConfigurationException;

import com.opengamma.strata.basics.StandardId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityIdentifier;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.InstrumentId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeEvent;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeNotification;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradeReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;

public class LoanTradeNotificationExporter {
  static public LoanTradeNotification convert(com.syndloanhub.loansum.product.facility.LoanTrade trade)
      throws DatatypeConfigurationException {
    FpMLHelper.clearPartyMap();

    LoanTradeNotification fpml = FpMLHelper.factory.createLoanTradeNotification();
    fpml.setTrade(LoanTradeExporter.convert(trade));
    fpml.setHeader(FpMLHelper.makeHeader());
    fpml.setFpmlVersion("5-11");
    fpml.setNoticeDate(LocalDate.now());
    fpml.setIsCorrection(false);
    fpml.setPartyReference(FpMLHelper.makePartyReference(trade.getProduct().getAgent()));
    fpml.setRole(FpMLHelper.makeTradingRole("Agent"));

    LoanTradeEvent event = FpMLHelper.factory.createLoanTradeEvent();
    event.getEventIdentifier().add(FpMLHelper.makeBusinessEventIdentifier(
        FpMLHelper.makePartyReference(trade.getProduct().getAgent())));
    LoanTradeReference ref = FpMLHelper.factory.createLoanTradeReference();
    ref.setHref(fpml.getTrade());
    event.setLoanTradeReference(ref);
    fpml.setLoanTradeEventGroup(FpMLHelper.factory.createLoanTrade(event));

    fpml.getParty().add((Party) fpml.getPartyReference().getHref());
    fpml.getParty().add((Party) fpml.getTrade().getBuyerPartyReference().getHref());
    fpml.getParty().add((Party) fpml.getTrade().getSellerPartyReference().getHref());

    FacilityIdentifier facid = FpMLHelper.factory.createFacilityIdentifier();
    facid.setId(FpMLHelper.nextID());
    facid.setPartyReference(fpml.getPartyReference());

    for (StandardId id : trade.getProduct().getIdentifiers()) {
      InstrumentId instrumentId = FpMLHelper.factory.createInstrumentId();
      instrumentId.setInstrumentIdScheme(id.getScheme());
      instrumentId.setValue(id.getValue());
      facid.getInstrumentId().add(instrumentId);
    }

    fpml.setFacilityIdentifier(facid);

    FacilityReference facref = FpMLHelper.factory.createFacilityReference();
    facref.setHref(facid);
    fpml.getTrade().setFacilityReference(facref);

    return fpml;
  }
}