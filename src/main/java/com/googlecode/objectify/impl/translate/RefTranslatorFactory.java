package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.LoadConditions;
import com.googlecode.objectify.impl.Path;


/**
 * Knows how to convert Ref<?> objects to datastore-native Key objects and vice-versa.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RefTranslatorFactory extends ValueTranslatorFactory<Ref<?>, com.google.cloud.datastore.Key>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public RefTranslatorFactory() {
		super((Class)Ref.class);
	}

	@Override
	protected ValueTranslator<Ref<?>, com.google.cloud.datastore.Key> createValueTranslator(final TypeKey<Ref<?>> tk, final CreateContext ctx, final Path path) {

		final LoadConditions loadConditions = new LoadConditions(tk.getAnnotation(Load.class), tk.getAnnotation(Parent.class));

		return new ValueTranslator<Ref<?>, com.google.cloud.datastore.Key>(ValueType.KEY) {

			@Override
			protected Ref<?> loadValue(final Value<com.google.cloud.datastore.Key> value, final LoadContext ctx, final Path path) throws SkipException {
				return ctx.loadRef(Key.create(value.get()), loadConditions);
			}

			@Override
			protected Value<com.google.cloud.datastore.Key> saveValue(final Ref<?> value, final SaveContext ctx, final Path path) throws SkipException {
				return KeyValue.of(ctx.saveRef(value, loadConditions));
			}
		};
	}
}