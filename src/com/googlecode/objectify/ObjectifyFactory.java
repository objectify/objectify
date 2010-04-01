package com.googlecode.objectify;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.impl.CachingDatastoreService;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ObjectifyImpl;

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
	/** */
	protected Map<String, EntityMetadata<?>> types = new ConcurrentHashMap<String, EntityMetadata<?>>();
	
	/** True if any @Cached entities have been registered */
	protected boolean hasCachedEntities;
	
	/**
	 * @param ds the DatastoreService
	 * @param txn the Transaction
	 * @return an instance of Objectify for use by the factory
	 * 
	 * Override this in your factory if you wish to use a different impl.
	 */
	protected Objectify createObjectify(DatastoreService ds, Transaction txn) 
	{
		return new ObjectifyImpl(this, ds, txn);
	}
	
	/**
	 * @return a DatastoreService which *might* be a caching version if any cached
	 * entities have been registered.  Delegates to getRawDatastoreService() to
	 * actually obtain the instance from appengine.
	 */
	protected DatastoreService getDatastoreService(ObjectifyOpts opts)
	{
		DatastoreServiceConfig cfg = DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(opts.getConsistency()));
		
		if (opts.getDeadline() != null)
			cfg.deadline(opts.getDeadline());
		
		if (opts.getMemCache() && this.hasCachedEntities)
			return new CachingDatastoreService(this, this.getRawDatastoreService(cfg));
		else
			return this.getRawDatastoreService(cfg);
	}
	
	/**
	 * You can override this to add behavior at the raw datastoreservice level.
	 */
	protected DatastoreService getRawDatastoreService(DatastoreServiceConfig cfg)
	{
		return DatastoreServiceFactory.getDatastoreService(cfg);
	}
	
	/**
	 * Create a lightweight Objectify instance with the default options.
	 * Equivalent to begin(new ObjectifyOpts()).
	 */
	public Objectify begin()
	{
		return this.begin(new ObjectifyOpts());
	}
	
	/**
	 * @return an Objectify from the DatastoreService with the specified options.
	 * This is a lightweight operation and can be used freely.
	 */
	public Objectify begin(ObjectifyOpts opts)
	{
		DatastoreService ds = this.getDatastoreService(opts);
		
		if (opts.getBeginTransaction())
			return createObjectify(ds, ds.beginTransaction());
		else
			return createObjectify(ds, null);
	}
	
	/**
	 * @return an Objectify which uses a transaction.  Be careful, you cannot
	 *  access entities across differing entity groups. 
	 */
	public Objectify beginTransaction()
	{
		return this.begin(new ObjectifyOpts().setBeginTransaction(true));
	}
	
	/**
	 * <p>Registers a class with the system so that we can recompose an
	 * object from its key kind.  The default kind is the simplename
	 * of the class, overridden by the @Entity annotation.</p>
	 * 
	 * <p>This method must be called in a single-threaded mode, around the
	 * time of app initialization.  After all types are registered, the
	 * get() method can be called.</p>
	 */
	public <T> void register(Class<T> clazz)
	{
		String kind = getKind(clazz);
		EntityMetadata<T> meta = new EntityMetadata<T>(this, clazz);
		
		this.types.put(kind, meta);
		
		if (meta.getCached() != null)
			this.hasCachedEntities = true;
	}
	
	//
	// Stuff which should only be necessary internally, but might be useful to others.
	//
	
	/**
	 * @return the kind associated with a particular entity class, checking both @Entity
	 *  annotations and defaulting to the class' simplename.
	 */
	public String getKind(Class<?> clazz)
	{
		com.googlecode.objectify.annotation.Entity ourAnn = clazz.getAnnotation(com.googlecode.objectify.annotation.Entity.class);
		if (ourAnn != null && ourAnn.name() != null && ourAnn.name().length() != 0)
			return ourAnn.name();
		
		javax.persistence.Entity jpaAnn = clazz.getAnnotation(javax.persistence.Entity.class);
		if (jpaAnn != null && jpaAnn.name() != null && jpaAnn.name().length() != 0)
			return jpaAnn.name();
		
		return clazz.getSimpleName();
	}
	
	/**
	 * @return the kind associated with a particular entity class
	 */
	public String getKind(String className)
	{
		try
		{
			Class<?> clazz = Class.forName(className);
			return this.getKind(clazz);
		}
		catch (ClassNotFoundException e) { throw new RuntimeException(e); }
	}
	
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
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadata(Key<T> key)
	{
		// I would love to know why this produces a warning
		return (EntityMetadata<T>)this.getMetadata(this.getKind(key.getKindClassName()));
	}
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<? extends T> getMetadata(Class<T> clazz)
	{
		return this.getMetadata(this.getKind(clazz));
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
	
	/** */
	@SuppressWarnings("unchecked")
	protected <T> EntityMetadata<T> getMetadata(String kind)
	{
		EntityMetadata<T> metadata = (EntityMetadata<T>)types.get(kind);
		if (metadata == null)
			throw new IllegalArgumentException("No registered type for kind " + kind);
		else
			return metadata;
	}

	/** 
	 * Converts a typed Key<?> into a raw datastore Key.
	 * @param typedKey can be null, resulting in a null Key
	 */
	public com.google.appengine.api.datastore.Key typedKeyToRawKey(Key<?> typedKey)
	{
		if (typedKey == null)
			return null;
		
		if (typedKey.getName() != null)
			return KeyFactory.createKey(this.typedKeyToRawKey(typedKey.getParent()), this.getKind(typedKey.getKindClassName()), typedKey.getName());
		else
			return KeyFactory.createKey(this.typedKeyToRawKey(typedKey.getParent()), this.getKind(typedKey.getKindClassName()), typedKey.getId());
	}
	
	/** 
	 * Converts a raw datastore Key into a typed Key<?>.
	 * @param rawKey can be null, resulting in a null Key
	 */
	public <T> Key<T> rawKeyToTypedKey(com.google.appengine.api.datastore.Key rawKey)
	{
		if (rawKey == null)
			return null;
		
		EntityMetadata<T> meta = this.getMetadata(rawKey);
		Class<T> entityClass = meta.getEntityClass();
		
		if (rawKey.getName() != null)
			return new Key<T>(this.rawKeyToTypedKey(rawKey.getParent()), entityClass, rawKey.getName());
		else
			return new Key<T>(this.rawKeyToTypedKey(rawKey.getParent()), entityClass, rawKey.getId());
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
		if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return this.rawKeyToTypedKey((com.google.appengine.api.datastore.Key)keyOrEntity);
		else if (keyOrEntity instanceof Key<?>)
			return (Key<T>)keyOrEntity;
		else
			return this.rawKeyToTypedKey(this.getMetadataForEntity(keyOrEntity).getKey(keyOrEntity));
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
			return this.typedKeyToRawKey((Key<?>)keyOrEntity);
		else
			return this.getMetadataForEntity(keyOrEntity).getKey(keyOrEntity);
	}

	/**
	 * Translate Key<?> or Entity objects into something that can be used in a filter clause.
	 * Anything unknown (including null) is simply returned as-is and we hope that the filter works.
	 * 
	 * @return whatever can be put into a filter clause.
	 */
	public Object makeFilterable(Object keyOrEntityOrOther)
	{
		if (keyOrEntityOrOther == null)
		{
			return null;
		}
		else if (keyOrEntityOrOther instanceof Key<?>)
		{
			return this.typedKeyToRawKey((Key<?>)keyOrEntityOrOther);
		}
		else
		{
			// Unfortunately we can't use getRawKey() because it throws IllegalArgumentException
			String kind = this.getKind(keyOrEntityOrOther.getClass());
			EntityMetadata<?> meta = this.types.get(kind);
			if (meta == null)
				return keyOrEntityOrOther;
			else
				return meta.getKey(keyOrEntityOrOther);
		}
	}
}