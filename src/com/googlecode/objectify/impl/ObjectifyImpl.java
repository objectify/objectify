package com.googlecode.objectify.impl;

import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.AsyncObjectify;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.Query;

/**
 * Implementation of the Objectify interface.  This actually just calls through to
 * the AsyncObjectify implementation and performs an immediate get().
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify
{
	/** Keep our original opts around so we can generate a DatastoreService when requested */
	protected ObjectifyOpts opts;
	
	/** This must be passed in */
	protected AsyncObjectifyImpl async;
	
	/**
	 * Note that this sets the pointer back to the synchronous version in AsyncObjectifyImpl.
	 */
	public ObjectifyImpl(ObjectifyOpts opts, AsyncObjectifyImpl async)
	{
		this.opts = opts;
		this.async = async;
		this.async.sync = this;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(java.lang.Iterable)
	 */
	@Override
	public <T> Map<Key<T>, T> get(Iterable<? extends Key<? extends T>> keys)
	{
		return this.async.get(keys).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(com.googlecode.objectify.Key<? extends T>[])
	 */
	@Override
	public <T> Map<Key<T>, T> get(Key<? extends T>... keys)
	{
		return this.async.get(keys).get();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T get(Key<? extends T> key) throws NotFoundException
	{
		return this.async.get(key).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, long)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, long id) throws NotFoundException
	{
		return this.async.get(clazz, id).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T get(Class<? extends T> clazz, String name) throws NotFoundException
	{
		return this.async.get(clazz, name).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, java.lang.Iterable)
	 */
	@Override
	public <S, T> Map<S, T> get(Class<? extends T> clazz, Iterable<S> ids)
	{
		return this.async.get(clazz, ids).get();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#get(java.lang.Class, S[])
	 */
	@Override
	public <S, T> Map<S, T> get(Class<? extends T> clazz, S... idsOrNames)
	{
		return this.async.get(clazz, idsOrNames).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T find(Key<? extends T> key)
	{
		return this.async.find(key).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(java.lang.Class, long)
	 */
	@Override
	public <T> T find(Class<? extends T> clazz, long id)
	{
		return this.async.find(clazz, id).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find(java.lang.Class, java.lang.String)
	 */
	@Override
	public <T> T find(Class<? extends T> clazz, String name)
	{
		return this.async.find(clazz, name).get();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Object)
	 */
	@Override
	public <T> Key<T> put(T obj)
	{
		return this.async.put(obj).get();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public <T> Map<Key<T>, T> put(Iterable<? extends T> objs)
	{
		return this.async.put(objs).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put(T[])
	 */
	@Override
	public <T> Map<Key<T>, T> put(T... objs)
	{
		return this.async.put(objs).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Object[])
	 */
	@Override
	public void delete(Object... keysOrEntities)
	{
		this.async.delete(keysOrEntities).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(java.lang.Class, long)
	 */
	@Override
	public <T> void delete(Class<T> clazz, long id)
	{
		this.async.delete(clazz, id).get();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete(Class, String)
	 */
	@Override
	public <T> void delete(Class<T> clazz, String name)
	{
		this.async.delete(clazz, name).get();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<?> keysOrEntities)
	{
		this.async.delete(keysOrEntities).get();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query()
	 */
	@Override
	public <T> Query<T> query()
	{
		return this.async.query();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#query(java.lang.Class)
	 */
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		return this.async.query(clazz);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	@Override
	public Transaction getTxn()
	{
		return this.async.getTxn();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	@Override
	public ObjectifyFactory getFactory()
	{
		return this.async.getFactory();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#async()
	 */
	@Override
	public AsyncObjectify async()
	{
		return this.async;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getDatastore()
	 */
	@Override
	public DatastoreService getDatastore()
	{
		return this.getFactory().getDatastoreService(this.opts);
	}
}