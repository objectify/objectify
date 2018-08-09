package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.ReadOption;
import com.google.cloud.datastore.Transaction.Response;
import com.google.protobuf.ByteString;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.AsyncTransaction;
import com.googlecode.objectify.impl.PrivateAsyncTransaction;
import com.googlecode.objectify.util.FutureHelper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

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
@Slf4j
public class CachingAsyncTransaction extends CachingAsyncDatastoreReaderWriter implements PrivateAsyncTransaction
{
	/** */
	private final AsyncTransaction raw;

	/** */
	private final EntityMemcache memcache;

	/** Keys to remove from cache iff transaction commits */
	private final Set<Key> deferred = new HashSet<>();

	/**
	 * All futures that have been enlisted in this transaction.  In the future, when we can
	 * hook into the raw Future<?>, we shouldn't need this - the GAE SDK automatically calls
	 * quietGet() on all the enlisted Futures before a transaction commits.
	 */
	private final List<Future<?>> enlistedFutures = new ArrayList<>();

	/** */
	public CachingAsyncTransaction(final AsyncTransaction raw, final EntityMemcache memcache) {
		super(raw);
		this.raw = raw;
		this.memcache = memcache;
	}

	@Override
	protected void empty(final Iterable<Key> keys) {
		for (final Key key: keys)
			deferEmptyFromCache(key);
	}

	@Override
	public Response commit() {
		// We need to ensure that any enlisted Futures are completed before we try
		// to run the commit.  The GAE SDK does this itself, but unfortunately this
		// doesn't help our wrapped Futures.  When we can hook into Futures natively
		// we won't have to do this ourselves
		for (final Future<?> fut: this.enlistedFutures)
			FutureHelper.quietGet(fut);

		final Response response = raw.commit();

		// Only after a commit should we modify the cache
		if (!deferred.isEmpty()) {
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

			memcache.empty(deferred);
		}

		return response;
	}

	@Override
	public boolean isActive() {
		return raw.isActive();
	}

	@Override
	public void rollback() {
		raw.rollback();
	}

	@Override
	public void listenForCommit(final Runnable listener) {
		raw.listenForCommit(listener);
	}

	@Override
	public ByteString getTransactionHandle() {
		return raw.getTransactionHandle();
	}

	@Override
	public void runCommitListeners() {
		((PrivateAsyncTransaction)raw).runCommitListeners();
	}

	@Override
	public void enlist(final Result<?> result) {
		((PrivateAsyncTransaction)raw).enlist(result);
	}

	/**
	 * Adds some keys which will be deleted if the commit is successful.
	 */
	private void deferEmptyFromCache(final Key key) {
		this.deferred.add(key);
	}

	/**
	 * Adds a Future to our transaction; this Future will be completed before the transaction commits.
	 */
	private void enlist(final Future<?> future) {
		this.enlistedFutures.add(future);
	}

	@Override
	public Future<Void> delete(final Iterable<Key> keys) {
		final Future<Void> future = super.delete(keys);

		enlist(future);
		
		return future;
	}

	@Override
	public Future<Map<Key, Entity>> get(final Collection<Key> keys, final ReadOption... options) {
		// Must not populate the cache since we are looking at a frozen moment in time.
		return this.raw.get(keys, options);
	}

	@Override
	public <T> QueryResults<T> run(final Query<T> query) {
		return this.raw.run(query);
	}

	@Override
	public Future<List<Key>> put(final Iterable<? extends FullEntity<?>> entities) {
		final Future<List<Key>> future = super.put(entities);

		enlist(future);
		
		return future;
	}
}


