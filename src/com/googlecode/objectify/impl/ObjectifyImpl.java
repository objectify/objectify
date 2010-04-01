package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;

/**
 * Implementation of the Objectify interface.  Note we *always* use the DatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify
{
	/** The factory that produced us */
	protected ObjectifyFactory factory;
	
	/** The google object that does the actual heavy lifting */
	protected DatastoreService ds;
	
	/** The transaction to use.  If null, do not use transactions. */
	protected Transaction txn;
	
	/**
	 * Protected constructor creates a wrapper on the datastore with
	 * the specified txn.
	 * 
	 * @param txn can be null to not use transactions. 
	 */
	public ObjectifyImpl(ObjectifyFactory fact, DatastoreService ds, Transaction txn)
	{
		this.factory = fact;
		this.ds = ds;
		this.txn = txn;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<Key<T>, T> get(Iterable<? extends Key<? extends T>> keys)
	{
		// First we need to turn the keys into raw keys
		List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<com.google.appengine.api.datastore.Key>();
		for (Key<? extends T> obKey: keys)
			rawKeys.add(this.factory.typedKeyToRawKey(obKey));
			
		Map<com.google.appengine.api.datastore.Key, Entity> entities = this.ds.get(this.txn, rawKeys);
		Map<Key<T>, T> result = new LinkedHashMap<Key<T>, T>(entities.size() * 2);

		for (com.google.appengine.api.datastore.Key rawKey : rawKeys)
		{
			Entity entity = entities.get(rawKey);
			if (entity != null) {
				EntityMetadata metadata = this.factory.getMetadata(rawKey);
				Key<T> obKey = this.factory.rawKeyToTypedKey(rawKey);
				result.put(obKey, (T)metadata.toObject(entity));
			}
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T get(Key<? extends T> key) throws NotFoundException
	{
		try
		{
			Entity ent = this.ds.get(this.txn, this.factory.typedKeyToRawKey(key));
			return this.factory.getMetadata(key).toObject(ent);
		}
		catch (EntityNotFoundException e)
		{
			throw new NotFoundException(key);
		}	
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, long)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, long id) throws NotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(new Key<T>(clazz, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, String name) throws NotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(new Key<T>(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <S, T> Map<S, T> get(Class<? extends T> clazz, Iterable<S> ids)
	{
		List<Key<? extends T>> keys = new ArrayList<Key<? extends T>>();
		
		for (Object id: ids)
		{
			if (id instanceof Long)
				keys.add(new Key<T>(clazz, (Long)id));
			else if (id instanceof String)
				keys.add(new Key<T>(clazz, (String)id));
			else
				throw new IllegalArgumentException("Only Long or String is allowed, not " + id.getClass().getName() + " (" + id + ")");
		}
		
		Map<Key<T>, T> fetched = this.get(keys);
		Map<S, T> result = new LinkedHashMap<S, T>(fetched.size() * 2);
		
		for (Map.Entry<Key<T>, T> entry: fetched.entrySet())
		{
			Object mapKey = entry.getKey().getName() != null ? entry.getKey().getName() : entry.getKey().getId();
			result.put((S)mapKey, entry.getValue());
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T find(Key<? extends T> key)
	{
		try { return (T)this.get(key); }
		catch (NotFoundException e) { return null; }
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(java.lang.Class, long)
	 */
	@Override
	public <T> T find(Class<? extends T> clazz, long id)
	{
		try { return this.get(clazz, id); }
		catch (NotFoundException e) { return null; }
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T find(Class<? extends T> clazz, String name)
	{
		try { return this.get(clazz, name); }
		catch (NotFoundException e) { return null; }
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Object)
	 */
	@Override
	public <T> Key<T> put(T obj)
	{
		EntityMetadata<T> metadata = this.factory.getMetadataForEntity(obj);
		
		Entity ent = metadata.toEntity(obj);
		
		com.google.appengine.api.datastore.Key rawKey = this.ds.put(this.txn, ent);

		// Need to reset the key value in case the value was generated
		metadata.setKey(obj, rawKey);
		
		return this.factory.rawKeyToTypedKey(rawKey);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> Map<Key<T>, T> put(Iterable<? extends T> objs)
	{
		List<Entity> entityList = new ArrayList<Entity>();
		for (T obj: objs)
		{
			EntityMetadata<T> metadata = this.factory.getMetadataForEntity(obj);
			entityList.add(metadata.toEntity(obj));
		}
		
		List<com.google.appengine.api.datastore.Key> rawKeys = this.ds.put(this.txn, entityList);
		
		Map<Key<T>, T> result = new LinkedHashMap<Key<T>, T>(rawKeys.size() * 2);
		
		// Patch up any generated keys in the original objects while building new key list
		Iterator<com.google.appengine.api.datastore.Key> keysIt = rawKeys.iterator();
		for (T obj: objs)
		{
			com.google.appengine.api.datastore.Key k = keysIt.next();
			EntityMetadata<T> metadata = this.factory.getMetadataForEntity(obj);
			metadata.setKey(obj, k);
			
			Key<T> obKey = this.factory.rawKeyToTypedKey(k);
			result.put(obKey, obj);
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Object)
	 */
	@Override
	public void delete(Object keyOrEntity)
	{
		this.ds.delete(this.txn, this.factory.getRawKey(keyOrEntity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Class, long)
	 */
	@Override
	public <T> void delete(Class<T> clazz, long id)
	{
		this.delete(new Key<T>(clazz, id));
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(Class, String)
	 */
	@Override
	public <T> void delete(Class<T> clazz, String name)
	{
		this.delete(new Key<T>(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<?> keysOrEntities)
	{
		// We have to be careful here, objs could contain raw Keys or Keys or entity objects or both!
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
		
		for (Object obj: keysOrEntities)
			keys.add(this.factory.getRawKey(obj));
		
		this.ds.delete(this.txn, keys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query()
	 */
	@Override
	public <T> Query<T> query()
	{
		return new QueryImpl<T>(this.factory, this);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query(java.lang.Class)
	 */
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		return new QueryImpl<T>(this.factory, this, clazz);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	@Override
	public Transaction getTxn()
	{
		return this.txn;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#getDatastore()
	 */
	@Override
	public DatastoreService getDatastore()
	{
		return this.ds;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	@Override
	public ObjectifyFactory getFactory()
	{
		return this.factory;
	}

}