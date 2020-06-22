package com.syndloanhub.loansum.product.facility;

import java.time.LocalDate;

import javax.xml.datatype.DatatypeConfigurationException;

import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.OutstandingContractsStatement;

public class OutstandingContractsStatementExporter implements FpMLExportable<OutstandingContractsStatement> {
	private final Facility facility;

	public OutstandingContractsStatementExporter(Facility facility) {
		this.facility = facility;
	}

	@Override
	public OutstandingContractsStatement export() throws DatatypeConfigurationException {
		ObjectFactory factory = new ObjectFactory();
		OutstandingContractsStatement fpml = factory.createOutstandingContractsStatement();
		fpml.setFpmlVersion("5-11");
		fpml.setHeader(FpMLHelper.getHeader(factory));
		fpml.setStatementDate(LocalDate.now());
		return fpml;
	}

}
