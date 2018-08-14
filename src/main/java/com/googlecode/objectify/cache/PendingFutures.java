package com.googlecode.objectify.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.logging.Level;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.util.FutureHelper;

import lombok.NonNull;
import lombok.extern.java.Log;

/**
 * <p>This class maintains a thread local list of all the outstanding Future<?> objects
 * that have pending triggers.  When a Future<?> is done and its trigger is executed,
 * it is removed from the list.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Log
public class PendingFutures
{
	/**
	 * We use ConcurrentHashMap not for concurrency but because it doesn't throw
	 * ConcurrentModificationException. We need to be able to iterate while Futures remove
	 * themselves from the Map.
	 */
	private static ThreadLocal<Map<Future<?>, Optional<Objectify>>> pending = new ThreadLocal<Map<Future<?>, Optional<Objectify>>>() {
		@Override
		protected Map<Future<?>, Optional<Objectify>> initialValue() {
			return new ConcurrentHashMap<>(64, 0.75f, 1);
		}
	};
	
	/**
	 * Register a pending Future that has a callback.
	 * @param future must have at least one callback
	 */
	public static void addPending(Future<?> future) {
		pending.get().put(future, ofyOptional());
	}
	
	// it is possible that CachingAsyncDatastoreService gets used - which utilizes this functionality - without any Objectify context
	private static Optional<Objectify> ofyOptional() {
		try {
			return Optional.of(ObjectifyService.ofy());
		} catch (@SuppressWarnings("unused") RuntimeException e) {
			return Optional.absent();
		}
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
		for (Future<?> fut : pending.get().keySet()) {
			quietGet(fut);
		}
	}
	
	/**
	 * Iterate through all pending futures that belong to the provided Objectify instance and get() them, forcing any callbacks to be called.
	 * This is used only by the close() method called on the Objectify instances created by ObjectifyService.begin().
	 */
	public static void completeAllPendingFutures(@NonNull Objectify ofy) {
		// This will cause done Futures to fire callbacks and remove themselves
		for (Map.Entry<Future<?>, Optional<Objectify>> entry : pending.get().entrySet()) {
			if (Objects.equal(entry.getValue().orNull(), ofy)) {
				quietGet(entry.getKey());
			}
		}
	}
	
	private static void quietGet(Future<?> fut) {
		try {
			FutureHelper.quietGet(fut);
		} catch (RuntimeException e) {
			log.log(Level.SEVERE, "Error cleaning up pending Future: " + fut, e);
		}
	}
	
}
