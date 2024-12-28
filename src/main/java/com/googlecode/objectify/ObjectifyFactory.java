package com.googlecode.objectify;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOpenTelemetryOptions;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.KeyFactory;
import com.googlecode.objectify.cache.CachingAsyncDatastore;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.cache.spymemcached.SpyMemcacheService;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.impl.AsyncDatastoreImpl;
import com.googlecode.objectify.impl.CacheControlImpl;
import com.googlecode.objectify.impl.EntityMemcacheStats;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.impl.ObjectifyOptions;
import com.googlecode.objectify.impl.Registrar;
import com.googlecode.objectify.impl.Transactor;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.translate.Translators;
import com.googlecode.objectify.util.Closeable;
import io.opentelemetry.api.OpenTelemetry;
import net.spy.memcached.MemcachedClient;

import java.lang.reflect.Constructor;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * <p>ObjectifyFactory encapsulates a connection to a single datastore, and allows the datastore
 * to be queries and manipulated.</p>
 *
 * <p>For most applications which connect to a single datastore, you should use the
 * ObjectifyService class to initialize the ObjectifyFactory and make {@code ofy()} calls.
 * If your application connects to multiple datastores, you can skip the ObjectifyService
 * and manage multiple ObjectifyFactory instances yourself.</p>
 *
 * <p>Unlike many software libraries with a hard distinction between public and private APIs,
 * Objectify has three layers. Public methods are robust and only change on major version numbers.
 * However, there is quite a lot of internal behavior exposed, especially if you subclass the
 * ObjectifyFactory. This "middle ground" is available to you, though we can't promise it won't change.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyFactory implements Forge
{
	/** Default memcache namespace */
	public static final String MEMCACHE_NAMESPACE = "ObjectifyCache";

	/**
	 * Thread local stack of Objectify instances corresponding to transaction depth
	 */
	private final ThreadLocal<Deque<Objectify>> stacks = ThreadLocal.withInitial(ArrayDeque::new);

	/** The raw interface to the datastore from the Cloud SDK */
	protected Datastore datastore;

	/** The low-level interface to memcache */
	protected MemcacheService memcache;

	/** Encapsulates entity registration info */
	protected Registrar registrar;

	/** Some useful tools for working with keys */
	protected Keys keys;

	/** */
	protected Translators translators;

	/** */
	protected EntityMemcacheStats memcacheStats = new EntityMemcacheStats();

	/** Manages caching of entities; might be null to indicate "no cache" */
	protected EntityMemcache entityMemcache;

	/** Uses default datastore, no memcache */
	public ObjectifyFactory() {
		this(DatastoreOptions.getDefaultInstance().getService());
	}

	/** Use default datastore but with the configured telemetry. No memcache. */
	public ObjectifyFactory(final OpenTelemetry openTelemetry) {
		this(
			DatastoreOptions.newBuilder().setOpenTelemetryOptions(
				DatastoreOpenTelemetryOptions.newBuilder().setOpenTelemetry(openTelemetry).build()
			).build().getService()
		);
	}

	/**
	 * No memcache
	 */
	public ObjectifyFactory(final Datastore datastore) {
		this(datastore, (MemcacheService)null);
	}

	/**
	 * Uses default datastore
	 * @deprecated call {@code ObjectifyFactory(new SpyMemcacheService(memcache))} instead
	 */
	@Deprecated
	public ObjectifyFactory(final MemcachedClient memcache) {
		this(DatastoreOptions.getDefaultInstance().getService(), memcache);
	}

	/** Uses default datastore */
	public ObjectifyFactory(final MemcacheService memcache) {
		this(DatastoreOptions.getDefaultInstance().getService(), memcache);
	}

	/**
	 * @deprecated call {@code ObjectifyFactory(datastore, new SpyMemcacheService(memcache))} instead
	 */
	@Deprecated
	public ObjectifyFactory(final Datastore datastore, final MemcachedClient memcache) {
		this(datastore, new SpyMemcacheService(memcache));
	}

	/**
	 */
	public ObjectifyFactory(final Datastore datastore, final MemcacheService memcache) {
		this.datastore = datastore;
		this.registrar = new Registrar(this);
		this.keys = new Keys(datastore, registrar);
		this.translators = new Translators(this);
		this.memcache = memcache;

		this.entityMemcache = memcache == null ? null : new EntityMemcache(memcache, MEMCACHE_NAMESPACE, new CacheControlImpl(this), this.memcacheStats);
	}

	/** */
	public Datastore datastore() {
		return this.datastore;
	}

	/** */
	public MemcacheService memcache() {
		return this.memcache;
	}

	/** Always the non-caching version */
	public AsyncDatastore asyncDatastore() {
		return new AsyncDatastoreImpl(datastore);
	}

	/**
	 * Might produce a caching version if caching is enabled.
	 */
	public AsyncDatastore asyncDatastore(final boolean enableGlobalCache) {
		if (this.entityMemcache != null && enableGlobalCache && this.registrar.isCacheEnabled())
			return new CachingAsyncDatastore(asyncDatastore(), this.entityMemcache);
		else
			return asyncDatastore();
	}

	/**
	 * <p>Construct an instance of the specified type.  Objectify uses this method whenever possible to create
	 * instances of entities, condition classes, or other types; by overriding this method you can substitute Guice or other
	 * dependency injection mechanisms.  By default it constructs with a simple no-args constructor.</p>
	 */
	@Override
	public <T> T construct(final Class<T> type) {
		// We do this instead of calling newInstance directly because this lets us work around accessiblity
		final Constructor<T> ctor = TypeUtils.getNoArgConstructor(type);
		return TypeUtils.newInstance(ctor);
	}

	/**
	 * <p>Construct a collection of the specified type and the specified size for use on a POJO field.  You can override
	 * this with Guice or whatnot.</p>
	 *
	 * <p>The default is to call construct(Class), with one twist - if a Set, SortedSet, or List interface is presented,
	 * Objectify will construct a HashSet, TreeSet, or ArrayList (respectively).  If you override this method with
	 * dependency injection and you use uninitialized fields of these interface types in your entity pojos, you will
	 * need to bind these interfaces to concrete types.</p>
	 */
	@SuppressWarnings("unchecked")
	public <T extends Collection<?>> T constructCollection(final Class<T> type, final int size) {
		if ((Class<?>)type == List.class || (Class<?>)type == Collection.class)
			return (T)new ArrayList<>(size);
		else if ((Class<?>)type == Set.class)
			return (T)new HashSet<>((int)(size * 1.5));
		else if ((Class<?>)type == SortedSet.class)
			return (T)new TreeSet<>();
		else
			return construct(type);
	}

	/**
	 * <p>Construct a map of the specified type for use on a POJO field.  You can override this with Guice or whatnot.</p>
	 *
	 * <p>The default is to call construct(Class), with one twist - if a Map or SortedMap List interface is presented,
	 * Objectify will construct a HashMap or TreeMap (respectively).  If you override this method with
	 * dependency injection and you use uninitialized fields of these interface types in your entity pojos, you will
	 * need to bind these interfaces to concrete types.</p>
	 */
	@SuppressWarnings("unchecked")
	public <T extends Map<?, ?>> T constructMap(final Class<T> type) {
		if ((Class<?>)type == Map.class)
			return (T)new HashMap<>();
		else if ((Class<?>)type == SortedMap.class)
			return (T)new TreeMap<>();
		else
			return construct(type);
	}

	/**
	 * <p>All POJO entity classes which are to be managed by Objectify
	 * must be registered first.  This method must be called in a single-threaded
	 * mode sometime around application initialization.</p>
	 *
	 * <p>Any extra translators must be added to the Translators *before*
	 * entity classes are registered.</p>
	 *
	 * <p>Attempts to re-register entity classes are ignored.</p>
	 */
	public <T> void register(final Class<T> clazz) {
		this.registrar.register(clazz);
	}

	/**
	 * <p>Gets the master list of all registered TranslatorFactory objects.  By adding Translators, Objectify
	 * can process additional field types which are not part of the standard GAE SDK.  <b>You must
	 * add translators *before* registering entity pojo classes.</b></p>
	 *
	 * @return the repository of TranslatorFactory objects, to which you can optionally add translators
	 */
	public Translators getTranslators() {
		return this.translators;
	}

	/**
	 * Get the object that tracks memcache stats.
	 */
	public EntityMemcacheStats getMemcacheStats() { return this.memcacheStats; }

	/**
	 * Allocates a single id from the allocator for the specified kind.  Safe to use in concert
	 * with the automatic generator.  This is just a convenience method for allocateIds().
	 *
	 * @param clazz must be a registered entity class with a Long or long id field.
	 * @return a key with an id that is unique to the kind
	 */
	public <T> Key<T> allocateId(final Class<T> clazz) {
		return allocateIds(clazz, 1).iterator().next();
	}

	/**
	 * Allocates a single id from the allocator for the specified kind.  Safe to use in concert
	 * with the automatic generator.  This is just a convenience method for allocateIds().
	 *
	 * Note that the id is only unique within the parent, not across the entire kind.
	 *
	 * @param parentKeyOrEntity must be a legitimate parent for the class type.  It need not
	 * point to an existent entity, but it must be the correct type for clazz.
	 * @param clazz must be a registered entity class with a Long or long id field, and
	 * a parent key of the correct type.
	 * @return a key with a new id unique to the kind and parent
	 */
	public <T> Key<T> allocateId(final Object parentKeyOrEntity, final Class<T> clazz) {
		return allocateIds(parentKeyOrEntity, clazz, 1).iterator().next();
	}

	/**
	 * <p>Preallocate multiple unique ids within the namespace of the
	 * specified entity class.  These ids can be used in concert with the normal
	 * automatic allocation of ids when save()ing entities with null Long id fields.</p>
	 *
	 * <p>The {@code KeyRange<?>} class is deprecated; when using this method,
	 * treat the return value as {@code List<Key<T>>}.</p>
	 *
	 * @param clazz must be a registered entity class with a Long or long id field.
	 * @param num must be >= 1 and small enough we can fit a set of keys in RAM.
	 */
	public <T> KeyRange<T> allocateIds(final Class<T> clazz, final int num) {
		final String kind = Key.getKind(clazz);
		final IncompleteKey incompleteKey = datastore().newKeyFactory().setKind(kind).newKey();

		return allocate(incompleteKey, num);
	}

	/**
	 * Preallocate a contiguous range of unique ids within the namespace of the
	 * specified entity class and the parent key.  These ids can be used in concert with the normal
	 * automatic allocation of ids when put()ing entities with null Long id fields.
	 *
	 * @param parentKeyOrEntity must be a legitimate parent for the class type.  It need not
	 * point to an existent entity, but it must be the correct type for clazz.
	 * @param clazz must be a registered entity class with a Long or long id field, and
	 * a parent key of the correct type.
	 * @param num must be >= 1 and <= 1 billion
	 */
	public <T> KeyRange<T> allocateIds(final Object parentKeyOrEntity, final Class<T> clazz, final int num) {
		final Key<?> parent = keys().anythingToKey(parentKeyOrEntity, null);
		final String kind = Key.getKind(clazz);

		final IncompleteKey incompleteKey = com.google.cloud.datastore.Key.newBuilder(parent.getRaw(), kind).build();

		return allocate(incompleteKey, num);
	}

	/** Allocate num copies of the incompleteKey */
	private <T> KeyRange<T> allocate(final IncompleteKey incompleteKey, final int num) {
		final IncompleteKey[] allocations = new IncompleteKey[num];
		Arrays.fill(allocations, incompleteKey);

		final List<Key<T>> typedKeys = datastore().allocateId(allocations).stream()
				.map(Key::<T>create)
				.collect(Collectors.toList());

		return new KeyRange<>(typedKeys);
	}

	/**
	 * <p>Runs one unit of work, making the root Objectify context available and performing all necessary
	 * housekeeping. Either this method or {@code begin()} must be called before {@code ofy()} can be called.</p>
	 *
	 * <p>Does not start a transaction. If you want a transaction, call {@code ofy().transact()}.</p>
	 *
	 * @return the result of the work.
	 */
	public <R> R run(final Work<R> work) {
		try (Closeable closeable = begin()) {
			return work.run();
		}
	}

	/**
	 * <p>Exactly the same behavior as the method that takes a {@code Work<R>}, but doesn't force you to return
	 * something from your lambda.</p>
	 */
	public void run(final Runnable work) {
		run(() -> {
			work.run();
			return null;
		});
	}

	/**
	 * <p>An alternative to run() which is somewhat easier to use with testing (ie, @Before and @After) frameworks.
	 * You must close the return value at the end of the request in a finally block.</p>
	 *
	 * <p>This method is not typically necessary - in a normal request, the ObjectifyFilter takes care of this housekeeping
	 * for you. However, in unit tests or remote API calls it can be useful.</p>
	 */
	public Closeable begin() {
		return this.open();
	}

	/**
	 * The method to call at any time to get the current Objectify, which may change depending on txn context. This
	 * is the start point for queries and data manipulation.
	 */
	public Objectify ofy() {
		final Deque<Objectify> stack = stacks.get();

		if (stack.isEmpty())
			throw new IllegalStateException("You have not started an Objectify context. You are missing " +
					"a call to run() or you do not have the ObjectifyFilter installed.");

		return stack.getLast();
	}

	/**
	 * <p>This will be removed from the public API in the future.</p>
	 */
	private ObjectifyImpl open() {
		final ObjectifyImpl objectify = new ObjectifyImpl(this);
		stacks.get().add(objectify);
		return objectify;
	}

	/** This is for internal housekeeping and is not part of the public API */
	public ObjectifyImpl open(final ObjectifyOptions opts, final Transactor transactor) {
		final ObjectifyImpl objectify = new ObjectifyImpl(this, opts, transactor);
		stacks.get().add(objectify);
		return objectify;
	}

	/** This is for internal housekeeping and is not part of the public API */
	public void close(final Objectify ofy) {
		final Deque<Objectify> stack = stacks.get();
		if (stack.isEmpty())
			throw new IllegalStateException("You have already destroyed the Objectify context.");

		final Objectify popped = stack.removeLast();
		assert popped == ofy : "Mismatched objectify instances; somehow the stack was corrupted";
	}

	//
	// Stuff which should only be necessary internally, but might be useful to others.
	//

	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(final Class<T> clazz) throws IllegalArgumentException {
		return this.registrar.getMetadataSafe(clazz);
	}

	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(final com.google.cloud.datastore.Key key) throws IllegalArgumentException {
		return this.registrar.getMetadataSafe(key.getKind());
	}

	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(final Key<T> key) throws IllegalArgumentException {
		return this.registrar.getMetadataSafe(key.getKind());
	}

	/**
	 * Gets metadata for the specified kind, returning null if nothing registered. This method is not like
	 * the others because it returns null instead of throwing an exception if the kind is not found.
	 * @return null if the kind is not registered.
	 */
	public <T> EntityMetadata<T> getMetadata(final String kind) {
		return this.registrar.getMetadata(kind);
	}

	/**
	 * Named differently so you don't accidentally use the Object form
	 * @return the metadata for a kind of typed object.
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadataForEntity(final T obj) throws IllegalArgumentException {
		// Type erasure sucks
		return (EntityMetadata<T>)this.getMetadata(obj.getClass());
	}

	/**
	 * Some tools for working with keys. This is an internal Objectify API and subject to change without
	 * notice. You probably want the key() methods instead.
	 */
	public Keys keys() {
		return keys;
	}

	/** Create an Objectify key from the native datastore key */
	public <T> Key<T> key(final com.google.cloud.datastore.Key raw) {
		if (raw == null)
			throw new NullPointerException("Cannot create a Key<?> from a null datastore Key");

		return new Key<>(raw);
	}

	/** Create an Objectify key from a type and numeric id */
	public <T> Key<T> key(final Class<? extends T> kindClass, final long id) {
		return key((String)null, kindClass, id);
	}

	/** Create an Objectify key from a type and string id */
	public <T> Key<T> key(final Class<? extends T> kindClass, final String name) {
		return key((String)null, kindClass, name);
	}

	/** Create an Objectify key from a parent, type, and numeric id */
	public <T> Key<T> key(final Key<?> parent, final Class<? extends T> kindClass, final long id) {
		final String kind = Key.getKind(kindClass);

		if (parent == null) {
			final KeyFactory kf = Keys.adjustNamespace(datastore().newKeyFactory().setKind(kind), null);
			final com.google.cloud.datastore.Key raw = kf.newKey(id);
			return new Key<>(raw);
		} else {
			final com.google.cloud.datastore.Key raw = com.google.cloud.datastore.Key.newBuilder(Key.key(parent), kind, id).build();
			return new Key<>(raw);
		}
	}

	/** Create an Objectify key from a parent, type, and string id */
	public <T> Key<T> key(final Key<?> parent, final Class<? extends T> kindClass, final String name) {
		final String kind = Key.getKind(kindClass);

		if (parent == null) {
			final KeyFactory kf = Keys.adjustNamespace(datastore().newKeyFactory().setKind(kind), null);
			final com.google.cloud.datastore.Key raw = kf.newKey(name);
			return new Key<>(raw);
		} else {
			final com.google.cloud.datastore.Key raw = com.google.cloud.datastore.Key.newBuilder(Key.key(parent), kind, name).build();
			return new Key<>(raw);
		}
	}

	/** Create an Objectify key from a namespace, type, and numeric id */
	public <T> Key<T> key(final String namespace, final Class<? extends T> kindClass, final long id) {
		final String kind = Key.getKind(kindClass);

		final KeyFactory kf = Keys.adjustNamespace(datastore().newKeyFactory().setKind(kind), namespace);
		final com.google.cloud.datastore.Key raw = kf.newKey(id);
		return new Key<>(raw);
	}

	/** Create an Objectify key from a namespace, type, and string id */
	public <T> Key<T> key(final String namespace, final Class<? extends T> kindClass, final String name) {
		final String kind = Key.getKind(kindClass);

		final KeyFactory kf = Keys.adjustNamespace(datastore().newKeyFactory().setKind(kind), namespace);
		final com.google.cloud.datastore.Key raw = kf.newKey(name);
		return new Key<>(raw);
	}

	/** Create a key from a registered POJO entity. */
	public <T> Key<T> key(final T pojo) {
		return keys().keyOf(pojo, null);
	}

	/** Create a Ref from an existing key */
	public <T> Ref<T> ref(final Key<T> key) {
		return new Ref<>(key, this);
	}

	/** Creates a Ref from a registered pojo entity */
	public <T> Ref<T> ref(final T value) {
		return ref(key(value));
	}
}