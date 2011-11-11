package com.googlecode.objectify;

import java.io.Serializable;

import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.annotation.Subclass;

/**
 * <p>A typesafe wrapper for the datastore Key object.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Scott Hernandez
 */
public class Key<T> implements Serializable, Comparable<Key<?>>
{
	private static final long serialVersionUID = 2L;
	
	/** Key.create(Blah.class, id) is easier to type than new Key<Blah>(Blah.class, id) */
	public static <T> Key<T> create(Class<? extends T> kindClass, long id) {
		return new Key<T>(kindClass, id);
	}

	/** Key.create(Blah.class, name) is easier to type than new Key<Blah>(Blah.class, name) */
	public static <T> Key<T> create(Class<? extends T> kindClass, String name) {
		return new Key<T>(kindClass, name);
	}

	/** Key.create(parent, Blah.class, id) is easier to type than new Key<Blah>(parent, Blah.class, id) */
	public static <T> Key<T> create(Key<?> parent, Class<? extends T> kindClass, long id) {
		return new Key<T>(parent, kindClass, id);
	}

	/** Key.create(parent, Blah.class, name) is easier to type than new Key<Blah>(parent, Blah.class, name) */
	public static <T> Key<T> create(Key<?> parent, Class<? extends T> kindClass, String name) {
		return new Key<T>(parent, kindClass, name);
	}

	/** Key.create(webSafeString) is easier to type than new Key<Blah>(webSafeString) */
	public static <T> Key<T> create(String webSafeString) {
		return new Key<T>(webSafeString);
	}
	
	/** */
	protected com.google.appengine.api.datastore.Key raw;
	
	/** Cache the instance of the parent wrapper to avoid unnecessary garbage */
	transient protected Key<?> parent;
	
	/** For GWT serialization */
	protected Key() {}

	/** Wrap a raw Key */
	public Key(com.google.appengine.api.datastore.Key raw)
	{
		this.raw = raw;
	}

	/** Create a key with a long id */
	public Key(Class<? extends T> kindClass, long id)
	{
		this(null, kindClass, id);
	}
	
	/** Create a key with a String name */
	public Key(Class<? extends T> kindClass, String name)
	{
		this(null, kindClass, name);
	}
	
	/** Create a key with a parent and a long id */
	public Key(Key<?> parent, Class<? extends T> kindClass, long id)
	{
		this.raw = KeyFactory.createKey(raw(parent), getKind(kindClass), id);
		this.parent = parent;
	}
	
	/** Create a key with a parent and a String name */
	public Key(Key<?> parent, Class<? extends T> kindClass, String name)
	{
		this.raw = KeyFactory.createKey(raw(parent), getKind(kindClass), name);
		this.parent = parent;
	}
	
	/**
	 * Reconstitute a Key from a web safe string.  This can be generated with getString()
	 * or KeyFactory.strongToKey().
	 */
	public Key(String webSafe)
	{
		this(KeyFactory.stringToKey(webSafe));
	}
	
	/**
	 * @return the raw datastore version of this key
	 */
	public com.google.appengine.api.datastore.Key getRaw()
	{
		return this.raw;
	}
	
	/**
	 * @return the id associated with this key, or 0 if this key has a name.
	 */
	public long getId()
	{
		return this.raw.getId();
	}
	
	/**
	 * @return the name associated with this key, or null if this key has an id
	 */
	public String getName()
	{
		return this.raw.getName();
	}
	
	/**
	 * @return the low-level datastore kind associated with this Key
	 */
	public String getKind()
	{
		return this.raw.getKind();
	}
	
	/**
	 * @return the parent key, or null if there is no parent.  Note that
	 *  the parent could potentially have any type. 
	 */
	@SuppressWarnings("unchecked")
	public <V> Key<V> getParent()
	{
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
	public <V> Key<V> getRoot()
	{
		if (this.getParent() == null)
			return (Key<V>)this;
		else
			return this.getParent().getRoot();
	}

	/**
	 * <p>Compares based on comparison of the raw key</p>
	 */
	@Override
	public int compareTo(Key<?> other)
	{
		return this.raw.compareTo(other.raw);
	}

	/** */
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		
		if (!(obj instanceof Key<?>))
			return false;
		
		return this.compareTo((Key<?>)obj) == 0;
	}

	/** */
	@Override
	public int hashCode()
	{
		return this.raw.hashCode();
	}

	/** Creates a human-readable version of this key */
	@Override
	public String toString()
	{
		return "Key<?>(" + this.raw + ")";
	}
	
	/**
	 * Call KeyFactory.keyToString() on the underlying Key.  You can reconstitute a Key<?> using the
	 * constructor that takes a websafe string.
	 */
	public String getString()
	{
		return KeyFactory.keyToString(this.raw);
	}
	
	/**
	 * Easy null-safe conversion of the raw key.
	 */
	public static <V> Key<V> typed(com.google.appengine.api.datastore.Key raw)
	{
		if (raw == null)
			return null;
		else
			return new Key<V>(raw);
	}
	
	/**
	 * Easy null-safe conversion of the typed key.
	 */
	public static com.google.appengine.api.datastore.Key raw(Key<?> typed)
	{
		if (typed == null)
			return null;
		else
			return typed.getRaw();
	}
	
	/**
	 * <p>Determines the kind for a Class, as understood by the datastore.  The logic for this
	 * is approximately:</p>
	 * 
	 * <ul>
	 * <li>If the class has an @Entity (either JPA or Objectify) annotation, the kind is the "name" attribute of the annotation.</li>
	 * <li>If the class has no @Entity, or the "name" attribute is empty, the kind is the simplename of the class.</li>
	 * <li>If the class has @Subclass, the kind is drawn from the first parent class that has an @Entity annotation.</li>
	 * </ul>
	 * 
	 * @throws IllegalArgumentException if a kind cannot be determined (ie @Subclass with invalid hierarchy).
	 */
	public static String getKind(Class<?> clazz)
	{
		// Check this one directly
		String kind = getKindHere(clazz);
		if (kind != null)
			return kind;
		
		// @Subclass is treated differently, a superclass must have a mandatory @Entity
		if (clazz.getAnnotation(Subclass.class) != null)
		{
			kind = getRequiredEntityKind(clazz.getSuperclass());
			if (kind != null)
				return kind;
			else
				throw new IllegalArgumentException("@Subclass entity " + clazz.getName() + " must have a superclass with @Entity");
		}
		
		return clazz.getSimpleName();
	}
	
	/**
	 * Recursively climbs the class hierarchy looking for the first @Entity annotation.
	 * @return the kind of the first @Entity found, or null if nothing can be found 
	 */
	private static String getRequiredEntityKind(Class<?> clazz)
	{
		if (clazz == Object.class)
			return null;
		
		String kind = getKindHere(clazz);
		if (kind != null)
			return kind;
		else
			return getRequiredEntityKind(clazz.getSuperclass());
	}

	/**
	 * Get the kind from the class if the class has an @Entity annotation, otherwise return null.
	 */
	private static String getKindHere(Class<?> clazz)
	{
		com.googlecode.objectify.annotation.Entity ourAnn = clazz.getAnnotation(com.googlecode.objectify.annotation.Entity.class);
		if (ourAnn != null)
			if (ourAnn.name() != null && ourAnn.name().length() != 0)
				return ourAnn.name();
			else
				return clazz.getSimpleName();
		
		javax.persistence.Entity jpaAnn = clazz.getAnnotation(javax.persistence.Entity.class);
		if (jpaAnn != null)
			if (jpaAnn.name() != null && jpaAnn.name().length() != 0)
				return jpaAnn.name();
			else
				return clazz.getSimpleName();
		
		return null;
	}
}