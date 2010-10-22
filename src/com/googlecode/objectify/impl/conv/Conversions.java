package com.googlecode.objectify.impl.conv;

import java.util.LinkedList;

import com.googlecode.objectify.ObjectifyFactory;


/** 
 * Manages all the converters used to translate between POJO fields and the
 * types that the Datastore can actually persist.  Essentially acts as an
 * aggregator for all the Converter objects.
 * 
 * Note that this implements Converter just for convenience; the return values
 * of the methods are not the same when a conversion is not found. 
 */
public class Conversions implements Converter
{
	LinkedList<Converter> converters = new LinkedList<Converter>();
	
	/**
	 * Initialize the default set of converters.
	 */
	public Conversions(ObjectifyFactory fact)
	{
		this.converters.add(new StringConverter());
		this.converters.add(new NumberConverter());
		this.converters.add(new BooleanConverter());
		this.converters.add(new EnumConverter());
		this.converters.add(new KeyConverter(fact));
		this.converters.add(new ArrayConverter(this));
		this.converters.add(new CollectionConverter(this));
		this.converters.add(new SqlDateConverter());
	}

	/**
	 * Run it through all the converters.
	 * @return the original value if no hits
	 */
	@Override
	public Object toDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value == null)
			return null;
		
		for (Converter cvt: this.converters)
		{
			Object soFar = cvt.toDatastore(value, ctx);
			if (soFar != null)
				return soFar;
		}
		
		return value;
	}

	/**
	 * Run it through all the converters.
	 * @throws IllegalArgumentException if we weren't able to find a proper conversion
	 */
	@Override
	public Object toPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx)
	{
		if (value == null)
			return null;
		
		for (Converter cvt: this.converters)
		{
			Object soFar = cvt.toPojo(value, fieldType, ctx);
			if (soFar != null)
				return soFar;
		}

		// We need to run the assignable check last because of generic collections
		// that need to have their contents processed (eg, a List<Enum>).
		if (fieldType.isAssignableFrom(value.getClass()))
			return value;
		
		throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + fieldType + " for " + ctx.getField());
	}
	
}