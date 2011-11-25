package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedList;
import java.util.ListIterator;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;


/** 
 * <p>Manages all the translators used to map between POJO fields and the
 * types that the Datastore can actually persist.  Essentially acts as an
 * aggregator for all the TranslatorFactory objects.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslatorRegistry
{
	/** */
	ObjectifyFactory fact;
	
	/** */
	LinkedList<TranslatorFactory<?>> translators = new LinkedList<TranslatorFactory<?>>();
	
	/** This lets us insert in order at the head of the list*/
	ListIterator<TranslatorFactory<?>> inserter;
	
	/** We hold on to the root factory because it gets used specially for root entities */
	EmbedTranslatorFactory<?> rootFactory;
	
	/**
	 * Initialize the default set of converters in the proper order.
	 */
	public TranslatorRegistry(ObjectifyFactory fact)
	{
		this.fact = fact;
		
		// This is special, lets us create translators for root objects
		rootFactory = fact.construct(EmbedTranslatorFactory.class);
		
		// The order is CRITICAL!
		this.translators.add(fact.construct(SerializeTranslatorFactory.class));
		this.translators.add(fact.construct(ByteArrayTranslatorFactory.class));
		this.translators.add(fact.construct(ArrayTranslatorFactory.class));
		this.translators.add(fact.construct(CollectionTranslatorFactory.class));
		this.translators.add(fact.construct(MapTranslatorFactory.class));
		this.translators.add(rootFactory);	// EmbedTranslatorFactory
		
		// Magic inflection point at which we want to prioritize added translators
		this.inserter = this.translators.listIterator();
		
		this.translators.add(fact.construct(StringTranslatorFactory.class));
		this.translators.add(fact.construct(EnumTranslatorFactory.class));
		this.translators.add(fact.construct(KeyTranslatorFactory.class));
		this.translators.add(fact.construct(NumberTranslatorFactory.class));
		this.translators.add(fact.construct(SqlDateTranslatorFactory.class));
		this.translators.add(fact.construct(TimeZoneTranslatorFactory.class));
		
		// LAST!  It catches everything.
		this.translators.add(fact.construct(UnmodifiedValueTranslatorFactory.class));
	}
	
	/**
	 * Add a new translator to the list.  Translators are added in order at a very particular place in the chain.
	 */
	public void add(TranslatorFactory<?> trans) {
		this.inserter.add(trans);
	}
	
	/**
	 * Create a loader for root entity classes.
	 * 
	 * @throws IllegalStateException if no matching loader can be found
	 */
	@SuppressWarnings("unchecked")
	public <T> Translator<T> createRoot(Type type) {
		return ((EmbedTranslatorFactory<T>)rootFactory).createRoot(type, new CreateContext(fact));
	}
	
	/**
	 * Goes through our list of known translators and returns the first one that succeeds
	 * @param path is the path to this type, used for logging and debugging
	 * @throws IllegalStateException if no matching loader can be found
	 */
	public <T> Translator<T> create(Path path, Annotation[] fieldAnnotations, Type type) {
		CreateContext ctx = new CreateContext(fact);
		
		for (TranslatorFactory<?> trans: this.translators) {
			@SuppressWarnings("unchecked")
			Translator<T> soFar = (Translator<T>)trans.create(path, fieldAnnotations, type, ctx);
			if (soFar != null)
				return soFar;
		}
		
		throw new IllegalArgumentException("Don't know how to translate " + type);
	}
}