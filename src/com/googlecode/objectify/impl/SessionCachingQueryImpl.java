package com.googlecode.objectify.impl;

import java.util.Map;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.util.QueryResultIteratorWrapper;

/**
 * Extends the QueryImpl to add a session cache.  Note that it only needs
 * to override the iterator() method.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionCachingQueryImpl<T> extends QueryImpl<T>
{
	/** The cache is a simple hashmap, obtained from the SessionCachingObjectifyImpl */
	final protected Map<Key<?>, Object> cache;
	
	/** */
	public SessionCachingQueryImpl(ObjectifyFactory fact, Objectify ofy, Map<Key<?>, Object> cache)
	{
		super(fact, ofy);
		this.cache = cache;
	}
	
	/** */
	public SessionCachingQueryImpl(ObjectifyFactory fact, Objectify ofy, Map<Key<?>, Object> cache, Class<T> clazz)
	{
		super(fact, ofy, clazz);
		this.cache = cache;
	}
	
	@Override
	public QueryResultIterator<T> iterator()
	{
		return new SessionCachingQueryResultIterator(super.iterator());
	}

	/**
	 * Simple iterator passes through and merges with the cache.
	 */
	protected class SessionCachingQueryResultIterator extends QueryResultIteratorWrapper<T>
	{
		public SessionCachingQueryResultIterator(QueryResultIterator<T> base)
		{
			super(base);
		}

		@Override
		@SuppressWarnings("unchecked")
		public T next()
		{
			T t = super.next();
			Key<T> key = factory.getKey(t);
			T cached = (T)cache.get(key);
			
			if (cached == null || cached == SessionCachingAsyncObjectifyImpl.NEGATIVE_RESULT)
			{
				cache.put(key, t);
				cached = t;
			}
			
			return cached;
		}
	}
}

