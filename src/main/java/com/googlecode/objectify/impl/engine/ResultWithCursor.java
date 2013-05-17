package com.googlecode.objectify.impl.engine;

import com.google.appengine.api.datastore.Cursor;

/**
 * Associates a result value with a base cursor + offset to this particular item. Watch out when creating cursors;
 * typically you want a cursor to the next item not this one.
 */
public class ResultWithCursor<T> {
	final Cursor cursor;
	public Cursor getCursor() { return cursor; }

	/** Offset is the offset of *this* item; not necessarily what you want to use for a cursor (usually the _next_) */
	final int offset;
	public int getOffset() { return offset; }

	final T result;
	public T getResult() { return result; }

	/** True if this is the last item in the chunk */
	final boolean lastInChunk;
	public boolean isLast() { return lastInChunk; }

	public ResultWithCursor(Cursor cursor, int offset, T result, boolean lastInChunk) {
		this.cursor = cursor;
		this.offset = offset;
		this.result = result;
		this.lastInChunk = lastInChunk;
	}
}