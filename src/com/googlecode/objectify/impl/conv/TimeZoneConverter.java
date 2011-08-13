package com.googlecode.objectify.impl.conv;

import java.util.TimeZone;


/**
 * Converts java.util.TimeZone 
 */
public class TimeZoneConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof TimeZone)
			return ((TimeZone)value).getID();
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value instanceof String && fieldType == TimeZone.class)
			return TimeZone.getTimeZone((String)value);
		else
			return null;
	}
}