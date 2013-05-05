package com.googlecode.objectify;

import java.io.Serializable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.NotFoundException;
import com.googlecode.objectify.impl.ref.DeadRef;

/**
 * <p>GWT emulation of the Ref<?> class. Not complete; there's a lot we can't do client-side.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Ref<T> implements Serializable, Comparable<Ref<T>>
{
	private static final long serialVersionUID = 1L;

	Key<T> key;
	T value;

	/** */
	public static <T> Ref<T> create(Key<T> key) {
		if (key == null)
			throw new NullPointerException("Cannot create a Ref from a null key");

		return new DeadRef<T>(key);
	}

	/** */
	public static <T> Ref<T> create(Key<T> key, T value) {
		return new DeadRef<T>(key, value);
	}

	/** Doesn't set the key! Dangerous. */
	public static <T> Ref<T> create(T value) {
		return new DeadRef<T>(value);
	}

	/** For GWT */
	protected Ref() {}

	/** */
	public Ref(Key<T> key) {
		this.key = key;
	}

	/** */
	public Ref(T value) {
		this.value = value;
	}

	/** */
	public Ref(Key<T> key, T value) {
		this.key = key;
		this.value = value;
	}

	/**
	 */
	public Key<T> key() {
		if (key == null)
			throw new IllegalStateException("This ref was created without a key, and we cannot determine keys on GWT client-side");

		return key;
	}

	/**
	 */
	public T get() {
		return value;
	}

	/**
	 */
	public T getValue() {
		return value;
	}

	/**
	 */
	final public Key<T> getKey() {
		return key();
	}

	/**
	 */
	final public Key<T> safeKey() {
		Key<T> k = this.key();
		if (k == null)
			throw new NotFoundException();
		else
			return k;
	}

	/**
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
		return "Ref<?>(key=" + key() + ", value=" + value + ")";
	}
}