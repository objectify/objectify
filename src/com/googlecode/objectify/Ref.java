package com.googlecode.objectify;

import java.io.Serializable;

import com.googlecode.objectify.impl.ref.StdRef;


/**
 * <p>Ref associates a Key<?> with an entity value.</p>
 * 
 * <p>Note that the methods might or might not throw runtime exceptions related to datastore operations;
 * ConcurrentModificationException, DatastoreTimeoutException, DatastoreFailureException, and DatastoreNeedIndexException.
 * Some Refs hide asynchronous operations that could throw these exceptions.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class Ref<T> implements Serializable, Comparable<Ref<T>>
{
	private static final long serialVersionUID = 1L;
	
	/** Key.create(Blah.class, id) is easier to type than new Key<Blah>(Blah.class, id) */
	public static <T> Ref<T> create(Key<T> key) {
		return new StdRef<T>(key);
	}

	/**
	 * @return the key associated with this Ref
	 */
	abstract public Key<T> key();
	
	/**
	 * Obtain the entity value associated with the key.
	 * 
	 * @return the entity referenced, or null if the entity was not found
	 * @throws IllegalStateException if the value has not been initialized
	 */
	abstract public T get();
	
	/**
	 * Nearly identical to get() but conforms to JavaBean conventions and returns null instead of
	 * throwing IllegalStateException if uninitialized.  This is convenient for use in a JSON
	 * converter or an expression language.
	 * 
	 * @return the entity referenced, or null if either the entity was not found or this Ref is uninitialized
	 */
	abstract public T getValue();
	
	/**
	 * Explicitly sets (or resets) the value of this Ref.
	 */
	abstract public void set(Result<T> value);
	
	/**
	 * Same as key() but conforms to JavaBeans conventions in case this is being processed by a JSON
	 * converter or expression language.
	 */
	final public Key<T> getKey() {
		return key();
	}
	
	/**
	 * Obtain the key if it has been found, throwing an exception if no value was found.
	 * 
	 * @return the key referenced
	 * @throws NotFoundException if the specified entity was not found
	 * @throws IllegalStateException if the value has not been initialized (say, through a database fetch)
	 */
	final public Key<T> safeKey() {
		Key<T> k = this.key();
		if (k == null)
			throw new NotFoundException();
		else
			return k;
	}

	/**
	 * Obtain the entity value if it has been initialized, throwing an exception if the entity was not found.
	 * 
	 * @return the entity referenced
	 * @throws NotFoundException if the specified entity was not found
	 * @throws IllegalStateException if the value has not been initialized (say, through a database fetch)
	 */
	final public T safeGet() {
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
	
	/** Equality comparison is based on key and nothing else */
	@Override
	public boolean equals(Object obj) {
		return obj != null && obj instanceof Ref && key().equals(((Ref<?>)obj).key());
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