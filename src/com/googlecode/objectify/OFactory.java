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
 * {@code OFactory.instance()} or by dependency injection if you think static methods are fugly.</p>
 * 
 * <p>In general, however, you probably want to use the ObjectifyFactory methods.</p>
 * 
 * <p>Note that all method documentation is on ObjectifyFactory.</p>
 * 
 * @see ObjectifyFactory
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OFactory
{
	/** */
	protected static OFactory instance = new OFactory();
	
	/** Obtain the singleton as an alternative to using the static methods */
	public static OFactory instance() { return instance; }
	
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
	public <T> OKey<T> createKey(Class<T> kind, long id)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(getKind(kind), id));
	}
	
	/** @see ObjectifyFactory#createKey(Class, String) */
	public <T> OKey<T> createKey(Class<T> kind, String name)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(getKind(kind), name));
	}
	
	/** @see ObjectifyFactory#createKey(Key, Class, long) */
	public <T> OKey<T> createKey(Key parent, Class<T> kind, long id)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(parent, getKind(kind), id));
	}
	
	/** @see ObjectifyFactory#createKey(Key, Class, String) */
	public <T> OKey<T> createKey(Key parent, Class<T> kind, String name)
	{
		return this.rawKeyToOKey(KeyFactory.createKey(parent, getKind(kind), name));
	}
	
	/** @see ObjectifyFactory#createKey(Object) */
	public <T> OKey<T> createKey(T entity)
	{
		return this.rawKeyToOKey(this.getMetadataForEntity(entity).getKey(entity));
	}
	
	//
	// Friendly query creation methods
	//
	
	/** @see ObjectifyFactory#createQuery() */
	public <T> OQuery<T> createQuery()
	{
		return new OQuery<T>(this);
	}
	
	/** @see ObjectifyFactory#createQuery(Class) */
	public <T> OQuery<T> createQuery(Class<T> entityClazz)
	{
		return new OQuery<T>(this, entityClazz);
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
	
	/** @see ObjectifyFactory#getMetadata(OKey) */
	public EntityMetadata getMetadata(OKey<?> key)
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