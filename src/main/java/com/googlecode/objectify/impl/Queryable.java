package com.googlecode.objectify.impl;

import com.google.cloud.datastore.QueryResults;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.QueryResultIterable;

import java.util.List;


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
	Queryable(final LoaderImpl loader) {
		super(loader);
	}

	@Override
	public LoadResult<T> first() {
		final QueryImpl<T> q = createQuery();
		return q.first();
	}

	@Override
	public QueryResults<T> iterator() {
		final QueryImpl<T> q = createQuery();
		return q.iterator();
	}

	@Override
	public QueryResultIterable<T> iterable() {
		final QueryImpl<T> q = createQuery();
		return q.iterable();
	}

	@Override
	public int count() {
		final QueryImpl<T> q = createQuery();
		return q.count();
	}

	@Override
	public List<T> list() {
		final QueryImpl<T> q = createQuery();
		return q.list();
	}

}
