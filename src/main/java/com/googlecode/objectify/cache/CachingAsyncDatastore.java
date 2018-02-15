package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.ReadOption;
import com.googlecode.objectify.cache.EntityMemcache.Bucket;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.impl.AsyncTransaction;
import com.googlecode.objectify.util.FutureNow;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class CachingAsyncDatastore extends CachingAsyncDatastoreReaderWriter implements AsyncDatastore
{
	/** */
	private final AsyncDatastore raw;
	
	/** */
	private final EntityMemcache memcache;

	public CachingAsyncDatastore(final AsyncDatastore raw, final EntityMemcache memcache) {
		super(raw);
		this.raw = raw;
		this.memcache = memcache;
	}

	@Override
	protected void empty(final Iterable<Key> keys) {
		memcache.empty(keys);
	}

	@Override
	public AsyncTransaction newTransaction(final Runnable afterCommit) {
		return new CachingAsyncTransaction(raw.newTransaction(afterCommit), memcache);
	}

	@Override
	public Future<Map<Key, Entity>> get(final Collection<Key> keys, final ReadOption... options) {
		final Map<Key, Bucket> soFar = this.memcache.getAll(keys);

		final List<Bucket> uncached = new ArrayList<>(soFar.size());
		final Map<Key, Entity> cached = new HashMap<>();

		for (final Bucket buck: soFar.values())
			if (buck.isEmpty())
				uncached.add(buck);
			else if (!buck.isNegative())
				cached.put(buck.getKey(), buck.getEntity());

		// Maybe we need to fetch some more
		Future<Map<Key, Entity>> pending = null;
		if (!uncached.isEmpty()) {
			final Future<Map<Key, Entity>> fromDatastore = this.raw.get(EntityMemcache.keysOf(uncached), options);

			pending = new TriggerSuccessFuture<Map<Key, Entity>>(fromDatastore) {
				@Override
				public void success(final Map<Key, Entity> result) {
					for (final Bucket buck: uncached) {
						final Entity value = result.get(buck.getKey());
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
				return new FutureNow<>(cached);	// empty!
			else
				return pending;
		else
			return new MergeFuture<>(cached, pending);
	}
}


