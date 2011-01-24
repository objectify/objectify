package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;

/**
 * Simple wrapper/decorator for a QueryResultIterator.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryResultIteratorWrapper<T> implements QueryResultIterator<T>
{
	/** */
	QueryResultIterator<T> base;
	
	/** */
	public QueryResultIteratorWrapper(QueryResultIterator<T> base) 
	{
		this.base = base;
	}

	@Override
	public Cursor getCursor()
	{
		return this.base.getCursor();
	}

	@Override
	public boolean hasNext()
	{
		return this.base.hasNext();
	}

	@Override
	public T next()
	{
		return this.base.next();
	}

	@Override
	public void remove()
	{
		this.base.remove();
	}
}
