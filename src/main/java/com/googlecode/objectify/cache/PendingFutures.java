package com.googlecode.objectify.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

/**
 * <p>This class maintains a thread local list of all the outstanding Future<?> objects
 * that have pending triggers.  When a Future<?> is done and its trigger is executed,
 * it is removed from the list.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class PendingFutures
{
	/**
	 * We use ConcurrentHashMap not for concurrency but because it doesn't throw
	 * ConcurrentModificationException.  We need to be able to iterate while Futures remove
	 * themselves from the set. A Set is just a Map of key to key.
	 */
	private static ThreadLocal<Map<Future<?>, Future<?>>> pending = ThreadLocal.withInitial(() -> new ConcurrentHashMap<>(64, 0.75f, 1));
	
	/**
	 * Register a pending Future that has a callback.
	 * @param future must have at least one callback
	 */
	public static void addPending(Future<?> future) {
		pending.get().put(future, future);
	}
	
	/**
	 * Deregister a pending Future that had a callback.
	 */
	public static void removePending(Future<?> future) {
		pending.get().remove(future);
	}
	
	/**
	 * Iterate through all pending futures and get() them, forcing any callbacks to be called.
	 * This is used only by the AsyncCacheFilter (if using cache without Objectify) or ObjectifyFilter
	 * (if using Objectify normally) because we don't have a proper hook otherwise.
	 */
	public static void completeAllPendingFutures() {
		// This will cause done Futures to fire callbacks and remove themselves
		for (Future<?> fut: pending.get().keySet()) {
			try {
				fut.get();
			}
			catch (Exception e) {
				log.error("Error cleaning up pending Future: " + fut, e);
			}
		}
	}
}
