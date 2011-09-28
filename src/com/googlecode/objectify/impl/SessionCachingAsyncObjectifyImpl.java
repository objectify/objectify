package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cache.TriggerSuccessFuture;
import com.googlecode.objectify.util.NowFuture;
import com.googlecode.objectify.util.SimpleFutureWrapper;

/**
 * Extends the AsyncObjectifyImpl to add a session cache.  Note that it only needs
 * to override a few key methods.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionCachingAsyncObjectifyImpl extends AsyncObjectifyImpl
{
	/** Value which gets put in the cache for negative results */
	protected static final Object NEGATIVE_RESULT = new Object();

	/** The cache is a simple hashmap */
	protected Map<Key<?>, Object> cache = new HashMap<Key<?>, Object>();
	
	/**
	 */
	public SessionCachingAsyncObjectifyImpl(ObjectifyFactory fact, AsyncDatastoreService ds, Transaction txn)
	{
		super(fact, ds, txn);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.AsyncObjectify#get(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> Result<Map<Key<T>, T>> get(final Iterable<? extends Key<? extends T>> keys)
	{
		final List<Key<? extends T>> needFetching = new ArrayList<Key<? extends T>>();
		Map<Key<T>, T> foundInCache = new LinkedHashMap<Key<T>, T>();
		
		for (Key<? extends T> key: keys)
		{
			T obj = (T)this.cache.get(key);
			if (obj == null)
				needFetching.add(key);
			else if (obj != NEGATIVE_RESULT)
				foundInCache.put((Key<T>)key, obj);
		}

		if (needFetching.isEmpty())
		{
			// We can just use the foundInCache as-is
			Future<Map<Key<T>, T>> fut = new NowFuture<Map<Key<T>, T>>(foundInCache);
			return new ResultAdapter<Map<Key<T>, T>>(fut);
		}
		else
		{
			Future<Map<Key<T>, T>> fromDatastore = super.get(keys).getFuture();

			// Needs to add in the cached values, creating a map with the proper order
			Future<Map<Key<T>, T>> wrapped = new SimpleFutureWrapper<Map<Key<T>, T>, Map<Key<T>, T>>(fromDatastore) {
				@Override
				protected Map<Key<T>, T> wrap(Map<Key<T>, T> fetched) throws Exception
				{
					Map<Key<T>, T> result = new LinkedHashMap<Key<T>, T>();
					
					for (Key<? extends T> key: keys)
					{
						T t = (T)cache.get(key);
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
								cache.put(key, t);
							}
							else
							{
								cache.put(key, NEGATIVE_RESULT);
							}
						}
					}
					
					return result;
				}
			};
			
			return new ResultAdapter<Map<Key<T>,T>>(wrapped);
		}
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.AsyncObjectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> Result<Map<Key<T>, T>> put(final Iterable<? extends T> objs)
	{
		// Unfortunately we can't put the data in the session cache right away because
		// the entities might not have populated ids.  They keys/ids only get populated
		// when the data is fetched.
		//for (T t: objs)
		//	this.cache.put(this.factory.getKey(t), t);
		//
		//return super.put(objs);
		
		Result<Map<Key<T>, T>> orig = super.put(objs);
		
		Future<Map<Key<T>, T>> triggered = new TriggerSuccessFuture<Map<Key<T>, T>>(orig.getFuture()) {
			@Override
			protected void success(Map<Key<T>, T> result)
			{
				for (Map.Entry<Key<T>, T> entry: result.entrySet())
					cache.put(entry.getKey(), entry.getValue());
			}
		};

		return new ResultAdapter<Map<Key<T>, T>>(triggered);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public Result<Void> delete(Iterable<?> keysOrEntities)
	{
		for (Object obj: keysOrEntities)
			this.cache.put(this.factory.getKey(obj), NEGATIVE_RESULT);

		return super.delete(keysOrEntities);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query()
	 */
	@Override
	public <T> Query<T> query()
	{
		return new SessionCachingQueryImpl<T>(this.factory, this.sync, this.cache);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query(java.lang.Class)
	 */
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		return new SessionCachingQueryImpl<T>(this.factory, this.sync, this.cache, clazz);
	}
}