/*
 */

package com.googlecode.objectify;

import com.google.common.base.Preconditions;
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
	private static ObjectifyFactory factory;

	/** This is a shortcut for {@code ObjectifyService.init(new ObjectifyFactory())}*/
	public static void init() {
		init(new ObjectifyFactory());
	}

	/** */
	public static void init(final ObjectifyFactory fact) {
		factory = fact;
	}

	/**
	 * @return the current factory
	 */
	public static ObjectifyFactory factory() {
		Preconditions.checkState(factory != null, "You must call ObjectifyService.init() before using Objectify");
		return factory;
	}

	/**
	 * A shortcut for {@code factory().register()}
	 *  
	 * @see ObjectifyFactory#register(Class) 
	 */
	public static void register(Class<?> clazz) {
		factory().register(clazz); 
	}

	/**
	 * The method to call at any time to get the current Objectify, which may change depending on txn context
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
		try (Closeable closeable = begin()) {
			return work.run();
		}
	}

	/**
	 * <p>An alternative to run() which is somewhat easier to use with testing (ie, @Before and @After) frameworks.
	 * You must close the return value at the end of the request in a finally block. It's better/safer to use run().</p>
	 *
	 * <p>This method is not typically necessary - in a normal request, the ObjectifyFilter takes care of this housekeeping
	 * for you. However, in unit tests or remote API calls it can be useful.</p>
	 */
	public static Closeable begin() {
		return factory().open();
	}
}