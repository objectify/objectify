package com.googlecode.objectify;

import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>Factory which allows us to construct implementations of the Objectify interface.
 * Just call {@code begin()}.</p>
 * 
 * <p>Note that unlike the DatastoreService, there is no implicit transaction management.
 * You either create an Objectify without a transaction (by calling {@code begin()} or you
 * create one with a transaction (by calling {@code beginTransaction()}.  If you create
 * an Objectify with a transaction, you should use it like this:</p>
 * <code><pre>
 * 	Objectify data = ObjectifyFactory.beginTransaction()
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
	protected Map<String, EntityMetadata> types = new HashMap<String, EntityMetadata>();
	
	/** If >0, uses a proxy to retry DatastoreTimeoutExceptions */
	protected int datastoreTimeoutRetryCount;
	
	/**
	 * @return an Objectify from the DatastoreService which does NOT use
	 *  transactions.  This is a lightweight operation and can be used freely.
	 */
	public Objectify begin()
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Objectify impl = new ObjectifyImpl(this, ds, null);

		return (Objectify)this.maybeWrap(impl);
	}
	
	/**
	 * @return an Objectify which uses a transaction.  Be careful, you cannot
	 *  access entities across differing entity groups. 
	 */
	public Objectify beginTransaction()
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Objectify impl = new ObjectifyImpl(this, ds, ds.beginTransaction());
		
		return (Objectify)this.maybeWrap(impl);
	}
	
	/**
	 * Use this method when you already have a Transaction object, say one
	 * that was created by using the raw DatastoreService.  This method should
	 * not commonly be used.
	 * 
	 * @return an Objectify which uses the specified transaction.
	 */
	public Objectify withTransaction(Transaction txn)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		return (Objectify)this.maybeWrap(new ObjectifyImpl(this, ds, txn));
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
	public void register(Class<?> clazz)
	{
		String kind = getKind(clazz);
		this.types.put(kind, new EntityMetadata(this, clazz));
	}
	
	/**
	 * <p>If this value is set to something greater than zero, Objectify will
	 * retry all calls to the datastore whenever the it throws a
	 * DatastoreTimeoutException.  The datastore produces a trickle
	 * of these errors *all the time*, even in good health.  They can be
	 * retried without ill effect.</p>
	 * 
	 * <p>If you want more fine grained control of retries, you can leave this
	 * value at 0 and manually wrap Objectify, ObjPreparedQuery, and Iterator
	 * instances by calling DatastoreTimeoutRetryProxy.wrap() directly.</p>
	 * 
	 * <p>Beware setting this value to something large; sometimes the datastore
	 * starts choking and returning timeout errors closer to 100% of the time
	 * rather than the usual 0.1%-1%. A low number like 2 is safe and effective.</p>
	 * 
	 * @param value is the number of retries to attempt; ie 1 means two total tries
	 *  before giving up and propagating the DatastoreTimeoutException.
	 */
	public void setDatastoreTimeoutRetryCount(int value)
	{
		this.datastoreTimeoutRetryCount = value;
	}

	/**
	 * @return the current setting for datastore timeout retry count
	 */
	public int getDatastoreTimeoutRetryCount()
	{
		return this.datastoreTimeoutRetryCount;
	}
	
	/**
	 * Wraps impl in a proxy that detects DatastoreTimeoutException if
	 * datastoreTimeoutRetryCount > 0.
	 */
	protected <T> T maybeWrap(T impl)
	{
		if (this.datastoreTimeoutRetryCount > 0)
			return DatastoreTimeoutRetryProxy.wrap(impl, this.datastoreTimeoutRetryCount);
		else
			return impl;
	}
	
	//
	// Methods equivalent to those on KeyFactory, but which use typed Classes instead of kind.
	//
	
	/** Creates a key for the class with the specified id */
	public <T> OKey<T> createKey(Class<T> kind, long id)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(getKind(kind), id));
	}
	
	/** Creates a key for the class with the specified name */
	public <T> OKey<T> createKey(Class<T> kind, String name)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(getKind(kind), name));
	}
	
	/** Creates a key for the class with the specified id having the specified parent */
	public <T> OKey<T> createKey(OKey<?> parent, Class<T> kind, long id)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(oKeyToRawKey(parent), getKind(kind), id));
	}
	
	/** Creates a key for the class with the specified name having the specified parent */
	public <T> OKey<T> createKey(OKey<?> parent, Class<T> kind, String name)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(oKeyToRawKey(parent), getKind(kind), name));
	}
	
	/**
	 * <p>Creates a key from a registered entity object that does *NOT* have
	 * a null id.  This method does not have an equivalent on KeyFactory.</p>
	 * 
	 * @throws IllegalArgumentException if the entity has a null id.
	 */
	public <T> OKey<T> createKey(T entity)
	{
		return this.rawKeyToOKey(this.getMetadataForEntity(entity).getKey(entity));
	}
	
	//
	// Friendly query creation methods
	//
	
	/**
	 * Creates a new kind-less query that finds entities.
	 * @see Query#Query()
	 */
	public <T> OQuery<T> createQuery()
	{
		return new OQuery<T>(this);
	}
	
	/**
	 * Creates a query that finds entities with the specified type
	 * @see Query#Query(String)
	 */
	public <T> OQuery<T> createQuery(Class<T> entityClazz)
	{
		return new OQuery<T>(this, entityClazz);
	}
	
	//
	// Stuff which should only be necessary internally, but might be useful to others.
	//
	
	/**
	 * @return the kind associated with a particular entity class
	 */
	public String getKind(Class<?> clazz)
	{
		javax.persistence.Entity entityAnn = clazz.getAnnotation(javax.persistence.Entity.class);
		if (entityAnn == null || entityAnn.name() == null || entityAnn.name().length() == 0)
			return clazz.getSimpleName();
		else
			return entityAnn.name();
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public EntityMetadata getMetadata(Key key)
	{
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public EntityMetadata getMetadata(OKey<?> key)
	{
		return this.getMetadata(key.getKind());
	}
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public EntityMetadata getMetadata(Class<?> clazz)
	{
		return this.getMetadata(this.getKind(clazz));
	}
	
	/**
	 * Named differently so you don't accidentally use the Object form
	 * @return the metadata for a kind of typed object.
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public EntityMetadata getMetadataForEntity(Object obj)
	{
		return this.getMetadata(obj.getClass());
	}
	
	/** */
	protected EntityMetadata getMetadata(String kind)
	{
		EntityMetadata metadata = types.get(kind);
		if (metadata == null)
			throw new IllegalArgumentException("No registered type for kind " + kind);
		else
			return metadata;
	}

	/** 
	 * Converts an OKey into a raw Key.
	 * @param obKey can be null, resulting in a null Key
	 */
	public Key oKeyToRawKey(OKey<?> obKey)
	{
		if (obKey == null)
			return null;
		
		if (obKey.getName() != null)
			return KeyFactory.createKey(this.oKeyToRawKey(obKey.getParent()), this.getKind(obKey.getKind()), obKey.getName());
		else
			return KeyFactory.createKey(this.oKeyToRawKey(obKey.getParent()), this.getKind(obKey.getKind()), obKey.getId());
	}
	
	/** 
	 * Converts a raw Key into an OKey.
	 * @param rawKey can be null, resulting in a null OKey
	 */
	public <T> OKey<T> rawKeyToOKey(Key rawKey)
	{
		if (rawKey == null)
			return null;
		
		if (rawKey.getName() != null)
			return new OKey<T>(this.rawKeyToOKey(rawKey.getParent()), this.getMetadata(rawKey).getEntityClass(), rawKey.getName());
		else
			return new OKey<T>(this.rawKeyToOKey(rawKey.getParent()), this.getMetadata(rawKey).getEntityClass(), rawKey.getId());
	}
}