package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;

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
	private final ObjectifyFactory fact;

	/** */
	private final List<TranslatorFactory<?, ?>> translatorFactories = new ArrayList<>();

	/** Where we should insert new translators */
	private int insertPoint;
	
	/** Where we should insert new early translators */
	private int earlyInsertPoint;

	/**
	 * This needs to be threadsafe because new translators can show up at runtime. Filtering
	 * and ObjectTranslatorFactory can both create new translators dynamically.
	 */
	private final Map<TypeKey, Translator<?, ?>> translators = new ConcurrentHashMap<>();

	/**
	 * Initialize the default set of converters in the proper order.
	 */
	public Translators(final ObjectifyFactory fact) {
		this.fact = fact;

		// The order is CRITICAL!
		this.translatorFactories.add(new TranslateTranslatorFactory(true));	// Early translators get first shot at everything

		// Magic inflection point at which we want to prioritize added EARLY translators
		this.earlyInsertPoint = this.translatorFactories.size();

		// Annotation based translators go first
		this.translatorFactories.add(new ContainerTranslatorFactory());
		this.translatorFactories.add(new SerializeTranslatorFactory());	// Serialize has priority over everything
		this.translatorFactories.add(new MapifyTranslatorFactory());

		// Magic inflection point at which we want to prioritize added normal translators
		this.insertPoint = this.translatorFactories.size();

		this.translatorFactories.add(new ByteArrayTranslatorFactory());
		this.translatorFactories.add(new ArrayTranslatorFactory());		// AFTER byte array otherwise we will occlude it
		this.translatorFactories.add(new CollectionTranslatorFactory());
		this.translatorFactories.add(new EmbeddedMapTranslatorFactory());
		this.translatorFactories.add(new TranslateTranslatorFactory(false));	// Late translators get a shot after collections

		this.translatorFactories.add(new StringTranslatorFactory());
		this.translatorFactories.add(new IntegerTranslatorFactory());
		this.translatorFactories.add(new FloatTranslatorFactory());
		this.translatorFactories.add(new BooleanTranslatorFactory());
		this.translatorFactories.add(new KeyTranslatorFactory());
		this.translatorFactories.add(new RawKeyTranslatorFactory());
		this.translatorFactories.add(new RawEntityTranslatorFactory());
		this.translatorFactories.add(new TimestampTranslatorFactory());
		this.translatorFactories.add(new RefTranslatorFactory());
		this.translatorFactories.add(new EnumTranslatorFactory());
		this.translatorFactories.add(new InstantTranslatorFactory());
		this.translatorFactories.add(new SqlDateTranslatorFactory());
		this.translatorFactories.add(new DateTranslatorFactory());
		this.translatorFactories.add(new TimeZoneTranslatorFactory());
		this.translatorFactories.add(new URLTranslatorFactory());
		this.translatorFactories.add(new LatLngTranslatorFactory());
		this.translatorFactories.add(new BlobTranslatorFactory());
		this.translatorFactories.add(new RawValueTranslatorFactory());
		this.translatorFactories.add(new ObjectTranslatorFactory(this));

		// LAST! It catches everything.
		this.translatorFactories.add(new ClassTranslatorFactory<>());
	}

	/**
	 * <p>Add a new translator to the list.  Translators are added in order after most of the "plumbing" translators
	 * (collections, arrays, maps, serialize, embeds, references) but before any of the standard value conversions
	 * like String, Number, Key, Enum, SqlDate, TimeZone, etc.</p>
	 *
	 * <p>Translators are added in-order so earlier translaters pre-empt later translators.</p>
	 */
	public void add(final TranslatorFactory<?, ?> trans) {
		this.translatorFactories.add(insertPoint, trans);
		insertPoint++;
	}
	
	/**
	 * <p>Add a new translator to the beginning of the list, before all other translators
	 * except other translators that have been added early.</p>
	 */
	public void addEarly(final TranslatorFactory<?, ?> trans) {
		this.translatorFactories.add(earlyInsertPoint, trans);
		earlyInsertPoint++;
		insertPoint++;
	}

	/**
	 * Obtains the Translator appropriate for this type and annotations. May be a cached
	 * translator; if not, one will be discovered and cached.
	 */
	public <P, D> Translator<P, D> get(final TypeKey tk, final CreateContext ctx, final Path path) {

		Translator<?, ?> translator = translators.get(tk);
		if (translator == null) {
			translator = create(tk, ctx, path);
			translators.put(tk, translator);
		}

		//noinspection unchecked
		return (Translator<P, D>)translator;
	}

	/**
	 * Get the translator for a root entity class
	 */
	public <P> Translator<P, FullEntity<?>> getRoot(final Class<P> clazz) {
		return get(new TypeKey(clazz), new CreateContext(fact), Path.root());
	}

	/**
	 * Create a translator from scratch by going through the discovery process.
	 */
	private Translator<?, ?> create(final TypeKey tk, final CreateContext ctx, final Path path) {
		for (final TranslatorFactory<?, ?> trans: this.translatorFactories) {
			@SuppressWarnings("unchecked")
			final Translator<?, ?> soFar = trans.create(tk, ctx, path);
			if (soFar != null)
				return soFar;
		}

		throw new IllegalArgumentException("Don't know how to translate " + tk.getType() + " with annotations " + Arrays.toString(tk.getAnnotations()));
	}
}