package com.googlecode.objectify.cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.utils.SystemProperty;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

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
public class TriggerFutureHook implements Delegate<Environment>
{
	/** */
	Delegate<Environment> parent;
	
	/** Automatically install when loaded */
	static {
		install();
	}
	
	/** The thread local value will be removed (null) if there are none pending */
	private static ThreadLocal<Pending> pending = new ThreadLocal<Pending>();
	
	/**
	 * Install our hook in the delegate system.  This happens automatically when this class is
	 * loaded, which is typically what you want when working with Objectify.  It gets a lot
	 * more complicated when you're working with the RemoteApiInstaller.
	 */
	public static synchronized void install()
	{
		// Already installed
		if (ApiProxy.getDelegate() instanceof TriggerFutureHook)
			return;
		
		@SuppressWarnings("unchecked")
		TriggerFutureHook hook = new TriggerFutureHook(ApiProxy.getDelegate());
		
		if (SystemProperty.environment.value() == SystemProperty.Environment.Value.Production)
			ApiProxy.setDelegate(hook);
		else
			ApiProxy.setDelegate(wrapPartially(ApiProxy.getDelegate(), hook));
	}
	
// This just doesn't work because of the dynamic proxy hack necessary on local.  I've tried adding
// a GetParentDelegate interface to the proxy to get the value out but I just get an "interface
// is not visible to the class loader" exception no matter what I try.  So basically you can't
// uninstall until Google fixes at least one of these bugs:
// http://code.google.com/p/googleappengine/issues/detail?id=4271
// http://code.google.com/p/googleappengine/issues/detail?id=4442
// http://code.google.com/p/googleappengine/issues/detail?id=5965
//
//	/**
//	 * Remove the TriggerFutureHook from the delegate system.  This only works if we were the last delegate to be installed.
//	 * You'll need to call this before calling RemoteApiInstaller.uninstall() if you have been using Objectify.
//	 * 
//	 * @see http://code.google.com/p/googleappengine/issues/detail?id=5965
//	 */
//	public static synchronized void uninstall()
//	{
//		if (!(ApiProxy.getDelegate() instanceof GetParentDelegate))
//			throw new IllegalStateException("Can't uninstall because another delegate has been registered, and Google doesn't provide a way to handle this case.  See http://code.google.com/p/googleappengine/issues/detail?id=5965");
//		
//		GetParentDelegate current = (GetParentDelegate)ApiProxy.getDelegate();
//		
//		ApiProxy.setDelegate(current.getParent());
//	}
	
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
	
	/** */
	public TriggerFutureHook(Delegate<Environment> parent)
	{
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see com.google.apphosting.api.ApiProxy.Delegate#log(com.google.apphosting.api.ApiProxy.Environment, com.google.apphosting.api.ApiProxy.LogRecord)
	 */
	@Override
	public void log(Environment arg0, LogRecord arg1)
	{
		this.parent.log(arg0, arg1);
	}

	/* (non-Javadoc)
	 * @see com.google.apphosting.api.ApiProxy.Delegate#makeAsyncCall(com.google.apphosting.api.ApiProxy.Environment, java.lang.String, java.lang.String, byte[], com.google.apphosting.api.ApiProxy.ApiConfig)
	 */
	@Override
	public Future<byte[]> makeAsyncCall(Environment arg0, String arg1, String arg2, byte[] arg3, ApiConfig arg4)
	{
		this.checkPendingFutures();
		return this.parent.makeAsyncCall(arg0, arg1, arg2, arg3, arg4);
	}

	/* (non-Javadoc)
	 * @see com.google.apphosting.api.ApiProxy.Delegate#makeSyncCall(com.google.apphosting.api.ApiProxy.Environment, java.lang.String, java.lang.String, byte[])
	 */
	@Override
	public byte[] makeSyncCall(Environment arg0, String arg1, String arg2, byte[] arg3) throws ApiProxyException
	{
		this.checkPendingFutures();
		return this.parent.makeSyncCall(arg0, arg1, arg2, arg3);
	}
	
	/**
	 * If any futures are pending, check if they are done.  This will process their callbacks.
	 */
	private void checkPendingFutures()
	{
		Pending pend = pending.get();
		if (pend != null)
			pend.checkPendingFutures();
	}
	
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

	/**
	 * This is a bit of cleverness from Max Ross (Google) to work around the problem
	 * that the local unit testing framework explicitly casts the delegate to ApiProxyLocal.
	 * Until that bug is removed, this method uses a dynamic proxy to fake the interface.
	 * 
	 * Create a proxy that implements all the interfaces that the original
	 * implements. Whenever a method is called that the wrapper supports, the
	 * wrapper will be called. Otherwise, the method will be invoked on the
	 * original object.
	 * 
	 * CAREFUL!  The call to getClassLoader() throws a security exception on prod GAE,
	 * so only use this mechanism when not on prod.
	 */
	@SuppressWarnings("unchecked")
	static <S, T extends S> S wrapPartially(final S original, final T wrapper)
	{
		// We need to make sure GetParentDelegate is one of the interfaces supported by the proxy
		Class<?>[] interfaces = original.getClass().getInterfaces();
		
		InvocationHandler handler = new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
			{
				Method wrapperMethod = null;
				try
				{
					wrapperMethod = wrapper.getClass().getMethod(method.getName(), method.getParameterTypes());
				}
				catch (NoSuchMethodException e)
				{
					try { return method.invoke(original, args); }
					catch (InvocationTargetException ex) { throw ex.getTargetException(); }
				}
				
				try { return wrapperMethod.invoke(wrapper, args); }
				catch (InvocationTargetException ex) { throw ex.getTargetException(); }
			}
		};
		
		return (S)Proxy.newProxyInstance(original.getClass().getClassLoader(), interfaces, handler);
	}

	/* (non-Javadoc)
	 * @see com.google.apphosting.api.ApiProxy.Delegate#flushLogs(com.google.apphosting.api.ApiProxy.Environment)
	 */
	@Override
	public void flushLogs(Environment paramE)
	{
		parent.flushLogs(paramE);
	}

	/* (non-Javadoc)
	 * @see com.google.apphosting.api.ApiProxy.Delegate#getRequestThreads(com.google.apphosting.api.ApiProxy.Environment)
	 */
	@Override
	public List<Thread> getRequestThreads(Environment paramE)
	{
		return parent.getRequestThreads(paramE);
	}
}
