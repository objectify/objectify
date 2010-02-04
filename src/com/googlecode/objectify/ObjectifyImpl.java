package com.googlecode.objectify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Transaction;

/**
 * Implementation of the Objectify interface.  Note we *always* use the DatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify
{
	/** The factory that produced us */
	ObjectifyFactory factory;
	
	/** The google object that does the actual heavy lifting */
	DatastoreService ds;
	
	/** The transaction to use.  If null, do not use transactions. */
	Transaction txn;
	
	/**
	 * Protected constructor creates a wrapper on the datastore with
	 * the specified txn.
	 * 
	 * @param txn can be null to not use transactions. 
	 */
	protected ObjectifyImpl(ObjectifyFactory fact, DatastoreService ds, Transaction txn)
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
			rawKeys.add(this.factory.oKeyToRawKey(obKey));
			
		Map<com.google.appengine.api.datastore.Key, Entity> entities = this.ds.get(this.txn, rawKeys);
		Map<Key<T>, T> result = new HashMap<Key<T>, T>(entities.size() * 2);
		
		for (Map.Entry<com.google.appengine.api.datastore.Key, Entity> entry: entities.entrySet())
		{
			EntityMetadata metadata = this.factory.getMetadata(entry.getKey());
			Key<T> obKey = this.factory.rawKeyToOKey(entry.getKey());
			result.put(obKey, (T)metadata.toObject(entry.getValue()));
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T get(Key<? extends T> key) throws EntityNotFoundException
	{
		Entity ent = this.ds.get(this.txn, this.factory.oKeyToRawKey(key));
		
		return this.factory.getMetadata(key).toObject(ent);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, long)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, long id) throws EntityNotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(new Key<T>(clazz, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, String name) throws EntityNotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(new Key<T>(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.Iterable)
	 */
	@Override
	public <T> Map<Key<T>, T> get(Class<? extends T> clazz, Iterable<?> ids)
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
		
		return this.get(keys);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T find(Key<? extends T> key)
	{
		try { return (T)this.get(key); }
		catch (EntityNotFoundException e) { return null; }
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(java.lang.Class, long)
	 */
	@Override
	public <T> T find(Class<? extends T> clazz, long id)
	{
		try { return this.get(clazz, id); }
		catch (EntityNotFoundException e) { return null; }
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T find(Class<? extends T> clazz, String name)
	{
		try { return this.get(clazz, name); }
		catch (EntityNotFoundException e) { return null; }
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
		
		return this.factory.rawKeyToOKey(rawKey);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> List<Key<T>> put(Iterable<? extends T> objs)
	{
		List<Entity> entityList = new ArrayList<Entity>();
		for (T obj: objs)
		{
			EntityMetadata<T> metadata = this.factory.getMetadataForEntity(obj);
			entityList.add(metadata.toEntity(obj));
		}
		
		List<com.google.appengine.api.datastore.Key> rawKeys = this.ds.put(this.txn, entityList);
		
		List<Key<T>> obKeys = new ArrayList<Key<T>>(rawKeys.size());
		
		// Patch up any generated keys in the original objects while building new key list
		Iterator<com.google.appengine.api.datastore.Key> keysIt = rawKeys.iterator();
		for (T obj: objs)
		{
			com.google.appengine.api.datastore.Key k = keysIt.next();
			EntityMetadata<T> metadata = this.factory.getMetadataForEntity(obj);
			metadata.setKey(obj, k);
			
			Key<T> obKey = this.factory.rawKeyToOKey(k);
			obKeys.add(obKey);
		}
		
		return obKeys;
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
		Query<T> query = new QueryImpl<T>(this.factory, this);
		return this.factory.maybeWrap(query);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query(java.lang.Class)
	 */
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		Query<T> query = new QueryImpl<T>(this.factory, this, clazz);
		return this.factory.maybeWrap(query);
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

}