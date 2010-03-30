/*
 * $Id$
 */

package com.googlecode.objectify.helper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;


/**
 * <p>Experimental session cache.  When used to wrap an Objectify instance,
 * all fetches will put key/values in a session cache (hashmap) that will
 * be referenced in future calls to get().  Potentially skips calls to
 * the datastore (or even the memcache).</p>
 * 
 * @author Jeff Schnitzer
 */
public class CachingObjectify extends ObjectifyWrapper
{
	/** Value which gets put in the cache for negative results */
	private static final Object NEGATIVE_RESULT = new Object();

	/** The cache is a simple hashmap */
	private Map<Key<?>, Object> cache = new HashMap<Key<?>, Object>();
	
	/** */
	public CachingObjectify(Objectify ofy)
	{
		super(ofy);
	}

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
	
	@Override
	public <T> T get(Key<? extends T> key) throws NotFoundException
	{
		T t = this.find(key);
		if (t == null)
			throw new NotFoundException(key);
		else
			return t;
	}
	
	@Override
	public <T> T get(Class<? extends T> clazz, long id) throws NotFoundException
	{
		return this.get(new Key<T>(clazz, id));
	}
	
	@Override
	public <T> T get(Class<? extends T> clazz, String name) throws NotFoundException
	{
		return this.get(new Key<T>(clazz, name));	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <S, T> Map<S, T> get(Class<? extends T> clazz, Iterable<S> idsOrNames)
	{
		List<Key<? extends T>> keys = new ArrayList<Key<? extends T>>();
		
		for (Object id: idsOrNames)
		{
			if (id instanceof Long)
				keys.add(new Key<T>(clazz, (Long)id));
			else if (id instanceof String)
				keys.add(new Key<T>(clazz, (String)id));
			else
				throw new IllegalArgumentException("Only Long or String is allowed, not " + id.getClass().getName() + " (" + id + ")");
		}
		
		Map<Key<T>, T> fetched = this.get(keys);
		Map<S, T> result = new LinkedHashMap<S, T>(fetched.size() * 2);
		
		for (Map.Entry<Key<T>, T> entry: fetched.entrySet())
		{
			Object mapKey = entry.getKey().getName() != null ? entry.getKey().getName() : entry.getKey().getId();
			result.put((S)mapKey, entry.getValue());
		}
		
		return result;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public <T> T find(Key<? extends T> key)
	{
		T t = (T)this.cache.get(key);
		if (t != null)
		{
			return (t == NEGATIVE_RESULT) ? null : t;
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
	
	@Override
	public <T> T find(Class<? extends T> clazz, long id)
	{
		return this.find(new Key<T>(clazz, id));
	}
	
	@Override
	public <T> T find(Class<? extends T> clazz, String name)
	{
		return this.find(new Key<T>(clazz, name));
	}

	@Override
	public <T> Key<T> put(T obj)
	{
		Key<T> key = super.put(obj);
		this.cache.put(key, obj);
		return key;
	}
	
	@Override
	public <T> Map<Key<T>, T> put(Iterable<? extends T> objs)
	{
		Map<Key<T>, T> result = super.put(objs);
		this.cache.putAll(result);
		return result;
	}
	
	@Override
	public void delete(Object keyOrEntity)
	{
		super.delete(keyOrEntity);
		
		Key<?> key = this.getFactory().getKey(keyOrEntity);
		this.cache.put(key, NEGATIVE_RESULT);
	}

	@Override
	public void delete(Iterable<?> keysOrEntities)
	{
		super.delete(keysOrEntities);
		
		for (Object obj: keysOrEntities)
			this.cache.put(this.getFactory().getKey(obj), NEGATIVE_RESULT);
	}

	@Override
	public <T> void delete(Class<T> clazz, long id)
	{
		this.delete(new Key<T>(clazz, id));
	}
	
	@Override
	public <T> void delete(Class<T> clazz, String name)
	{
		this.delete(new Key<T>(clazz, name));
	}

	@Override
	public <T> Query<T> query()
	{
		return new CachingQuery<T>(super.<T>query());
	}
	
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		return new CachingQuery<T>(super.<T>query(clazz));
	}
	
	/**
	 * QueryWrapper which adds any obtained objects to our cache.
	 */
	class CachingQuery<T> extends QueryWrapper<T>
	{
		public CachingQuery(Query<T> base)
		{
			super(base);
		}
		
		@Override
		public QueryResultIterable<T> fetch()
		{
			return new QueryResultIterable<T>() {
				@Override
				public QueryResultIterator<T> iterator()
				{
					return CachingQuery.this.iterator();
				}
			};
		}

		@Override
		public <V> Map<Key<V>, V> fetchParents()
		{
			Map<Key<V>, V> result = super.fetchParents();
			cache.putAll(result);
			return result;
		}

		@Override
		public T get()
		{
			T t = super.get();
			cache.put(getFactory().getKey(t), t);
			return t;
		}

		@Override
		public QueryResultIterator<T> iterator()
		{
			return new CachingQueryResultIterator<T>(super.iterator());
		}
	}
	
	/**
	 * Simple iterator passes through and caches what it gets.
	 */
	class CachingQueryResultIterator<T> extends QueryResultIteratorWrapper<T>
	{
		public CachingQueryResultIterator(QueryResultIterator<T> base)
		{
			super(base);
		}

		@Override
		public T next()
		{
			T t = super.next();
			cache.put(getFactory().getKey(t), t);
			return t;
		}
	}
}