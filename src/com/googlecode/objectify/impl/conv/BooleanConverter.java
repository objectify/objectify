package com.googlecode.objectify.impl.conv;


/**
 * Knows how to convert Booleans.  This is only required because of the Java's
 * funky incoherence of primitive types vs wrapper types - when you have a primitive
 * type field, the value will be Boolean.class but the fieldType will be Boolean.TYPE.
 * The normal assignableTo test will fail and we'll go through the converters.  This
 * converter is just smart enough to recognize Boolean.TYPE and continue on as normal
 * for the (expected) wrapper type.
 */
public class BooleanConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Boolean)
			return value;
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if ((fieldType == Boolean.TYPE) && (value instanceof Boolean))
			return value;
		else
			return null;
	}
}