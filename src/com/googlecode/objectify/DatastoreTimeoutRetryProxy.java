package com.googlecode.objectify;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Iterator;

import com.google.appengine.api.datastore.DatastoreTimeoutException;

/**
 * A dynamic proxy that catches DatastoreTimeoutException and retries
 * the action up to a specified number of times.  Works with objects
 * of type Objectify, ObjPreparedQuery, and Iterator.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DatastoreTimeoutRetryProxy implements InvocationHandler
{
	/** The actual implementation of Objectify */
	Object wrapped;
	
	/** The number of times we should retry.  1 means two possible tries. */
	int retries;
	
	/** Only works on Objectify, ObPreparedQuery, and Iterator */
	@SuppressWarnings("unchecked")
	public static <T> T wrap(T impl, int retries)
	{
		return (T)Proxy.newProxyInstance(
				impl.getClass().getClassLoader(),
				new Class<?>[] { Objectify.class, OPreparedQuery.class, Iterator.class },
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