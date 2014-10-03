package com.googlecode.objectify.cache;

import com.googlecode.objectify.util.AbstractFilter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * <p>This Filter is a companion to the CachingAsyncDatastoreService, and must be
 * installed any time the CachingAsyncDatastoreService is used without Objectify.</p>
 *
 * <p>This Filter is a temporary measure until Google provides a hook that lets
 * us intercept the raw Future<?> calls at the end of a request.  At that point
 * this filter can be eliminated in favor of the hook.</p>
 *
 * In your web.xml:
 *<pre>
 *       &lt;filter&gt;
 *              &lt;filter-name&gt;AsyncCacheFilter&lt;/filter-name&gt;
 *              &lt;filter-class&gt;com.googlecode.objectify.cache.AsyncCacheFilter&lt;/filter-class&gt;
 *      &lt;/filter&gt;
 *      &lt;filter-mapping&gt;
 *              &lt;filter-name&gt;AsyncCacheFilter&lt;/filter-name&gt;
 *              &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 *      &lt;/filter-mapping&gt;
 *</pre>
 *
 * Or, if you use Guice:
 *
 *<pre>
 *      filter("/*").through(AsyncCacheFilter.class);
 *</pre>
 *
 * <p>Note that you do not need to configure this filter if you use the {@code ObjectifyFilter}.</p>
 * 
 * <p>If you use the CachingAsyncDatastoreService outside of the context of a request (say, using the remote
 * API or from a unit test), then you should call {@code AsyncCacheFilter.complete()} after every operation
 * that you consider a "request".  For example, after each test.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AsyncCacheFilter extends AbstractFilter
{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try {
			chain.doFilter(request, response);
		} finally {
			complete();
		}
	}
	
	/**
	 * Perform the actions that are performed upon normal completion of a request.
	 */
	public static void complete() {
		PendingFutures.completeAllPendingFutures();
	}
}
