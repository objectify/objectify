package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;


/**
 * Knows how to convert Ref<?> objects to datastore-native Key objects and vice-versa.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RefTranslatorFactory extends ValueTranslatorFactory<Ref<?>, com.google.appengine.api.datastore.Key>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public RefTranslatorFactory() {
		super((Class)Ref.class);
	}

	@Override
	protected ValueTranslator<Ref<?>, com.google.appengine.api.datastore.Key> createSafe(Path path, final Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<Ref<?>, com.google.appengine.api.datastore.Key>(path, com.google.appengine.api.datastore.Key.class) {
			@Override
			protected Ref<?> loadValue(com.google.appengine.api.datastore.Key value, LoadContext ctx) {
				return ctx.makeRef(property, Key.create(value));
			}

			@Override
			protected com.google.appengine.api.datastore.Key saveValue(Ref<?> value, SaveContext ctx) {

				ctx.registerReference(property, value);

				return value.key().getRaw();
			}
		};
	}
}