package com.googlecode.objectify.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>This is the state maintained on a per-thread basis for all of the oustanding Future<?> objects
 * that have pending callbacks.  When a Future<?> is done and its callbacks are executed,
 * it is removed from the list.  At various times (anytime an API call is made) the registered
 * futures are checked for doneness and processed.</p>
 * 
 * <p>The AsyncCacheFilter is necessary to guarantee that any pending callbacks are processed
 * at the end of the request.  A future GAE SDK which allows us to hook into the Future<?>
 * creation process might make this extra Filter unnecessary.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Pending
{
	/** */
	private static final Logger log = Logger.getLogger(Pending.class.getName());
	
	/**
	 * We use this not for concurrency but because it is a HashMap that doesn't throw
	 * ConcurrentModificationException.  We need to be able to iterate while Futures remove
	 * themselves from the set. 
	 */
	ConcurrentHashMap<Future<?>, Future<?>> pendingFutures = new ConcurrentHashMap<Future<?>, Future<?>>(64, 0.75f, 1);
	
	/** 
	 * True while we are iterating on the pendingFutures.  This prevents reentrancy problems.
	 */
	boolean iterating = false;
	
	/**
	 * Register a pending Future that has a callback.
	 * @param future must have at least one callback
	 */
	public void add(Future<?> future)
	{
		this.pendingFutures.put(future, future);
	}
	
	/**
	 * De-register a pending Future.
	 */
	public void remove(Future<?> future)
	{
		this.pendingFutures.remove(future);
	}
	
	/**
	 * @return true if there are no more pending futures.
	 */
	public boolean isEmpty()
	{
		return this.pendingFutures.isEmpty();
	}
	
	/**
	 * If any futures are pending, check if they are done.  This will process their callbacks.
	 */
	public void checkPendingFutures()
	{
		// Re-entrancy would cause us to recurse endlessly
		if (this.iterating)
			return;
			
		try
		{
			this.iterating = true;
			
			// This will cause done Futures to fire callbacks and remove themselves 
			for (Future<?> fut: this.pendingFutures.keySet())
				fut.isDone();
		}
		finally
		{
			this.iterating = false;
		}
	}
	
	/**
	 * Iterate through all pending futures and get() them, forcing any callbacks to be called.
	 */
	public void completeAllPendingFutures()
	{
		try
		{
			this.iterating = true;
			
			// This will cause done Futures to fire callbacks and remove themselves 
			for (Future<?> fut: this.pendingFutures.keySet())
			{
				try
				{
					fut.get();
				}
				catch (Exception e)
				{
					log.log(Level.SEVERE, "Error cleaning up pending Future", e);
				}
			}
		}
		finally
		{
			this.iterating = false;
		}
	}
}
