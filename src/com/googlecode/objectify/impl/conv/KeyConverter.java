package com.googlecode.objectify.impl.conv;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;


/**
 * Knows how to convert Key<?> objects to datastore-native Key objects and vice-versa.
 */
public class KeyConverter implements Converter
{
	/** Need this to do conversions */
	ObjectifyFactory factory;
	
	/** */
	public KeyConverter(ObjectifyFactory fact)
	{
		this.factory = fact;
	}
	
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Key<?>)
			return this.factory.typedKeyToRawKey((Key<?>)value);
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (Key.class.isAssignableFrom(fieldType) && value instanceof com.google.appengine.api.datastore.Key)
			return this.factory.rawKeyToTypedKey((com.google.appengine.api.datastore.Key)value);
		else
			return null;
	}
}