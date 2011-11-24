package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.ListIterator;

import com.googlecode.objectify.ObjectifyFactory;


/** 
 * <p>Manages all the converters used to translate between POJO fields and the
 * types that the Datastore can actually persist.  Essentially acts as an
 * aggregator for all the Converter objects.</p>
 */
public class LoaderRegistry
{
	/** */
	ObjectifyFactory fact;
	
	/** */
	LinkedList<LoaderFactory<?>> loaders = new LinkedList<LoaderFactory<?>>();
	
	/** This lets us insert in order at the head of the list*/
	ListIterator<LoaderFactory<?>> inserter;
	
	/** We hold on to the root factory because it gets used specially for root entities */
	ClassLoadr<?> rootFactory;
	
	/**
	 * Initialize the default set of converters.
	 */
	public LoaderRegistry(ObjectifyFactory fact)
	{
		this.fact = fact;
		
		rootFactory = fact.construct(ClassLoadr.class);
		
		this.loaders.add(rootFactory);
		
//		this.converters.add(fact.construct(StringConverter.class));
//		this.converters.add(fact.construct(NumberConverter.class));
//		this.converters.add(fact.construct(BooleanConverter.class));
//		this.converters.add(fact.construct(EnumConverter.class));
//		this.converters.add(fact.construct(ByteArrayConverter.class));	// BEFORE the ArrayConverter
//		this.converters.add(fact.construct(ArrayConverter.class));
//		this.converters.add(fact.construct(CollectionConverter.class));
//		this.converters.add(fact.construct(SqlDateConverter.class));
//		this.converters.add(fact.construct(TimeZoneConverter.class));
//		this.converters.add(fact.construct(KeyConverter.class));
		
		this.inserter = this.loaders.listIterator();
	}
	
	/**
	 * Add a new loader to the list.  Loaders are added in order but before the builtin loaders.
	 */
	public void add(LoaderFactory<?> cvt) {
		this.inserter.add(cvt);
	}
	
	/**
	 * Create a loader for root entity classes.
	 * 
	 * @throws IllegalStateException if no matching loader can be found
	 */
	@SuppressWarnings("unchecked")
	public <T> Loader<T> createRoot(Type type) {
		return ((ClassLoadr<T>)rootFactory).createRoot(fact, type);
	}
	
	/**
	 * Goes through our list of known loaders and returns the first one that succeeds
	 * @throws IllegalStateException if no matching loader can be found
	 */
	public <T> Loader<T> create(Type type, Annotation[] fieldAnnotations) {
		for (LoaderFactory<?> cvt: this.loaders) {
			@SuppressWarnings("unchecked")
			Loader<T> soFar = (Loader<T>)cvt.create(fact, type, fieldAnnotations);
			if (soFar != null)
				return soFar;
		}
		
		throw new IllegalArgumentException("Don't know how to translate " + type);
	}
}