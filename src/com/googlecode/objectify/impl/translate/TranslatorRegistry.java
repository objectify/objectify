package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;


/** 
 * <p>Manages all the translators used to map between POJO fields and the
 * types that the Datastore can actually persist.  Essentially acts as an
 * aggregator for all the TranslatorFactory objects.</p>
 * 
 * <p>When Objectify arranges a translator for a type at registration time, it runs
 * through the available TranslatorFactory instances one at time looking for one that
 * will provide a Translator.  The first one found is kept and used during runtime
 * assembly and disassembly of entities.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TranslatorRegistry
{
	/** */
	ObjectifyFactory fact;
	
	/** */
	List<TranslatorFactory<?>> translators = new ArrayList<TranslatorFactory<?>>();
	
	/** Where we should insert new translators */
	int insertPoint;
	
	/**
	 * Initialize the default set of converters in the proper order.
	 */
	public TranslatorRegistry(ObjectifyFactory fact)
	{
		this.fact = fact;
		
		// The order is CRITICAL!
		this.translators.add(fact.construct(SerializeTranslatorFactory.class));	// Serialize has priority over everything
		this.translators.add(fact.construct(ByteArrayTranslatorFactory.class));
		this.translators.add(fact.construct(ArrayTranslatorFactory.class));		// AFTER byte array otherwise we will occlude it
		this.translators.add(fact.construct(CollectionTranslatorFactory.class));
		this.translators.add(fact.construct(MapTranslatorFactory.class));
		this.translators.add(fact.construct(EmbedClassTranslatorFactory.class));	// AFTER the various collections so we only process the content
		this.translators.add(fact.construct(ReferenceTranslatorFactory.class));	// AFTER embed so that you can embed entities if you want
		
		// Magic inflection point at which we want to prioritize added translators
		this.insertPoint = this.translators.size();
		
		this.translators.add(fact.construct(StringTranslatorFactory.class));
		this.translators.add(fact.construct(NumberTranslatorFactory.class));
		this.translators.add(fact.construct(KeyTranslatorFactory.class));
		this.translators.add(fact.construct(RefTranslatorFactory.class));
		this.translators.add(fact.construct(EnumTranslatorFactory.class));
		this.translators.add(fact.construct(SqlDateTranslatorFactory.class));
		this.translators.add(fact.construct(TimeZoneTranslatorFactory.class));
		
		// LAST!  It catches everything.
		this.translators.add(fact.construct(UnmodifiedValueTranslatorFactory.class));
	}
	
	/**
	 * <p>Add a new translator to the list.  Translators are added in order after most of the "plumbing" translators
	 * (collections, arrays, maps, serialize, embeds, references) but before any of the standard value conversions
	 * like String, Number, Key, Enum, SqlDate, TimeZone, etc.</p>
	 * 
	 * <p>Translators are added in-order so earlier translaters pre-empt later translators.</p>
	 */
	public void add(TranslatorFactory<?> trans) {
		this.translators.add(insertPoint, trans);
		insertPoint++;
	}
	
	/**
	 * Goes through our list of known translators and returns the first one that succeeds
	 * @param path is the path to this type, used for logging and debugging
	 * @param ctx is the context we pass down from the root
	 * @throws IllegalStateException if no matching loader can be found
	 */
	public <T> Translator<T> create(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx) {
		for (TranslatorFactory<?> trans: this.translators) {
			@SuppressWarnings("unchecked")
			Translator<T> soFar = (Translator<T>)trans.create(path, fieldAnnotations, type, ctx);
			if (soFar != null)
				return soFar;
		}
		
		throw new IllegalArgumentException("Don't know how to translate " + type);
	}
}