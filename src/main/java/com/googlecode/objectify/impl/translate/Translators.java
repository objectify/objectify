package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


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
public class Translators
{
	/** */
	ObjectifyFactory fact;

	/** */
	List<TranslatorFactory<?, ?>> translatorFactories = new ArrayList<>();

	/** Where we should insert new translators */
	int insertPoint;
	
	/** Where we should insert new early translators */
	int earlyInsertPoint;

	/** */
	Map<TranslatorKey, Translator<?, ?>> translators = new ConcurrentHashMap<>();

	/**
	 * Initialize the default set of converters in the proper order.
	 */
	public Translators(ObjectifyFactory fact)
	{
		this.fact = fact;

		// The order is CRITICAL!
		this.translatorFactories.add(new TranslateTranslatorFactory(true));	// Early translators get first shot at everything

		// Magic inflection point at which we want to prioritize added EARLY translators
		this.earlyInsertPoint = this.translatorFactories.size();

		this.translatorFactories.add(new OwnerTranslatorFactory());
		this.translatorFactories.add(new SerializeTranslatorFactory());	// Serialize has priority over everything
		this.translatorFactories.add(new ByteArrayTranslatorFactory());
		this.translatorFactories.add(new ArrayTranslatorFactory());		// AFTER byte array otherwise we will occlude it
		this.translatorFactories.add(new CollectionTranslatorFactory());
		this.translatorFactories.add(new MapifyTranslatorFactory());
		this.translatorFactories.add(new EmbeddedMapTranslatorFactory());
		this.translatorFactories.add(new TranslateTranslatorFactory(false));	// Late translators get a shot after collections

		// Magic inflection point at which we want to prioritize added normal translators
		this.insertPoint = this.translatorFactories.size();

		this.translatorFactories.add(new StringTranslatorFactory());
		this.translatorFactories.add(new TextTranslatorFactory());
		this.translatorFactories.add(new NumberTranslatorFactory());
		this.translatorFactories.add(new KeyTranslatorFactory());
		this.translatorFactories.add(new RefTranslatorFactory());
		this.translatorFactories.add(new EnumTranslatorFactory());
		this.translatorFactories.add(new SqlDateTranslatorFactory());
		this.translatorFactories.add(new TimeZoneTranslatorFactory());
		this.translatorFactories.add(new URLTranslatorFactory());

		// Things that just work as they are (fundamental datastore classes)
		this.translatorFactories.add(new AsIsTranslatorFactory());

		// LAST! It catches everything.
		this.translatorFactories.add(new ClassTranslatorFactory<Object>());
	}

	/**
	 * <p>Add a new translator to the list.  Translators are added in order after most of the "plumbing" translators
	 * (collections, arrays, maps, serialize, embeds, references) but before any of the standard value conversions
	 * like String, Number, Key, Enum, SqlDate, TimeZone, etc.</p>
	 *
	 * <p>Translators are added in-order so earlier translaters pre-empt later translators.</p>
	 */
	public void add(TranslatorFactory<?, ?> trans) {
		this.translatorFactories.add(insertPoint, trans);
		insertPoint++;
	}
	
	/**
	 * <p>Add a new translator to the beginning of the list, before all other translators
	 * except other translators that have been added early.</p>
	 */
	public void addEarly(TranslatorFactory<?, ?> trans) {
		this.translatorFactories.add(earlyInsertPoint, trans);
		earlyInsertPoint++;
		insertPoint++;
	}

	/**
	 * Obtains the Translator appropriate for this type and annotations. May be a cached
	 * translator; if not, one will be discovered and cached.
	 */
	public <P, D> Translator<P, D> get(Type type, Annotation[] annotations, CreateContext ctx, Path path) {

		TranslatorKey key = new TranslatorKey(type, annotations);

		Translator<?, ?> translator = translators.get(key);
		if (translator == null) {
			translator = create(type, annotations, ctx, path);
			translators.put(key, translator);
		}

		return (Translator<P, D>)translator;
	}

	/**
	 * Get the translator for a root entity class
	 */
	public <P> Translator<P, PropertyContainer> getRoot(Class<P> clazz) {
		return get(clazz, new Annotation[0], new CreateContext(fact), Path.root());
	}

	/**
	 * Create a translator from scratch by going through the discovery process.
	 */
	private Translator<?, ?> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		for (TranslatorFactory<?, ?> trans: this.translatorFactories) {
			Translator<?, ?> soFar = trans.create(type, annotations, ctx, path);
			if (soFar != null)
				return soFar;
		}

		throw new IllegalArgumentException("Don't know how to translate " + type + " with annotations " + Arrays.toString(annotations));
	}
}