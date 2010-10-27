package com.googlecode.objectify.impl.conv;


/**
 * The datastore can't store java.sql.Date, but it can do java.util.Date. 
 */
public class SqlDateConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof java.sql.Date)
			return new java.util.Date(((java.sql.Date)value).getTime());
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value instanceof java.util.Date && fieldType == java.sql.Date.class)
			return new java.sql.Date(((java.util.Date)value).getTime());
		else
			return null;
	}
}