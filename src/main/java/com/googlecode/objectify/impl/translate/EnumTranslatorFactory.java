package com.googlecode.objectify.impl.translate;

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
	protected ValueTranslator<Enum<E>, String> createValueTranslator(final TypeKey<Enum<E>> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<Enum<E>, String>(String.class) {
			@Override
			@SuppressWarnings("unchecked")
			protected Enum<E> loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return Enum.valueOf((Class)tk.getTypeAsClass(), value);
			}

			@Override
			protected String saveValue(Enum<E> value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.name();
			}
		};
	}
}
