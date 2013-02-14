package com.googlecode.objectify;

import java.io.Serializable;

/**
 * <p>We need to provide an alternate, stripped-down version of this so that we
 * can exclude the constructors that tie into non-GWT-safe code (the factories).</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Key<T> implements Serializable, Comparable<Key<?>>
{
	private static final long serialVersionUID = 2L;

	/** Key.create(key) is easier to type than new Key<Blah>(key) */
	public static <T> Key<T> create(com.google.appengine.api.datastore.Key raw) {
		if (raw == null)
			throw new NullPointerException("Cannot create a Key<?> from a null datastore Key");

		return new Key<T>(raw);
	}

	/** */
	protected com.google.appengine.api.datastore.Key raw;

	/** Cache the instance of the parent wrapper to avoid unnecessary garbage */
	transient protected Key<?> parent;

	/** For GWT serialization */
	protected Key() {}

	/** Wrap a raw Key */
	private Key(com.google.appengine.api.datastore.Key raw) {
		this.raw = raw;
	}

	/**
	 * @return the raw datastore version of this key
	 */
	public com.google.appengine.api.datastore.Key getRaw() {
		return this.raw;
	}

	/**
	 * @return the id associated with this key, or 0 if this key has a name.
	 */
	public long getId() {
		return this.raw.getId();
	}

	/**
	 * @return the name associated with this key, or null if this key has an id
	 */
	public String getName() {
		return this.raw.getName();
	}

	/**
	 * @return the low-level datastore kind associated with this Key
	 */
	public String getKind() {
		return this.raw.getKind();
	}

	/**
	 * @return the parent key, or null if there is no parent.  Note that
	 *  the parent could potentially have any type.
	 */
	@SuppressWarnings("unchecked")
	public <V> Key<V> getParent() {
		if (this.parent == null && this.raw.getParent() != null)
			this.parent = new Key<V>(this.raw.getParent());

		return (Key<V>)this.parent;
	}

	/**
	 * Gets the root of a parent graph of keys.  If a Key has no parent, it is the root.
	 *
	 * @return the topmost parent key, or this object itself if it is the root.
	 * Note that the root key could potentially have any type.
	 */
	@SuppressWarnings("unchecked")
	public <V> Key<V> getRoot() {
		if (this.getParent() == null)
			return (Key<V>)this;
		else
			return this.getParent().getRoot();
	}

	/**
	 * <p>Compares based on comparison of the raw key</p>
	 */
	@Override
	public int compareTo(Key<?> other) {
		return this.raw.compareTo(other.raw);
	}

	/** */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (!(obj instanceof Key<?>))
			return false;

		return this.compareTo((Key<?>)obj) == 0;
	}

	/** */
	@Override
	public int hashCode() {
		return this.raw.hashCode();
	}

	/** Creates a human-readable version of this key */
	@Override
	public String toString() {
		return "Key<?>(" + this.raw + ")";
	}

	/**
	 * Easy null-safe conversion of the raw key.
	 */
	public static <V> Key<V> key(com.google.appengine.api.datastore.Key raw) {
		if (raw == null)
			return null;
		else
			return new Key<V>(raw);
	}

	/**
	 * Easy null-safe conversion of the typed key.
	 */
	public static com.google.appengine.api.datastore.Key raw(Key<?> typed) {
		if (typed == null)
			return null;
		else
			return typed.getRaw();
	}
}