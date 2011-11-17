package com.googlecode.objectify.impl.cmd;

import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.googlecode.objectify.cmd.Query;


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
	public Query<T> filter(String condition, Object value)
	{
		QueryImpl<T> q = createQuery();
		q.addFilter(condition, value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#order(java.lang.String)
	 */
	@Override
	public Query<T> order(String condition)
	{
		QueryImpl<T> q = createQuery();
		q.addOrder(condition);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#ancestor(java.lang.Object)
	 */
	@Override
	public Query<T> ancestor(Object keyOrEntity)
	{
		QueryImpl<T> q = createQuery();
		q.setAncestor(keyOrEntity);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#limit(int)
	 */
	@Override
	public Query<T> limit(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setLimit(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#offset(int)
	 */
	@Override
	public Query<T> offset(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setOffset(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#startCursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public Query<T> startAt(Cursor value)
	{
		QueryImpl<T> q = createQuery();
		q.setStartCursor(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#endCursor(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public Query<T> endAt(Cursor value)
	{
		QueryImpl<T> q = createQuery();
		q.setEndCursor(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#chunkSize(int)
	 */
	@Override
	public Query<T> chunkSize(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setChunkSize(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#prefetchSize(int)
	 */
	@Override
	public Query<T> prefetchSize(int value)
	{
		QueryImpl<T> q = createQuery();
		q.setPrefetchSize(value);
		return q;
	}
	
	@Override
	public Query<T> keysOnly()
	{
		QueryImpl<T> q = createQuery();
		q.setKeysOnly();
		return q;
	}
}
