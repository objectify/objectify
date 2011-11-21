package com.googlecode.objectify.impl.conv.joda;

import org.joda.money.BigMoney;

import com.googlecode.objectify.impl.conv.ConverterCreateContext;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import com.googlecode.objectify.impl.conv.SimpleConverterFactory;
import com.googlecode.objectify.impl.conv.Converter;

/**
 * Stores BigMoney as its string representation.  Note that this does not index properly;
 * you can't safely use inequality filters.  However, indexing is not always necessary
 * and this is a very useful thing to be able to store.
 */
public class BigMoneyStringConverter extends SimpleConverterFactory<BigMoney, String>
{
	public BigMoneyStringConverter() {
		super(BigMoney.class);
	}
	
	@Override
	protected Converter<BigMoney, String> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<BigMoney, String>() {

			@Override
			public String toDatastore(BigMoney value, ConverterSaveContext ctx) {
				return value.toString();
			}

			@Override
			public BigMoney toPojo(String value, ConverterLoadContext ctx) {
				return BigMoney.parse(value);
			}
		};
	}
}