package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;


/**
 * Knows how to convert Key<?> objects to datastore-native Key objects and vice-versa.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyTranslatorFactory extends AbstractValueTranslatorFactory<Key<?>, com.google.appengine.api.datastore.Key>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public KeyTranslatorFactory() {
		super((Class)Key.class);
	}

	@Override
	protected AbstractValueTranslator<Key<?>, com.google.appengine.api.datastore.Key> createSafe(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type)
	{
		return new AbstractValueTranslator<Key<?>, com.google.appengine.api.datastore.Key>(path, com.google.appengine.api.datastore.Key.class) {
			@Override
			protected Key<?> loadValue(com.google.appengine.api.datastore.Key value, LoadContext ctx) {
				return Key.create(value);
			}
			
			@Override
			protected com.google.appengine.api.datastore.Key saveValue(Key<?> value, SaveContext ctx) {
				return value.getRaw();
			}
		};
	}
}