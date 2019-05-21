package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.QueryResults;
import com.google.datastore.v1.QueryResultBatch.MoreResultsType;
import com.googlecode.objectify.Key;
import lombok.RequiredArgsConstructor;

/**
 * Converts from native Keys to Objectify Keys
 */
@RequiredArgsConstructor
class KeyQueryResults<T> implements QueryResults<Key<T>> {
	/** Input values */
	private final QueryResults<com.google.cloud.datastore.Key> source;

	@Override
	public boolean hasNext() {
		return source.hasNext();
	}

	@Override
	public Key<T> next() {
		final com.google.cloud.datastore.Key key = source.next();
		return Key.create(key);
	}

	/**
	 */
	@Override
	public Cursor getCursorAfter() {
		return source.getCursorAfter();
	}

	@Override
	public Class<?> getResultClass() {
		return Key.class;
	}

	@Override
	public int getSkippedResults() {
		return source.getSkippedResults();
	}

	@Override
	public MoreResultsType getMoreResults() {
		return source.getMoreResults();
	}
}