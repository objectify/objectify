package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.ListIterator;

import com.googlecode.objectify.ObjectifyFactory;


/** 
 * <p>Manages all the converters used to translate between POJO fields and the
 * types that the Datastore can actually persist.  Essentially acts as an
 * aggregator for all the Converter objects.</p>
 */
public class ConverterRegistry
{
	LinkedList<ConverterFactory<?, ?>> converters = new LinkedList<ConverterFactory<?, ?>>();
	
	/** This lets us insert in order at the head of the list*/
	ListIterator<ConverterFactory<?, ?>> inserter;
	
	/**
	 * Initialize the default set of converters.
	 */
	public ConverterRegistry(ObjectifyFactory fact)
	{
		this.converters.add(fact.construct(StringConverter.class));
		this.converters.add(fact.construct(NumberConverter.class));
		this.converters.add(fact.construct(BooleanConverter.class));
		this.converters.add(fact.construct(EnumConverter.class));
		this.converters.add(fact.construct(ByteArrayConverter.class));	// BEFORE the ArrayConverter
		this.converters.add(fact.construct(ArrayConverter.class));
		this.converters.add(fact.construct(CollectionConverter.class));
		this.converters.add(fact.construct(SqlDateConverter.class));
		this.converters.add(fact.construct(TimeZoneConverter.class));
		this.converters.add(fact.construct(KeyConverter.class));
		
		this.inserter = this.converters.listIterator();
	}
	
	/**
	 * Add a new converter to the list.  Converters are added in order but
	 * before the builtin conversions.
	 */
	public void add(ConverterFactory<?, ?> cvt)
	{
		this.inserter.add(cvt);
	}
	
	/**
	 * Goes through our list of known converters and returns the first one that succeeds
	 * @throws IllegalStateException if no matching converter can be found
	 */
	public Converter<?, ?> create(Type type, ConverterCreateContext ctx) {
		for (ConverterFactory<?, ?> cvt: this.converters) {
			Converter<?, ?> soFar = cvt.create(type, ctx, this);
			if (soFar != null)
				return soFar;
		}
		
		throw new IllegalArgumentException("Don't know how to convert " + type);
	}
	
//	/** Get a type converter for a field */
//	public TypeConverter<?, ?> getTypeConverter(Field field) {
//		
//	}
//	
//	public TypeConverter<?, ?> getTypeConverter(Class<?> clazz) {
//		
//	}
}