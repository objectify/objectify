package com.googlecode.objectify.impl.conv;

import com.googlecode.objectify.Key;


/**
 * Knows how to convert Key<?> objects to datastore-native Key objects and vice-versa.
 */
public class KeyConverter extends SimpleConverterFactory<Key<?>, com.google.appengine.api.datastore.Key>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public KeyConverter() {
		super((Class)Key.class);
	}
	
	@Override
	protected Converter<Key<?>, com.google.appengine.api.datastore.Key> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<Key<?>, com.google.appengine.api.datastore.Key>() {
			
			@Override
			public Key<?> toPojo(com.google.appengine.api.datastore.Key value, ConverterLoadContext ctx) {
				return Key.create(value);
			}
			
			@Override
			public com.google.appengine.api.datastore.Key toDatastore(Key<?> value, ConverterSaveContext ctx) {
				return value.getRaw();
			}
		};
	}
}