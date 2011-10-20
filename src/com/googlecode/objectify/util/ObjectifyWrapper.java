/*
 * $Id$
 */

package com.googlecode.objectify.util;

import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.AsyncObjectify;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Query;


/**
 * <p>Simple wrapper/decorator for an Objectify interface.</p>
 * 
 * @author Jeff Schnitzer
 */
public class ObjectifyWrapper implements Objectify
{
	/** */
	private Objectify base;
	
	/** Wraps the  */
	public ObjectifyWrapper(Objectify ofy)
	{
		this.base = ofy;
	}

	@Override
	public <T> Map<Key<T>, T> get(Iterable<? extends Key<? extends T>> keys)
	{
		return this.base.get(keys);
	}
	
	@Override
	public <T> Map<Key<T>, T> get(Key<? extends T>... keys)
	{
		return this.base.get(keys);
	}

	@Override
	public <T> T get(Key<? extends T> key) throws NotFoundException
	{
		return this.base.get(key);
	}
	
	@Override
	public <T> T get(Class<? extends T> clazz, long id) throws NotFoundException
	{
		return this.base.get(clazz, id);
	}
	
	@Override
	public <T> T get(Class<? extends T> clazz, String name) throws NotFoundException
	{
		return this.base.get(clazz, name);
	}
	
	@Override
	public <S, T> Map<S, T> get(Class<? extends T> clazz, Iterable<S> idsOrNames)
	{
		return this.base.get(clazz, idsOrNames);
	}
	
	@Override
	public <S, T> Map<S, T> get(Class<? extends T> clazz, S... idsOrNames)
	{
		return this.base.get(clazz, idsOrNames);
	}

	@Override
	public <T> T find(Key<? extends T> key)
	{
		return this.base.find(key);
	}
	
	@Override
	public <T> T find(Class<? extends T> clazz, long id)
	{
		return this.base.find(clazz, id);
	}
	
	@Override
	public <T> T find(Class<? extends T> clazz, String name)
	{
		return this.base.find(clazz, name);
	}

	@Override
	public <T> Key<T> put(T obj)
	{
		return this.base.put(obj);
	}
	
	@Override
	public <T> Map<Key<T>, T> put(Iterable<? extends T> objs)
	{
		return this.base.put(objs);
	}
	
	@Override
	public <T> Map<Key<T>, T> put(T... objs)
	{
		return this.base.put(objs);
	}

	@Override
	public void delete(Object... keysOrEntities)
	{
		this.base.delete(keysOrEntities);
	}

	@Override
	public void delete(Iterable<?> keysOrEntities)
	{
		this.base.delete(keysOrEntities);
	}

	@Override
	public <T> void delete(Class<T> clazz, long id)
	{
		this.base.delete(clazz, id);
	}
	
	@Override
	public <T> void delete(Class<T> clazz, String name)
	{
		this.base.delete(clazz, name);
	}

	@Override
	public <T> Query<T> query()
	{
		return this.base.query();
	}
	
	@Override
	public <T> Query<T> query(Class<T> clazz)
	{
		return this.base.query(clazz);
	}
	
	@Override
	public Transaction getTxn()
	{
		return this.base.getTxn();
	}

	@Override
	public ObjectifyFactory getFactory()
	{
		return this.base.getFactory();
	}

	@Override
	public AsyncObjectify async()
	{
		return this.base.async();
	}

	@Override
	public DatastoreService getDatastore()
	{
		return this.base.getDatastore();
	}
}