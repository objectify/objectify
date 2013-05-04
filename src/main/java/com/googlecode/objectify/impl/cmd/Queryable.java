package com.googlecode.objectify.impl.cmd;

import java.util.List;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.LoadResult;


/**
 * Common behavior for command implementations that delegate query execution to a real query implementation.
 * Used by LoadCmdImpl and LoadTypeImpl.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract class Queryable<T> extends SimpleQueryImpl<T>
{
	/**
	 */
	Queryable(LoaderImpl loader) {
		super(loader);
	}

	@Override
	public LoadResult<T> first()
	{
		QueryImpl<T> q = createQuery();
		return q.first();
	}

	@Override
	public QueryResultIterator<T> iterator()
	{
		QueryImpl<T> q = createQuery();
		return q.iterator();
	}

	@Override
	public QueryResultIterable<T> iterable()
	{
		QueryImpl<T> q = createQuery();
		return q.iterable();
	}

	@Override
	public int count()
	{
		QueryImpl<T> q = createQuery();
		return q.count();
	}

	@Override
	public List<T> list()
	{
		QueryImpl<T> q = createQuery();
		return q.list();
	}

}
