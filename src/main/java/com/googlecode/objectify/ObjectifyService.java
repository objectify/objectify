/*
 */

package com.googlecode.objectify;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.util.Closeable;

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
	 *
	 */
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
	 * @deprecated use {@code factory().register()} instead.
	 */
	@Deprecated
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
}