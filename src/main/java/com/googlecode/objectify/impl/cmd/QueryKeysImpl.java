package com.googlecode.objectify.impl.cmd;

import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.QueryExecute;
import com.googlecode.objectify.cmd.QueryKeys;
import com.googlecode.objectify.impl.ref.QueryRef;
import com.googlecode.objectify.util.MakeListResult;
import com.googlecode.objectify.util.IteratorFirstResult;
import com.googlecode.objectify.util.ResultProxy;

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
	public Ref<T> first() {
		// We are already keysonly
		Iterator<Key<T>> it = impl.limit(1).keysIterable().iterator();

		return new QueryRef<T>(new IteratorFirstResult<Key<T>>(it));
	}

	@Override
	public QueryResultIterable<Key<T>> iterable() {
		return impl.keysIterable();
	}

	@Override
	public List<Key<T>> list() {
		return ResultProxy.create(List.class, new MakeListResult<Key<T>>(impl.chunk(Integer.MAX_VALUE).keysIterable()));
	}

	@Override
	public QueryResultIterator<Key<T>> iterator() {
		return iterable().iterator();
	}

	@Override
	public QueryExecute<T> asEntities() {
		// Since we are already keys-only, the original query should spit out partials
		return impl;
	}

}
