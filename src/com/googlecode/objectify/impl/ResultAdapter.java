package com.googlecode.objectify.impl;

import java.util.concurrent.Future;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.FutureHelper;

/**
 * Adapts a Future object to a (much more convenient) Result object.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ResultAdapter<T> implements Result<T>
{
	/** */
	Future<T> future;
	
	/** */
	public ResultAdapter(Future<T> fut)
	{
		this.future = fut;
	}

	@Override
	public T get()
	{
		try
		{
			return this.future.get();
		}
		catch (Exception e)
		{
			FutureHelper.unwrapAndThrow(e);
			return null;	// make compiler happy
		}
	}

	@Override
	public Future<T> getFuture()
	{
		return this.future;
	}
}