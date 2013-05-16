package com.googlecode.objectify.impl.engine;

import com.google.appengine.api.datastore.Cursor;

/**
 *
 */
public class QueryResultBatch<T> {
	final Cursor cursor;
	public Cursor getCursor() { return cursor; }

	final Iterable<T> result;
	public Iterable<T> getResult() { return result; }

	public QueryResultBatch(Cursor cursor, Iterable<T> result) {
		this.cursor = cursor;
		this.result = result;
	}
}