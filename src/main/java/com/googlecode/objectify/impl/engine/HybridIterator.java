package com.googlecode.objectify.impl.engine;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;

/**
 * Takes a keys-only iterable source, breaks it down into batches of a specific chunk size, and
 * uses batch loading to load the actual values.  This makes @Cache and @Load annotations work.
 */
class HybridIterator<T> extends ChunkingIterator<T, Key<T>> {

	/** */
	public HybridIterator(LoadEngine loadEngine, PreparedQuery pq, FetchOptions fetchOpts) {
		super(loadEngine, pq, new KeysOnlyIterator<T>(pq, fetchOpts), fetchOpts.getChunkSize());
	}

	/** */
	@Override
	protected Key<T> next(QueryResultIterator<Key<T>> src) {
		return src.next();
	}
}