package com.googlecode.objectify.cache;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;


/**
 * <p>
 * Extends TriggerFuture so that it only gets triggered on successful (no exception)
 * completion of the Future.  This prevents, for example, cache put()s
 * from firing when concurrency exceptions are thrown.
 * </p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
abstract public class TriggerSuccessFuture<T> extends TriggerFuture<T>
{
	/** Wrap a normal Future<?> */
	public TriggerSuccessFuture(Future<T> raw) {
		super(raw);
	}
	
	/**
	 * This method will be called ONCE upon successful completion of the future.
	 */
	abstract protected void success(T result);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cache.NotifyFuture#execute()
	 */
	protected final void trigger() {
		try {
			this.success(this.get());
		} catch (Exception ex) {
			log.warn("Future<?> threw an exception during trigger", ex);
		}
	}
}