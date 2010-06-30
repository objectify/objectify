package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cached;

/**
 * <p>A write-through memcache for Entity objects that works for both transactional
 * and nontransactional sessions.  Entity cacheability and expiration are determined
 * by the {@code @Cached} annotation on the POJO.</p>
 * 
 * <ul>
 * <li>Caches negative results as well as positive results.</li>
 * <li>Queries do not affect the cache in any way.</li>
 * <li>Transactional reads bypass the cache, but successful transaction commits will update the cache.</li>
 * </ul>
 * 
 * <p>Note:  There is a horrible, obscure, and utterly bizarre bug in GAE's memcache
 * relating to Key serialization.  It manifests in certain circumstances when a Key
 * has a parent Key that has the same String name.  For this reason, we use the
 * keyToString method to stringify Keys as cache keys.  The actual structure
 * stored in the memcache will be String -> Entity.</p>  
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingDatastoreService implements DatastoreService
{
	/** Our memcache namespace */
	public static final String MEMCACHE_NAMESPACE = "ObjectifyCache";
	
	/**
	 * This is necessary to track writes and update the cache only on successful commit. 
	 */
	class TransactionWrapper implements Transaction
	{
		/** The real implementation */
		Transaction raw;
		
		/** Lazily constructed set of keys we will delete if transaction commits */
		Set<Key> deferredDeletes;
		
		/** Lazily constructed set of values we will put in the cache if the transaction commits */
		Map<Key, Entity> deferredPuts;
		
		/** */
		public TransactionWrapper(Transaction raw)
		{
			this.raw = raw;
		}

		@Override
		public void commit()
		{
			this.raw.commit();
			
			// Only after successful commit should we modify the cache
			if (this.deferredDeletes != null)
				deleteFromCache(this.deferredDeletes);
			
			if (this.deferredPuts != null)
				putInCache(this.deferredPuts);
		}

		@Override
		public String getId()
		{
			return this.raw.getId();
		}

		@Override
		public boolean isActive()
		{
			return this.raw.isActive();
		}

		@Override
		public void rollback()
		{
			this.raw.rollback();
		}

		@Override
		public String getApp()
		{
			return this.raw.getApp();
		}

		/**
		 * Adds some keys which will be deleted if the commit is successful.
		 */
		public void deferCacheDelete(Key key)
		{
			Cached cachedAnno = fact.getMetadata(key).getCached();
			if (cachedAnno == null)
				return;
				
			// If there was a put, we must not put it!
			if (this.deferredPuts != null)
				this.deferredPuts.remove(key);
			
			if (this.deferredDeletes == null)
				this.deferredDeletes = new HashSet<Key>();
			
			this.deferredDeletes.add(key);
		}
		
		/**
		 * Adds some entities that will be added to the cache if the commit is successful.
		 */
		public void deferCachePut(Entity entity)
		{
			Cached cachedAnno = fact.getMetadata(entity.getKey()).getCached();
			if (cachedAnno == null)
				return;
			
			Key key = entity.getKey();
			
			// If there was a delete, we must not delete it!
			if (this.deferredDeletes != null)
				this.deferredDeletes.remove(key);
			
			if (this.deferredPuts == null)
				this.deferredPuts = new HashMap<Key, Entity>();
			
			this.deferredPuts.put(key, entity);
		}
	}
	
	/** Source of metadata so we know which kinds to cache */
	ObjectifyFactory fact;
	
	/** The real datastore service */
	DatastoreService raw;
	
	/** Lazily create this */
	MemcacheService memcache;
	
	/**
	 */
	public CachingDatastoreService(ObjectifyFactory fact, DatastoreService raw)
	{
		this.fact = fact;
		this.raw = raw;
	}
	
	/** Use this to lazily get the memcache service */
	protected MemcacheService getMemcache()
	{
		if (this.memcache == null)
			this.memcache = MemcacheServiceFactory.getMemcacheService(MEMCACHE_NAMESPACE);
		
		return this.memcache;
	}
	
	/**
	 * Breaks down the map into groupings based on which are cacheable and for how long.
	 * 
	 * @return a map of expiration to Key/Entity map for only the entities that are cacheable 
	 */
	private Map<Integer, Map<Key, Entity>> categorize(Map<Key, Entity> entities)
	{
		Map<Integer, Map<Key, Entity>> result = new HashMap<Integer, Map<Key, Entity>>();
		
		for (Map.Entry<Key, Entity> entry: entities.entrySet())
		{
			Cached cachedAnno = this.fact.getMetadata(entry.getKey()).getCached();
			if (cachedAnno != null)
			{
				Integer expiry = cachedAnno.expirationSeconds();
				
				Map<Key, Entity> grouping = result.get(expiry);
				if (grouping == null)
				{
					grouping = new HashMap<Key, Entity>();
					result.put(expiry, grouping);
				}
				
				grouping.put(entry.getKey(), entry.getValue());
			}
		}
		
		return result;
	}

	/**
	 * Get values from the datastore, inserting negative results (null values) for any keys
	 * that are requested but don't come back.
	 */
	private Map<Key, Entity> getFromDatastore(Transaction txn, Set<Key> stillNeeded)
	{
		Map<Key, Entity> result = this.raw.get(txn, stillNeeded);

		// Add null values for any keys not in the result set
		if (result.size() != stillNeeded.size())
			for (Key key: stillNeeded)
				if (!result.containsKey(key))
					result.put(key, null);
		
		return result;
	}

	/** Hides the ugly casting and deals with String/Key conversion */
	@SuppressWarnings("unchecked")
	private Map<Key, Entity> getFromCacheRaw(Iterable<Key> keys)
	{
		Collection<String> keysColl = new ArrayList<String>();
		for (Key key: keys)
			keysColl.add(KeyFactory.keyToString(key));
		
		Map<String, Entity> rawResults;
		try {
			rawResults = (Map)this.getMemcache().getAll((Collection)keysColl);
		}
		catch (Exception ex) {
			// This should only be an issue if Google changes the serialization
			// format of an Entity.  It's possible, but this is just a cache so we
			// can safely ignore the error.
			return new HashMap<Key, Entity>();
		}
		
		Map<Key, Entity> keyMapped = new HashMap<Key, Entity>((int)(rawResults.size() * 1.5));
		for(Map.Entry<String, Entity> entry: rawResults.entrySet())
			keyMapped.put(KeyFactory.stringToKey(entry.getKey()), entry.getValue());

		return keyMapped;
	}
	
	/**
	 * Get entries from cache.  Ignores uncacheable keys.
	 */
	private Map<Key, Entity> getFromCache(Iterable<Key> keys)
	{
		Collection<Key> fetch = new ArrayList<Key>();
		
		for (Key key: keys)
			if (this.fact.getMetadata(key).getCached() != null)
				fetch.add(key);
		
		return this.getFromCacheRaw(fetch);
	}
	
	/**
	 * Puts entries in the cache with the specified expiration.
	 * @param expirationSeconds can be -1 to indicate "keep as long as possible". 
	 */
	@SuppressWarnings("unchecked")
	private void putInCache(Map<Key, Entity> entities, int expirationSeconds)
	{
		Map<String, Entity> rawMap = new HashMap<String, Entity>((int)(entities.size() * 1.5));

		for (Map.Entry<Key, Entity> entry: entities.entrySet())
			rawMap.put(KeyFactory.keyToString(entry.getKey()), entry.getValue());
		
		if (expirationSeconds < 0)
			this.getMemcache().putAll((Map)rawMap);
		else
			this.getMemcache().putAll((Map)rawMap, Expiration.byDeltaSeconds(expirationSeconds));
	}
	
	/**
	 * Puts entries in the cache with the appropriate expirations.
	 */
	private void putInCache(Map<Key, Entity> entities)
	{
		Map<Integer, Map<Key, Entity>> categories = this.categorize(entities);
		
		for (Map.Entry<Integer, Map<Key, Entity>> entry: categories.entrySet())
			this.putInCache(entry.getValue(), entry.getKey());
	}
	
	/**
	 * Deletes from the cache, ignoring any noncacheable keys
	 */
	@SuppressWarnings("unchecked")
	private void deleteFromCache(Iterable<Key> keys)
	{
		Collection<String> cacheables = new ArrayList<String>();
		
		for (Key key: keys)
			if (this.fact.getMetadata(key).getCached() != null)
				cacheables.add(KeyFactory.keyToString(key));
		
		if (!cacheables.isEmpty())
			this.getMemcache().deleteAll((Collection)cacheables);
	}
	
	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#allocateIds(java.lang.String, long)
	 */
	@Override
	public KeyRange allocateIds(String kind, long num)
	{
		return this.raw.allocateIds(kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#allocateIds(com.google.appengine.api.datastore.Key, java.lang.String, long)
	 */
	@Override
	public KeyRange allocateIds(Key parent, String kind, long num)
	{
		return this.raw.allocateIds(parent, kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#beginTransaction()
	 */
	@Override
	public Transaction beginTransaction()
	{
		return new TransactionWrapper(this.raw.beginTransaction());
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public void delete(Key... keys)
	{
		this.delete(null, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<Key> keys)
	{
		this.delete(null, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public void delete(Transaction txn, Key... keys)
	{
		this.delete(txn, Arrays.asList(keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public void delete(Transaction txn, Iterable<Key> keys)
	{
		this.raw.delete(txn, keys);
		
		if (txn != null)
		{
			for (Key key: keys)
				((TransactionWrapper)txn).deferCacheDelete(key);
		}
		else
		{
			this.deleteFromCache(keys);
		}
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public Entity get(Key key) throws EntityNotFoundException
	{
		return this.get(null, key);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(java.lang.Iterable)
	 */
	@Override
	public Map<Key, Entity> get(Iterable<Key> keys)
	{
		return this.get(null, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Key)
	 */
	@Override
	public Entity get(Transaction txn, Key key) throws EntityNotFoundException
	{
		Cached cachedAnnotation = this.fact.getMetadata(key).getCached();
		
		if (txn != null || cachedAnnotation == null)
		{
			// Must ignore the cache when reading in a transaction since the
			// transaction looks at a frozen moment of time.  We can't even
			// populate the cache because the data may be old.
			return this.raw.get(txn, key);
		}
		else
		{
			// Must fetch as a collection to distinguish negative results.
			Map<Key, Entity> map = this.getFromCacheRaw(Collections.singleton(key));
			if (map.isEmpty())
			{
				try
				{
					Entity ent = this.raw.get(txn, key);
					map.put(key, ent);
					this.putInCache(map, cachedAnnotation.expirationSeconds());
					return ent;
				}
				catch (EntityNotFoundException e)
				{
					// cache negative result
					map.put(key, null);
					this.putInCache(map, cachedAnnotation.expirationSeconds());
					throw e;
				}
			}
			else
			{
				Entity result = map.values().iterator().next();
				if (result == null)
					throw new EntityNotFoundException(key);
				else
					return result;
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public Map<Key, Entity> get(Transaction txn, Iterable<Key> keys)
	{
		if (txn != null)
		{
			// Must not populate the cache since we are looking at a frozen moment in time.
			return this.raw.get(txn, keys);
		}
		else
		{
			// soFar will not containe uncacheables
			Map<Key, Entity> soFar = this.getFromCache(keys);

			Set<Key> stillNeeded = new HashSet<Key>();
			for (Key getKey: keys)
				if (!soFar.containsKey(getKey))
					stillNeeded.add(getKey);
			
			if (!stillNeeded.isEmpty())
			{
				// Includes negative results
				Map<Key, Entity> fetched = this.getFromDatastore(txn, stillNeeded);
				
				soFar.putAll(fetched);
	
				this.putInCache(fetched);
			}
			
			// Strip out any negative results
			Iterator<Entity> it = soFar.values().iterator();
			while (it.hasNext())
				if (it.next() == null)
					it.remove();
			
			return soFar;
		}
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getActiveTransactions()
	 */
	@Override
	public Collection<Transaction> getActiveTransactions()
	{
		return this.raw.getActiveTransactions();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getCurrentTransaction()
	 */
	@Override
	public Transaction getCurrentTransaction()
	{
		return this.raw.getCurrentTransaction();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getCurrentTransaction(com.google.appengine.api.datastore.Transaction)
	 */
	@Override
	public Transaction getCurrentTransaction(Transaction txn)
	{
		return this.raw.getCurrentTransaction(txn);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#prepare(com.google.appengine.api.datastore.Query)
	 */
	@Override
	public PreparedQuery prepare(Query query)
	{
		return this.raw.prepare(query);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#prepare(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Query)
	 */
	@Override
	public PreparedQuery prepare(Transaction txn, Query query)
	{
		return this.raw.prepare(txn, query);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public Key put(Entity entity)
	{
		return this.put(null, entity);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(java.lang.Iterable)
	 */
	@Override
	public List<Key> put(Iterable<Entity> entities)
	{
		return this.put(null, entities);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public Key put(Transaction txn, Entity entity)
	{
		Key result = this.raw.put(txn, entity);

		// Cacheability checking is handled inside these methods
		if (txn != null)
			((TransactionWrapper)txn).deferCachePut(entity);
		else
			this.putInCache(Collections.singletonMap(entity.getKey(), entity));
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public List<Key> put(Transaction txn, Iterable<Entity> entities)
	{
		List<Key> result = this.raw.put(txn, entities);
		
		if (txn != null)
		{
			for (Entity ent: entities)
				((TransactionWrapper)txn).deferCachePut(ent);
		}
		else
		{
			Map<Key, Entity> map = new HashMap<Key, Entity>();
			for (Entity entity: entities)
				map.put(entity.getKey(), entity);
			
			this.putInCache(map);
		}
		
		return result;
	}
}


