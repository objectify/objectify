package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Type;


/**
 * 
 */
public interface ConverterFactory<P, D>
{
	/**
	 * Try to create a converter that will actually process the specific type.
	 * @return null if this factory does not know how to deal with that type. 
	 */
	Converter<P, D> create(Type type, ConverterCreateContext ctx, ConverterRegistry conv);
}