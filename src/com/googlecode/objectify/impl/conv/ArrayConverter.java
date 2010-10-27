package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Blob;


/**
 * Knows how to convert java arrays.  Note that each individual element must be
 * converted as well. 
 */
public class ArrayConverter implements Converter
{
	/** Need this to convert members */
	Conversions conversions;
	
	/** */
	public ArrayConverter(Conversions conv)
	{
		this.conversions = conv;
	}
	
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (!value.getClass().isArray())
			return null;

		if (ctx.inEmbeddedCollection())
			throw new IllegalStateException("You cannot have arrays within @Embedded arrays or collections");
		
		if (value.getClass().getComponentType() == Byte.TYPE)
		{
			// Special case!  byte[] gets turned into Blob.
			return new Blob((byte[])value);
		}
		else
		{
			// The datastore cannot persist arrays, but it can persist ArrayList
			int length = Array.getLength(value);
			ArrayList<Object> list = new ArrayList<Object>(length);
			
			for (int i=0; i<length; i++)
				list.add(this.conversions.forDatastore(Array.get(value, i), ctx));
			
			return list;
		}
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (fieldType.isArray())
		{
			if (value instanceof Blob && fieldType.getComponentType().equals(Byte.TYPE))
			{
				return ((Blob)value).getBytes();
			}
			else if (value instanceof Collection<?>)
			{
				Class<?> componentType = fieldType.getComponentType();
				Collection<?> datastoreCollection = (Collection<?>)value;

				Object array = Array.newInstance(componentType, datastoreCollection.size());

				int index = 0;
				for (Object componentValue: datastoreCollection)
				{
					componentValue = this.conversions.forPojo(componentValue, componentType, ctx, onPojo);
					Array.set(array, index++, componentValue);
				}

				return array;
			}
		}

		return null;
	}
}