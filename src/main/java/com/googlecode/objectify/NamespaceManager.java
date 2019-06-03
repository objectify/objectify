package com.googlecode.objectify;

import com.googlecode.objectify.util.Closeable;

/**
 * A simple thread local namespace manager, similar to legacy GAE's {@code NamespaceManager}.
 */
public class NamespaceManager {

	/** */
	private static final ThreadLocal<String> NAMESPACE = new ThreadLocal<>();

	/**
	 * <p>Sets the default namespace for this thread. Similar to legacy GAE's {@code NamespaceManager}. While a namespace
	 * is set, all keys which are not created with an explicit namespace and all queries without an explicit
	 * namespace will inherit this value.</p>
	 *
	 * <p>To exit the namespace, call {@code close()} on the return value. This should be performed in a finally block,
	 * or even better with the a try-with-resources idiom:</p>
	 * {@code try (Closeable ignored = NamespaceManager.set("blah")) { ... }}
	 *
	 * <p>Note that this namespace affects key creation, but once a key has been created, it has an inherent namespace.</p>
	 *
	 * <pre>
	 *     final Key&lt;Foo&gt; key = Key.create(Foo.class, 123);
	 *
	 *     try (final Closeable ignored = NamespaceManager.set("blah")) {
	 *         ofy().load().key(key);  // The key already has the default namespace
	 *         ofy().load().type(Foo.class).id(123);	// Uses the 'blah' namespace
	 *         ofy().load().key(Key.create(Foo.class, 123));	// Uses the 'blah' namespace
	 *     }
	 * </pre>
	 *
	 * <p>You can call {@code set(null)} to clear the namespace; this is identical to calling {@code close()} on the return value.</p>
	 */
	public static Closeable set(final String namespaceName) {
		final String previous = NAMESPACE.get();
		NAMESPACE.set(namespaceName);
		return () -> NAMESPACE.set(previous);
	}

	/**
	 * @return the currently set default namespace, or null if one is not set
	 */
	public static String get() {
		return NAMESPACE.get();
	}
}
