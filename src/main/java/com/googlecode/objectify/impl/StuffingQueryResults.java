package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.QueryResults;
import com.google.datastore.v1.QueryResultBatch.MoreResultsType;
import lombok.RequiredArgsConstructor;

/**
 * Takes a normal Entity-based QueryResults and converts it to a Key-based QueryResults while
 * simultaneously stuffing the entity values into the load engine.
 */
@RequiredArgsConstructor
class StuffingQueryResults implements QueryResults<Key> {

	private final LoadEngine loadEngine;
	private final QueryResults<Entity> base;

	@Override
	public Class<?> getResultClass() {
		return Key.class;
	}

	@Override
	public Cursor getCursorAfter() {
		return base.getCursorAfter();
	}

	@Override
	public boolean hasNext() {
		return base.hasNext();
	}

	@Override
	public Key next() {
		final Entity entity = base.next();
		loadEngine.stuff(entity);
		return entity.getKey();
	}

	@Override
	public int getSkippedResults() {
		return base.getSkippedResults();
	}

	@Override
	public MoreResultsType getMoreResults() {
		return base.getMoreResults();
	}
}