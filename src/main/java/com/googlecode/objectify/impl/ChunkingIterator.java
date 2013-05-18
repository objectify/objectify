package com.googlecode.objectify.impl;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;
import com.googlecode.objectify.Key;

/**
 * Base class for normal and hybrid iterators, handles the chunking logic.
 *
 * The bulk of the complexity is in the QueryResultStreamIterator; this just handles stripping out
 * null values but being careful about preserving cursor behavior.
 */
public class ChunkingIterator<T> implements QueryResultIterator<T> {
	/** */
	static final Logger log = Logger.getLogger(ChunkingIterator.class.getName());

	/** Input values */
	private PreparedQuery pq;
	private QueryResultIterator<Key<T>> source;

	/** As we process */
	PeekingIterator<ResultWithCursor<T>> stream;

	/** Track the values for the next time we need to get this */
	Cursor nextCursor;
	int nextOffset;

	/** */
	public ChunkingIterator(LoadEngine loadEngine, PreparedQuery pq, QueryResultIterator<Key<T>> source, int chunkSize) {
		this.pq = pq;
		this.source = source;

		ChunkIterator<T> chunkIt = new ChunkIterator<T>(source, chunkSize, loadEngine);
		this.stream = Iterators.peekingIterator(Iterators.concat(chunkIt));

		// Always start with a cursor; there might actually be any results
		this.nextCursor = source.getCursor();
	}

	@Override
	public boolean hasNext() {
		while (stream.hasNext()) {
			ResultWithCursor<T> peek = stream.peek();
			nextCursor = peek.getCursor();
			nextOffset = peek.getOffset();

			if (peek.getResult() != null)
				return true;
			else
				stream.next();
		}

		return false;
	}

	@Override
	public T next() {
		while (stream.hasNext()) {
			ResultWithCursor<T> rc = stream.next();

			if (rc.isLast()) {
				// We know we are back to the beginning of a batch, and the source cursor should be pointed the right place.
				nextCursor = source.getCursor();
				nextOffset = 0;
			} else {
				nextCursor = rc.getCursor();
				nextOffset = rc.getOffset() + 1;
			}

			if (rc.getResult() != null) {
				return rc.getResult();
			}
		}

		throw new NoSuchElementException();
	}

	/** Not implemented */
	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	/**
	 * From Alfred Fuller (principal GAE datastore guru):
	 *
	 * Calling getCursor() for results in the middle of a batch forces the sdk to run a new query as seen here:
	 * http://code.google.com/p/googleappengine/source/browse/trunk/java/src/main/com/google/appengine/api/datastore/Cursor.java#70
	 *
	 * Doing this for every result will definitely give you really bad performance. I have several yet to be implemented ideas
	 * that would solve this problem (which you potentially could push me into prioritizing), but I believe you can solve the
	 * performance problem today by saving the start_cursor an offset into the batch. Then you can evaluate the real cursor on
	 * demand using "query.asQueryResultIterator(withStartCursor(cursor).offset(n).limit(0)).getCursor()"
	 */
	@Override
	public Cursor getCursor() {
		if (nextOffset == 0) {
			return nextCursor;
		} else {
			// There may not be a baseCursor if we haven't iterated yet
			FetchOptions opts = FetchOptions.Builder.withDefaults();
			if (nextCursor != null)
				opts = opts.startCursor(nextCursor);

			return pq.asQueryResultIterator(opts.offset(nextOffset).limit(0)).getCursor();
		}
	}

	@Override
	public List<Index> getIndexList() {
		return this.source.getIndexList();
	}
}