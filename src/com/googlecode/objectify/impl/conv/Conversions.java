package com.googlecode.objectify.impl.conv;

import java.util.LinkedList;
import java.util.ListIterator;

import com.googlecode.objectify.ObjectifyFactory;


/** 
 * <p>Manages all the converters used to translate between POJO fields and the
 * types that the Datastore can actually persist.  Essentially acts as an
 * aggregator for all the Converter objects.</p>
 * 
 * <p>Note that this does NOT implement Converter; the return values
 * of the methods are not the same when a conversion is not found.</p>
 * 
 * <p>THIS API IS EXPERIMENTAL.  It may change significantly in minor point releases.</p>
 */
public class Conversions
{
	LinkedList<Converter> converters = new LinkedList<Converter>();
	
	/** This lets us insert in order at the head of the list*/
	ListIterator<Converter> inserter;
	
	/**
	 * Initialize the default set of converters.
	 */
	public Conversions(ObjectifyFactory fact)
	{
		this.converters.add(new StringConverter());
		this.converters.add(new NumberConverter());
		this.converters.add(new BooleanConverter());
		this.converters.add(new EnumConverter());
		this.converters.add(new KeyConverter());
		this.converters.add(new ArrayConverter(this));
		this.converters.add(new CollectionConverter(this));
		this.converters.add(new SqlDateConverter());
		this.converters.add(new TimeZoneConverter());
		
		this.inserter = this.converters.listIterator();
	}
	
	/**
	 * Add a new converter to the list.  Converters are added in order but
	 * before the builtin conversions.
	 */
	public void add(Converter cvt)
	{
		this.inserter.add(cvt);
	}

	/**
	 * Run the value through all the converters; the first converter that returns
	 * a non-null value produces the response for this method.
	 * 
	 * @return the original value if no hits.  This is different than the normal
	 * return value defined by the Converter interface.
	 */
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value == null)
			return null;
		
		for (Converter cvt: this.converters)
		{
			Object soFar = cvt.forDatastore(value, ctx);
			if (soFar != null)
				return soFar;
		}
		
		return value;
	}

	/**
	 * Run the value through all the converters; the first converter that returns
	 * a non-null value produces the response for this method.
	 * 
	 * @param fieldType if null will simply return the value as-is; we can't do any conversion if we don't know the type!
	 * 
	 * @return the converted object
	 * @throws IllegalArgumentException if we weren't able to find a proper conversion
	 */
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value == null)
			return null;
		
		if (fieldType == null)
			return value;
		
		for (Converter cvt: this.converters)
		{
			Object soFar = cvt.forPojo(value, fieldType, ctx, onPojo);
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