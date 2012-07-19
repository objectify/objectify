/*
 */

package com.googlecode.objectify;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.googlecode.objectify.cache.AsyncCacheFilter;

/**
 * Configure this filter to use Objectify in your application.  It works in concert with {@code ObjectifyService}
 * to provide the correct {@code Objectify} instance when {@code ObjectifyService.ofy()} is called.
 * 
 * In your web.xml:
 *<pre>
 *       &lt;filter&gt;
 *              &lt;filter-name&gt;ObjectifyFilter&lt;/filter-name&gt;
 *              &lt;filter-class&gt;com.googlecode.objectify.ObjectifyFilter&lt;/filter-class&gt;
 *      &lt;/filter&gt;
 *      &lt;filter-mapping&gt;
 *              &lt;filter-name&gt;ObjectifyFilter&lt;/filter-name&gt;
 *              &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *      &lt;/filter-mapping&gt;
 *</pre>
 *
 * Or, if you use Guice:
 *
 *<pre>
 *      filter("/*").through(ObjectifyFilter.class);
 *</pre>
 *
 * <p>If you use the Objectify outside of the context of a request (say, using the remote
 * API or from a unit test), then you should call {@code ObjectifyFilter.complete()} after every operation
 * that you consider a "request".  For example, after each test.</p>
 * 
 * @author Jeff Schnitzer
 */
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

	/**
	 * Perform the actions that are performed upon normal completion of a request.
	 */
	public static void complete() {
		AsyncCacheFilter.complete();
		ObjectifyService.reset();
	}
}