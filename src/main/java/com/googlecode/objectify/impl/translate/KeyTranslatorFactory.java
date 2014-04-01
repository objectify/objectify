package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;


/**
 * Knows how to convert Key<?> objects to datastore-native Key objects and vice-versa.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyTranslatorFactory extends ValueTranslatorFactory<Key<?>, com.google.appengine.api.datastore.Key>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public KeyTranslatorFactory() {
		super((Class)Key.class);
	}

	@Override
	protected ValueTranslator<Key<?>, com.google.appengine.api.datastore.Key> createValueTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<Key<?>, com.google.appengine.api.datastore.Key>(com.google.appengine.api.datastore.Key.class) {
			@Override
			protected Key<?> loadValue(com.google.appengine.api.datastore.Key value, LoadContext ctx, Path path) throws SkipException {
				return Key.create(value);
			}

			@Override
			protected com.google.appengine.api.datastore.Key saveValue(Key<?> value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.getRaw();
			}
		};
	}
}