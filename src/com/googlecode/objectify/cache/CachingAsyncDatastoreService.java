package com.googlecode.objectify.cache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreAttributes;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.Index.IndexState;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.cache.EntityMemcache.Bucket;
import com.googlecode.objectify.util.NowFuture;
import com.googlecode.objectify.util.SimpleFutureWrapper;

/**
 * <p>A write-through memcache for Entity objects that works for both transactional
 * and nontransactional sessions.</p>
 * 
 * <ul>
 * <li>Caches negative results as well as positive results.</li>
 * <li>Queries do not affect the cache in any way.</li>
 * <li>Transactional reads bypass the cache, but successful transaction commits will update the cache.</li>
 * <li>This cache has near-transactional integrity.  As long as DeadlineExceededException is not hit, cache should
 * not go out of sync even under heavy contention.</li>
 * <li>Heavy contention will reduce the hit rate of the cache, possibly severely.  Please star
 * <a href="http://code.google.com/p/googleappengine/issues/detail?id=5859">this issue</a>.</li>
 * </ul>
 * 
 * <p>Note:  Until Google adds a hook that lets us wrap native Future<?> implementations,
 * you muse install the {@code AsyncCacheFilter} to use this cache asynchronously.  This
 * is not necessary for synchronous use of {@code CachingDatastoreService}, but asynchronous
 * operation requires an extra hook for the end of a request when fired-and-forgotten put()s
 * and delete()s get processed.  <strong>If you use this cache asynchronously, and you do not
 * use the {@code AsyncCacheFilter}, your cache will go out of sync.</strong></p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingAsyncDatastoreService implements AsyncDatastoreService
{
	/** The real datastore service objects - we need both */
	AsyncDatastoreService rawAsync;
	
	/** */
	EntityMemcache memcache;
	
	/**
	 */
	public CachingAsyncDatastoreService(AsyncDatastoreService rawAsync, EntityMemcache memcache)
	{
		this.rawAsync = rawAsync;
		this.memcache = memcache;
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
	
	/**
	 * Need this for beingTransaction()
	 */
	private class TransactionFutureWrapper extends SimpleFutureWrapper<Transaction, Transaction>
	{
		TransactionWrapper xact;

		public TransactionFutureWrapper(Future<Transaction> base)
		{
			super(base);
		}

		@Override
		protected Transaction wrap(Transaction t)
		{
			if (xact == null)
				xact = new TransactionWrapper(memcache, t);
			
			return xact;
		}
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#beginTransaction()
	 */
	@Override
	public Future<Transaction> beginTransaction()
	{
		return new TransactionFutureWrapper(this.rawAsync.beginTransaction());
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#beginTransaction(com.google.appengine.api.datastore.TransactionOptions)
	 */
	@Override
	public Future<Transaction> beginTransaction(TransactionOptions options)
	{
		return new TransactionFutureWrapper(this.rawAsync.beginTransaction(options));
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
		Future<Void> future = new TriggerSuccessFuture<Void>(this.rawAsync.delete(txn, keys)) {
			@Override
			protected void success(Void result)
			{
				if (txn != null)
				{
					for (Key key: keys)
						((TransactionWrapper)txn).deferEmptyFromCache(key);
				}
				else
				{
					memcache.empty(keys);
				}
			}
		};
		
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
			Map<Key, Bucket> soFar = this.memcache.getAll(keys);

			final List<Bucket> uncached = new ArrayList<Bucket>(soFar.size());
			Map<Key, Entity> cached = new HashMap<Key, Entity>();
			
			for (Bucket buck: soFar.values())
				if (buck.isEmpty())
					uncached.add(buck);
				else if (!buck.isNegative())
					cached.put(buck.getKey(), buck.getEntity());

			// Maybe we need to fetch some more
			Future<Map<Key, Entity>> pending = null;
			if (!uncached.isEmpty())
			{
				Future<Map<Key, Entity>> fromDatastore = this.rawAsync.get(null, EntityMemcache.keysOf(uncached));
				pending = new TriggerSuccessFuture<Map<Key, Entity>>(fromDatastore) {
					@Override
					public void success(Map<Key, Entity> result)
					{
						for (Bucket buck: uncached)
						{
							Entity value = result.get(buck.getKey());
							if (value != null)
								buck.setNext(value);
						}
						
						memcache.putAll(uncached);
					}
				};
			}
			
			// If there was nothing from the cache, don't need to merge!
			if (cached.isEmpty())
				if (pending == null)
					return new NowFuture<Map<Key, Entity>>(cached);	// empty!
				else
					return pending;
			else
				return new MergeFuture<Key, Entity>(cached, pending);
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
		Future<List<Key>> bulk = this.put(txn, Collections.singleton(entity));
		
		return new SimpleFutureWrapper<List<Key>, Key>(bulk) {
			@Override
			protected Key wrap(List<Key> keys) throws Exception
			{
				return keys.get(0);
			}
		};
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#put(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public Future<List<Key>> put(final Transaction txn, Iterable<Entity> entities)
	{
		Future<List<Key>> future = new TriggerSuccessFuture<List<Key>>(this.rawAsync.put(txn, entities)) {
			@Override
			protected void success(List<Key> result)
			{
				if (txn != null)
				{
					for (Key key: result)
						((TransactionWrapper)txn).deferEmptyFromCache(key);
				}
				else
				{
					memcache.empty(result);
				}
			}
		};
		
		if (txn instanceof TransactionWrapper)
			((TransactionWrapper)txn).enlist(future);
		
		return future;
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#getDatastoreAttributes()
	 */
	@Override
	public Future<DatastoreAttributes> getDatastoreAttributes()
	{
		return this.rawAsync.getDatastoreAttributes();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.AsyncDatastoreService#getIndexes()
	 */
	@Override
	public Future<Map<Index, IndexState>> getIndexes()
	{
		return this.rawAsync.getIndexes();
	}
}


