package com.googlecode.objectify.impl.conv.joda;

import org.joda.time.DateTimeZone;

import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;


/**
 * Stores a joda DateTimeZone as its id. 
 */
public class DateTimeZoneConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof DateTimeZone)
			return ((DateTimeZone) value).getID();
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value instanceof String && DateTimeZone.class.isAssignableFrom(fieldType))
			return DateTimeZone.forID((String)value);
		else
			return null;
	}
}