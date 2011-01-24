package com.googlecode.objectify.cache;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * <p>This Filter is a companion to the CachingAsyncDatastoreService, and must be
 * installed any time the CachingAsyncDatastoreService is used.</p>
 * 
 * <p>This Filter is a temporary measure until Google provides a hook that lets
 * us intercept the raw Future<?> calls at the end of a request.  At that point
 * this filter can be eliminated in favor of the hook.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AsyncCacheFilter implements Filter
{
	@Override
	public void init(FilterConfig config) throws ServletException
	{
	}

	@Override
	public void destroy()
	{
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
	{
		try
		{
			chain.doFilter(request, response);
		}
		finally
		{
			ListenableHook.completeAllPendingFutures();
		}
	}
}
