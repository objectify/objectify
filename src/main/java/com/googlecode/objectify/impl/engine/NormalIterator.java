package com.googlecode.objectify.impl.engine;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;

/**
 * Normal iteration is actually slightly more complicated than hybrid iteration because
 * we need to stuff the entity objects in the load engine so that we don't need to do
 * further datastore hits for session misses.
 */
class NormalIterator<T> extends ChunkingIterator<T, Entity> {

	/** */
	public NormalIterator(LoadEngine loadEngine, PreparedQuery pq, FetchOptions fetchOpts) {
		super(loadEngine, pq, pq.asQueryResultIterator(fetchOpts), fetchOpts.getChunkSize());
	}

	/** */
	@Override
	protected Key<T> next(QueryResultIterator<Entity> src) {
		Entity ent = src.next();
		loadEngine.stuff(ent);
		return Key.<T>create(ent.getKey());
	}
}