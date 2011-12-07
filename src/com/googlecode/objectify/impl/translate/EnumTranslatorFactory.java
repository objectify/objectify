package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;

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
	protected ValueTranslator<Enum<?>, String> createSafe(Path path, Property property, final Type type, CreateContext ctx)
	{
		return new ValueTranslator<Enum<?>, String>(path, String.class) {
			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Enum<?> loadValue(String value, LoadContext ctx) {
				// Anyone have any idea how to avoid this generics warning?
				return Enum.valueOf((Class<Enum>)type, value.toString());
			}
			
			@Override
			protected String saveValue(Enum<?> value, SaveContext ctx) {
				return value.name();
			}
		};
	}
}