/*
 */

package com.googlecode.objectify;

import com.googlecode.objectify.cache.PendingFutures;
import com.googlecode.objectify.util.Closeable;

/**
 * Holder of the master ObjectifyFactory and provider of the current thread-local Objectify instance.
 * Call {@code ofy()} at any point to get the current Objectify with the correct transaction context.
 * 
 * @author Jeff Schnitzer
 */
public class ObjectifyService
{
	/** */
	private static ObjectifyFactory factory = new ObjectifyFactory();

	/** */
	public static void setFactory(ObjectifyFactory fact) {
		factory = fact;
	}

	/**
	 * Thread local stack of Objectify instances corresponding to transaction depth
	 */
	private static final ThreadLocal<Objectify> ofy = new ThreadLocal<Objectify>();

	/**
	 * The method to call at any time to get the current Objectify, which may change depending on txn context
	 */
	public static Objectify ofy() {
		Objectify stack = ofy.get();

		if (stack == null)
			throw new IllegalStateException("You have not started an Objectify context. You are probably missing the " +
					"ObjectifyFilter. If you are not running in the context of an http request, see the " +
					"ObjectifyService.run() method.");

		return stack;
	}

	/**
	 * @return the current factory
	 */
	public static ObjectifyFactory factory() {
		return factory;
	}

	/**
	 * A shortcut for {@code ObjectifyFactory.register()}
	 *  
	 * @see ObjectifyFactory#register(Class) 
	 */
	public static void register(Class<?> clazz) {
		factory().register(clazz); 
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
	public static <R> R run(Work<R> work) {
		return run(new ObjectifyOptions(), work);
	}

	/**
	 * <p>Runs one unit of work, making the root Objectify context available, configured per the options parameter.</p>
	 *
	 * <p>Normally you do not need to use this method. When servicing a normal request, the ObjectifyFilter
	 * will run this for you. This method is useful for using Objectify outside of a normal request -
	 * using the remote api, for example.</p>
	 *
	 * <p>Alternatively, you can use the begin() method and close the session manually.</p>
	 *
	 * @return the result of the work.
	 */
	public static <R> R run(ObjectifyOptions options, Work<R> work) {
		try (Closeable closeable = begin(options)) {
			return work.run();
		}
	}

	public static Closeable begin() {
		return begin(new ObjectifyOptions());
	}

	/**
	 * <p>An alternative to run() which is somewhat easier to use with testing (ie, @Before and @After) frameworks.
	 * You must close the return value at the end of the request in a finally block. It's better/safer to use run().</p>
	 *
	 * <p>This method is not typically necessary - in a normal request, the ObjectifyFilter takes care of this housekeeping
	 * for you. However, in unit tests or remote API calls it can be useful.</p>
	 */
	public static Closeable begin(ObjectifyOptions options) {
		// TODO: can <filter-mapping><dispatcher> fit the issue described below?
		// Request forwarding in the container runs all the filters again, including the ObjectifyFilter. Since we
		// have established a context already, we can't just throw an exception. We can't even really warn. Let's
		// just give them a new context; the bummer is that if programmers screw up and fail to close the context,
		// we have no way of warning them about the leak.
		if (ofy.get() != null)
			throw new IllegalStateException("You already have an initial Objectify context. Perhaps you want to use the ofy() method?");

		ofy.set(factory.begin(options));

		return new Closeable() {
			@Override
			public void close() {
				Objectify ofy = ObjectifyService.ofy.get();
				if (ofy == null)
					throw new IllegalStateException("You have already destroyed the Objectify context.");

				ofy.close();

				PendingFutures.completeAllPendingFutures();

				ObjectifyService.ofy.remove();
			}
		};
	}
}