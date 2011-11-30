package com.googlecode.objectify.impl.cmd;

import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;


/**
 * Base for command classes that include methods for defining a query (filter, order, limit, etc).
 * Does not include the methods for executing a query.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract class QueryDefinition<T> implements Query<T>
{
	/** */
	ObjectifyImpl ofy;
	
	/** */
	Set<String> fetchGroups;
	
	/**
	 * Takes ownership of the fetch groups set.
	 */
	QueryDefinition(ObjectifyImpl ofy, Set<String> fetchGroups) {
		this.ofy = ofy;
		this.fetchGroups = fetchGroups;
	}
	
	/**
	 * Create an initial query object; for a real FindType this will have a class, otherwise it will be generic.
	 * For the real QueryImpl itself this is a clone() operation.
	 */
	abstract QueryImpl<T> createQuery();

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#filter(java.lang.String, java.lang.Object)
	 */
	@Override
	public QueryImpl<T> filter(String condition, Object value)
	{
		QueryImpl<T> q = createQuery();
		q.addFilter(condition, value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#order(java.lang.String)
	 */
	@Override
	public QueryImpl<T> order(String condition)
	{
		QueryImpl<T> q = createQuery();
		q.addOrder(condition);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#ancestor(java.lang.Object)
	 */
	@Override
	public QueryImpl<T> ancestor(Object keyOrEntity)
	{
		QueryImpl<T> q = createQuery();
		q.setAncestor(keyOrEntity);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#limit(int)
	 */
	@Override
	public QueryImpl<T> limit(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setLimit(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#offset(int)
	 */
	@Override
	public QueryImpl<T> offset(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setOffset(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#startCursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public QueryImpl<T> startAt(Cursor value)
	{
		QueryImpl<T> q = createQuery();
		q.setStartCursor(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#endCursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public QueryImpl<T> endAt(Cursor value)
	{
		QueryImpl<T> q = createQuery();
		q.setEndCursor(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#chunk(int)
	 */
	@Override
	public QueryImpl<T> chunk(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setChunk(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#keys()
	 */
	@Override
	public QueryKeys<T> keys()
	{
		QueryImpl<T> q = createQuery();
		q.setKeysOnly();
		return new QueryKeysImpl<T>(q);
	}
}
