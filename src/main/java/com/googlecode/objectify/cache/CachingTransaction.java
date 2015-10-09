package com.googlecode.objectify.cache;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.util.FutureHelper;
import com.googlecode.objectify.util.cmd.TransactionWrapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * This is necessary to track writes and update the cache only on successful commit.
 */
class CachingTransaction extends TransactionWrapper
{
	/** */
	private EntityMemcache cache;

	/** Lazily constructed set of keys we will EMPTY if transaction commits */
	private Set<Key> deferred;

	/**
	 * All futures that have been enlisted in this transaction.  In the future, when we can
	 * hook into the raw Future<?>, we shouldn't need this - the GAE SDK automatically calls
	 * quietGet() on all the enlisted Futures before a transaction commits.
	 */
	private List<Future<?>> enlistedFutures = new ArrayList<>();

	/** */
	public CachingTransaction(EntityMemcache cache, Transaction raw) {
		super(raw);
		this.cache = cache;
	}

	@Override
	public void commit() {
		FutureHelper.quietGet(this.commitAsync());
	}

	@Override
	public Future<Void> commitAsync() {
		// We need to ensure that any enlisted Futures are completed before we try
		// to run the commit.  The GAE SDK does this itself, but unfortunately this
		// doesn't help our wrapped Futures.  When we can hook into Futures natively
		// we won't have to do this ourselves
		for (Future<?> fut: this.enlistedFutures)
			FutureHelper.quietGet(fut);

		return new TriggerFuture<Void>(super.commitAsync()) {
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
	}

	/**
	 * Adds some keys which will be deleted if the commit is successful.
	 */
	public void deferEmptyFromCache(Key key) {
		if (this.deferred == null)
			this.deferred = new HashSet<>();

		this.deferred.add(key);
	}

	/**
	 * Adds a Future to our transaction; this Future will be completed before the transaction commits.
	 * TODO:  remove this method when the GAE SDK provides a way to hook into Futures.
	 */
	public void enlist(Future<?> future) {
		this.enlistedFutures.add(future);
	}
}
