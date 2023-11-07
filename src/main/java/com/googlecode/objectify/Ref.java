package com.googlecode.objectify;


/**
 * <p>Ref<?> is a Key<?> which allows the entity value to be fetched directly.</p>
 *
 * <p>Note that the methods might or might not throw runtime exceptions related to datastore operations;
 * ConcurrentModificationException, DatastoreTimeoutException, DatastoreFailureException, and DatastoreNeedIndexException.
 * Some Refs hide datastore operations that could throw these exceptions.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Ref<T> implements Comparable<Ref<T>>
{
	/**
	 * A shortcut for {@code ObjectifyService.ref(key)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Ref<T> create(final Key<T> key) {
		return ObjectifyService.ref(key);
	}

	/**
	 * A shortcut for {@code ObjectifyService.ref(value)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Ref<T> create(final T value) {
		return ObjectifyService.ref(value);
	}

	/** The key associated with this ref */
	private final Key<T> key;

	/** The factory associated with this ref */
	private final ObjectifyFactory factory;

	/**
	 * Create a Ref based on the key, with the specified factory
	 */
	Ref(final Key<T> key, final ObjectifyFactory factory) {
		if (key == null)
			throw new NullPointerException("Cannot create a Ref for a null key");

		if (factory == null)
			throw new NullPointerException("Cannot create a Ref with a null factory");

		this.key = key;
		this.factory = factory;
	}

	/**
	 * @return the key associated with this Ref
	 */
	public Key<T> key() {
		return key;
	}

	/**
	 * Obtain the entity value associated with the key. Will pull from session if present, otherwise will
	 * fetch from the datastore.
	 *
	 * @return the entity referenced, or null if the entity was not found
	 */
	public T get() {
		return factory.ofy().load().now(key());
	}

	/**
	 * If an entity has been loaded into the session or is otherwise available, this will return true.
	 * Calls to get() will not require a trip to backing store.
	 * Note that even when loaded, get() can still return null if there is no entity which corresponds to the key.
	 *
	 * @return true if the value is in the session or otherwise immediately available; false if get() will
	 * require a trip to the datastore or memcache.
	 */
	public boolean isLoaded() {
		return factory.ofy().isLoaded(key());
	}

	/**
	 * This method exists to facilitate serialization via javabeans conventions. Unlike get(),
	 * it will return null if isLoaded() is false.
	 *
	 * @return the entity referenced, or null if either the entity was not found or isLoaded() is false.
	 */
	final public T getValue() {
		return isLoaded() ? get() : null;
	}

	/**
	 * Same as key() but conforms to JavaBeans conventions in case this is being processed by a JSON
	 * converter or expression language.
	 */
	final public Key<T> getKey() {
		return key();
	}

	/**
	 * Obtain the entity value, throwing an exception if the entity was not found.
	 *
	 * @return the entity referenced. Never returns null.
	 * @throws NotFoundException if the specified entity was not found
	 */
	final public T safe() throws NotFoundException {
		T t = this.get();
		if (t == null)
			throw new NotFoundException(key());
		else
			return t;
	}

	/** Comparison is based on key */
	@Override
	public int compareTo(final Ref<T> o) {
		return this.key().compareTo(o.key());
	}

	/** Equality comparison is based on key equivalence */
	@Override
	public boolean equals(final Object obj) {
		return obj instanceof Ref && key().equals(((Ref<?>) obj).key());
	}

	/** Type-safe comparison for key equivalence */
	public boolean equivalent(final Ref<T> other) {
		return equals(other);
	}

	/** Type safe comparison for key equivalence */
	public boolean equivalent(final Key<T> other) {
		return key().equivalent(other);
	}

	/** Hash code is simply that of key */
	@Override
	public int hashCode() {
		return key().hashCode();
	}

	/** Renders some info about the key */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + key() + ")";
	}
}