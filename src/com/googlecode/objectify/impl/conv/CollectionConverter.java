package com.googlecode.objectify.impl.conv;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * Knows how to convert java Collections.  Note that each individual element must be
 * converted too, so the produced converter will aggregate another converter for the
 * component type.
 */
public class CollectionConverter implements ConverterFactory<Collection<?>, List<?>>
{
	@Override
	public Converter<Collection<?>, List<?>> create(Type type, ConverterCreateContext ctx, ConverterRegistry conv) {
		
		final Class<?> erasedType = GenericTypeReflector.erase(type);
		
		if (Collection.class.isAssignableFrom(erasedType)) {
			if (ctx.inEmbeddedCollection())
				throw new IllegalStateException("You cannot have collections inside @Embed arrays or collections");

			// Get type converter for the component 
			ParameterizedType superType = (ParameterizedType)GenericTypeReflector.getExactSuperType(type, Collection.class);
			final Type componentType = superType.getActualTypeArguments()[0];
			
			@SuppressWarnings("unchecked")
			final Converter<Object, Object> componentConverter = (Converter<Object, Object>)conv.create(componentType, ctx);
			
			return new Converter<Collection<?>, List<?>>() {
				/* */
				@Override
				public Collection<?> toPojo(List<?> value, ConverterLoadContext ctx) {
					@SuppressWarnings("unchecked")
					Collection<Object> target = (Collection<Object>)ctx.getField().get(ctx.getPojo());
					if (target == null) {
						target = TypeUtils.createCollection(erasedType, value.size());
						ctx.getField().set(ctx.getPojo(), target);
					}
							
					for (Object datastoreValue: value)
						target.add(componentConverter.toPojo(datastoreValue, ctx));
					
					return target;
				}
				
				/* */
				@Override
				public List<?> toDatastore(Collection<?> value, ConverterSaveContext ctx) {
					// All collections get turned into a List that preserves the order.  We must
					// also be sure to convert anything contained in the collection
					ArrayList<Object> list = new ArrayList<Object>(value.size());

					for (Object obj: value)
						list.add(componentConverter.toDatastore(obj, ctx));
					
					return list;
				}
			};
		} else {
			return null;
		}
	}
}