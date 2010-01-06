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
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortPredicate;

/**
 * Implementation of the Objectify interface.  Note we *always* use the DatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify
{
	/** The factory that produced us */
	OFactory factory;
	
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
	protected ObjectifyImpl(OFactory fact, DatastoreService ds, Transaction txn)
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
	public <T> Map<OKey<T>, T> get(Iterable<? extends OKey<? extends T>> keys)
	{
		// First we need to turn the keys into raw keys
		List<Key> rawKeys = new ArrayList<Key>();
		for (OKey<? extends T> obKey: keys)
			rawKeys.add(this.factory.oKeyToRawKey(obKey));
			
		Map<Key, Entity> entities = this.ds.get(this.txn, rawKeys);
		Map<OKey<T>, T> result = new HashMap<OKey<T>, T>(entities.size() * 2);
		
		for (Map.Entry<Key, Entity> entry: entities.entrySet())
		{
			EntityMetadata metadata = this.factory.getMetadata(entry.getKey());
			OKey<T> obKey = this.factory.rawKeyToOKey(entry.getKey());
			result.put(obKey, (T)metadata.toObject(entry.getValue()));
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(OKey<? extends T> key) throws EntityNotFoundException
	{
		Entity ent = this.ds.get(this.txn, this.factory.oKeyToRawKey(key));
		
		return (T)this.factory.getMetadata(key).toObject(ent);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, long)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, long id) throws EntityNotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(this.factory.createKey(clazz, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, String name) throws EntityNotFoundException
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (T)this.get(this.factory.createKey(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.Iterable)
	 */
	@Override
	public <T> Map<OKey<T>, T> get(Class<? extends T> clazz, Iterable<?> ids)
	{
		List<OKey<? extends T>> keys = new ArrayList<OKey<? extends T>>();
		
		for (Object id: ids)
		{
			if (id instanceof Long)
				keys.add(this.factory.createKey(clazz, (Long)id));
			else if (id instanceof String)
				keys.add(this.factory.createKey(clazz, (String)id));
			else
				throw new IllegalArgumentException("Only Long or String is allowed, not " + id.getClass().getName() + " (" + id + ")");
		}
		
		return this.get(keys);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T find(OKey<? extends T> key)
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
	public <T> OKey<T> put(T obj)
	{
		EntityMetadata metadata = this.factory.getMetadataForEntity(obj);
		
		Entity ent = metadata.toEntity(obj);
		
		Key rawKey = this.ds.put(this.txn, ent);

		// Need to reset the key value in case the value was generated
		metadata.setKey(obj, rawKey);
		
		return this.factory.rawKeyToOKey(rawKey);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> List<OKey<T>> put(Iterable<?> objs)
	{
		List<Entity> entityList = new ArrayList<Entity>();
		for (Object obj: objs)
		{
			EntityMetadata metadata = this.factory.getMetadataForEntity(obj);
			entityList.add(metadata.toEntity(obj));
		}
		
		List<Key> rawKeys = this.ds.put(this.txn, entityList);
		
		List<OKey<T>> obKeys = new ArrayList<OKey<T>>(rawKeys.size());
		
		// Patch up any generated keys in the original objects while building new key list
		Iterator<Key> keysIt = rawKeys.iterator();
		for (Object obj: objs)
		{
			Key k = keysIt.next();
			EntityMetadata metadata = this.factory.getMetadataForEntity(obj);
			metadata.setKey(obj, k);
			
			OKey<T> obKey = this.factory.rawKeyToOKey(k);
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
		if (keyOrEntity instanceof Key)
			this.ds.delete(this.txn, (Key)keyOrEntity);
		if (keyOrEntity instanceof OKey<?>)
			this.ds.delete(this.txn, this.factory.oKeyToRawKey((OKey<?>)keyOrEntity));
		else
			this.ds.delete(this.txn, this.factory.getMetadataForEntity(keyOrEntity).getKey(keyOrEntity));
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<?> keysOrEntities)
	{
		// We have to be careful here, objs could contain Keys or entity objects or both!
		List<Key> keys = new ArrayList<Key>();
		
		for (Object obj: keysOrEntities)
		{
			if (obj instanceof Key)
				keys.add((Key)obj);
			else
				keys.add(this.factory.getMetadataForEntity(obj).getKey(obj));
		}
		
		this.ds.delete(this.txn, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#prepare(com.google.appengine.api.datastore.Query)
	 */
	@Override
	public <T> OPreparedQuery<T> prepare(OQuery query)
	{
		PreparedQuery pq = this.ds.prepare(this.txn, query.getActual());
		OPreparedQuery<T> prepared = new OPreparedQueryImpl<T>(this.factory, pq, false);

		return this.factory.maybeWrap(prepared);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#prepareKeysOnly(com.googlecode.objectify.ObQuery)
	 */
	@Override
	public <T> OPreparedQuery<OKey<T>> prepareKeysOnly(OQuery query)
	{
		// Make sure we don't mangle the original query object, it might get used again
		Query actual = this.cloneRawQuery(query.getActual());
		actual.setKeysOnly();
		
		PreparedQuery pq = this.ds.prepare(this.txn, actual);
		OPreparedQuery<OKey<T>> prepared = new OPreparedQueryImpl<OKey<T>>(this.factory, pq, true);

		return this.factory.maybeWrap(prepared);
	}
	
	/**
	 * Make a new Query object that is exactly like the old.  Too bad Query isn't Cloneable. 
	 */
	protected Query cloneRawQuery(Query orig)
	{
		Query copy = new Query(orig.getKind(), orig.getAncestor());
		
		for (FilterPredicate filter: orig.getFilterPredicates())
			copy.addFilter(filter.getPropertyName(), filter.getOperator(), filter.getValue());
		
		for (SortPredicate sort: orig.getSortPredicates())
			copy.addSort(sort.getPropertyName(), sort.getDirection());
		
		// This should be impossible but who knows what might happen in the future
		if (orig.isKeysOnly())
			copy.setKeysOnly();
		
		return copy;
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