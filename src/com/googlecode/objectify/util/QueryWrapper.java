package com.googlecode.objectify.util;

import java.util.List;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;

/**
 * Simple wrapper/decorator for a Query.  Use it like this:
 * {@code class MyQuery<T> extends QueryWrapper<MyQuery<T>, T>} 
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryWrapper<H extends QueryWrapper<H, T>, T> implements Query<T>, Cloneable
{
	/** */
	Query<T> base;
	
	/** */
	public QueryWrapper(Query<T> base) 
	{
		this.base = base;
	}
	
	@Override
	public H filter(String condition, Object value)
	{
		H next = this.clone();
		next.base = base.filter(condition, value);
		return next;
	}
	
	@Override
	public H order(String condition)
	{
		H next = this.clone();
		next.base = base.order(condition);
		return next;
	}
	
	@Override
	public H ancestor(Object keyOrEntity)
	{
		H next = this.clone();
		next.base = base.ancestor(keyOrEntity);
		return next;
	}
	
	@Override
	public H limit(int value)
	{
		H next = this.clone();
		next.base = base.limit(value);
		return next;
	}
	
	@Override
	public H offset(int value)
	{
		H next = this.clone();
		next.base = base.offset(value);
		return next;
	}

	@Override
	public H startAt(Cursor value)
	{
		H next = this.clone();
		next.base = base.startAt(value);
		return next;
	}

	@Override
	public H endAt(Cursor value)
	{
		H next = this.clone();
		next.base = base.endAt(value);
		return next;
	}

	@Override
	public String toString()
	{
		return base.toString();
	}

	@Override
	public Ref<T> first()
	{
		return base.first();
	}

	@Override
	public int count()
	{
		return base.count();
	}

	@Override
	public QueryResultIterable<T> iterable()
	{
		return base.iterable();
	}

	@Override
	public List<T> list()
	{
		return base.list();
	}

	@Override
	public H chunk(int value)
	{
		H next = this.clone();
		next.base = base.chunk(value);
		return next;
	}

	@Override
	public QueryResultIterator<T> iterator()
	{
		return base.iterator();
	}
	
	@Override
	public QueryKeys<T> keys()
	{
		return base.keys();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	protected H clone()
	{
		try {
			return (H)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // impossible
		}
	}
}
