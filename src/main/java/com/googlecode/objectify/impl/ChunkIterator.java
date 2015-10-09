package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.IterateFunction;
import com.googlecode.objectify.util.ResultNowFunction;

import java.util.Iterator;
import java.util.List;

/**
 * Splits a QueryResultIterator into a series of chunks which include the Cursor for
 * the beginning of the chunk. The results are materialized in the results as well.
 */
public class ChunkIterator<T> implements Iterator<Chunk<T>> {

	QueryResultIterator<Key<T>> allKeys;
	Iterator<Iterator<Key<T>>> chunks;
	LoadEngine engine;

	public ChunkIterator(QueryResultIterator<Key<T>> allKeys, int chunkSize, LoadEngine engine) {
		this.allKeys = allKeys;

		// Iterators.partition() allocates lists with capacity of whatever batch size you pass in; if batch
		// size is unlimited, we end up trying to allocate maxint.
		this.chunks = (chunkSize == Integer.MAX_VALUE)
				? Iterators.<Iterator<Key<T>>>singletonIterator(allKeys)
				: Iterators.transform(Iterators.partition(allKeys, chunkSize), IterateFunction.<Key<T>>instance());
		this.engine = engine;
	}

	@Override
	public boolean hasNext() {
		return chunks.hasNext();
	}

	@Override
	public Chunk<T> next() {
		Cursor cursor = allKeys.getCursor();
		Iterator<Key<T>> keys = chunks.next();
		List<Result<T>> results = Lists.newArrayList();

		while (keys.hasNext()) {
			results.add(engine.load(keys.next()));
		}

		engine.execute();

		Iterable<T> materialized = Iterables.transform(results, ResultNowFunction.<T>instance());

		return new Chunk<>(cursor, materialized);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
