package com.googlecode.objectify.impl;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;



/**
 * <p>Holds static metadata about entity keys.  Since key information is defined by static annotations,
 * it's safe to maintain the metadata in a static context.  There is no synchronization; you are
 * expected to perform all registration at app startup.</p>
 *
 * <p>Note that this is an internal mechanism which shouldn't be relied upon by the public; use
 * Key.create(pojo) instead.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Keys
{
	/** This maps class to KeyMetadata for all registered classes */
	private static Map<Class<?>, KeyMetadata<?>> byClass = new HashMap<Class<?>, KeyMetadata<?>>();

	/** This maps kind to KeyMetadata for all registered classes */
	private static Map<String, KeyMetadata<?>> byKind = new HashMap<String, KeyMetadata<?>>();

	/**
	 * @return the Key<?> for a registered pojo entity.
	 */
	public static <T> Key<T> keyOf(T pojo) {
		return Key.create(rawKeyOf(pojo));
	}

	/**
	 * @return the native datastore key for a registered pojo entity.
	 */
	public static com.google.appengine.api.datastore.Key rawKeyOf(Object pojo) {
		return getMetadataSafe(pojo).getRawKey(pojo);
	}

	/**
	 * @return the metadata for a registerd pojo, or null if there is none
	 */
	@SuppressWarnings("unchecked")
	public static <T> KeyMetadata<T> getMetadata(T pojo) {
		return (KeyMetadata<T>)getMetadata(pojo.getClass());
	}

	/**
	 * @return the metadata for a registerd pojo, or null if there is none
	 */
	@SuppressWarnings("unchecked")
	public static <T> KeyMetadata<T> getMetadata(Class<T> clazz) {
		return (KeyMetadata<T>)byClass.get(clazz);
	}

	/**
	 * @return the metadata for a registerd pojo, or null if there is none
	 */
	public static <T> KeyMetadata<T> getMetadataSafe(Class<T> clazz) {
		KeyMetadata<T> meta = getMetadata(clazz);
		if (meta == null)
			throw new IllegalStateException(clazz + " has not been registered");
		else
			return meta;
	}

	/**
	 * @return the metadata for a registerd pojo, or throw exception if none
	 * @throws IllegalStateException if the pojo class has not been registered
	 */
	@SuppressWarnings("unchecked")
	public static <T> KeyMetadata<T> getMetadataSafe(T pojo) {
		return (KeyMetadata<T>)getMetadataSafe(pojo.getClass());
	}

	/**
	 * @return the metadata for a registerd pojo, or null if there is none
	 */
	@SuppressWarnings("unchecked")
	public static <T> KeyMetadata<T> getMetadata(Key<T> key) {
		return (KeyMetadata<T>)byKind.get(key.getKind());
	}

	/**
	 * <p>Gets the Key<T> given an object that might be a Key, Key<T>, or entity.</p>
	 *
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	@SuppressWarnings("unchecked")
	public static <T> Key<T> toKey(Object keyOrEntity) {

		if (keyOrEntity instanceof Key<?>)
			return (Key<T>)keyOrEntity;
		else if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return Key.create((com.google.appengine.api.datastore.Key)keyOrEntity);
		else if (keyOrEntity instanceof Ref)
			return ((Ref<T>)keyOrEntity).key();
		else
			return keyOf((T)keyOrEntity);
	}

	/**
	 * <p>Gets the raw datstore Key given an object that might be a Key, Key<T>, or entity.</p>
	 *
	 * @param keyOrEntity must be a Key, Key<T>, or registered entity.
	 * @throws NullPointerException if keyOrEntity is null
	 * @throws IllegalArgumentException if keyOrEntity is not a Key, Key<T>, or registered entity
	 */
	public static com.google.appengine.api.datastore.Key toRawKey(Object keyOrEntity) {

		if (keyOrEntity instanceof com.google.appengine.api.datastore.Key)
			return (com.google.appengine.api.datastore.Key)keyOrEntity;
		else if (keyOrEntity instanceof Key<?>)
			return ((Key<?>)keyOrEntity).getRaw();
		else if (keyOrEntity instanceof Ref)
			return ((Ref<?>)keyOrEntity).key().getRaw();
		else
			return rawKeyOf(keyOrEntity);
	}

	/**
	 * Register some key metadata.  This gets called by the Registrar.
	 */
	public static <T> void register(Class<T> clazz, KeyMetadata<T> meta) {
		byClass.put(clazz, meta);
		byKind.put(meta.getKind(), meta);
	}
}
