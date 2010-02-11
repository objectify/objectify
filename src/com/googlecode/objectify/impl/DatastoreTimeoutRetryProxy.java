package com.googlecode.objectify.impl;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;

import com.google.appengine.api.datastore.DatastoreTimeoutException;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;

/**
 * A dynamic proxy that catches DatastoreTimeoutException and retries
 * the action up to a specified number of times.  Works with objects
 * of type Objectify, Query<?>, QueryResultIterable<?>, QueryResultIterator<?>,
 * Iterable<?>, and Iterator<?>.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DatastoreTimeoutRetryProxy implements InvocationHandler
{
	/** The actual object wrapped */
	Object wrapped;
	
	/** The number of times we should retry.  1 means two possible tries. */
	int retries;
	
	/** Only works on Objectify, Query<?>, QueryResultIterable<?>, QueryResultIterator<?>, Iterable<?>, and Iterator<?> */
	@SuppressWarnings("unchecked")
	public static <T> T wrap(T impl, int retries)
	{
		return (T)Proxy.newProxyInstance(
				Thread.currentThread().getContextClassLoader(),
				new Class<?>[] { Objectify.class, Query.class, QueryResultIterable.class, QueryResultIterator.class, Iterable.class, Iterator.class },
				new DatastoreTimeoutRetryProxy(impl, retries));
	}
	
	/** */
	protected DatastoreTimeoutRetryProxy(Object wrapped, int retries)
	{
		this.wrapped = wrapped;
		this.retries = retries;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
	{
		int tries = 0;
		while (true)
		{
			try
			{
				return method.invoke(this.wrapped, args);
			}
			catch (InvocationTargetException ex)
			{
				if (ex.getCause() instanceof DatastoreTimeoutException)
				{
					if (tries == this.retries)
						throw ex.getCause();
					else
						tries++;
				}
				else
				{
					throw ex.getCause();
				}
			}
		}
	}
}