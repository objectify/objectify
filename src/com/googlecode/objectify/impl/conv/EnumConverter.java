package com.googlecode.objectify.impl.conv;



/**
 * Knows how to convert Enums to the datastore String
 */
public class EnumConverter extends SimpleConverterFactory<Enum<?>, String> {
	
	/** */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EnumConverter() {
		super((Class)Enum.class);
	}
	
	@Override
	public Converter<Enum<?>, String> create(final Class<?> type, ConverterCreateContext ctx) {
		return new Converter<Enum<?>, String>() {
			@Override
			public String toDatastore(Enum<?> value, ConverterSaveContext ctx) {
				return value.name();
			}

			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Enum<?> toPojo(String value, ConverterLoadContext ctx) {
				// Anyone have any idea how to avoid this generics warning?
				return Enum.valueOf((Class<Enum>)type, value.toString());
			}
		};
	}
}