package com.googlecode.objectify.util;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.QueryResults;
import com.google.datastore.v1.QueryResultBatch.MoreResultsType;

/**
 * QueryResultIterator wrapper that translates from one type to another
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TranslatingQueryResults<F, T> extends TranslatingIterator<F, T> implements QueryResults<T>
{
	/** */
	public TranslatingQueryResults(final QueryResults<F> base)
	{
		super(base);
	}
	
	@Override
	public Cursor getCursorAfter()
	{
		return ((QueryResults<F>)this.base).getCursorAfter();
	}

	@Override
	public Class<?> getResultClass() {
		// This probably can't be implemented in any reasonable way; POJO classes can vary result to result.
		// For example, a kind-less query can have lots of kinds!
		return Object.class;
	}

	@Override
	public int getSkippedResults() {
		return ((QueryResults<F>)this.base).getSkippedResults();
	}

	@Override
	public MoreResultsType getMoreResults() {
		return ((QueryResults<F>)this.base).getMoreResults();
	}

}
