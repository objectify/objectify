package com.googlecode.objectify;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.google.appengine.api.datastore.DatastoreTimeoutException;

/**
 * A dynamic proxy that catches DatastoreTimeoutException and retries
 * the action up to a specified number of times.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DatastoreTimeoutRetryProxy implements InvocationHandler
{
	/** The actual implementation of Objectify */
	Objectify base;
	
	/** The number of times we should retry.  1 means two possible tries. */
	int retries;
	
	/** */
	public static Objectify wrap(Objectify impl, int retries)
	{
		return (Objectify)Proxy.newProxyInstance(
				impl.getClass().getClassLoader(),
				new Class<?>[] { Objectify.class },
				new DatastoreTimeoutRetryProxy(impl, retries));
	}
	
	/** */
	protected DatastoreTimeoutRetryProxy(Objectify base, int retries)
	{
		this.base = base;
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
				return method.invoke(this.base, args);
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