package com.googlecode.objectify.impl.conv;


/**
 * Knows how to convert Enums 
 */
public class EnumConverter implements Converter
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toDatastore(java.lang.Object, com.googlecode.objectify.impl.conv.ConverterContext)
	 */
	@Override
	public Object toDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Enum<?>)
			return ((Enum<?>)value).name();
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toPojo(java.lang.Object, java.lang.Class)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx)
	{
		if (Enum.class.isAssignableFrom(fieldType))
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>)fieldType, value.toString());
		else
			return null;
	}
}