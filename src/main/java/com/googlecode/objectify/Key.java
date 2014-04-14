package com.googlecode.objectify;

import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.impl.TypeUtils;

import java.io.Serializable;

/**
 * <p>A typesafe wrapper for the datastore Key object.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Scott Hernandez
 */
public class Key<T> implements Serializable, Comparable<Key<?>>
{
	private static final long serialVersionUID = 2L;

	/** Key.create(key) is easier to type than new Key<Blah>(key) */
	public static <T> Key<T> create(com.google.appengine.api.datastore.Key raw) {
		if (raw == null)
			throw new NullPointerException("Cannot create a Key<?> from a null datastore Key");

		return new Key<>(raw);
	}

	/** Key.create(Blah.class, id) is easier to type than new Key<Blah>(Blah.class, id) */
	public static <T> Key<T> create(Class<? extends T> kindClass, long id) {
		return new Key<>(kindClass, id);
	}

	/** Key.create(Blah.class, name) is easier to type than new Key<Blah>(Blah.class, name) */
	public static <T> Key<T> create(Class<? extends T> kindClass, String name) {
		return new Key<>(kindClass, name);
	}

	/** Key.create(parent, Blah.class, id) is easier to type than new Key<Blah>(parent, Blah.class, id) */
	public static <T> Key<T> create(Key<?> parent, Class<? extends T> kindClass, long id) {
		return new Key<>(parent, kindClass, id);
	}

	/** Key.create(parent, Blah.class, name) is easier to type than new Key<Blah>(parent, Blah.class, name) */
	public static <T> Key<T> create(Key<?> parent, Class<? extends T> kindClass, String name) {
		return new Key<>(parent, kindClass, name);
	}

	/** Key.create(webSafeString) is easier to type than new Key<Blah>(webSafeString) */
	public static <T> Key<T> create(String webSafeString) {
		if (webSafeString == null)
			throw new NullPointerException("Cannot create a Key<?> from a null String");

		return new Key<>(webSafeString);
	}

	/** This is an alias for Key.create(String) which exists for JAX-RS compliance. */
	public static <T> Key<T> valueOf(String webSafeString) {
		return Key.create(webSafeString);
	}

	/** Create a key from a registered POJO entity. */
	public static <T> Key<T> create(T pojo) {
		return ObjectifyService.factory().keys().keyOf(pojo);
	}

	/** */
	protected com.google.appengine.api.datastore.Key raw;

	/** Cache the instance of the parent wrapper to avoid unnecessary garbage */
	transient protected Key<?> parent;

	/** For GWT serialization */
	private Key() {}

	/** Wrap a raw Key */
	private Key(com.google.appengine.api.datastore.Key raw) {
		this.raw = raw;
	}

	/** Create a key with a long id */
	private Key(Class<? extends T> kindClass, long id) {
		this(null, kindClass, id);
	}

	/** Create a key with a String name */
	private Key(Class<? extends T> kindClass, String name) {
		this(null, kindClass, name);
	}

	/** Create a key with a parent and a long id */
	private Key(Key<?> parent, Class<? extends T> kindClass, long id) {
		this.raw = KeyFactory.createKey(key(parent), getKind(kindClass), id);
		this.parent = parent;
	}

	/** Create a key with a parent and a String name */
	private Key(Key<?> parent, Class<? extends T> kindClass, String name) {
		this.raw = KeyFactory.createKey(key(parent), getKind(kindClass), name);
		this.parent = parent;
	}

	/**
	 * Reconstitute a Key from a web safe string.  This can be generated with getString()
	 * or KeyFactory.strongToKey().
	 */
	private Key(String webSafe) {
		this(KeyFactory.stringToKey(webSafe));
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

	/** A type-safe equivalence comparison */
	public boolean equivalent(Key<T> other) {
		return equals(other);
	}

	/** A type-safe equivalence comparison */
	public boolean equivalent(Ref<T> other) {
		return (other != null) && equals(other.key());
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
	 * Call KeyFactory.keyToString() on the underlying Key.  You can reconstitute a Key<?> using the
	 * constructor that takes a websafe string.
	 */
	public String getString() {
		return KeyFactory.keyToString(this.raw);
	}

	/**
	 * Easy null-safe conversion of the raw key.
	 */
	public static <V> Key<V> key(com.google.appengine.api.datastore.Key raw) {
		if (raw == null)
			return null;
		else
			return new Key<>(raw);
	}

	/**
	 * Easy null-safe conversion of the typed key.
	 */
	public static com.google.appengine.api.datastore.Key key(Key<?> typed) {
		if (typed == null)
			return null;
		else
			return typed.getRaw();
	}

	/**
	 * <p>Determines the kind for a Class, as understood by the datastore.  The first class in a
	 * hierarchy that has @Entity defines the kind (either explicitly or as that class' simplename).</p>
	 *
	 * @throws IllegalArgumentException if a kind cannot be determined (ie no @Entity in hierarchy).
	 */
	public static String getKind(Class<?> clazz) {
		String kind = getKindRecursive(clazz);
		if (kind == null)
			throw new IllegalArgumentException("Class hierarchy for " + clazz + " has no @Entity annotation");
		else
			return kind;
	}

	/**
	 * <p>Recursively looks for the @Entity annotation.</p>
	 *
	 * @return null if kind cannot be found
	 */
	private static String getKindRecursive(Class<?> clazz) {
		if (clazz == Object.class)
			return null;

		String kind = getKindHere(clazz);
		if (kind != null)
			return kind;
		else
			return getKindRecursive(clazz.getSuperclass());
	}

	/**
	 * Get the kind from the class if the class has an @Entity annotation, otherwise return null.
	 */
	private static String getKindHere(Class<?> clazz) {
		// @Entity is inherited so we have to be explicit about the declared annotations
		Entity ourAnn = TypeUtils.getDeclaredAnnotation(clazz, Entity.class);
		if (ourAnn != null)
			if (ourAnn.name() != null && ourAnn.name().length() != 0)
				return ourAnn.name();
			else
				return clazz.getSimpleName();

		return null;
	}
}