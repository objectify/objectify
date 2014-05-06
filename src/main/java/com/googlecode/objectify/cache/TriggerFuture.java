package com.googlecode.objectify.cache;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * <p>
 * A Future<?> wrapper that executes an abstract method with the result at some point after
 * the data becomes available.  A "best effort" is made to ensure execution, but it may be
 * left untriggered until the end of a request.
 * </p>
 * 
 * <p>
 * Notification will happen ONCE:
 * </p>
 * 
 * <ul>
 * <li>After get() is called</li>
 * <li>When the future is done and isDone() is called</li>
 * <li>At the end of a request that has the AsyncCacheFilter enabled.</li>
 * </ul>
 * 
 * <p>Use the AsyncCacheFilter for normal requests. For situations where a filter is not appropriate
 * (ie, the remote api) be sure to call PendingFutures.completeAllPendingFutures() manually.</p>
 *
 * <p>Note that if you are using this with Objectify, you probably want to use ObjectifyFilter.complete()
 * rather than PendingFutures or AsyncCacheFilter static methods.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TriggerFuture<T> implements Future<T>
{
	/** Wrap the raw Future<?> */
	protected Future<T> raw;
	
	/** If we have run the trigger() method already */
	boolean triggered = false;
	
	/** Wrap a normal Future<?> */
	public TriggerFuture(Future<T> raw) {
		this.raw = raw;
		
		// We now need to register ourself so that we'll get checked at future API calls
		PendingFutures.addPending(this);
	}
	
	/**
	 * This method will be called ONCE upon completion of the future, successful or not.
	 * Beware that this.get() may throw an exception.
	 */
	abstract protected void trigger();

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		throw new UnsupportedOperationException("This makes my head spin. Don't do it.");
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled()
	{
		return this.raw.isCancelled();
	}

	/**
	 * This version also checks to see if we are done and we still need to call the trigger.
	 * If so, it calls it.
	 * 
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone() {
		boolean done = this.raw.isDone();
		
		if (!triggered && done) {
			this.triggered = true;
			PendingFutures.removePending(this);
			
			this.trigger();
		}

		return done;
	}
	
	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException
	{
		try {
			return this.raw.get();
		} finally {
			this.isDone();
		}
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		try {
			return this.raw.get(timeout, unit);
		} finally {
			this.isDone();
		}
	}
}