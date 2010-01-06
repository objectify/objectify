package com.googlecode.objectify;

import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>This is the underlying implementation of the ObjectifyFactory static methods, available
 * as a singleton.  If you wish to override factory behavior, you can inject a subclass of
 * this object in your code.  You can also make calls to these methods by calling
 * {@code ObFactory.instance()} or by dependency injection if you think static methods are fugly.</p>
 * 
 * <p>In general, however, you probably want to use the ObjectifyFactory methods.</p>
 * 
 * <p>Note that all method documentation is on ObjectifyFactory.</p>
 * 
 * @see ObjectifyFactory
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObFactory
{
	/** */
	protected static ObFactory instance = new ObFactory();
	
	/** Obtain the singleton as an alternative to using the static methods */
	public static ObFactory instance() { return instance; }
	
	/** */
	protected Map<String, EntityMetadata> types = new HashMap<String, EntityMetadata>();
	
	/** If >0, uses a proxy to retry DatastoreTimeoutExceptions */
	protected int datastoreTimeoutRetryCount;
	
	/** @see ObjectifyFactory#begin() */
	public Objectify begin()
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Objectify impl = new ObjectifyImpl(this, ds, null);

		return (Objectify)this.maybeWrap(impl);
	}
	
	/** @see ObjectifyFactory#beginTransaction() */
	public Objectify beginTransaction()
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		Objectify impl = new ObjectifyImpl(this, ds, ds.beginTransaction());
		
		return (Objectify)this.maybeWrap(impl);
	}
	
	/** @see ObjectifyFactory#withTransaction(Transaction) */
	public Objectify withTransaction(Transaction txn)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		return (Objectify)this.maybeWrap(new ObjectifyImpl(this, ds, txn));
	}
	
	/** @see ObjectifyFactory#register(Class) */
	public void register(Class<?> clazz)
	{
		String kind = getKind(clazz);
		this.types.put(kind, new EntityMetadata(this, clazz));
	}
	
	/** @see ObjectifyFactory#setDatastoreTimeoutRetryCount(int) */
	public void setDatastoreTimeoutRetryCount(int value)
	{
		this.datastoreTimeoutRetryCount = value;
	}

	/** @see ObjectifyFactory#getDatastoreTimeoutRetryCount() */
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
	
	/** @see ObjectifyFactory#createKey(Class, long) */
	public <T> ObKey<T> createKey(Class<T> kind, long id)
	{
		return this.rawKeyToObKey(KeyFactory.createKey(getKind(kind), id));
	}
	
	/** @see ObjectifyFactory#createKey(Class, String) */
	public <T> ObKey<T> createKey(Class<T> kind, String name)
	{
		return this.rawKeyToObKey(KeyFactory.createKey(getKind(kind), name));
	}
	
	/** @see ObjectifyFactory#createKey(Key, Class, long) */
	public <T> ObKey<T> createKey(Key parent, Class<T> kind, long id)
	{
		return this.rawKeyToObKey(KeyFactory.createKey(parent, getKind(kind), id));
	}
	
	/** @see ObjectifyFactory#createKey(Key, Class, String) */
	public <T> ObKey<T> createKey(Key parent, Class<T> kind, String name)
	{
		return this.rawKeyToObKey(KeyFactory.createKey(parent, getKind(kind), name));
	}
	
	/** @see ObjectifyFactory#createKey(T) */
	public <T> ObKey<T> createKey(T entity)
	{
		return this.rawKeyToObKey(this.getMetadataForEntity(entity).getKey(entity));
	}
	
	//
	// Friendly query creation methods
	//
	
	/** @see ObjectifyFactory#createQuery() */
	public ObQuery createQuery()
	{
		return new ObQuery(this);
	}
	
	/** @see ObjectifyFactory#createQuery(Class) */
	public ObQuery createQuery(Class<?> entityClazz)
	{
		return new ObQuery(this, entityClazz);
	}
	
	//
	// Stuff which should only be necessary internally, but might be useful to others.
	//
	
	/** @see ObjectifyFactory#getKind(Class) */
	public String getKind(Class<?> clazz)
	{
		javax.persistence.Entity entityAnn = clazz.getAnnotation(javax.persistence.Entity.class);
		if (entityAnn == null || entityAnn.name() == null || entityAnn.name().length() == 0)
			return clazz.getSimpleName();
		else
			return entityAnn.name();
	}
	
	/** @see ObjectifyFactory#getMetadata(Key) */
	public EntityMetadata getMetadata(Key key)
	{
		return this.getMetadata(key.getKind());
	}
	
	/** @see ObjectifyFactory#getMetadata(ObKey) */
	public EntityMetadata getMetadata(ObKey<?> key)
	{
		return this.getMetadata(key.getKind());
	}
	
	/** @see ObjectifyFactory#getMetadata(Class) */
	public EntityMetadata getMetadata(Class<?> clazz)
	{
		return this.getMetadata(this.getKind(clazz));
	}
	
	/** @see ObjectifyFactory#getMetadataForEntity(Object) */
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
	 * Converts an obKey into a raw Key.
	 * @param obKey can be null, resulting in a null Key
	 */
	public Key obKeyToRawKey(ObKey<?> obKey)
	{
		if (obKey == null)
			return null;
		
		if (obKey.getName() != null)
			return KeyFactory.createKey(this.obKeyToRawKey(obKey.getParent()), this.getKind(obKey.getKind()), obKey.getName());
		else
			return KeyFactory.createKey(this.obKeyToRawKey(obKey.getParent()), this.getKind(obKey.getKind()), obKey.getId());
	}
	
	/** 
	 * Converts a raw Key into an ObKey.
	 * @param rawKey can be null, resulting in a null ObKey
	 */
	public <T> ObKey<T> rawKeyToObKey(Key rawKey)
	{
		if (rawKey == null)
			return null;
		
		if (rawKey.getName() != null)
			return new ObKey<T>(this.rawKeyToObKey(rawKey.getParent()), this.getMetadata(rawKey).getEntityClass(), rawKey.getName());
		else
			return new ObKey<T>(this.rawKeyToObKey(rawKey.getParent()), this.getMetadata(rawKey).getEntityClass(), rawKey.getId());
	}
}