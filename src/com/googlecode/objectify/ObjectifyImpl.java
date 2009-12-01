package com.googlecode.objectify;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * Implementation of the Objectify interface.  Note we *always* use the DatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify
{
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
	ObjectifyImpl(DatastoreService ds, Transaction txn)
	{
		this.ds = ds;
		this.txn = txn;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<Key, T> get(Iterable<Key> keys)
	{
		Map<Key, Entity> entities = this.ds.get(this.txn, keys);
		Map<Key, T> result = new HashMap<Key, T>(entities.size() * 2);
		
		for (Map.Entry<Key, Entity> entry: entities.entrySet())
		{
			EntityMetadata metadata = ObjectifyFactory.getMetadata(entry.getKey());
			result.put(entry.getKey(), (T)metadata.toObject(entry.getValue()));
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Key key) throws EntityNotFoundException
	{
		Entity ent = this.ds.get(this.txn, key);
		
		return (T)ObjectifyFactory.getMetadata(key).toObject(ent);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, long)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> clazz, long id) throws EntityNotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(ObjectifyFactory.createKey(clazz, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.String)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(Class<T> clazz, String name) throws EntityNotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(ObjectifyFactory.createKey(clazz, name));
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Object)
	 */
	@Override
	public Key put(Object obj)
	{
		EntityMetadata metadata = ObjectifyFactory.getMetadata(obj);
		
		Entity ent = metadata.toEntity(obj);
		
		Key resultKey = this.ds.put(this.txn, ent);
		
		// Need to reset the key value in case the value was generated
		metadata.setKey(obj, resultKey);
		
		return resultKey;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public List<Key> put(Iterable<?> objs)
	{
		List<Entity> entityList = new ArrayList<Entity>();
		for (Object obj: objs)
		{
			EntityMetadata metadata = ObjectifyFactory.getMetadata(obj);
			entityList.add(metadata.toEntity(obj));
		}
		
		List<Key> keys = this.ds.put(this.txn, entityList);
		
		// Patch up any generated keys in the original objects
		Iterator<Key> keysIt = keys.iterator();
		for (Object obj: objs)
		{
			Key k = keysIt.next();
			EntityMetadata metadata = ObjectifyFactory.getMetadata(obj);
			metadata.setKey(obj, k);
		}
		
		return keys;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public void delete(Key... keys)
	{
		this.ds.delete(this.txn, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<Key> keys)
	{
		this.ds.delete(this.txn, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#prepare(com.google.appengine.api.datastore.Query)
	 */
	@Override
	public <T> ObjPreparedQuery<T> prepare(Query query)
	{
		PreparedQuery pq = this.ds.prepare(this.txn, query);
			
		return new ObjPreparedQueryImpl<T>(pq, query.isKeysOnly());
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