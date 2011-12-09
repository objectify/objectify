package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;


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
		this.translators.add(new SerializeTranslatorFactory());	// Serialize has priority over everything
		this.translators.add(new ByteArrayTranslatorFactory());
		this.translators.add(new ArrayTranslatorFactory());		// AFTER byte array otherwise we will occlude it
		this.translators.add(new CollectionTranslatorFactory());
		this.translators.add(new MapifyTranslatorFactory());
		this.translators.add(new MapTranslatorFactory());
		this.translators.add(new EmbedClassTranslatorFactory<Object>());	// AFTER the various collections so we only process the content
		this.translators.add(new EntityReferenceTranslatorFactory());	// AFTER embed so that you can embed entities if you want
		
		// Magic inflection point at which we want to prioritize added translators
		this.insertPoint = this.translators.size();
		
		this.translators.add(new StringTranslatorFactory());
		this.translators.add(new NumberTranslatorFactory());
		this.translators.add(new KeyTranslatorFactory());
		this.translators.add(new RefTranslatorFactory());
		this.translators.add(new EnumTranslatorFactory());
		this.translators.add(new SqlDateTranslatorFactory());
		this.translators.add(new TimeZoneTranslatorFactory());
		
		// LAST!  It catches everything.
		this.translators.add(new AsIsTranslatorFactory());
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
	public <T> Translator<T> create(Path path, Property property, Type type, CreateContext ctx) {
		for (TranslatorFactory<?> trans: this.translators) {
			@SuppressWarnings("unchecked")
			Translator<T> soFar = (Translator<T>)trans.create(path, property, type, ctx);
			if (soFar != null)
				return soFar;
		}
		
		throw new IllegalArgumentException("Don't know how to translate " + type);
	}
}