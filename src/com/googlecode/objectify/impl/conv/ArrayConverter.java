package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * The datastore can't persist native java arrays, but it can persist ArrayList.
 * Note that each individual element must be converted as well.
 * 
 * This doesn't do any special handling of bye arrays (which get converted to blob); for that,
 * make sure the ByteArrayConverter is registered before this one.
 */
public class ArrayConverter implements ConverterFactory<Object, List<?>>
{
	@Override
	public Converter<Object, List<?>> create(Type type, ConverterCreateContext ctx, final ConverterRegistry conv) {
		final Type componentType = GenericTypeReflector.getArrayComponentType(type);
		if (componentType != null) {
			if (ctx.inEmbeddedCollection())
				throw new IllegalStateException("You cannot have arrays within @Embed arrays or collections");
			
			@SuppressWarnings("unchecked")
			final Converter<Object, Object> componentConverter = (Converter<Object, Object>)conv.create(componentType, ctx);
			
			return new Converter<Object, List<?>>() {
				
				@Override
				public Object toPojo(List<?> value, ConverterLoadContext ctx) {
					Class<?> componentTypeClass = GenericTypeReflector.erase(componentType);

					Object array = Array.newInstance(componentTypeClass, value.size());

					int index = 0;
					for (Object componentValue: value) {
						componentValue = componentConverter.toPojo(componentValue, ctx);
						Array.set(array, index++, componentValue);
					}

					return array;
				}
				
				@Override
				public List<?> toDatastore(Object value, ConverterSaveContext ctx) {
					// The datastore cannot persist arrays, but it can persist ArrayList
					int length = Array.getLength(value);
					ArrayList<Object> list = new ArrayList<Object>(length);
					
					for (int i=0; i<length; i++)
						list.add(componentConverter.toDatastore(Array.get(value, i), ctx));
					
					return list;
				}
			};
		} else {
			return null;
		}
	}
}