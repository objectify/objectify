package com.googlecode.objectify.impl.conv.joda;

import org.joda.money.Money;

import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;


/**
 * Stores Money as its string representation.  Note that this does not index properly;
 * you can't safely use inequality filters.  However, indexing is not always necessary
 * and this is a very useful thing to be able to store.
 */
public class MoneyStringConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Money)
			return ((Money) value).toString();
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value instanceof String && Money.class.isAssignableFrom(fieldType))
			return Money.parse((String)value);
		else
			return null;
	}
}