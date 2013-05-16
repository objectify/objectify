package com.googlecode.objectify.impl.engine;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;

/**
 * Adds stuffing of the entity result.
 */
class StuffingIterator<T> extends KeysOnlyIterator<T> {

	final LoadEngine loadEngine;

	/** */
	public StuffingIterator(PreparedQuery pq, FetchOptions fetchOpts, LoadEngine loadEngine) {
		super(pq, fetchOpts);

		this.loadEngine = loadEngine;
	}

	@Override
	protected void loaded(Entity ent) {
		loadEngine.stuff(ent);
	}
}