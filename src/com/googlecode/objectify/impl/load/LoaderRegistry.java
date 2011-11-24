package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.ListIterator;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;


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
	EmbedLoader<?> rootFactory;
	
	/**
	 * Initialize the default set of converters in the proper order.
	 */
	public LoaderRegistry(ObjectifyFactory fact)
	{
		this.fact = fact;
		
		rootFactory = fact.construct(EmbedLoader.class);
		
		// The order is CRITICAL!
		this.loaders.add(fact.construct(SerializeLoader.class));
		this.loaders.add(fact.construct(CollectionLoader.class));
		this.loaders.add(fact.construct(ArrayLoader.class));
		this.loaders.add(rootFactory);	// EmbeddedClassLoader
		
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
		return ((EmbedLoader<T>)rootFactory).createRoot(fact, type);
	}
	
	/**
	 * Goes through our list of known loaders and returns the first one that succeeds
	 * @param path is the path to this type, used for logging and debugging
	 * @throws IllegalStateException if no matching loader can be found
	 */
	public <T> Loader<T> create(Path path, Annotation[] fieldAnnotations, Type type) {
		for (LoaderFactory<?> cvt: this.loaders) {
			@SuppressWarnings("unchecked")
			Loader<T> soFar = (Loader<T>)cvt.create(fact, path, fieldAnnotations, type);
			if (soFar != null)
				return soFar;
		}
		
		throw new IllegalArgumentException("Don't know how to translate " + type);
	}
}