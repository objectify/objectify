package com.googlecode.objectify.cache;

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
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
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
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.util.FutureHelper;
import com.googlecode.objectify.util.SimpleFutureWrapper;

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
 * <p>Note2:  Until Google adds a hook that lets us wrap native Future<?> implementations,
 * this cache requires the AsyncCacheFilter to be installed.  This wasn't necessary when
 * the cache was synchronous, but async caching requires an extra hook for the end of
 * a request when fired-and-forgotten put()s and delete()s get processed.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingAsyncDatastoreService implements AsyncDatastoreService
{
	/** Source of metadata so we know which kinds to cache */
	ObjectifyFactory fact;
	
	/** The real datastore service objects - we need both */
	AsyncDatastoreService rawAsync;
	
	/** */
	MemcacheService memcache;
	
	/**
	 */
	public CachingAsyncDatastoreService(ObjectifyFactory fact, AsyncDatastoreService rawAsync, MemcacheService memcache)
	{
		this.fact = fact;
		this.rawAsync = rawAsync;
		this.memcache = memcache;
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
	private Future<Map<Key, Entity>> getFromDatastore(Transaction txn, final Set<Key> stillNeeded)
	{
		Future<Map<Key, Entity>> prelim = this.rawAsync.get(txn, stillNeeded);
		
		return new SimpleFutureWrapper<Map<Key, Entity>, Map<Key, Entity>>(prelim) {
			@Override
			protected Map<Key, Entity> wrap(Map<Key, Entity> t)
			{
				// Add null values for any keys not in the result set
				if (t.size() != stillNeeded.size())
					for (Key key: stillNeeded)
						if (!t.containsKey(key))
							t.put(key, null);
				
				return t;
			}
		};
	}

	/** Hides the ugly casting and deals with String/Key conversion */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Map<Key, Entity> getFromCacheRaw(Iterable<Key> keys)
	{
		Collection<String> keysColl = new ArrayList<String>();
		for (Key key: keys)
			keysColl.add(KeyFactory.keyToString(key));
		
		Map<String, Entity> rawResults;
		try {
			rawResults = (Map)this.memcache.getAll((Collection)keysColl);
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
	@SuppressWarnings("rawtypes")
	private void putInCache(Map<Key, Entity> entities, int expirationSeconds)
	{
		Map<String, Entity> rawMap = new HashMap<String, Entity>((int)(entities.size() * 1.5));

		for (Map.Entry<Key, Entity> entry: entities.entrySet())
			rawMap.put(KeyFactory.keyToString(entry.getKey()), entry.getValue());
		
		if (expirationSeconds < 0)
			this.memcache.putAll((Map)rawMap);
		else
			this.memcache.putAll((Map)rawMap, Expiration.byDeltaSeconds(expirationSeconds));
	}
	
	/**
	 * Puts entries in the cache with the appropriate expirations.
	 */
	void putInCache(Map<Key, Entity> entities)
	{
		Map<Integer, Map<Key, Entity>> categories = this.categorize(entities);
		
		for (Map.Entry<Integer, Map<Key, Entity>> entry: categories.entrySet())
			this.putInCache(entry.getValue(), entry.getKey());
	}
	
	/**
	 * Deletes from the cache, ignoring any noncacheable keys
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void deleteFromCache(Iterable<Key> keys)
	{
		Collection<String> cacheables = new ArrayList<String>();
		
		for (Key key: keys)
			if (this.fact.getMetadata(key).getCached() != null)
				cacheables.add(KeyFactory.keyToString(key));
		
		if (!cacheables.isEmpty())
			this.memcache.deleteAll((Collection)cacheables);
	}
	
	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#allocateIds(java.lang.String, long)
	 */
	@Override
	public Future<KeyRange> allocateIds(String kind, long num)
	{
		return this.rawAsync.allocateIds(kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#allocateIds(com.google.appengine.api.datastore.Key, java.lang.String, long)
	 */
	@Override
	public Future<KeyRange> allocateIds(Key parent, String kind, long num)
	{
		return this.rawAsync.allocateIds(parent, kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#beginTransaction()
	 */
	@Override
	public Future<Transaction> beginTransaction()
	{
		return new SimpleFutureWrapper<Transaction, Transaction>(this.rawAsync.beginTransaction()) {
			TransactionWrapper xact;

			@Override
			protected Transaction wrap(Transaction t)
			{
				if (xact == null)
					xact = new TransactionWrapper(CachingAsyncDatastoreService.this, t);
				
				return xact;
			}
		};
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#delete(com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public Future<Void> delete(Key... keys)
	{
		return this.delete(null, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#delete(java.lang.Iterable)
	 */
	@Override
	public Future<Void> delete(Iterable<Key> keys)
	{
		return this.delete(null, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#delete(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public Future<Void> delete(Transaction txn, Key... keys)
	{
		return this.delete(txn, Arrays.asList(keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#delete(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public Future<Void> delete(final Transaction txn, final Iterable<Key> keys)
	{
		ListenableFuture<Void> future = new ListenableFuture<Void>(this.rawAsync.delete(txn, keys));
		future.addCallback(new Runnable() {
			@Override
			public void run()
			{
				if (txn != null)
				{
					for (Key key: keys)
						((TransactionWrapper)txn).deferCacheDelete(key);
				}
				else
				{
					deleteFromCache(keys);
				}
			}
		});
		
		if (txn instanceof TransactionWrapper)
			((TransactionWrapper)txn).enlist(future);
		
		return future;
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public Future<Entity> get(Key key)
	{
		return this.get(null, key);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#get(java.lang.Iterable)
	 */
	@Override
	public Future<Map<Key, Entity>> get(Iterable<Key> keys)
	{
		return this.get(null, keys);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#get(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Key)
	 */
	@Override
	public Future<Entity> get(Transaction txn, final Key key)
	{
		Future<Map<Key, Entity>> bulk = this.get(txn, Collections.singleton(key));
		
		return new SimpleFutureWrapper<Map<Key, Entity>, Entity>(bulk) {
			@Override
			protected Entity wrap(Map<Key, Entity> entities) throws Exception
			{
				Entity ent = entities.get(key);
				if (ent == null)
					throw new EntityNotFoundException(key);
				else
					return ent;
			}
		};
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#get(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public Future<Map<Key, Entity>> get(Transaction txn, Iterable<Key> keys)
	{
		if (txn != null)
		{
			// Must not populate the cache since we are looking at a frozen moment in time.
			return this.rawAsync.get(txn, keys);
		}
		else
		{
			// soFar will not containe uncacheables, but it will have negative results
			Map<Key, Entity> soFar = this.getFromCache(keys);

			Set<Key> stillNeeded = new HashSet<Key>();
			for (Key getKey: keys)
				if (!soFar.containsKey(getKey))
					stillNeeded.add(getKey);

			// Maybe we need to fetch some more
			Future<Map<Key, Entity>> pending = null;
			if (!stillNeeded.isEmpty())
			{
				// Includes negative results
				Future<Map<Key, Entity>> fromDatastore = this.getFromDatastore(txn, stillNeeded);
				final ListenableFuture<Map<Key, Entity>> listenable = new ListenableFuture<Map<Key, Entity>>(fromDatastore);
				listenable.addCallback(new Runnable() {
					@Override
					public void run()
					{
						try
						{
							putInCache(listenable.get());
						}
						catch (Exception e)
						{
							// Not entirely certain what to do with this
							throw new RuntimeException(e);
						}
					}
				});
				
				pending = listenable;
			}
			
			Future<Map<Key, Entity>> merged = new MergeFuture<Key, Entity>(soFar, pending);
			
			// Need to strip out any negative results
			Future<Map<Key, Entity>> stripped = new SimpleFutureWrapper<Map<Key, Entity>, Map<Key, Entity>>(merged) {
				@Override
				protected Map<Key, Entity> wrap(Map<Key, Entity> t)
				{
					Iterator<Entity> it = t.values().iterator();
					while (it.hasNext())
						if (it.next() == null)
							it.remove();
					
					return t;
				}
			};

			if (txn instanceof TransactionWrapper)
				((TransactionWrapper)txn).enlist(stripped);
			
			return stripped;
		}
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.BaseDatastoreService#getActiveTransactions()
	 */
	@Override
	public Collection<Transaction> getActiveTransactions()
	{
		// This would conflict with the wrapped transaction object
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.BaseDatastoreService#getCurrentTransaction()
	 */
	@Override
	public Transaction getCurrentTransaction()
	{
		// This would conflict with the wrapped transaction object
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.BaseDatastoreService#getCurrentTransaction(com.google.appengine.api.datastore.Transaction)
	 */
	@Override
	public Transaction getCurrentTransaction(Transaction txn)
	{
		// This would conflict with the wrapped transaction object
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.BaseDatastoreService#prepare(com.google.appengine.api.datastore.Query)
	 */
	@Override
	public PreparedQuery prepare(Query query)
	{
		return this.rawAsync.prepare(query);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.BaseDatastoreService#prepare(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Query)
	 */
	@Override
	public PreparedQuery prepare(Transaction txn, Query query)
	{
		return this.rawAsync.prepare(txn, query);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#put(com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public Future<Key> put(Entity entity)
	{
		return this.put(null, entity);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(java.lang.Iterable)
	 */
	@Override
	public Future<List<Key>> put(Iterable<Entity> entities)
	{
		return this.put(null, entities);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#put(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public Future<Key> put(final Transaction txn, final Entity entity)
	{
		final ListenableFuture<Key> result = new ListenableFuture<Key>(this.rawAsync.put(txn, entity));
		result.addCallback(new Runnable() {
			@Override
			public void run()
			{
				// This forces the GAE future to update the key in the entity
				FutureHelper.quietGet(result);
				
				// Cacheability checking is handled inside these methods
				if (txn != null)
					((TransactionWrapper)txn).deferCachePut(entity);
				else
					putInCache(Collections.singletonMap(entity.getKey(), entity));
				
			}
		});

		if (txn instanceof TransactionWrapper)
			((TransactionWrapper)txn).enlist(result);
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#put(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public Future<List<Key>> put(final Transaction txn, final Iterable<Entity> entities)
	{
		final ListenableFuture<List<Key>> result = new ListenableFuture<List<Key>>(this.rawAsync.put(txn, entities));
		result.addCallback(new Runnable() {
			@Override
			public void run()
			{
				// This forces the GAE future to update the keys in the entities
				FutureHelper.quietGet(result);
				
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
					
					putInCache(map);
				}
				
			}
		});
		
		if (txn instanceof TransactionWrapper)
			((TransactionWrapper)txn).enlist(result);
		
		return result;
	}
}


