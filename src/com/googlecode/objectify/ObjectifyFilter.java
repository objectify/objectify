/*
 */

package com.googlecode.objectify;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.googlecode.objectify.cache.AsyncCacheFilter;

/**
 * Filter which resets the Objectify thread-local stack at the end of every request.
 *
 * @author Jeff Schnitzer
 */
@Singleton
public class ObjectifyFilter extends AsyncCacheFilter
{
	/** */
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			super.doFilter(request, response, chain);
		} finally {
			ObjectifyService.reset();
		}
	}
}