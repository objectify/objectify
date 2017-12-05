package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.QueryResults;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * Associates a result value with a cursor.
 */
@Data
public class ResultWithCursor<T> {
	private final T result;
	private final Cursor cursorAfter;

	/**
	 * Turns QueryResults into an iterator of the {result, cursor} tuple
	 */
	@RequiredArgsConstructor
	public static class Iterator<T> implements java.util.Iterator<ResultWithCursor<T>> {

		private final QueryResults<T> base;

		@Override
		public boolean hasNext() {
			return base.hasNext();
		}

		@Override
		public ResultWithCursor<T> next() {
			final T next = base.next();
			final Cursor cursor = base.getCursorAfter();
			return new ResultWithCursor<>(next, cursor);
		}
	}
}