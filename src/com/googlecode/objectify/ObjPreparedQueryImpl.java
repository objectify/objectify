package com.googlecode.objectify;

import java.util.List;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;

/**
 * Implementation of the ObjPreparedQuery interface.
 */
public class ObjPreparedQueryImpl<T> implements ObjPreparedQuery<T>
{
	/** The backing result set */
	PreparedQuery pq;
	
	/** Wrap the prepared query */
	public ObjPreparedQueryImpl(PreparedQuery pq)
	{
		this.pq = pq;
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asIterable()
	 */
	@Override
	public Iterable<T> asIterable()
	{
		//TODO
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asIterable(com.google.appengine.api.datastore.FetchOptions)
	 */
	@Override
	public Iterable<T> asIterable(FetchOptions fetchOptions)
	{
		//TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asList(com.google.appengine.api.datastore.FetchOptions)
	 */
	public List<T> asList(FetchOptions fetchOptions)
	{
		//TODO
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asSingleEntity()
	 */
	public T asSingleEntity()
	{
		//TODO
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#countEntities()
	 */
	public int countEntities()
	{
		//TODO
		return 0;
	}
}