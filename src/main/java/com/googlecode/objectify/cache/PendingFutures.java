package com.googlecode.objectify.cache;

import java.util.concurrent.Future;

/**
 * <p>This bit of appengine magic hooks into the ApiProxy and does the heavy lifting of
 * making the TriggerFuture<?> work.</p>
 * 
 * <p>This class maintains a thread local list of all the outstanding Future<?> objects
 * that have pending triggers.  When a Future<?> is done and its trigger is executed,
 * it is removed from the list.  At various times (anytime an API call is made) the registered
 * futures are checked for doneness and processed.</p>
 * 
 * <p>The AsyncCacheFilter is necessary to guarantee that any pending triggers are processed
 * at the end of the request.  A future GAE SDK which allows us to hook into the Future<?>
 * creation process might make this extra Filter unnecessary.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PendingFutures
{
	/** The thread local value will be removed (null) if there are none pending */
	private static ThreadLocal<Pending> pending = new ThreadLocal<>();
	
	
	/**
	 * Register a pending Future that has a callback.
	 * @param future must have at least one callback
	 */
	public static void addPending(Future<?> future)
	{
		Pending pend = pending.get();
		if (pend == null)
		{
			pend = new Pending();
			pending.set(pend);
		}
		
		pend.add(future);
	}
	
	/**
	 * Deregister a pending Future that had a callback.
	 */
	public static void removePending(Future<?> future)
	{
		Pending pend = pending.get();
		if (pend != null)
		{
			pend.remove(future);
			
			// When the last one is gone, we don't need this thread local anymore
			if (pend.isEmpty())
				pending.remove();
		}
	}
	
//	/**
//	 * If any futures are pending, check if they are done.  This will process their callbacks.
//	 * Don't use this method - see comments on Pending.checkPendingFutures()
//	 */
//	@Deprecated
//	public static void checkPendingFutures()
//	{
//		Pending pend = pending.get();
//		if (pend != null)
//			pend.checkPendingFutures();
//	}
	
	/**
	 * Iterate through all pending futures and get() them, forcing any callbacks to be called.
	 * This is used only by the AsyncCacheFilter because we don't have a proper hook otherwise.
	 */
	public static void completeAllPendingFutures()
	{
		Pending pend = pending.get();
		if (pend != null)
			pend.completeAllPendingFutures();
	}
}
