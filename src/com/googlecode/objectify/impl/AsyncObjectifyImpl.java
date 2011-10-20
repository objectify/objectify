package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.AsyncObjectify;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.SimpleFutureWrapper;

/**
 * Implementation of the Objectify interface.  Note we *always* use the AsyncDatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AsyncObjectifyImpl implements AsyncObjectify
{
	/** The factory that produced us */
	protected ObjectifyFactory factory;
	
	/** The google object that does the actual heavy lifting */
	protected AsyncDatastoreService ads;
	
	/** The transaction to use.  If null, do not use transactions. */
	protected Transaction txn;
	
	/** The synchronous version of this API; gets initialized by ObjectifyImpl's constructor */
	protected Objectify sync;
	
	/**
	 * Protected constructor creates a wrapper on the datastore with
	 * the specified txn.
	 * 
	 * @param txn can be null to not use transactions. 
	 */
	public AsyncObjectifyImpl(ObjectifyFactory fact, AsyncDatastoreService ds, Transaction txn)
	{
		this.factory = fact;
		this.ads = ds;
		this.txn = txn;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(java.lang.Iterable)
	 */
	@Override
	public <T> Result<Map<Key<T>, T>> get(Iterable<? extends Key<? extends T>> keys)
	{
		// First we need to turn the keys into raw keys
		final List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<com.google.appengine.api.datastore.Key>();
		for (Key<? extends T> obKey: keys)
			rawKeys.add(obKey.getRaw());
			
		Future<Map<com.google.appengine.api.datastore.Key, Entity>> futureEntities = this.ads.get(this.txn, rawKeys);
		Future<Map<Key<T>, T>> wrapped = new SimpleFutureWrapper<Map<com.google.appengine.api.datastore.Key, Entity>, Map<Key<T>, T>>(futureEntities) {
			@Override
			protected Map<Key<T>, T> wrap(Map<com.google.appengine.api.datastore.Key, Entity> orig) throws Exception
			{
				Map<Key<T>, T> result = new LinkedHashMap<Key<T>, T>(orig.size() * 2);

				// We preserve the order of the original keys
				for (com.google.appengine.api.datastore.Key rawKey: rawKeys)
				{
					Entity entity = orig.get(rawKey);
					if (entity != null)
					{
						EntityMetadata<T> metadata = factory.getMetadata(rawKey);
						result.put(new Key<T>(rawKey), (T)metadata.toObject(entity, sync()));
					}
				}
				
				return result;
			}
		};
		
		return new ResultAdapter<Map<Key<T>, T>>(wrapped);
	}


	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#get(com.googlecode.objectify.Key<? extends T>[])
	 */
	@Override
	public <T> Result<Map<Key<T>, T>> get(Key<? extends T>... keys)
	{
		return this.get(Arrays.asList(keys));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#get(com.googlecode.objectify.Key)
	 */
	@Override
	public <T> Result<T> get(final Key<? extends T> key)
	{
		// The actual implementation is find().
		Result<T> found = this.find(key);
		
		Future<T> wrapped = new SimpleFutureWrapper<T, T>(found.getFuture()) {
			@Override
			protected T wrap(T t) throws Exception
			{
				if (t != null)
					return t;
				else
					throw new NotFoundException(key);
			}
		};
		
		return new ResultAdapter<T>(wrapped);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#get(java.lang.Class, long)
	 */
	@Override
	public <T> Result<T> get(Class<? extends T> clazz, long id)
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (Result<T>)this.get(new Key<T>(clazz, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#get(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> Result<T> get(Class<? extends T> clazz, String name)
	{
		// The cast gets rid of "no unique maximal instance exists" compiler error
		return (Result<T>)this.get(new Key<T>(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#get(java.lang.Class, java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <S, T> Result<Map<S, T>> get(Class<? extends T> clazz, Iterable<S> ids)
	{
		List<Key<? extends T>> keys = new ArrayList<Key<? extends T>>();
		
		for (Object id: ids)
		{
			if (id instanceof Long)
				keys.add(new Key<T>(clazz, (Long)id));
			else if (id instanceof String)
				keys.add(new Key<T>(clazz, (String)id));
			else
				throw new IllegalArgumentException("Only Long or String is allowed, not " + id.getClass().getName() + " (" + id + ")");
		}
		
		Result<Map<Key<T>, T>> fetched = this.get(keys);
		
		Future<Map<S, T>> wrapped = new SimpleFutureWrapper<Map<Key<T>, T>, Map<S, T>>(fetched.getFuture()) {
			@Override
			protected Map<S, T> wrap(Map<Key<T>, T> base) throws Exception
			{
				Map<S, T> result = new LinkedHashMap<S, T>(base.size() * 2);
				
				for (Map.Entry<Key<T>, T> entry: base.entrySet())
				{
					Object mapKey = entry.getKey().getName() != null ? entry.getKey().getName() : entry.getKey().getId();
					result.put((S)mapKey, entry.getValue());
				}
				
				return result;
			}
		};
		
		return new ResultAdapter<Map<S, T>>(wrapped);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#get(java.lang.Class, S[])
	 */
	@Override
	public <S, T> Result<Map<S, T>> get(Class<? extends T> clazz, S... idsOrNames)
	{
		return this.get(clazz, Arrays.asList(idsOrNames));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#find(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> Result<T> find(final Key<? extends T> key)
	{
		Result<Map<Key<T>, T>> multi = this.get(Collections.singleton(key));
		
		Future<T> wrapped = new SimpleFutureWrapper<Map<Key<T>, T>, T>(multi.getFuture()) {
			@Override
			protected T wrap(Map<Key<T>, T> base) throws Exception
			{
				return base.get(key);
			}
		};

		return new ResultAdapter<T>(wrapped);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#find(java.lang.Class, long)
	 */
	@Override
	public <T> Result<T> find(Class<? extends T> clazz, long id)
	{
		return this.find(new Key<T>(clazz, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#find(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> Result<T> find(Class<? extends T> clazz, String name)
	{
		return this.find(new Key<T>(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.AsyncObjectify#put(java.lang.Object)
	 */
	@Override
	public <T> Result<Key<T>> put(final T obj)
	{
		// let's just translate this to a put(iterable) call
		Result<Map<Key<T>, T>> result = this.put(Collections.singleton(obj));
		
		Future<Key<T>> future = new SimpleFutureWrapper<Map<Key<T>, T>, Key<T>>(result.getFuture()) {
			@Override
			protected Key<T> wrap(Map<Key<T>, T> putted) throws Exception
			{
				return putted.keySet().iterator().next();
			}
		};
		
		return new ResultAdapter<Key<T>>(future);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.AsyncObjectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> Result<Map<Key<T>, T>> put(final Iterable<? extends T> objs)
	{
		List<Entity> entityList = new ArrayList<Entity>();
		for (T obj: objs)
		{
			EntityMetadata<T> metadata = this.factory.getMetadataForEntity(obj);
			entityList.add(metadata.toEntity(obj, this.sync()));
		}

		Future<List<com.google.appengine.api.datastore.Key>> raw = this.ads.put(this.txn, entityList);
		Future<Map<Key<T>, T>> wrapped = new SimpleFutureWrapper<List<com.google.appengine.api.datastore.Key>, Map<Key<T>, T>>(raw) {
			@Override
			protected Map<Key<T>, T> wrap(List<com.google.appengine.api.datastore.Key> rawKeys) throws Exception
			{
				Map<Key<T>, T> result = new LinkedHashMap<Key<T>, T>(rawKeys.size() * 2);
				
				// Patch up any generated keys in the original objects while building new key list
				Iterator<com.google.appengine.api.datastore.Key> keysIt = rawKeys.iterator();
				for (T obj: objs)
				{
					com.google.appengine.api.datastore.Key k = keysIt.next();
					EntityMetadata<T> metadata = factory.getMetadataForEntity(obj);
					metadata.setKey(obj, k);
					
					result.put(new Key<T>(k), obj);
				}
				
				return result;
			}
		};

		return new ResultAdapter<Map<Key<T>, T>>(wrapped);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#put(T[])
	 */
	@Override
	public <T> Result<Map<Key<T>, T>> put(T... objs)
	{
		return this.put(Arrays.asList(objs));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Object[])
	 */
	@Override
	public Result<Void> delete(Object... keysOrEntities)
	{
		return this.delete(Arrays.asList(keysOrEntities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Class, long)
	 */
	@Override
	public <T> Result<Void> delete(Class<T> clazz, long id)
	{
		return this.delete(new Key<T>(clazz, id));
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(Class, String)
	 */
	@Override
	public <T> Result<Void> delete(Class<T> clazz, String name)
	{
		return this.delete(new Key<T>(clazz, name));
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public Result<Void> delete(Iterable<?> keysOrEntities)
	{
		// We have to be careful here, objs could contain raw Keys or Keys or entity objects or both!
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
		
		for (Object obj: keysOrEntities)
			keys.add(this.factory.getRawKey(obj));
		
		return new ResultAdapter<Void>(this.ads.delete(this.txn, keys));
	}

	/**
	 * Not currently part of the AsyncObjectify api, but the actual logic lives here.
	 */
	public <T> Query<T> query()
	{
		return new QueryImpl<T>(this.factory, this.sync());
	}
	
	/**
	 * Not currently part of the AsyncObjectify api, but the actual logic lives here.
	 */
	public <T> Query<T> query(Class<T> clazz)
	{
		return new QueryImpl<T>(this.factory, this.sync(), clazz);
	}
	
	/**
	 * Not currently part of the AsyncObjectify api, but the actual logic lives here.
	 */
	public Transaction getTxn()
	{
		return this.txn;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.AsyncObjectify#getAsyncDatastore()
	 */
	@Override
	public AsyncDatastoreService getAsyncDatastore()
	{
		return this.ads;
	}

	/**
	 * Not currently part of the AsyncObjectify api, but the actual logic lives here.
	 */
	public ObjectifyFactory getFactory()
	{
		return this.factory;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.AsyncObjectify#sync()
	 */
	@Override
	public Objectify sync()
	{
		return this.sync;
	}
}