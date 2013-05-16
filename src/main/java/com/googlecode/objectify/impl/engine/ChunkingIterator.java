package com.googlecode.objectify.impl.engine;

import java.util.List;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;

/**
 * Base class for normal and hybrid iterators, handles the chunking logic.
 */
public class ChunkingIterator<T> implements QueryResultIterator<T> {
	/** */
	static final Logger log = Logger.getLogger(ChunkingIterator.class.getName());

	/** Input values */
	private PreparedQuery pq;
	private QueryResultIterator<Key<T>> source;

	/** As we process */
	private QueryResultStreamIterator<T> stream;

	/** */
	public ChunkingIterator(LoadEngine loadEngine, PreparedQuery pq, QueryResultIterator<Key<T>> source, int chunkSize) {
		this.pq = pq;
		this.source = source;

		this.stream = new QueryResultStreamIterator<T>(source, chunkSize, loadEngine);
	}

	@Override
	public boolean hasNext() {
		return stream.hasNext();
	}

	@Override
	public T next() {
		return stream.next();
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
		if (stream.getOffset() == 0) {
			return stream.getBaseCursor();
		} else {
			// There may not be a baseCursor if we haven't iterated yet
			FetchOptions opts = FetchOptions.Builder.withDefaults();
			if (stream.getBaseCursor() != null)
				opts = opts.startCursor(stream.getBaseCursor());

			return pq.asQueryResultIterator(opts.offset(stream.getOffset()).limit(0)).getCursor();
		}
	}

	@Override
	public List<Index> getIndexList() {
		return this.source.getIndexList();
	}
}