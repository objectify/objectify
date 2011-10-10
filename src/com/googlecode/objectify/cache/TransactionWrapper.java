package com.googlecode.objectify.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.util.FutureHelper;

/**
 * This is necessary to track writes and update the cache only on successful commit. 
 */
class TransactionWrapper implements Transaction
{
	/** */
	EntityMemcache cache;
	
	/** The real implementation */
	Transaction raw;
	
	/** Lazily constructed set of keys we will EMPTY if transaction commits */
	Set<Key> deferred;
	
	/** 
	 * All futures that have been enlisted in this transaction.  In the future, when we can
	 * hook into the raw Future<?>, we shouldn't need this - the GAE SDK automatically calls
	 * quietGet() on all the enlisted Futures before a transaction commits. 
	 */
	List<Future<?>> enlistedFutures = new ArrayList<Future<?>>();
	
	/** */
	public TransactionWrapper(EntityMemcache cache, Transaction raw)
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
		
		Future<Void> future = new TriggerFuture<Void>(this.raw.commitAsync()) {
			@Override
			protected void trigger()
			{
				// Only after a commit should we modify the cache
				if (deferred != null)
				{
					// According to Alfred, ConcurrentModificationException does not necessarily mean
					// the write failed.  So this optimization is a bad idea.
					//
					// There is one special case - if we have a ConcurrentModificationException, we don't
					// need to empty the cache because whoever succeeded in their write took care of it.
					//try {
					//	this.raw.get();
					//} catch (ExecutionException ex) {
					//	if (ex.getCause() instanceof ConcurrentModificationException)
					//		return;
					//} catch (Exception ex) {}
					
					cache.empty(deferred);
				}
			}
		};
		
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
	public void deferEmptyFromCache(Key key)
	{
		if (this.deferred == null)
			this.deferred = new HashSet<Key>();
		
		this.deferred.add(key);
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