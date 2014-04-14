package com.googlecode.objectify;

import com.googlecode.objectify.impl.ref.LiveRef;

import java.io.Serializable;


/**
 * <p>Ref<?> is a Key<?> which allows the entity value to be fetched directly.</p>
 *
 * <p>Note that the methods might or might not throw runtime exceptions related to datastore operations;
 * ConcurrentModificationException, DatastoreTimeoutException, DatastoreFailureException, and DatastoreNeedIndexException.
 * Some Refs hide datastore operations that could throw these exceptions.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class Ref<T> implements Serializable, Comparable<Ref<T>>
{
	private static final long serialVersionUID = 1L;

	/** Key.create(Blah.class, id) is easier to type than new Key<Blah>(Blah.class, id) */
	public static <T> Ref<T> create(Key<T> key) {
		if (key == null)
			throw new NullPointerException("Cannot create a Ref from a null key");

		return new LiveRef<>(key);
	}

	/** Creates a Ref from a registered pojo entity */
	public static <T> Ref<T> create(T value) {
		Key<T> key = Key.create(value);
		return create(key);
	}

	/** The key associated with this ref */
	protected Key<T> key;

	/** For GWT serialization */
	protected Ref() {}

	/**
	 * Create a Ref based on the key, with the specified session
	 */
	protected Ref(Key<T> key) {
		if (key == null)
			throw new NullPointerException("Cannot create a Ref for a null key");

		this.key = key;
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
	abstract public T get();

	/**
	 * If an entity has been loaded into the session or is otherwise available, this will return true.
	 * Calls to get() will not require a trip to backing store.
	 * Note that even when loaded, get() can still return null if there is no entity which corresponds to the key.
	 *
	 * @return true if the value is in the session or otherwise immediately available; false if get() will
	 * require a trip to the datastore or memcache.
	 */
	abstract public boolean isLoaded();

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
	public int compareTo(Ref<T> o) {
		return this.key().compareTo(o.key());
	}

	/** Equality comparison is based on key equivalence */
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof Ref && key().equals(((Ref<?>)obj).key());
	}

	/** Type-safe comparison for key equivalence */
	public boolean equivalent(Ref<T> other) {
		return equals(other);
	}

	/** Type safe comparison for key equivalence */
	public boolean equivalent(Key<T> other) {
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