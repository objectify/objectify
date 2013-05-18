package com.googlecode.objectify.impl;

import java.util.Iterator;

import com.google.appengine.api.datastore.Cursor;

/**
 * A single chunk during a query.
 */
public class Chunk<T> implements Iterator<ResultWithCursor<T>> {
	final Cursor cursor;
	final Iterator<T> valueIt;
	int offset = 0;

	public Chunk(Cursor cursor, Iterable<T> result) {
		this.cursor = cursor;
		this.valueIt = result.iterator();
	}

	@Override
	public boolean hasNext() {
		return valueIt.hasNext();
	}

	@Override
	public ResultWithCursor<T> next() {
		T value = valueIt.next();
		return new ResultWithCursor<T>(cursor, offset++, value, !valueIt.hasNext());
	}

	@Override
	public void remove() {
		valueIt.remove();
	}
}