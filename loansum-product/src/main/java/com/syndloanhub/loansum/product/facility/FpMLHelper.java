package com.syndloanhub.loansum.product.facility;

import java.math.BigDecimal;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingFormOfPurchaseEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.NonNegativeMoney;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.RequestMessageHeader;

public final class FpMLHelper {
  private static final Logger log = LoggerFactory.getLogger(FpMLHelper.class);
  private static final ObjectFactory factory = new ObjectFactory();

  public final static com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency exportCurrency(Currency value) {
    com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = factory.createCurrency();
    fpml.setValue(value.toString());
    return fpml;
  }

  public final static JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency>
      exportCurrencyElement(Currency value) {
    com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = exportCurrency(value);
    return new JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency>(new QName(value.getClass().getSimpleName()),
        com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency.class, fpml);
  }

  public final static NonNegativeMoney exportCurrencyAmount(CurrencyAmount value) {
    NonNegativeMoney fpml = factory.createNonNegativeMoney();
    fpml.setCurrency(exportCurrency(value.getCurrency()));
    fpml.setAmount(BigDecimal.valueOf(value.getAmount()));
    return fpml;
  }

  public final static JAXBElement<NonNegativeMoney> exportCurrencyAmountElement(CurrencyAmount value) {
    final NonNegativeMoney export = exportCurrencyAmount(value);
    final XmlType xmlType = export.getClass().getAnnotation(XmlType.class);
    final JAXBElement<NonNegativeMoney> element =
        new JAXBElement<NonNegativeMoney>(new QName(xmlType.name()), NonNegativeMoney.class,
            exportCurrencyAmount(value));

    return element;
  }

  public final static JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade>
      createLoanTradeElement(com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade export) {
    final XmlType xmlType = export.getClass().getAnnotation(XmlType.class);
    final JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade> element =
        new JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade>(new QName(xmlType.name()),
            com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade.class,
            export);

    return element;
  }

  public final static LoanTradingFormOfPurchaseEnum convert(LoanTradingFormOfPurchase fop) {
    switch (fop) {
      case Assignment:
        return LoanTradingFormOfPurchaseEnum.ASSIGNMENT;
      case Participation:
        return LoanTradingFormOfPurchaseEnum.PARTICIPATION;
      default:
        return LoanTradingFormOfPurchaseEnum.ASSIGNMENT;
    }
  }
  
  public final static RequestMessageHeader getHeader(ObjectFactory factory) {
	  RequestMessageHeader header = factory.createRequestMessageHeader();
	  return header;
  }
}
