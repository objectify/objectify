/*
 */

package com.googlecode.objectify;

import com.googlecode.objectify.util.Closeable;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @deprecated use {@code ObjectifyService.Filter} or {@code ObjectifyService.FilterJavax} instead
 */
@Deprecated
public class ObjectifyFilter implements Filter
{
	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		try (Closeable closeable = ObjectifyService.begin()) {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void init(final FilterConfig filterConfig) throws ServletException {}
	@Override
	public void destroy() {}
}