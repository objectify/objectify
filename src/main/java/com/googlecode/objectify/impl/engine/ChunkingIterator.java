package com.googlecode.objectify.impl.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;

/**
 * Base class for normal and hybrid iterators, handles the chunking logic.
 */
abstract class ChunkingIterator<T, E> implements QueryResultIterator<KeyResultPair<T>> {
	/** */
	static final Logger log = Logger.getLogger(ChunkingIterator.class.getName());

	/** Input values */
	protected LoadEngine loadEngine;
	private PreparedQuery pq;
	private QueryResultIterator<E> source;
	private int chunkSize;

	/** As we process */
	private Iterator<KeyResultPair<T>> batchIt;
	private Cursor baseCursor;
	private int offsetIntoBatch;

	/** */
	public ChunkingIterator(LoadEngine loadEngine, PreparedQuery pq, QueryResultIterator<E> source, int chunkSize) {
		this.loadEngine = loadEngine;
		this.pq = pq;
		this.source = source;
		this.chunkSize = chunkSize;

		this.advanceBatch();
	}

	@Override
	public boolean hasNext() {
		return batchIt.hasNext();
	}

	@Override
	public KeyResultPair<T> next() {
		KeyResultPair<T> pair = batchIt.next();
		offsetIntoBatch++;

		if (!batchIt.hasNext())
			this.advanceBatch();

		return pair;
	}

	private void advanceBatch() {
		List<KeyResultPair<T>> results = new ArrayList<KeyResultPair<T>>();

		// Initialize the cursor and the offset so that we can generate a cursor later
		baseCursor = source.getCursor();
		offsetIntoBatch = 0;

		for (int i=0; i<chunkSize; i++) {
			if (!source.hasNext())
				break;

			Key<T> key = next(source);

			if (log.isLoggable(Level.FINEST))
				log.finest("Query found " + key);

			Result<T> result = loadEngine.load(key);
			results.add(new KeyResultPair<T>(key, result));
		}

		loadEngine.execute();
		batchIt = results.iterator();
	}

	/**
	 * Implement this to get the next key from the source, and possibly do some other processing
	 * like stuffing an entity in the load engine.
	 */
	abstract protected Key<T> next(QueryResultIterator<E> src);

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
		if (offsetIntoBatch == 0) {
			return baseCursor;
		} else {
			// There may not be a baseCursor if we haven't iterated yet
			FetchOptions opts = FetchOptions.Builder.withDefaults();
			if (baseCursor != null)
				opts = opts.startCursor(baseCursor);

			return pq.asQueryResultIterator(opts.offset(offsetIntoBatch).limit(0)).getCursor();
		}
	}

	@Override
	public List<Index> getIndexList() {
		return this.source.getIndexList();
	}
}