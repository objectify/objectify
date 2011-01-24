package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;

/**
 * QueryResultIterator wrapper that translates from one type to another
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TranslatingQueryResultIterator<F, T> extends TranslatingIterator<F, T> implements QueryResultIterator<T>
{
	/** */
	public TranslatingQueryResultIterator(QueryResultIterator<F> base) 
	{
		super(base);
	}
	
	@Override
	public Cursor getCursor()
	{
		return ((QueryResultIterator<F>)this.base).getCursor();
	}
}
