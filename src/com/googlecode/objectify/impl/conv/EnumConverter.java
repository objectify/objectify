package com.googlecode.objectify.impl.conv;


/**
 * Knows how to convert Enums 
 */
public class EnumConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Enum<?>)
			return ((Enum<?>)value).name();
		else
			return null;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (Enum.class.isAssignableFrom(fieldType))
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>)fieldType, value.toString());
		else
			return null;
	}
}