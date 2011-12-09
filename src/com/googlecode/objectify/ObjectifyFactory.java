package com.googlecode.objectify;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreService.KeyRangeState;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.googlecode.objectify.cache.CachingAsyncDatastoreService;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.CacheControlImpl;
import com.googlecode.objectify.impl.EntityMemcacheStats;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.Registrar;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.translate.TranslatorRegistry;

/**
 * <p>Factory which allows us to construct implementations of the Objectify interface.
 * Just call {@code begin()}.</p>
 * 
 * <p>Note that unlike the DatastoreService, there is no implicit transaction management.
 * You either create an Objectify without a transaction (by calling {@code begin()} or you
 * create one with a transaction (by calling {@code beginTransaction()}.  If you create
 * an Objectify with a transaction, you should use it like this:</p>
 * <code><pre>
 * 	Objectify data = factory.beginTransaction()
 * 	try {
 * 		// do work
 * 		data.getTxn().commit();
 * 	}
 * 	finally {
 * 		if (data.getTxn().isActive()) data.getTxn().rollback();
 * 	}
 * </pre></code>
 * 
 * <p>ObjectifyFactory is designed to be subclassed; much default behavior can be changed
 * by overriding methods.  In particular, see createObjectify(), construct(), getAsyncDatastoreService().</p>  
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyFactory
{
	/** Default memcache namespace; override getRawMemcacheService() to change */
	public static final String MEMCACHE_NAMESPACE = "ObjectifyCache";
	
	/** Encapsulates entity registration info */
	protected Registrar registrar = new Registrar(this);
	
	/** All the various loaders */
	protected TranslatorRegistry translators = new TranslatorRegistry(this);
	
	/** Tracks stats */
	protected EntityMemcacheStats memcacheStats = new EntityMemcacheStats();
	
	/** Manages caching of entities at a low level */
	protected EntityMemcache entityMemcache = new EntityMemcache(MEMCACHE_NAMESPACE, new CacheControlImpl(this), this.memcacheStats);
	
	/**
	 * <p>Construct an instance of the specified type.  Objectify uses this method whenever possible to create
	 * instances of entities, condition classes, or other types; by overriding this method you can substitute Guice or other
	 * dependency injection mechanisms.  By default it constructs with a simple no-args constructor.</p>
	 */
	public <T> T construct(Class<T> type) {
		// We do this instead of calling newInstance directly because this lets us work around accessiblity
		Constructor<T> ctor = TypeUtils.getNoArgConstructor(type);
		return TypeUtils.newInstance(ctor, new Object[0]);
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
	public <T extends Collection<?>> T constructCollection(Class<T> type, int size) {
		if ((Class<?>)type == List.class || (Class<?>)type == Collection.class)
			return (T)new ArrayList<Object>(size);
		else if ((Class<?>)type == Set.class)
			return (T)new HashSet<Object>((int)(size * 1.5));
		else if ((Class<?>)type == SortedSet.class)
			return (T)new TreeSet<Object>();
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
	public <T extends Map<?, ?>> T constructMap(Class<T> type) {
		if ((Class<?>)type == Map.class)
			return (T)new HashMap<Object, Object>();
		else if ((Class<?>)type == SortedMap.class)
			return (T)new TreeMap<Object, Object>();
		else
			return construct(type);
	}
	
	/**
	 * Get an AsyncDatastoreService facade appropriate to the options.  All Objectify
	 * datastore interaction goes through an AsyncDatastoreService.  This might or
	 * might not produce a CachingAsyncDatastoreService.
	 * 
	 * @return an AsyncDatastoreService configured per the specified options.
	 */
	public AsyncDatastoreService createAsyncDatastoreService(DatastoreServiceConfig cfg, boolean globalCache)
	{
		AsyncDatastoreService ads = this.createRawAsyncDatastoreService(cfg);

		if (globalCache && this.registrar.isCacheEnabled())
			return new CachingAsyncDatastoreService(ads, this.entityMemcache);
		else
			return ads;
	}
	
	/**
	 * You can override this to add behavior at the raw datastoreservice level.
	 */
	protected AsyncDatastoreService createRawAsyncDatastoreService(DatastoreServiceConfig cfg) {
		return DatastoreServiceFactory.getAsyncDatastoreService(cfg);
	}
	
	/**
	 * This is the beginning of any Objectify session.  It creates an Objectify instance with the default
	 * options, unless you override this method to alter the options.  You can also override this method
	 * to produce a wholly different Objectify implementation (possibly using ObjectifyWrapper).
	 * 
	 * <p>The default options are:</p>
	 * 
	 * <ul>
	 * <li>Do NOT begin a transaction.</li>
	 * <li>Do NOT use a session cache.</li>
	 * <li>DO use a global cache.</li>
	 * <li>Use STRONG consistency.</li>
	 * <li>Apply no deadline to calls.</li>
	 * </ul>
	 * 
	 * @return a new Objectify instance
	 */
	public Objectify begin() {
		return new ObjectifyImpl(this);
	}
	
	/**
	 * <p>All POJO entity classes which are to be managed by Objectify
	 * must be registered first.  This method must be called in a single-threaded
	 * mode sometime around application initialization.</p> 
	 * 
	 * <p>Any extra translators must be added to the TranslatorRegistry *before*
	 * entity classes are registered.</p>
	 * 
	 * <p>Attempts to re-register entity classes are ignored.</p>
	 */
	public <T> void register(Class<T> clazz) {
		this.registrar.register(clazz);
	}
	
	/**
	 * Get the object that tracks memcache stats.
	 */
	public EntityMemcacheStats getMemcacheStats() { return this.memcacheStats; } 
	
	//
	// Stuff which should only be necessary internally, but might be useful to others.
	//
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(Class<T> clazz) throws IllegalArgumentException {
		EntityMetadata<T> metadata = this.registrar.getMetadata(clazz);
		if (metadata == null)
			throw new IllegalArgumentException("No class '" + clazz.getName() + "' was registered");
		else
			return metadata;
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key, or null if the kind was not registered
	 */
	public <T> EntityMetadata<T> getMetadata(com.google.appengine.api.datastore.Key key) {
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key, or null if the kind was not registered
	 */
	public <T> EntityMetadata<T> getMetadata(Key<T> key) {
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * Gets metadata for the specified kind, returning null if nothing registered
	 * @return null if the kind is not registered.
	 */
	public <T> EntityMetadata<T> getMetadata(String kind) {
		return this.registrar.getMetadata(kind);
	}

	/**
	 * Named differently so you don't accidentally use the Object form
	 * @return the metadata for a kind of typed object.
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadataForEntity(T obj) {
		// Type erasure sucks ass
		return (EntityMetadata<T>)this.getMetadata(obj.getClass());
	}
	
	/**
	 * <p>Gets the Key<T> given an object that might be a Key, Key<T>, or entity.</p>
	 * 
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	@SuppressWarnings("unchecked")
	public <T> Key<T> getKey(Object keyOrEntity) {
		
		if (keyOrEntity instanceof Key<?>)
			return (Key<T>)keyOrEntity;
		else if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return Key.create((com.google.appengine.api.datastore.Key)keyOrEntity);
		else if (keyOrEntity instanceof Ref)
			return ((Ref<T>)keyOrEntity).key();
		else
			return Key.create(this.getMetadataForEntity(keyOrEntity).getKeyMetadata().getRawKey(keyOrEntity));
	}
	
	/**
	 * <p>Gets the raw datstore Key given an object that might be a Key, Key<T>, or entity.</p>
	 * 
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	public com.google.appengine.api.datastore.Key getRawKey(Object keyOrEntity) {
		
		if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return (com.google.appengine.api.datastore.Key)keyOrEntity;
		else if (keyOrEntity instanceof Key<?>)
			return ((Key<?>)keyOrEntity).getRaw();
		else if (keyOrEntity instanceof Ref)
			return ((Ref<?>)keyOrEntity).key().getRaw();
		else
			return this.getMetadataForEntity(keyOrEntity).getKeyMetadata().getRawKey(keyOrEntity);
	}
	
	/**
	 * Allocates a single id from the allocator for the specified kind.  Safe to use in concert
	 * with the automatic generator.  This is just a convenience method for allocateIds().
	 * 
	 * @param clazz must be a registered entity class with a Long or long id field.
	 */
	public <T> long allocateId(Class<T> clazz) {
		return allocateIds(clazz, 1).iterator().next().getId();
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
	 */
	public <T> long allocateId(Object parentKeyOrEntity, Class<T> clazz) {
		return allocateIds(parentKeyOrEntity, clazz, 1).iterator().next().getId();
	}
	
	/**
	 * Preallocate a contiguous range of unique ids within the namespace of the
	 * specified entity class.  These ids can be used in concert with the normal
	 * automatic allocation of ids when put()ing entities with null Long id fields.
	 * 
	 * @param clazz must be a registered entity class with a Long or long id field.
	 * @param num must be >= 1 and <= 1 billion 
	 */
	public <T> KeyRange<T> allocateIds(Class<T> clazz, long num) {
		// Feels a little weird going directly to the DatastoreServiceFactory but the
		// allocateIds() method really is optionless.
		String kind = Key.getKind(clazz);
		return new KeyRange<T>(DatastoreServiceFactory.getDatastoreService().allocateIds(kind, num));
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
	public <T> KeyRange<T> allocateIds(Object parentKeyOrEntity, Class<T> clazz, long num) {
		Key<?> parent = this.getKey(parentKeyOrEntity);
		String kind = Key.getKind(clazz);
		
		// Feels a little weird going directly to the DatastoreServiceFactory but the
		// allocateIds() method really is optionless.
		return new KeyRange<T>(DatastoreServiceFactory.getDatastoreService().allocateIds(parent.getRaw(), kind, num));
	}
	
	/**
	 * Allocates a user-specified contiguous range of unique IDs, preventing the allocator from
	 * giving them out to entities (with autogeneration) or other calls to allocate methods.
	 * This lets you specify a specific range to block out (for example, you are bulk-loading a
	 * collection of pre-existing entities).  If you don't care about what id is allocated, use
	 * one of the other allocate methods.
	 */
	public <T> KeyRangeState allocateIdRange(KeyRange<T> range) {
		return DatastoreServiceFactory.getDatastoreService().allocateIdRange(range.getRaw());
	}
	
	/**
	 * <p>Gets the master list of all registered TranslatorFactory objects.  By adding Translators, Objectify
	 * can process additional field types which are not part of the standard GAE SDK.  <b>You must
	 * add translators *before* registering entity pojo classes.</b></p>
	 *  
	 * @return the repository of TranslatorFactory objects, to which you can optionally add translators
	 */
	public TranslatorRegistry getTranslators() {
		return this.translators;
	}
}