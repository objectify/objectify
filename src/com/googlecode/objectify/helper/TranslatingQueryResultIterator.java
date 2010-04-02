package com.googlecode.objectify.helper;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;

/**
 * QueryResultIterator wrapper that translates from one type to another
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TranslatingQueryResultIterator<F, T> implements QueryResultIterator<T>
{
	/** */
	QueryResultIterator<F> base;
	
	/** */
	public TranslatingQueryResultIterator(QueryResultIterator<F> base) 
	{
		this.base = base;
	}
	
	/**
	 * You implement this - convert from one object to the other
	 */
	abstract protected T translate(F from); 

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
		return this.translate(this.base.next());
	}

	@Override
	public void remove()
	{
		this.base.remove();
	}
}
