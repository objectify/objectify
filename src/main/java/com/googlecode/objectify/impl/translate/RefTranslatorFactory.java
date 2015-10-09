package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.LoadConditions;
import com.googlecode.objectify.impl.Path;


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
	protected ValueTranslator<Ref<?>, com.google.appengine.api.datastore.Key> createValueTranslator(TypeKey<Ref<?>> tk, CreateContext ctx, Path path) {

		final LoadConditions loadConditions = new LoadConditions(tk.getAnnotation(Load.class));

		return new ValueTranslator<Ref<?>, com.google.appengine.api.datastore.Key>(com.google.appengine.api.datastore.Key.class) {

			@Override
			protected Ref<?> loadValue(com.google.appengine.api.datastore.Key value, LoadContext ctx, Path path) throws SkipException {
				return ctx.loadRef(Key.create(value), loadConditions);
			}

			@Override
			protected com.google.appengine.api.datastore.Key saveValue(Ref<?> value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return ctx.saveRef(value, loadConditions);
			}
		};
	}
}
