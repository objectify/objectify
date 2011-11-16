package com.googlecode.objectify.impl.cmd;

import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.LoadIds;
import com.googlecode.objectify.cmd.LoadType;


/**
 * Implementation of the LoadType interface.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadTypeImpl<T> extends Queryable<T> implements LoadType<T>
{
	/** */
	Class<T> type;
	
	/**
	 */
	LoadTypeImpl(ObjectifyImpl ofy, Set<String> fetchGroups, Class<T> type) {
		super(ofy, fetchGroups);
		this.type = type;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.FindTypeBase#createQuery()
	 */
	@Override
	QueryImpl<T> createQuery() {
		return new QueryImpl<T>(ofy, fetchGroups, type);
	}

	@Override
	public Ref<T> id(long id)
	{
		return null;
	}

	@Override
	public Ref<T> id(String id)
	{
		return null;
	}

	@Override
	public Map<Long, T> ids(long... ids)
	{
		return null;
	}

	@Override
	public Map<String, T> ids(String... ids)
	{
		return null;
	}

	@Override
	public <S> Map<S, T> ids(Iterable<?> ids)
	{
		return null;
	}

	@Override
	public LoadIds<T> parent(Object keyOrEntity)
	{
		return null;
	}

}
