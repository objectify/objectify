package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Type;

/**
 * Provides a little boilerplate for converters that work on simple atomic types. 
 */
abstract public class SimpleConverterFactory<P, D> implements ConverterFactory<P, D>
{
	/** */
	Class<P> pojoType;
	
	/** */
	protected SimpleConverterFactory(Class<P> pojoType) {
		this.pojoType = pojoType;
	}
	
	@Override
	final public Converter<P, D> create(Type type, ConverterCreateContext ctx, ConverterRegistry conv) {
		if (type instanceof Class<?> && this.pojoType.isAssignableFrom((Class<?>)type)) {
			return create((Class<?>)type, ctx);
		} else {
			return null;
		}
	}
	
	/** 
	 * Create a converter for a simple atomc type.  Don't need to check for type matching.
	 * @param type is guaranteed to be assignable from Class<P> 
	 */
	abstract protected Converter<P, D> create(Class<?> type, ConverterCreateContext ctx);
}