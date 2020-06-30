package com.syndloanhub.loansum.fpml;

import java.time.LocalDate;

import javax.xml.datatype.DatatypeConfigurationException;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.FacilityStatement;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;

public class FacilityStatementExporter {

  static public FacilityStatement convert(com.syndloanhub.loansum.product.facility.Facility facility) throws DatatypeConfigurationException {
    FacilityStatement fpml = FpMLHelper.factory.createFacilityStatement();
    fpml.setFpmlVersion("5-11");
    fpml.setHeader(FpMLHelper.makeHeader());
    fpml.setStatementDate(LocalDate.now());
    fpml.setIsCorrection(false);
    
    switch(facility.getFacilityType()) {
      case Term:
        fpml.setFacilityGroup(FpMLHelper.factory.createTermLoan(TermLoanExporter.convert(facility)));
        break;
        default:
          break;
    }
    
    fpml.getParty().add((Party)fpml.getFacilityGroup().getValue().getAgentPartyReference().getHref());
    fpml.getParty().add((Party)fpml.getFacilityGroup().getValue().getBorrowerPartyReference().getHref());
   
    return fpml;
  }
}
