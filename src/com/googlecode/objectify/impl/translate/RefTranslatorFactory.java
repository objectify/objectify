package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;


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
	protected ValueTranslator<Ref<?>, com.google.appengine.api.datastore.Key> createSafe(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx)
	{
		final Load load = TypeUtils.getAnnotation(Load.class, fieldAnnotations);
		
		return new ValueTranslator<Ref<?>, com.google.appengine.api.datastore.Key>(path, com.google.appengine.api.datastore.Key.class) {
			@Override
			protected Ref<?> loadValue(com.google.appengine.api.datastore.Key value, LoadContext ctx) {
				Ref<?> ref = Ref.create(Key.create(value));
				ctx.maybeLoadRef(load, ref);
				return ref;
			}
			
			@Override
			protected com.google.appengine.api.datastore.Key saveValue(Ref<?> value, SaveContext ctx) {
				return value.key().getRaw();
			}
		};
	}
}