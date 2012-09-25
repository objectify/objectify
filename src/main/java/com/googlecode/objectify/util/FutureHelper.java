/*
 * $Id$
 */

package com.googlecode.objectify.util;

import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;


/**
 * This provides some of the methods of Google's (package-private) FutureHelper
 * 
 * @see com.google.appengine.api.datastore.FutureHelper
 * 
 * @author Jeff Schnitzer
 */
public class FutureHelper
{
	/** Quietly perform the get() on a future */
	public static <T> T quietGet(Future<T> future)
	{
		try
		{
			return future.get();
		}
		catch (Exception ex)
		{
			unwrapAndThrow(ex);
			return null;	// just to make the compiler happy
		}
	}
	
	/**
	 * Properly unwraps ExecutionException, throwing the relevant original cause.  Otherwise
	 * RuntimeExceptions get thrown and checked exceptions get wrapped in a RuntimeException.
	 */
	public static void unwrapAndThrow(Throwable ex)
	{
		if (ex instanceof RuntimeException)
			throw (RuntimeException)ex;
		else if (ex instanceof Error)
			throw (Error)ex;
		else if (ex instanceof ExecutionException)
			unwrapAndThrow(ex.getCause());
		else
			throw new UndeclaredThrowableException(ex);
	}
}