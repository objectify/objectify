package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.impl.Path;


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
	protected ValueTranslator<Key<?>, com.google.appengine.api.datastore.Key> createValueTranslator(TypeKey<Key<?>> tk, CreateContext ctx, Path path) {
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
