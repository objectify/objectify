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
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toDatastore(java.lang.Object, com.googlecode.objectify.impl.conv.ConverterContext)
	 */
	@Override
	public Object toDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Boolean)
			return value;
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toPojo(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx)
	{
		if ((fieldType == Boolean.TYPE) && (value instanceof Boolean))
			return value;
		else
			return null;
	}
}