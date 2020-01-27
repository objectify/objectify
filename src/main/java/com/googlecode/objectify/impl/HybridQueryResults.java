package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.QueryResults;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.datastore.v1.QueryResultBatch.MoreResultsType;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.IterateFunction;
import lombok.Getter;

import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

/**
 * Converts keys-only query results into hybrid query results. This involves chunking the keys into batches and loading
 * each from the datastore. Care is taken to preserve cursor behavior and filter null results (possible due to both
 * the time delay between the query and the load and also eventual consistency in general).
 */
public class HybridQueryResults<T> implements QueryResults<T> {

	/** */
	private final LoadEngine loadEngine;

	/** */
	private final Iterator<ResultWithCursor<T>> stream;

	/** We need this so we can acquire the cursor at the end */
	private final QueryResults<Key<T>> source;

	/** Track the values for the next time we need to get this */
	@Getter
	private Cursor cursorAfter;

	/**
	 * @param chunkSize can be MAX_VALUE to indicate "just one chunk" 
	 */
	public HybridQueryResults(
			final LoadEngine loadEngine,
			final QueryResults<Key<T>> source,
			final int chunkSize) {

		this.loadEngine = loadEngine;
		this.source = source;

		// Always start with whatever was in the source to begin with
		this.cursorAfter = source.getCursorAfter();

		// Turn the result in to {key, cursor} pairs
		final Iterator<ResultWithCursor<Key<T>>> withCursor = new ResultWithCursor.Iterator<>(source);

		// Break it into chunks
		final Iterator<Iterator<ResultWithCursor<Key<T>>>> chunked = safePartition(withCursor, chunkSize);
		
		// Load each chunk as a batch
		final Iterator<Iterator<ResultWithCursor<T>>> loaded = Iterators.transform(chunked, this::load);
		
		// Put the chunks back into a linear stream
		final Iterator<ResultWithCursor<T>> concatenated = Iterators.concat(loaded);
		
		// Filter out any null results
		this.stream = Iterators.filter(concatenated, rwc -> rwc.getResult() != null);
	}

	/** Detects Integer.MAX_VALUE and prevents OOM exceptions */
	private <T> Iterator<Iterator<T>> safePartition(final Iterator<T> input, int chunkSize) {
		// Cloud Datastore library errors if you try to fetch more than 1000 keys at a time
		if (chunkSize > AsyncDatastoreReaderWriterImpl.MAX_READ_SIZE) {
			chunkSize = AsyncDatastoreReaderWriterImpl.MAX_READ_SIZE;
		}
		return Iterators.transform(Iterators.partition(input, chunkSize), IterateFunction.instance());
	}

	/** Loads them; note that it's possible for some loaded results to be null */
	private Iterator<ResultWithCursor<T>> load(final Iterator<ResultWithCursor<Key<T>>> keys) {
		final List<Entry<ResultWithCursor<Key<T>>, Result<T>>> results = Lists.newArrayList();

		while (keys.hasNext()) {
			final ResultWithCursor<Key<T>> next = keys.next();
			results.add(Maps.immutableEntry(next, loadEngine.load(next.getResult())));
		}

		loadEngine.execute();

		return Iterators.transform(results.iterator(), entry -> new ResultWithCursor<>(entry.getValue().now(), entry.getKey().getCursorAfter()));
	}

	@Override
	public boolean hasNext() {
		final boolean hasNext = stream.hasNext();

		// This addresses the edge case of iterating on an empty result set. In that case, the
		// cursor we get after calling source.hasNext() is different from the one we get before.
		if (!hasNext)
			this.cursorAfter = source.getCursorAfter();

		return hasNext;
	}

	@Override
	public T next() {
		final ResultWithCursor<T> result = stream.next();
		cursorAfter = result.getCursorAfter();

		return result.getResult();
	}

	@Override
	public Class<?> getResultClass() {
		// Not really possible to do this; a query could produce anything
		return Object.class;
	}

	@Override
	public int getSkippedResults() {
		throw new UnsupportedOperationException();
	}

	@Override
	public MoreResultsType getMoreResults() {
		throw new UnsupportedOperationException();
	}
}