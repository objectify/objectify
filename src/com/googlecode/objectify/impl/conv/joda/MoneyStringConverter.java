package com.googlecode.objectify.impl.conv.joda;

import org.joda.money.Money;

import com.googlecode.objectify.impl.conv.ConverterCreateContext;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import com.googlecode.objectify.impl.conv.SimpleConverterFactory;
import com.googlecode.objectify.impl.conv.Converter;

/**
 * Stores Money as its string representation.  Note that this does not index properly;
 * you can't safely use inequality filters.  However, indexing is not always necessary
 * and this is a very useful thing to be able to store.
 */
public class MoneyStringConverter extends SimpleConverterFactory<Money, String>
{
	public MoneyStringConverter() {
		super(Money.class);
	}
	
	@Override
	protected Converter<Money, String> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<Money, String>() {

			@Override
			public String toDatastore(Money value, ConverterSaveContext ctx) {
				return value.toString();
			}

			@Override
			public Money toPojo(String value, ConverterLoadContext ctx) {
				return Money.parse(value);
			}
		};
	}
}