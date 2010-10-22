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
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toDatastore(java.lang.Object, com.googlecode.objectify.impl.conv.ConverterContext)
	 */
	@Override
	public Object toDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Key<?>)
			return this.factory.typedKeyToRawKey((Key<?>)value);
		else
			return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.conv.Converter#toPojo(java.lang.Object, java.lang.Class)
	 */
	@Override
	public Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx)
	{
		if (Key.class.isAssignableFrom(fieldType) && value instanceof com.google.appengine.api.datastore.Key)
			return this.factory.rawKeyToTypedKey((com.google.appengine.api.datastore.Key)value);
		else
			return null;
	}
}