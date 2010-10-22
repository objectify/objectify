package com.googlecode.objectify.impl.conv;


/**
 * The datastore can't store java.sql.Date, but it can do java.util.Date. 
 */
public class SqlDateConverter implements Converter
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toDatastore(java.lang.Object, com.googlecode.objectify.impl.conv.ConverterContext)
	 */
	@Override
	public Object toDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof java.sql.Date)
			return new java.util.Date(((java.sql.Date)value).getTime());
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toPojo(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx)
	{
		if (value instanceof java.util.Date && fieldType == java.sql.Date.class)
			return new java.sql.Date(((java.util.Date)value).getTime());
		else
			return null;
	}
}