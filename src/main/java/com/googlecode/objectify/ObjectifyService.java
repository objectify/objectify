/*
 */

package com.googlecode.objectify;

import com.googlecode.objectify.cache.PendingFutures;
import com.googlecode.objectify.util.Closeable;

import java.util.ArrayDeque;
import java.util.Deque;

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
	private static final ThreadLocal<Deque<Objectify>> STACK = new ThreadLocal<Deque<Objectify>>() {
		@Override
		protected Deque<Objectify> initialValue() {
			return new ArrayDeque<>();
		}
	};

	/**
	 * The method to call at any time to get the current Objectify, which may change depending on txn context
	 */
	public static Objectify ofy() {
		Deque<Objectify> stack = STACK.get();

		if (stack.isEmpty())
			throw new IllegalStateException("You have not started an Objectify context. You are probably missing the " +
					"ObjectifyFilter. If you are not running in the context of an http request, see the " +
					"ObjectifyService.run() method.");

		return stack.getLast();
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
		final Deque<Objectify> stack = STACK.get();

		// Request forwarding in the container runs all the filters again, including the ObjectifyFilter. Since we
		// have established a context already, we can't just throw an exception. We can't even really warn. Let's
		// just give them a new context; the bummer is that if programmers screw up and fail to close the context,
		// we have no way of warning them about the leak.
		//if (!stack.isEmpty())
		//	throw new IllegalStateException("You already have an initial Objectify context. Perhaps you want to use the ofy() method?");

		final Objectify ofy = factory.begin();

		stack.add(ofy);

		return new Closeable() {
			@Override
			public void close() {
				if (stack.isEmpty())
					throw new IllegalStateException("You have already destroyed the Objectify context.");

				try {
					// Same comment as above - we can't make claims about the state of the stack
					// beacuse of dispatch forwarding
					// if (stack.size() > 1)
					// throw new IllegalStateException("You are trying to close the root session
					// before all transactions have been unwound.");

					// The order of these three operations is significant

					ofy.flush();

					PendingFutures.completeAllPendingFutures();
				} finally {
					stack.removeLast();
				}
			}
		};
	}

	/** Pushes new context onto stack when a transaction starts. For internal housekeeping only. */
	public static void push(Objectify ofy) {
		STACK.get().add(ofy);
	}

	/** Pops context off of stack after a transaction completes. For internal housekeeping only. */
	public static void pop() {
		STACK.get().removeLast();
	}
}