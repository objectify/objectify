package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Cursor;

import java.util.Iterator;

/**
 * A single chunk during a query.
 */
public class Chunk<T> implements Iterator<ResultWithCursor<T>> {
	private final Cursor cursor;
	private final Iterator<T> valueIt;
	private int offset = 0;

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
		return new ResultWithCursor<>(cursor, offset++, value, !valueIt.hasNext());
	}

	@Override
	public void remove() {
		valueIt.remove();
	}
}
