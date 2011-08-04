package com.googlecode.objectify.util;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Query;

/**
 * Simple wrapper/decorator for a Query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryWrapper<T> implements Query<T>
{
	/** */
	Query<T> base;
	
	/** */
	public QueryWrapper(Query<T> base) 
	{
		this.base = base;
	}
	
	@Override
	public Query<T> filter(String condition, Object value)
	{
		return this.base.filter(condition, value);
	}
	
	@Override
	public Query<T> order(String condition)
	{
		return this.base.order(condition);
	}
	
	@Override
	public Query<T> ancestor(Object keyOrEntity)
	{
		return this.base.ancestor(keyOrEntity);
	}
	
	@Override
	public Query<T> limit(int value)
	{
		return this.base.limit(value);
	}
	
	@Override
	public Query<T> offset(int value)
	{
		return this.base.offset(value);
	}

	@Override
	public Query<T> startCursor(Cursor value)
	{
		return this.base.startCursor(value);
	}

	@Override
	public Query<T> endCursor(Cursor value)
	{
		return this.base.endCursor(value);
	}

	@Override
	public String toString()
	{
		return this.base.toString();
	}

	@Override
	public QueryResultIterator<T> iterator()
	{
		return this.base.iterator();
	}

	@Override
	public T get()
	{
		return this.base.get();
	}

	@Override
	public Key<T> getKey()
	{
		return this.base.getKey();
	}

	@Override
	public int count()
	{
		return this.base.count();
	}

	@Override
	public QueryResultIterable<T> fetch()
	{
		return this.base.fetch();
	}

	@Override
	public QueryResultIterable<Key<T>> fetchKeys()
	{
		return this.base.fetchKeys();
	}

	@Override
	public <V> Set<Key<V>> fetchParentKeys()
	{
		return this.base.fetchParentKeys();
	}

	@Override
	public <V> Map<Key<V>, V> fetchParents()
	{
		return this.base.fetchParents();
	}

	@Override
	public List<T> list()
	{
		return this.base.list();
	}

	@Override
	public List<Key<T>> listKeys()
	{
		return this.base.listKeys();
	}
	
	@Override
	public Query<T> clone()
	{
		return new QueryWrapper<T>(this.base.clone());
	}

	@Override
	public Query<T> chunkSize(int value)
	{
		return this.base.chunkSize(value);
	}

	@Override
	public Query<T> prefetchSize(int value)
	{
		return this.base.prefetchSize(value);
	}
}
