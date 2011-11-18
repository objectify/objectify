package com.googlecode.objectify;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
import com.googlecode.objectify.impl.conv.Conversions;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;

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
	
	/** All the various converters */
	protected Conversions conversions = new Conversions(this);
	
	/** Tracks stats */
	protected EntityMemcacheStats memcacheStats = new EntityMemcacheStats();
	
	/** Manages caching of entities at a low level */
	protected EntityMemcache entityMemcache = new EntityMemcache(MEMCACHE_NAMESPACE, new CacheControlImpl(this), this.memcacheStats);
	
	/**
	 * Construct an instance of the specified type.  Objectify uses this method whenever possible to create
	 * instances of entities, condition classes, or other types; by overriding this method you can substitute Guice or other
	 * dependency injection mechanisms.  The default is simple construction.
	 */
	public <T> T construct(Class<T> type) {
		return TypeUtils.newInstance(type);
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
	 * @return a new Objectify instance
	 */
	public Objectify begin() {
		return new ObjectifyImpl(this);
	}
	
	/**
	 * <p>All POJO entity classes which are to be managed by Objectify
	 * must be registered first.  This method must be called in a single-threaded
	 * mode sometime around application initialization.</p> 
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
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(com.google.appengine.api.datastore.Key key) {
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(Key<T> key) {
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<? extends T> getMetadata(Class<T> clazz) {
		EntityMetadata<T> metadata = this.registrar.getMetadata(clazz);
		if (metadata == null)
			throw new IllegalArgumentException("No class '" + clazz.getName() + "' was registered");
		else
			return metadata;
	}
	
	/**
	 * Gets metadata for the specified kind, or throws an exception if the kind is unknown
	 */
	public <T> EntityMetadata<T> getMetadata(String kind) {
		EntityMetadata<T> metadata = this.registrar.getMetadata(kind);
		if (metadata == null)
			throw new IllegalArgumentException("No class with kind '" + kind + "' was registered");
		else
			return metadata;
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
		else
			return Key.create(this.getMetadataForEntity(keyOrEntity).getRawKey(keyOrEntity));
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
		else
			return this.getMetadataForEntity(keyOrEntity).getRawKey(keyOrEntity);
	}
	
	/**
	 * Gets the raw datastore Keys given a collection of things that might be Key, Key<?>, or entities
	 * @param keysOrEntities must contain Key, Key<?>, or registered entities
	 * @return a List of the raw datastore Key objects
	 */
	public List<com.google.appengine.api.datastore.Key> getRawKeys(Iterable<?> keysOrEntities) {
		
		List<com.google.appengine.api.datastore.Key> result = new ArrayList<com.google.appengine.api.datastore.Key>();
		
		for (Object obj: keysOrEntities)
			result.add(getRawKey(obj));
		
		return result;
	}

	/** This is used just for makeFilterable() */
	private static final ConverterSaveContext NO_CONTEXT = new ConverterSaveContext() {
		@Override public boolean inEmbeddedCollection() { return false; }
		@Override public Field getField() { return null; }
	};
	
	/**
	 * Translate Key<?> or Entity objects into something that can be used in a filter clause.
	 * Anything unknown (including null) is simply returned as-is and we hope that the filter works.
	 * 
	 * @return whatever can be put into a filter clause.
	 */
	public Object makeFilterable(Object keyOrEntityOrOther) {
		if (keyOrEntityOrOther == null)
			return null;

		// Very important that we use the class rather than the Kind; many unregistered
		// classes would otherwise collide with real kinds eg User vs User.
		EntityMetadata<?> meta = this.registrar.getMetadata(keyOrEntityOrOther.getClass());
		if (meta == null)
			return this.getConversions().forDatastore(keyOrEntityOrOther, NO_CONTEXT);
		else
			return meta.getRawKey(keyOrEntityOrOther);
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
	 * @return the repository of Converter objects
	 */
	public Conversions getConversions() {
		return this.conversions;
	}
}