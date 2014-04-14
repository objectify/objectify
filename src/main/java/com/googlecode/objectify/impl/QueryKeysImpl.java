package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.QueryKeys;
import com.googlecode.objectify.util.IteratorFirstResult;
import com.googlecode.objectify.util.MakeListResult;
import com.googlecode.objectify.util.ResultProxy;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation of QueryKeys.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryKeysImpl<T> implements QueryKeys<T>
{
	QueryImpl<T> impl;

	/** */
	QueryKeysImpl(QueryImpl<T> query) {
		assert query.actual.isKeysOnly();
		this.impl = query;
	}

	@Override
	public LoadResult<Key<T>> first() {
		// We are already keysonly
		Iterator<Key<T>> it = impl.limit(1).keysIterable().iterator();
		Result<Key<T>> result = new IteratorFirstResult<>(it);

		return new LoadResult<>(null, result);
	}

	@Override
	public QueryResultIterable<Key<T>> iterable() {
		return impl.keysIterable();
	}

	@Override
	public List<Key<T>> list() {
		return ResultProxy.create(List.class, new MakeListResult<>(impl.chunk(Integer.MAX_VALUE).keysIterable()));
	}

	@Override
	public QueryResultIterator<Key<T>> iterator() {
		return iterable().iterator();
	}
}
