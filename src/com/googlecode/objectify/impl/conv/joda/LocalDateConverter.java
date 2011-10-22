package com.googlecode.objectify.impl.conv.joda;

import org.joda.time.LocalDate;

import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;


/**
 * Stores LocalDate as a String in ISO format:  yyyy-MM-dd 
 */
public class LocalDateConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof LocalDate)
			return ((LocalDate) value).toString();
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value instanceof String && LocalDate.class.isAssignableFrom(fieldType))
			return new LocalDate(value);
		else
			return null;
	}
}