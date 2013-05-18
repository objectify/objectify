package com.googlecode.objectify.impl;

import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.util.DatastoreUtils;

/**
 * Logic for dealing with queries.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryEngine
{
	/** */
	static final Logger log = Logger.getLogger(QueryEngine.class.getName());

	/** */
	protected LoaderImpl loader;
	protected AsyncDatastoreService ads;
	protected Transaction transactionRaw;

	/**
	 */
	public QueryEngine(LoaderImpl loader, AsyncDatastoreService ads, Transaction transactionRaw) {
		this.loader = loader;
		this.ads = ads;
		this.transactionRaw = transactionRaw;
	}

	/**
	 * Perform a keys-only query.
	 */
	public <T> QueryResultIterable<Key<T>> queryKeysOnly(com.google.appengine.api.datastore.Query query, final FetchOptions fetchOpts) {
		assert query.isKeysOnly();
		log.finest("Starting keys-only query");

		final PreparedQuery pq = prepare(query);

		return new QueryResultIterable<Key<T>>() {
			@Override
			public QueryResultIterator<Key<T>> iterator() {
				return new KeysOnlyIterator<T>(pq, fetchOpts);
			}
		};
	}

	/**
	 * Perform a keys-only plus batch gets.
	 */
	public <T> QueryResultIterable<T> queryHybrid(com.google.appengine.api.datastore.Query query, final FetchOptions fetchOpts) {
		assert !query.isKeysOnly();
		log.finest("Starting hybrid query");

		query = DatastoreUtils.cloneQuery(query).setKeysOnly();

		final PreparedQuery pq = prepare(query);

		return new QueryResultIterable<T>() {
			@Override
			public QueryResultIterator<T> iterator() {
				return new ChunkingIterator<T>(loader.createLoadEngine(), pq, new KeysOnlyIterator<T>(pq, fetchOpts), fetchOpts.getChunkSize());
			}
		};
	}

	/**
	 * A normal, non-hybrid query
	 */
	public <T> QueryResultIterable<T> queryNormal(com.google.appengine.api.datastore.Query query, final FetchOptions fetchOpts) {
		assert !query.isKeysOnly();
		log.finest("Starting normal query");

		final PreparedQuery pq = prepare(query);
		final LoadEngine loadEngine = loader.createLoadEngine();

		return new QueryResultIterable<T>() {
			@Override
			public QueryResultIterator<T> iterator() {
				return new ChunkingIterator<T>(loadEngine, pq, new StuffingIterator<T>(pq, fetchOpts, loadEngine), fetchOpts.getChunkSize());
			}
		};
	}

	/**
	 * The fundamental query count operation.  This is sufficiently different from normal query().
	 */
	public int queryCount(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = prepare(query);
		return pq.countEntities(fetchOpts);
	}

	/** */
	private PreparedQuery prepare(com.google.appengine.api.datastore.Query query) {
		return ads.prepare(transactionRaw, query);
	}
}