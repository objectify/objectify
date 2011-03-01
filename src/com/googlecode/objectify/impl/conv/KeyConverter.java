package com.googlecode.objectify.impl.conv;

import com.googlecode.objectify.Key;


/**
 * Knows how to convert Key<?> objects to datastore-native Key objects and vice-versa.
 */
public class KeyConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Key<?>)
			return ((Key<?>)value).getRaw();
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (Key.class.isAssignableFrom(fieldType) && value instanceof com.google.appengine.api.datastore.Key)
			return new Key<Object>((com.google.appengine.api.datastore.Key)value);
		else
			return null;
	}
}