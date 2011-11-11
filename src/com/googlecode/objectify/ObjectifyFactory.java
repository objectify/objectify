package com.googlecode.objectify;

import java.lang.reflect.Field;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreService.KeyRangeState;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.cache.CachingAsyncDatastoreService;
import com.googlecode.objectify.cache.CachingDatastoreService;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.AsyncObjectifyImpl;
import com.googlecode.objectify.impl.CacheControlImpl;
import com.googlecode.objectify.impl.EntityMemcacheStats;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.impl.Registrar;
import com.googlecode.objectify.impl.SessionCachingAsyncObjectifyImpl;
import com.googlecode.objectify.impl.conv.Conversions;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import com.googlecode.objectify.util.FutureHelper;

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
 * <p>It would be fairly easy for someone to implement a ScanningObjectifyFactory
 * on top of this class that looks for @Entity annotations based on Scannotation or
 * Reflections, but this would add extra dependency jars and need a hook for
 * application startup.</p>
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
	 * Creates the default options for begin() and beginTransaction().  You can
	 * override this if, for example, you wanted to enable session caching by default.
	 */
	protected ObjectifyOpts createDefaultOpts()
	{
		return new ObjectifyOpts();
	}
	
	/**
	 * Override this in your factory if you wish to use a different impl, say,
	 * one based on the ObjectifyWrapper.
	 * 
	 * @param ds the DatastoreService
	 * @param opts the options for creating this Objectify
	 * @return an instance of Objectify configured appropriately
	 */
	protected Objectify createObjectify(AsyncDatastoreService ds, ObjectifyOpts opts) 
	{
		TransactionOptions txnOpts = opts.getTransactionOptions();
		
		Transaction txn = (txnOpts == null) ? null : FutureHelper.quietGet(ds.beginTransaction(txnOpts));
		
		Objectify ofy = (opts.getSessionCache())
			? new ObjectifyImpl(opts, new SessionCachingAsyncObjectifyImpl(this, ds, txn))
			: new ObjectifyImpl(opts, new AsyncObjectifyImpl(this, ds, txn));
		
		return ofy;
	}
	
	/**
	 * Make a datastore service config that corresponds to the specified options.
	 * Note that not all options are defined by the config; some options (e.g. caching)
	 * have no analogue in the native datastore.
	 */
	protected DatastoreServiceConfig makeConfig(ObjectifyOpts opts)
	{
		DatastoreServiceConfig cfg = DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(opts.getConsistency()));
		
		if (opts.getDeadline() != null)
			cfg.deadline(opts.getDeadline());

		return cfg;
	}
	
	/**
	 * Get a DatastoreService facade appropriate to the options.  Note that
	 * Objectify does not itself use DatastoreService; this method solely
	 * exists to support Objectify.getDatastore().
	 * 
	 * @return a DatastoreService configured per the specified options.
	 */
	public DatastoreService getDatastoreService(ObjectifyOpts opts)
	{
		DatastoreServiceConfig cfg = this.makeConfig(opts);
		DatastoreService ds = this.getRawDatastoreService(cfg);
		
		if (opts.getGlobalCache() && this.registrar.isCacheEnabled())
		{
			CachingAsyncDatastoreService async = new CachingAsyncDatastoreService(this.getRawAsyncDatastoreService(cfg), this.entityMemcache);
			return new CachingDatastoreService(ds, async);
		}
		else
		{
			return ds;
		}
	}
	
	/**
	 * Get an AsyncDatastoreService facade appropriate to the options.  All Objectify
	 * datastore interaction goes through an AsyncDatastoreService, even the synchronous
	 * methods.  The GAE SDK works the same way; DatastoreService is a facade around
	 * AsyncDatastoreService.
	 * 
	 * @return an AsyncDatastoreService configured per the specified options.
	 */
	public AsyncDatastoreService getAsyncDatastoreService(ObjectifyOpts opts)
	{
		DatastoreServiceConfig cfg = this.makeConfig(opts);
		AsyncDatastoreService ads = this.getRawAsyncDatastoreService(cfg);

		if (opts.getGlobalCache() && this.registrar.isCacheEnabled())
			return new CachingAsyncDatastoreService(ads, this.entityMemcache);
		else
			return ads;
	}
	
	/**
	 * You can override this to add behavior at the raw datastoreservice level.
	 */
	protected DatastoreService getRawDatastoreService(DatastoreServiceConfig cfg)
	{
		return DatastoreServiceFactory.getDatastoreService(cfg);
	}
	
	/**
	 * You can override this to add behavior at the raw datastoreservice level.
	 */
	protected AsyncDatastoreService getRawAsyncDatastoreService(DatastoreServiceConfig cfg)
	{
		return DatastoreServiceFactory.getAsyncDatastoreService(cfg);
	}
	
	/**
	 * Create a lightweight Objectify instance with the default options.
	 * Equivalent to begin(new ObjectifyOpts()).
	 */
	public Objectify begin()
	{
		return this.begin(this.createDefaultOpts());
	}
	
	/**
	 * @return an Objectify from the DatastoreService with the specified options.
	 * This is a lightweight operation and can be used freely.
	 */
	public Objectify begin(ObjectifyOpts opts)
	{
		AsyncDatastoreService ds = this.getAsyncDatastoreService(opts);
		return this.createObjectify(ds, opts);
	}
	
	/**
	 * @return an Objectify which uses a transaction. The transaction supports cross-group access, but
	 * this has no extra overhead for a single-entity-group transaction.
	 */
	public Objectify beginTransaction()
	{
		return this.begin(this.createDefaultOpts().setBeginTransaction(true));
	}
	
	/**
	 * <p>All POJO entity classes which are to be managed by Objectify
	 * must be registered first.  This method must be called in a single-threaded
	 * mode sometime around application initialization.</p> 
	 */
	public <T> void register(Class<T> clazz)
	{
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
	public <T> EntityMetadata<T> getMetadata(com.google.appengine.api.datastore.Key key)
	{
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadata(Key<T> key)
	{
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<? extends T> getMetadata(Class<T> clazz)
	{
		EntityMetadata<T> metadata = this.registrar.getMetadata(clazz);
		if (metadata == null)
			throw new IllegalArgumentException("No class '" + clazz.getName() + "' was registered");
		else
			return metadata;
	}
	
	/**
	 * Gets metadata for the specified kind, or throws an exception if the kind is unknown
	 */
	public <T> EntityMetadata<T> getMetadata(String kind)
	{
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
	public <T> EntityMetadata<T> getMetadataForEntity(T obj)
	{
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
	public <T> Key<T> getKey(Object keyOrEntity)
	{
		if (keyOrEntity instanceof Key<?>)
			return (Key<T>)keyOrEntity;
		else if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return new Key<T>((com.google.appengine.api.datastore.Key)keyOrEntity);
		else
			return new Key<T>(this.getMetadataForEntity(keyOrEntity).getRawKey(keyOrEntity));
	}
	
	/**
	 * <p>Gets the raw datstore Key given an object that might be a Key, Key<T>, or entity.</p>
	 * 
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	public com.google.appengine.api.datastore.Key getRawKey(Object keyOrEntity)
	{
		if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return (com.google.appengine.api.datastore.Key)keyOrEntity;
		else if (keyOrEntity instanceof Key<?>)
			return ((Key<?>)keyOrEntity).getRaw();
		else
			return this.getMetadataForEntity(keyOrEntity).getRawKey(keyOrEntity);
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
	public Object makeFilterable(Object keyOrEntityOrOther)
	{
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
	 * <p>Converts a Key<?> into a web-safe string suitable for http parameters
	 * in URLs.  Note that you can convert back and forth with the {@code keyToString()}
	 * and {@code stringToKey()} methods.</p>
	 * 
	 * <p>The String is actually generated by using the KeyFactory {@code keyToString()}
	 * method on a raw version of the datastore key.  You can, if you wanted, use
	 * these web safe strings interchangeably.</p>
	 * 
	 * @param key is any Objectify key
	 * @return a simple String which does not need urlencoding
	 */
	public String keyToString(Key<?> key)
	{
		return KeyFactory.keyToString(key.getRaw());
	}
	
	/**
	 * Converts a String generated with {@code keyToString()} back into an Objectify
	 * Key.  The String could also have been generated by the GAE {@code KeyFactory}.
	 * 
	 * @param stringifiedKey is generated by either {@code ObjectifyFactory.keyToString()} or
	 *  {@code KeyFactory.keyToString()}.
	 * @return a Key<?>
	 */
	public <T> Key<T> stringToKey(String stringifiedKey)
	{
		return new Key<T>(KeyFactory.stringToKey(stringifiedKey));
	}
	
	/**
	 * Allocates a single id from the allocator for the specified kind.  Safe to use in concert
	 * with the automatic generator.  This is just a convenience method for allocateIds().
	 * 
	 * @param clazz must be a registered entity class with a Long or long id field.
	 */
	public <T> long allocateId(Class<T> clazz)
	{
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
	public <T> long allocateId(Object parentKeyOrEntity, Class<T> clazz)
	{
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
	public <T> KeyRange<T> allocateIds(Class<T> clazz, long num)
	{
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
	public <T> KeyRange<T> allocateIds(Object parentKeyOrEntity, Class<T> clazz, long num)
	{
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
	public <T> KeyRangeState allocateIdRange(KeyRange<T> range)
	{
		return DatastoreServiceFactory.getDatastoreService().allocateIdRange(range.getRaw());
	}
	
	/**
	 * @return the repository of Converter objects
	 */
	public Conversions getConversions()
	{
		return this.conversions;
	}
}