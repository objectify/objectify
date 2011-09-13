package com.googlecode.objectify.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * <p>
 * A Future<?> wrapper that adds the ability to define callbacks.  The callbacks
 * can be added before or after the Future<?> is completed and will be fired either way.
 * </p>
 * <p>
 * Any pending callbacks will be fired during any method call when done; calling isDone()
 * is the usual method.  
 * </p>
 * <p>
 * Callbacks will *not* be fired if the get() method throws an exception.  That is, callbacks
 * are only fired on normal completion of the result.  This prevents, for example, cache put()s
 * from firing when concurrency exceptions are thrown.
 * </p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ListenableFuture<T> implements Future<T>
{
	/** Wrap the raw Future<?> */
	Future<T> raw;
	
	/** Holds any pending callbacks; null is the sentinel value for "no callbacks" */
	List<Runnable> callbacks;
	
	/** Wrap a normal Future<?> */
	public ListenableFuture(Future<T> raw)
	{
		this.raw = raw;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		//return this.raw.cancel(mayInterruptIfRunning);
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
	 * This version also executes any pending callbacks if done.  Note that we might
	 * end up with no callbacks but the Future still registered; this is not a problem,
	 * they will eventually get cleaned up.
	 * 
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone()
	{
		boolean done = this.raw.isDone();

		if (done && this.callbacks != null)
		{
			// Reset to null ASAP to fix any reentrancy problems; very likely the get()
			// method will be called at some point during a callback.
			List<Runnable> doMe = this.callbacks;
			this.callbacks = null;

			// Make sure that we got an actual value rather than an exception
			boolean itWorked = false;
			try {
				this.raw.get();
				itWorked = true;
			} catch (Exception ex) {}
			
			if (itWorked)
				for (Runnable runnable: doMe)
					runnable.run();
			
			// Deregister us, we're done!
			ListenableHook.removePending(this);
		}
		
		return done;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public T get() throws InterruptedException, ExecutionException
	{
		T value = this.raw.get();
		this.isDone();
		return value;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		T value = this.raw.get(timeout, unit);
		this.isDone();
		return value;
	}
	
	/**
	 * Adds a callback.  If the underlying Future isDone the callback will be executed
	 * immediately, otherwise it will be saved until sometime later.
	 */
	public void addCallback(Runnable cb)
	{
		if (this.raw.isDone())
		{
			cb.run();
		}
		else
		{
			if (this.callbacks == null)
				this.callbacks = new ArrayList<Runnable>();
			
			this.callbacks.add(cb);
			
			// We now need to register ourself so that we'll get checked at future API calls
			// Doesn't matter if this gets called multiple times.
			ListenableHook.addPending(this);
		}
	}
}