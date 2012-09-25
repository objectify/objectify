package com.googlecode.objectify.cache;

import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>
 * Extends TriggerFuture so that it only gets triggered on successful (no exception)
 * completion of the Future.  This prevents, for example, cache put()s
 * from firing when concurrency exceptions are thrown.
 * </p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TriggerSuccessFuture<T> extends TriggerFuture<T>
{
	private static final Logger log = Logger.getLogger(TriggerSuccessFuture.class.getName());
	
	/** Wrap a normal Future<?> */
	public TriggerSuccessFuture(Future<T> raw)
	{
		super(raw);
	}
	
	/**
	 * This method will be called ONCE upon successful completion of the future.
	 */
	abstract protected void success(T result);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cache.NotifyFuture#execute()
	 */
	protected final void trigger()
	{
		try {
			this.success(this.get());
		} catch (Exception ex) {
			log.log(Level.WARNING, "Future<?> threw an exception during trigger", ex);
		}
	}
}