package com.googlecode.objectify.impl.conv;


/**
 * Knows how to convert Booleans.  This is only required because of the Java's
 * funky incoherence of primitive types vs wrapper types - when you have a primitive
 * type field, the value will be Boolean.class but the fieldType will be Boolean.TYPE.
 * The normal assignableTo test will fail and we'll go through the converters.  This
 * converter is just smart enough to recognize Boolean.TYPE and continue on as normal
 * for the (expected) wrapper type.
 */
public class BooleanConverter extends SimpleConverterFactory<Boolean, Boolean>
{
	public BooleanConverter() {
		super(Boolean.TYPE);
	}
	
	@Override
	protected Converter<Boolean, Boolean> create(final Class<?> type, ConverterCreateContext ctx) {
		return new Converter<Boolean, Boolean>() {
			
			@Override
			public Boolean toPojo(Boolean value, ConverterLoadContext ctx) {
				return value;
			}
			
			@Override
			public Boolean toDatastore(Boolean value, ConverterSaveContext ctx) {
				return value;
			}
		};
	}
}