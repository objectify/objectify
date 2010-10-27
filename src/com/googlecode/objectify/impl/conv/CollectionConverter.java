package com.googlecode.objectify.impl.conv;

import java.util.ArrayList;
import java.util.Collection;

import com.googlecode.objectify.impl.TypeUtils;


/**
 * Knows how to convert java Collections.  Note that each individual element must be
 * converted. 
 */
public class CollectionConverter implements Converter
{
	/** Need this to convert members */
	Conversions conversions;
	
	/** */
	public CollectionConverter(Conversions conv)
	{
		this.conversions = conv;
	}
	
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Collection<?>)
		{
			if (ctx.inEmbeddedCollection())
				throw new IllegalStateException("You cannot have collections inside @Embedded arrays or collections");
			
			// All collections get turned into a List that preserves the order.  We must
			// also be sure to convert anything contained in the collection
			ArrayList<Object> list = new ArrayList<Object>(((Collection<?>)value).size());

			for (Object obj: (Collection<?>)value)
				list.add(this.conversions.forDatastore(obj, ctx));
			
			return list;
		}
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (Collection.class.isAssignableFrom(fieldType) && value instanceof Collection<?>)
		{
			Class<?> componentType = TypeUtils.getComponentType(fieldType, ctx.getField().getGenericType());

			Collection<?> datastoreCollection = (Collection<?>)value;
			Collection<Object> target = TypeUtils.prepareCollection(onPojo, ctx.getField(), datastoreCollection.size());

			for (Object datastoreValue: datastoreCollection)
				target.add(this.conversions.forPojo(datastoreValue, componentType, ctx, onPojo));
			
			return target;
		}
		else
			return null;
	}
}