package com.googlecode.objectify;

import com.google.cloud.datastore.PathElement;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.util.KeyFormat;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Iterator;

/**
 * <p>A typesafe wrapper for the datastore Key object.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Scott Hernandez
 */
@EqualsAndHashCode(of="raw")
public class Key<T> implements Serializable, Comparable<Key<?>>
{
	private static final long serialVersionUID = -262390393952444121L;

	/**
	 * <p>Create an Objectify key from the native datastore key.</p>
	 *
	 * <p>This does not require a specific datastore connection
	 * so it can be a static method.</p>
	 */
	public static <T> Key<T> create(final com.google.cloud.datastore.Key raw) {
		if (raw == null)
			throw new NullPointerException("Cannot create a Key<?> from a null datastore Key");

		return new Key<>(raw);
	}

	/**
	 * <p>Create an Objectify key from a web safe string. Understands both 'modern' (Cloud SDK) and 'legacy' GAE formats
	 * (which always start with 'a'). These can be generated with toUrlSafeString() and toLegacyUrlSafeString(), respectively.</p>
	 *
	 * <p>This does not require a specific datastore connection
	 * so it can be a static method.</p>
	 */
	public static <T> Key<T> create(final String urlSafeString) {
		if (urlSafeString == null)
			throw new NullPointerException("Cannot create a Key<?> from a null String");

		return new Key<>(Keys.fromUrlSafe(urlSafeString));
	}

	/** This is an alias for Key.create(String). Helps with JAX-RS compliance. */
	public static <T> Key<T> valueOf(final String webSafeString) {
		return Key.create(webSafeString);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(kindClass, id)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final Class<? extends T> kindClass, final long id) {
		return ObjectifyService.key(kindClass, id);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(kindClass, id)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final Class<? extends T> kindClass, final String name) {
		return ObjectifyService.key(kindClass, name);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(parent, kindClass, id)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final Key<?> parent, final Class<? extends T> kindClass, final long id) {
		return ObjectifyService.key(parent, kindClass, id);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(parent, kindClass, name)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final Key<?> parent, final Class<? extends T> kindClass, final String name) {
		return ObjectifyService.key(parent, kindClass, name);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(namespace, kindClass, id)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final String namespace, final Class<? extends T> kindClass, final long id) {
		return ObjectifyService.key(namespace, kindClass, id);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(namespace, kindClass, name)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final String namespace, final Class<? extends T> kindClass, final String name) {
		return ObjectifyService.key(namespace, kindClass, name);
	}

	/**
	 * A shortcut for {@code ObjectifyService.key(pojo)}.
	 *
	 * @deprecated use the ObjectifyService method, or the relevant method on the appropriate ObjectifyFactory.
	 * This method will be removed in a future version of Objectify.
	 */
	@Deprecated
	public static <T> Key<T> create(final T pojo) {
		return ObjectifyService.key(pojo);
	}

	/** */
	private final com.google.cloud.datastore.Key raw;

	/** Wrap a raw Key */
	Key(final com.google.cloud.datastore.Key raw) {
		this.raw = raw;
	}

	/**
	 * @return the raw datastore version of this key
	 */
	public com.google.cloud.datastore.Key getRaw() {
		return this.raw;
	}

	/**
	 * @return the id associated with this key, or null if this key has a name.
	 */
	public Long getId() {
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
	 * @return the namespace associated with this key
	 */
	public String getNamespace() {
		return this.raw.getNamespace();
	}

	/**
	 * @return the parent key, or null if there is no parent.  Note that
	 *  the parent could potentially have any type.
	 */
	@SuppressWarnings("unchecked")
	public <V> Key<V> getParent() {
		return this.raw.getParent() == null ? null : new Key<>(this.raw.getParent());
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
	 * <p>The new cloud sdk Key doesn't have compareTo(), so we reimplement the logic from the old GAE SDK.</p>
	 */
	@Override
	public int compareTo(final Key<?> other) {
		if (this.raw == other.raw) {
			return 0;
		}

		{
			final int result = this.raw.getProjectId().compareTo(other.raw.getProjectId());
			if (result != 0)
				return result;
		}

		{
			final int result = this.raw.getNamespace().compareTo(other.raw.getNamespace());
			if (result != 0)
				return result;
		}

		{
			final int result = this.compareAncestors(other);
			if (result != 0)
				return result;
		}

		{
			// Too bad PathElement and Key don't share any kind of interface grrr
			final int result = this.getRaw().getKind().compareTo(other.getRaw().getKind());
			if (result != 0) {
				return result;
			}
			else if (this.raw.getNameOrId() == null && other.raw.getNameOrId() == null) {
				return compareToWithIdentityHash(this.raw, other.raw);
			}
			else if (this.raw.hasId()) {
				return other.raw.hasId() ? Long.compare(this.raw.getId(), other.raw.getId()) : -1;
			}
			else {
				return other.raw.hasId() ? 1 : this.raw.getName().compareTo(other.raw.getName());
			}
		}
	}

	private int compareAncestors(final Key<?> other) {
		final Iterator<PathElement> thisPath = this.raw.getAncestors().iterator();
		final Iterator<PathElement> otherPath = other.raw.getAncestors().iterator();

		int result;
		do {
			if (!thisPath.hasNext()) {
				return otherPath.hasNext() ? -1 : 0;
			}

			if (!otherPath.hasNext()) {
				return 1;
			}

			final PathElement thisKey = thisPath.next();
			final PathElement otherKey = otherPath.next();

			result = comparePathElements(thisKey, otherKey);
		} while (result == 0);

		return result;
	}

	private int comparePathElements(final PathElement here, final PathElement there) {
		final int result = here.getKind().compareTo(there.getKind());
		if (result != 0) {
			return result;
		}
		else if (here.getNameOrId() == null && there.getNameOrId() == null) {
			return compareToWithIdentityHash(here, there);
		}
		else if (here.hasId()) {
			return there.hasId() ? Long.compare(here.getId(), there.getId()) : -1;
		}
		else {
			return there.hasId() ? 1 : here.getName().compareTo(there.getName());
		}
	}

	/** I have no idea what this is about, it was in the old logic */
	private int compareToWithIdentityHash(final Object k1, final Object k2) {
		return Integer.compare(System.identityHashCode(k1), System.identityHashCode(k2));
	}

	/** A type-safe equivalence comparison */
	public boolean equivalent(final Key<T> other) {
		return equals(other);
	}

	/** A type-safe equivalence comparison */
	public boolean equivalent(final Ref<T> other) {
		return (other != null) && equals(other.key());
	}

	/** Creates a human-readable version of this key */
	@Override
	public String toString() {
		return "Key<?>(" + this.raw + ")";
	}

	/**
	 * Call toUrlSafe() on the underlying Key.  You can reconstitute a {@code Key<?>} using the
	 * constructor that takes a string. Note that toString() is only useful for debugging;
	 * it cannot be used to create a key with Key.create(String).
	 */
	public String toUrlSafe() {
		return this.raw.toUrlSafe();
	}

	/**
	 * Generates the string that would have been generated by the old appengine SDK.
	 * The strings look like 'ag1zfnZvb2Rvb2R5bmUwcgcLEgFCGAEM'. The String constructor
	 * for {@code Key<?>} understands both formats.
	 */
	public String toLegacyUrlSafe() {
		return KeyFormat.INSTANCE.formatOldStyleAppEngineKey(this.raw);
	}

	/**
	 * Easy null-safe conversion of the raw key.
	 */
	public static <V> Key<V> key(final com.google.cloud.datastore.Key raw) {
		if (raw == null)
			return null;
		else
			return new Key<>(raw);
	}

	/**
	 * Easy null-safe conversion of the typed key.
	 */
	public static com.google.cloud.datastore.Key key(final Key<?> typed) {
		if (typed == null)
			return null;
		else
			return typed.getRaw();
	}

	/**
	 * <p>Determines the kind for a Class, as understood by the datastore.  The first class in a
	 * hierarchy that has @Entity defines the kind (either explicitly or as that class' simplename).</p>
	 *
	 * <p>If no @Entity annotation is found, just uses the simplename as is.</p>
	 */
	public static String getKind(final Class<?> clazz) {
		final String kind = getKindRecursive(clazz);
		if (kind == null)
			return clazz.getSimpleName();
		else
			return kind;
	}

	/**
	 * <p>Recursively looks for the @Entity annotation.</p>
	 *
	 * @return null if kind cannot be found
	 */
	private static String getKindRecursive(final Class<?> clazz) {
		if (clazz == Object.class)
			return null;

		final String kind = getKindHere(clazz);
		if (kind != null)
			return kind;
		else
			return getKindRecursive(clazz.getSuperclass());
	}

	/**
	 * Get the kind from the class if the class has an @Entity annotation, otherwise return null.
	 */
	private static String getKindHere(final Class<?> clazz) {
		// @Entity is inherited so we have to be explicit about the declared annotations
		final Entity ourAnn = TypeUtils.getDeclaredAnnotation(clazz, Entity.class);
		if (ourAnn != null)
			if (!ourAnn.name().isEmpty())
				return ourAnn.name();
			else
				return clazz.getSimpleName();

		return null;
	}
}