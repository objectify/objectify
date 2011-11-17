package com.googlecode.objectify.impl.cmd;

import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * Common behavior for command implementations that delegate query execution to a real query implementation.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract class Queryable<T> extends QueryDefinition<T>
{
	/**
	 * Takes ownership of the fetch groups set.
	 */
	Queryable(ObjectifyImpl ofy, Set<String> fetchGroups) {
		super(ofy, fetchGroups);
	}
	
	@Override
	public Ref<T> first()
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
	public QueryResultIterable<T> entities()
	{
		QueryImpl<T> q = createQuery();
		return q.entities();
	}

	@Override
	public QueryResultIterable<Key<T>> keys()
	{
		QueryImpl<T> q = createQuery();
		return q.keys();
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

	@Override
	public List<Key<T>> listKeys()
	{
		QueryImpl<T> q = createQuery();
		return q.listKeys();
	}
}
