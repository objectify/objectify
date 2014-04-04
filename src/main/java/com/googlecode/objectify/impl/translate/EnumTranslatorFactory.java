package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Knows how to convert Enums to the datastore String
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EnumTranslatorFactory extends ValueTranslatorFactory<Enum<?>, String> {
	
	/** */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EnumTranslatorFactory() {
		super((Class)Enum.class);
	}

	@Override
	protected ValueTranslator<Enum<?>, String> createValueTranslator(final Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<Enum<?>, String>(String.class) {

			@Override
			@SuppressWarnings("unchecked")
			protected Enum<?> loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return Enum.valueOf((Class<Enum>)type, value);
			}

			@Override
			protected String saveValue(Enum<?> value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.name();
			}
		};
	}
}