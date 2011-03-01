package com.googlecode.objectify.cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.util.FutureHelper;

/**
 * This is necessary to track writes and update the cache only on successful commit. 
 */
class TransactionWrapper implements Transaction
{
	/** */
	CachingAsyncDatastoreService cache;
	
	/** The real implementation */
	Transaction raw;
	
	/** Lazily constructed set of keys we will delete if transaction commits */
	Set<Key> deferredDeletes;
	
	/** Lazily constructed set of values we will put in the cache if the transaction commits */
	Map<Key, Entity> deferredPuts;
	
	/** 
	 * All futures that have been enlisted in this transaction.  In the future, when we can
	 * hook into the raw Future<?>, we shouldn't need this - the GAE SDK automatically calls
	 * quietGet() on all the enlisted Futures before a transaction commits. 
	 */
	List<Future<?>> enlistedFutures = new ArrayList<Future<?>>();
	
	/** */
	public TransactionWrapper(CachingAsyncDatastoreService cache, Transaction raw)
	{
		this.cache = cache;
		this.raw = raw;
	}

	@Override
	public void commit()
	{
		FutureHelper.quietGet(this.commitAsync());
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
		FutureHelper.quietGet(this.rollbackAsync());
	}

	@Override
	public String getApp()
	{
		return this.raw.getApp();
	}

	@Override
	public Future<Void> commitAsync()
	{
		// We need to ensure that any enlisted Futures are completed before we try
		// to run the commit.  The GAE SDK does this itself, but unfortunately this
		// doesn't help our wrapped Futures.  When we can hook into Futures natively
		// we won't have to do this ourselves
		for (Future<?> fut: this.enlistedFutures)
			FutureHelper.quietGet(fut);
		
		ListenableFuture<Void> future = new ListenableFuture<Void>(this.raw.commitAsync());
		future.addCallback(new Runnable() {
			@Override
			public void run()
			{
				// Only after successful commit should we modify the cache
				if (deferredDeletes != null)
					cache.deleteFromCache(deferredDeletes);
				
				if (deferredPuts != null)
					cache.putInCache(deferredPuts);
			}
		});
		
		return future;
	}

	@Override
	public Future<Void> rollbackAsync()
	{
		return this.raw.rollbackAsync();
	}

	/**
	 * Adds some keys which will be deleted if the commit is successful.
	 */
	public void deferCacheDelete(Key key)
	{
		if (!this.cache.fact.getMetadata(key).mightBeInCache())
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
		Cached cachedAnno = this.cache.fact.getMetadata(entity.getKey()).getCached(entity);
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
	
	/**
	 * Adds a Future to our transaction; this Future will be completed before the transaction commits.
	 * TODO:  remove this method when the GAE SDK provides a way to hook into Futures.
	 */
	public void enlist(Future<?> future)
	{
		this.enlistedFutures.add(future);
	}
}