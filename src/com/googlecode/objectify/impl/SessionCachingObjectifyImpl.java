package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;

/**
 * Extends the ObjectifyImpl to add a session cache.  Note that it only needs
 * to override a few key methods.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionCachingObjectifyImpl extends ObjectifyImpl
{
	/** Value which gets put in the cache for negative results */
	protected static final Object NEGATIVE_RESULT = new Object();

	/** The cache is a simple hashmap */
	protected Map<Key<?>, Object> cache = new HashMap<Key<?>, Object>();
	
	/**
	 * Protected constructor creates a wrapper on the datastore with
	 * the specified txn.
	 * 
	 * @param txn can be null to not use transactions. 
	 */
	public SessionCachingObjectifyImpl(ObjectifyFactory fact, DatastoreService ds, Transaction txn)
	{
		super(fact, ds, txn);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Map<Key<T>, T> get(Iterable<? extends Key<? extends T>> keys)
	{
		List<Key<? extends T>> needFetching = new ArrayList<Key<? extends T>>();
		
		for (Key<? extends T> key: keys)
			if (!this.cache.containsKey(key))
				needFetching.add(key);
		
		Map<Key<T>, T> fetched = super.get(needFetching);
		
		Map<Key<T>, T> result = new LinkedHashMap<Key<T>, T>();
		
		for (Key<? extends T> key: keys)
		{
			T t = (T)this.cache.get(key);
			if (t != null)
			{
				if (t != NEGATIVE_RESULT)
					result.put((Key<T>)key, t);
			}
			else
			{
				t = fetched.get(key);
				if (t != null)
				{
					result.put((Key<T>)key, t);
					this.cache.put(key, t);
				}
				else
				{
					this.cache.put(key, NEGATIVE_RESULT);
				}
			}
		}
		
		return result;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(com.google.appengine.api.datastore.Key)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> T find(Key<? extends T> key)
	{
		T t = (T)this.cache.get(key);

		if (t != null)
		{
			if (t == NEGATIVE_RESULT)
				return null;
			else
				return t;
		}
		else
		{
			t = super.find(key);
			
			if (t == null)
				this.cache.put(key, NEGATIVE_RESULT);
			else
				this.cache.put(key, t);
			
			return t;
		}
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Object)
	 */
	@Override
	public <T> Key<T> put(T obj)
	{
		Key<T> key = super.put(obj);
		this.cache.put(key, obj);
		return key;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> Map<Key<T>, T> put(Iterable<? extends T> objs)
	{
		Map<Key<T>, T> result = super.put(objs);
		this.cache.putAll(result);
		return result;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Object)
	 */
	@Override
	public void delete(Object keyOrEntity)
	{
		super.delete(keyOrEntity);
		
		Key<?> key = this.getFactory().getKey(keyOrEntity);
		this.cache.put(key, NEGATIVE_RESULT);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<?> keysOrEntities)
	{
		super.delete(keysOrEntities);
		
		for (Object obj: keysOrEntities)
			this.cache.put(this.getFactory().getKey(obj), NEGATIVE_RESULT);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query()
	 */
	@Override
	public <T> Query<T> query()
	{
		return new SessionCachingQueryImpl<T>(this.factory, this, this.cache);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query(java.lang.Class)
	 */
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		return new SessionCachingQueryImpl<T>(this.factory, this, this.cache, clazz);
	}
}