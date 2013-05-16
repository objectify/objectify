package com.googlecode.objectify.impl.engine;

import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.IterateFunction;
import com.googlecode.objectify.util.ResultNowFunction;

/**
 * Splits a QueryResultIterator into a series of batches which include the Cursor for
 * the beginning of the batch.
 */
public class QueryResultBatchIterator<T> implements Iterator<QueryResultBatch<T>> {

	QueryResultIterator<Key<T>> base;
	Iterator<Iterator<Key<T>>> batches;
	LoadEngine engine;

	public QueryResultBatchIterator(QueryResultIterator<Key<T>> base, int batchSize, LoadEngine engine) {
		this.base = base;

		// Iterators.partition() allocates lists with capacity of whatever batch size you pass in; if batch
		// size is unlimited, we end up trying to allocate maxint.
		this.batches = (batchSize == Integer.MAX_VALUE)
				? Iterators.<Iterator<Key<T>>>singletonIterator(base)
				: Iterators.transform(Iterators.partition(base, batchSize), IterateFunction.<Key<T>>instance());
		this.engine = engine;
	}

	@Override
	public boolean hasNext() {
		return batches.hasNext();
	}

	@Override
	public QueryResultBatch<T> next() {
		Cursor cursor = base.getCursor();
		Iterator<Key<T>> keys = batches.next();
		List<Result<T>> results = Lists.newArrayList();

		while (keys.hasNext()) {
			results.add(engine.load(keys.next()));
		}

		engine.execute();

		Iterable<T> materialized = Iterables.transform(results, ResultNowFunction.<T>instance());

		return new QueryResultBatch<T>(cursor, materialized);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}