package com.googlecode.objectify.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A simple Future impl that provides some already-obtained static data.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FutureNow<T> implements Future<T>
{
	/** */
	private final T value;
	
	/**
	 */
	public FutureNow(final T value)
	{
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(final boolean mayInterruptIfRunning)
	{
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone()
	{
		return true;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException {
		return this.value;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return this.value;
	}
}