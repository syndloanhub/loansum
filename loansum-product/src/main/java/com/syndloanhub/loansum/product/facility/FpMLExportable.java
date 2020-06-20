package com.syndloanhub.loansum.product.facility;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.namespace.QName;

public interface FpMLExportable<T> {
  public abstract T export() throws DatatypeConfigurationException;

  public default JAXBElement<T> exportElement() throws DatatypeConfigurationException {
    System.out.println("JJS exportElement");
    T value = export();
    JAXBElement<T> element = new JAXBElement<T>(new QName(value.getClass().getSimpleName()), (Class<T>) value.getClass(), value);
    return element;
  }
}
