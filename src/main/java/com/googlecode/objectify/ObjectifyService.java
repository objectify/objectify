/*
 */

package com.googlecode.objectify;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.util.Closeable;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;

/**
 * <p>Most applications connect to a single datastore. To make your life easy, we offer this
 * holder for a singleton ObjectifyFactory instance. It's optional; you are free to manage
 * one or more ObjectifyFactories yourself.</p>
 *
 * <p>After you have started a context with {@code begin()} or {@code run()}, you can call
 * {@code ofy()} to execute queries.</p>
 *
 * <p>If you have multiple datastore connections (and thus multiple ObjectifyFactory instances),
 * you likely will not use this class.</p>
 *
 * <p>Note - in the history of Objectify, this class (and the singleton ObjectifyFactory) were
 * required parts of infrastructure. There are deprecated legacy methods (like {@code Key.create()})
 * which refer to ObjectifyService. When those methods are removed, ObjectifyService will be
 * just like any other holder of an ObjectifyFactory.</p>
 * 
 * @author Jeff Schnitzer
 */
public class ObjectifyService {
	/**
	 *
	 */
	private static ObjectifyFactory factory;

	/**
	 * This is a shortcut for {@code ObjectifyService.init(new ObjectifyFactory())}
	 */
	public static void init() {
		init(new ObjectifyFactory());
	}

	/**
	 * Initialize the singleton factory.
	 */
	public static void init(final ObjectifyFactory fact) {
		factory = fact;
	}

	/**
	 * @return the current singleton factory
	 */
	public static ObjectifyFactory factory() {
		Preconditions.checkState(factory != null, "You must call ObjectifyService.init() before using Objectify");
		return factory;
	}

	/**
	 * <p>A shortcut for {@code factory().register()}</p>
	 */
	public static void register(Class<?> clazz) {
		factory().register(clazz);
	}

	/**
	 * <p>A shortcut for {@code factory().ofy()}, this is your main start point for executing Objectify operations.
	 * It returns the current Objectify instance for the singleton factory held by ObjectifyService.</p>
	 *
	 * <p>Note that the current instance is not thread-safe and may change as you enter and exit transactions. We do not
	 * recommend that you keep this value in a local variable.</p>
	 */
	public static Objectify ofy() {
		return factory().ofy();
	}

	/**
	 * <p>Runs one unit of work, making the root Objectify context available. This does not start a transaction,
	 * but it makes the static ofy() method return an appropriate object.</p>
	 *
	 * <p>Normally you do not need to use this method. When servicing a normal request, the ObjectifyFilter
	 * will run this for you. This method is useful for using Objectify outside of a normal request -
	 * using the remote api, for example.</p>
	 *
	 * <p>Alternatively, you can use the begin() method and close the session manually.</p>
	 *
	 * @return the result of the work.
	 */
	public static <R> R run(final Work<R> work) {
		return factory().run(work);
	}

	/**
	 * <p>Exactly the same behavior as the method that takes a {@code Work<R>}, but doesn't force you to return
	 * something from your lambda.</p>
	 */
	public static void run(final Runnable work) {
		factory().run(work);
	}

	/**
	 * <p>An alternative to run() which is somewhat easier to use with testing (ie, @Before and @After) frameworks.
	 * You must close the return value at the end of the request in a finally block. It's better/safer to use run().</p>
	 *
	 * <p>This method is not typically necessary - in a normal request, the ObjectifyFilter takes care of this housekeeping
	 * for you. However, in unit tests or remote API calls it can be useful.</p>
	 */
	public static Closeable begin() {
		return factory().begin();
	}


	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final Class<? extends T> kindClass, final long id) {
		return factory().key(kindClass, id);
	}

	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final Class<? extends T> kindClass, final String name) {
		return factory().key(kindClass, name);
	}

	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final Key<?> parent, final Class<? extends T> kindClass, final long id) {
		return factory().key(parent, kindClass, id);
	}

	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final Key<?> parent, final Class<? extends T> kindClass, final String name) {
		return factory().key(parent, kindClass, name);
	}

	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final String namespace, final Class<? extends T> kindClass, final long id) {
		return factory().key(namespace, kindClass, id);
	}

	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final String namespace, final Class<? extends T> kindClass, final String name) {
		return factory().key(namespace, kindClass, name);
	}

	/** Shortcut for the equivalent {@code factory().key()} method, convenient as a static import. */
	public static <T> Key<T> key(final T pojo) {
		return factory().key(pojo);
	}

	/** Shortcut for the equivalent {@code factory().ref()} method, convenient as a static import. */
	public static <T> Ref<T> ref(final Key<T> key) {
		return factory().ref(key);
	}

	/** Shortcut for the equivalent {@code factory().ref()} method, convenient as a static import. */
	public static <T> Ref<T> ref(final T value) {
		return factory().ref(value);
	}

	/**
	 * <p>This version is for the newer jakarta.servlet.* API. If you are using the older javax.servlet.*, use {@code FilterJavax}.</p>
	 *
	 * <p>Configure this filter to use Objectify in your application.  It works in concert with {@code ObjectifyService}
	 * to provide the correct {@code Objectify} instance when {@code ObjectifyService.ofy()} is called.</p>
	 *
	 * <p>In your web.xml:</p>
	 *<pre>
	 *       &lt;filter&gt;
	 *              &lt;filter-name&gt;ObjectifyFilter&lt;/filter-name&gt;
	 *              &lt;filter-class&gt;com.googlecode.objectify.ObjectifyService$Filter&lt;/filter-class&gt;
	 *      &lt;/filter&gt;
	 *      &lt;filter-mapping&gt;
	 *              &lt;filter-name&gt;ObjectifyFilter&lt;/filter-name&gt;
	 *              &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	 *      &lt;/filter-mapping&gt;
	 *</pre>
	 *
	 * <p>Or, if you use Guice:</p>
	 *
	 *<pre>
	 *      filter("/*").through(ObjectifyService.Filter.class);
	 *</pre>
	 *
	 * <p>If you use the Objectify outside of the context of a request (say, using the remote
	 * API or from a unit test), then you should use the ObjectifyService.run() method.</p>
	 *
	 * @author Jeff Schnitzer
	 */
	public static class Filter implements jakarta.servlet.Filter {
		@Override
		public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
			try (Closeable closeable = ObjectifyService.begin()) {
				chain.doFilter(request, response);
			}
		}

		@Override
		public void init(final FilterConfig filterConfig) throws ServletException {}
		@Override
		public void destroy() {}
	}

	/**
	 * <p>This version is for the older javax.servlet.* API. If you are using the newer jakarta.servlet.*, use {@code ObjectifyFilter}.</p>
	 *
	 * <p>Configure this filter to use Objectify in your application.  It works in concert with {@code ObjectifyService}
	 * to provide the correct {@code Objectify} instance when {@code ObjectifyService.ofy()} is called.</p>
	 *
	 * <p>In your web.xml:</p>
	 *<pre>
	 *       &lt;filter&gt;
	 *              &lt;filter-name&gt;ObjectifyFilter&lt;/filter-name&gt;
	 *              &lt;filter-class&gt;com.googlecode.objectify.ObjectifyService$FilterJavax&lt;/filter-class&gt;
	 *      &lt;/filter&gt;
	 *      &lt;filter-mapping&gt;
	 *              &lt;filter-name&gt;ObjectifyFilter&lt;/filter-name&gt;
	 *              &lt;url-pattern&gt;/*&lt;/url-pattern&gt;
	 *      &lt;/filter-mapping&gt;
	 *</pre>
	 *
	 * <p>Or, if you use Guice:</p>
	 *
	 *<pre>
	 *      filter("/*").through(ObjectifyService.FilterJavax.class);
	 *</pre>
	 *
	 * <p>If you use the Objectify outside of the context of a request (say, using the remote
	 * API or from a unit test), then you should use the ObjectifyService.run() method.</p>
	 *
	 * @author Jeff Schnitzer
	 */
	public static class FilterJavax implements javax.servlet.Filter {
		@Override
		public void doFilter(javax.servlet.ServletRequest request, javax.servlet.ServletResponse response, javax.servlet.FilterChain chain) throws IOException, javax.servlet.ServletException {
			try (Closeable closeable = ObjectifyService.begin()) {
				chain.doFilter(request, response);
			}
		}

		@Override
		public void init(final javax.servlet.FilterConfig filterConfig) throws javax.servlet.ServletException {}
		@Override
		public void destroy() {}
	}
}