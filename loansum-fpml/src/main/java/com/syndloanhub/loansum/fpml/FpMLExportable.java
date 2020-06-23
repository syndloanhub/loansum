package com.syndloanhub.loansum.fpml;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

public interface FpMLExportable<T> {
	public abstract T export() throws DatatypeConfigurationException;

	@SuppressWarnings("unchecked")
	public default JAXBElement<T> exportElement() throws DatatypeConfigurationException {
		T value = export();
		JAXBElement<T> element = new JAXBElement<T>(new QName(value.getClass().getSimpleName()),
				(Class<T>) value.getClass(), value);
		return element;
	}
}
