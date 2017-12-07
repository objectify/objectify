package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.Path;

/**
 * Knows how to convert Enums to the datastore String
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EnumTranslatorFactory<E extends Enum<E>> extends ValueTranslatorFactory<Enum<E>, String> {
	
	/** */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EnumTranslatorFactory() {
		super((Class)Enum.class);
	}

	@Override
	protected ValueTranslator<Enum<E>, String> createValueTranslator(final TypeKey<Enum<E>> tk, final CreateContext ctx, final Path path) {
		return new ValueTranslator<Enum<E>, String>(ValueType.STRING) {

			@Override
			@SuppressWarnings("unchecked")
			protected Enum<E> loadValue(final Value<String> value, final LoadContext ctx, final Path path) throws SkipException {
				return Enum.valueOf((Class)tk.getTypeAsClass(), value.get());
			}

			@Override
			protected Value<String> saveValue(final Enum<E> value, final SaveContext ctx, final Path path) throws SkipException {
				return StringValue.of(value.name());
			}
		};
	}
}