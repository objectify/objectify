package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;

/**
 * QueryResultIterable wrapper that creates iterators that translate from one type to another
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TranslatingQueryResultIterable<F, T> implements QueryResultIterable<T>
{
	QueryResultIterable<F> base;
	
	/** */
	public TranslatingQueryResultIterable(QueryResultIterable<F> base) {
		this.base = base;
	}
	
	/** You implement this - convert from one object to the other */
	abstract protected T translate(F from); 

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.QueryResultIterable#iterator()
	 */
	@Override
	public QueryResultIterator<T> iterator() {
		return new TranslatingQueryResultIterator<F, T>(base.iterator()) {
			@Override
			protected T translate(F from) {
				return TranslatingQueryResultIterable.this.translate(from);
			}
		};
	}
}
