package com.syndloanhub.loansum.fpml;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.opengamma.strata.basics.StandardId;
import com.opengamma.strata.basics.currency.Currency;
import com.opengamma.strata.basics.currency.CurrencyAmount;
import com.opengamma.strata.basics.date.DayCount;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.DayCountFraction;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanContract;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTradingFormOfPurchaseEnum;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MessageAddress;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.MessageId;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.NonNegativeMoney;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.ObjectFactory;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.Party;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyName;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.PartyReference;
import com.syndloanhub.loansum.fpml.v5_11.confirmation.RequestMessageHeader;
import com.syndloanhub.loansum.product.facility.LoanTradingFormOfPurchase;

public final class FpMLHelper {
	public static final Logger log = LoggerFactory.getLogger(FpMLHelper.class);
	public static final ObjectFactory factory = new ObjectFactory();
	public static final String MESSAGE_ID_SCHEME = "http://www.syndloanhub.com/messaging/id";
	public static final String MESSAGE_ADDRESS_SCHEME = "http://www.syndloanhub.com/messaging/address";
	public static final String SYNDLOANHUB_EIN = "0600447425";
	public static final String NJEIN_SCHEME = "https://www.njportal.com";
	public static final String CUSIP_SCHEME = "http://www.fpml.org/coding-scheme/external/instrument-id-CUSIP";
	public static final String BBG_SCHEME = "http://www.fpml.org/coding-scheme/external/instrument-id-Bloomberg";
	public static final String PTY_SCHEME = "http://www.fpml.org/coding-scheme/external/iso9362";
	public static final String NA_SCHEME = "http://www.fpml.org/coding-scheme/external/na";
	public static final String DCF_SCHEME = "http://www.fpml.org/coding-scheme/day-count-fraction-2-2";
	
	private static Map<String, String> idMap = new HashMap<String, String>();
	private static int nextId = 0;

	public final static com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency exportCurrency(Currency value) {
		com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = factory.createCurrency();
		fpml.setValue(value.toString());
		return fpml;
	}

	public final static JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency> exportCurrencyElement(
			Currency value) {
		com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency fpml = exportCurrency(value);
		return new JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency>(
				new QName(value.getClass().getSimpleName()),
				com.syndloanhub.loansum.fpml.v5_11.confirmation.Currency.class, fpml);
	}

	public final static NonNegativeMoney exportCurrencyAmount(CurrencyAmount value) {
		NonNegativeMoney fpml = factory.createNonNegativeMoney();
		fpml.setCurrency(exportCurrency(value.getCurrency()));
		fpml.setAmount(value.getAmount());
		return fpml;
	}

	public final static JAXBElement<NonNegativeMoney> exportCurrencyAmountElement(CurrencyAmount value) {
		final NonNegativeMoney export = exportCurrencyAmount(value);
		final XmlType xmlType = export.getClass().getAnnotation(XmlType.class);
		final JAXBElement<NonNegativeMoney> element = new JAXBElement<NonNegativeMoney>(new QName(xmlType.name()),
				NonNegativeMoney.class, exportCurrencyAmount(value));

		return element;
	}

	public final static JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade> createLoanTradeElement(
			com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade export) {
		final XmlType xmlType = export.getClass().getAnnotation(XmlType.class);
		final JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade> element = new JAXBElement<com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade>(
				new QName(xmlType.name()), com.syndloanhub.loansum.fpml.v5_11.confirmation.LoanTrade.class, export);

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

	public final static XMLGregorianCalendar getXMLGregorianCalendarNow() throws DatatypeConfigurationException {
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
		XMLGregorianCalendar now = datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
		return now;
	}

	public final static RequestMessageHeader getHeader(ObjectFactory factory) throws DatatypeConfigurationException {
		RequestMessageHeader header = factory.createRequestMessageHeader();

		MessageId id = factory.createMessageId();
		id.setMessageIdScheme(MESSAGE_ID_SCHEME);
		id.setValue("" + new Random().nextInt());
		header.setMessageId(id);

		MessageAddress address = factory.createMessageAddress();
		address.setMessageAddressScheme(NJEIN_SCHEME);
		address.setValue(SYNDLOANHUB_EIN);
		header.setSentBy(address);

		header.setCreationTimestamp(getXMLGregorianCalendarNow());

		return header;
	}

	public final static PartyReference makePartyReference(ObjectFactory factory, StandardId id) {
		PartyReference ref = factory.createPartyReference();
		ref.setHref(id.toString());
		return ref;
	}

	public final static Party makeParty(ObjectFactory factory, StandardId id) {
		Party party = factory.createParty();
		party.setId(id.toString());
		return party;
	}
	
	public final static String makeID(String id) {
		if (!idMap.containsKey(id)) 
			idMap.put(id, String.format("ID%08d", nextId++));
		return idMap.get(id);
	}
	
	public final static DayCountFraction convert(DayCount dc) {
		DayCountFraction dc2 = factory.createDayCountFraction();
		dc2.setDayCountFractionScheme(DCF_SCHEME);
		dc2.setValue(DayCount.extendedEnum().externalNames("FpML").reverseLookup(dc));
		return dc2;
	}

}
