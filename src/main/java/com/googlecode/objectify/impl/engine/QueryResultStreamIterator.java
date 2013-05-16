package com.googlecode.objectify.impl.engine;

import java.util.Iterator;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.Iterators;
import com.googlecode.objectify.Key;

/**
 * Folds a series of batches back into a single stream of objects, tracking the base cursor
 * and offset into the batch.
 */
public class QueryResultStreamIterator<T> implements Iterator<T> {

	Iterator<T> master;

	/** The currently active cursor */
	Cursor currentCursor;
	CountingIterator<T> currentCounter;

	public QueryResultStreamIterator(QueryResultIterator<Key<T>> source, int chunkSize, LoadEngine engine) {
		// We should always initialize the cursor just in case there are no results and apply() never gets called.
		currentCursor = source.getCursor();

		QueryResultBatchIterator<T> batches = new QueryResultBatchIterator<T>(source, chunkSize, engine);

		Iterator<Iterator<T>> transformed = Iterators.transform(batches, new Function<QueryResultBatch<T>, Iterator<T>>() {
			  public Iterator<T> apply(QueryResultBatch<T> input) {
				  currentCursor = input.getCursor();
				  currentCounter = new CountingIterator<T>(input.getResult().iterator());
				  return currentCounter;
			  }
		});

		this.master = Iterators.concat(transformed);
	}

	@Override
	public boolean hasNext() {
		return master.hasNext();
	}

	@Override
	public T next() {
		return master.next();
	}

	public Cursor getBaseCursor() {
		return currentCursor;
	}

	public int getOffset() {
		return currentCounter == null ? 0 : currentCounter.getCount();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("baseCursor", getBaseCursor())
				.add("offset", getOffset())
				.toString();
	}
}