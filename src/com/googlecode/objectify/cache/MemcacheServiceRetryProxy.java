package com.googlecode.objectify.cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.memcache.ErrorHandler;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.StrictErrorHandler;

/**
 * <p>Dynamic proxy which wraps a MemcacheService and adds retries when an exception occurs.
 * It logs and masks exceptions on complete failure.</p> 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MemcacheServiceRetryProxy implements InvocationHandler
{
	/** */
	private static final Logger log = Logger.getLogger(MemcacheServiceRetryProxy.class.getName());
	
	/** */
	private static final int DEFAULT_RETRIES = 4;
	
	/**
	 * We use this on the memcacheservice for writes so that we get real exceptions when something goes wrong.
	 */
	private static final ErrorHandler DOES_NOT_MASK_EXCEPTIONS = new StrictErrorHandler();
	
	/**
	 * Create the proxy that does retries. Adds a strict error handler to the service.
	 */
	public static MemcacheService createProxy(MemcacheService raw)
	{
		return createProxy(raw, DEFAULT_RETRIES);
	}
	
	/**
	 * Create the proxy that does retries. Adds a strict error handler to the service.
	 */
	public static MemcacheService createProxy(MemcacheService raw, int retryCount)
	{
		raw.setErrorHandler(DOES_NOT_MASK_EXCEPTIONS);
		
		return (MemcacheService)java.lang.reflect.Proxy.newProxyInstance(
			raw.getClass().getClassLoader(),
			raw.getClass().getInterfaces(),
			new MemcacheServiceRetryProxy(raw, retryCount));
	}
	
	/** */
	private MemcacheService raw;
	
	/** */
	private int retries;
	
	/** */
	public MemcacheServiceRetryProxy(MemcacheService raw, int retries)
	{
		this.raw = raw;
		this.retries = retries;
	}

	/* (non-Javadoc)
	 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
	 */
	@Override
	public Object invoke(Object proxy, Method meth, Object[] args) throws Throwable
	{
		for (int i=0; i<this.retries; i++) {
			try {
				return meth.invoke(this.raw, args);
			} catch (InvocationTargetException ex) {
				if (i == (this.retries - 1))
					log.log(Level.SEVERE, "Memcache operation failed, giving up", ex);
				else
					log.log(Level.WARNING, "Error performing memcache operation, retrying: " + meth, ex);
			}
		}
		
		// Will reach this point of we have exhausted our retries.
		return null;
	}

}