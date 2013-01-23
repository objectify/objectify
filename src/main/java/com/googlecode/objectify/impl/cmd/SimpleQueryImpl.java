package com.googlecode.objectify.impl.cmd;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.cmd.QueryKeys;
import com.googlecode.objectify.cmd.SimpleQuery;


/**
 * Base for command classes that include methods for defining a query (filter, order, limit, etc).
 * Does not include the methods for executing a query.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract class SimpleQueryImpl<T> implements SimpleQuery<T>
{
	/** */
	protected LoaderImpl loader;

	/**
	 * There is a special case - if loader is null, use 'this' as the LoaderImpl
	 */
	SimpleQueryImpl(LoaderImpl loader) {
		this.loader = loader == null ? (LoaderImpl)this : loader;
	}

	/**
	 * Create an initial query object; for a real FindType this will have a class, otherwise it will be generic.
	 * For the real QueryImpl itself this is a clone() operation.
	 */
	abstract QueryImpl<T> createQuery();

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.QueryCommon#filterKey(java.lang.String, java.lang.Object)
	 */
	@Override
	public QueryImpl<T> filterKey(String condition, Object value)
	{
		QueryImpl<T> q = createQuery();
		q.addFilter(Entity.KEY_RESERVED_PROPERTY + " " + condition, value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#filterKey(java.lang.Object)
	 */
	@Override
	public QueryImpl<T> filterKey(Object value)
	{
		return filterKey("=", value);
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
	 * @see com.googlecode.objectify.cmd.SimpleQuery#distinct(boolean)
	 */
	@Override
	public QueryImpl<T> distinct(boolean value)
	{
		QueryImpl<T> q = createQuery();
		q.setDistinct(value);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#hybrid(boolean)
	 */
	@Override
	public QueryImpl<T> hybrid(boolean force)
	{
		QueryImpl<T> q = createQuery();
		q.setHybrid(force);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#chunkAll()
	 */
	@Override
	public QueryImpl<T> chunkAll()
	{
		return chunk(Integer.MAX_VALUE);
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
