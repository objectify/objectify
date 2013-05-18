package com.googlecode.objectify.impl;

import java.util.List;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;

/**
 * Takes a keys-only iterable source and produces keys. Not complicated.
 */
class KeysOnlyIterator<T> implements QueryResultIterator<Key<T>> {
	/** Input values */
	PreparedQuery pq;
	QueryResultIterator<Entity> source;

	/** */
	public KeysOnlyIterator(PreparedQuery pq, FetchOptions fetchOpts) {
		this.pq = pq;
		this.source = pq.asQueryResultIterator(fetchOpts);
	}

	@Override
	public boolean hasNext() {
		return source.hasNext();
	}

	@Override
	public Key<T> next() {
		Entity ent = source.next();
		loaded(ent);
		return Key.create(ent.getKey());
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 */
	@Override
	public Cursor getCursor() {
		return source.getCursor();
	}

	@Override
	public List<Index> getIndexList() {
		return source.getIndexList();
	}

	/** Can be overriden to add behavior when an entity is loaded */
	protected void loaded(Entity ent) {
		// Default do nothing
	}
}