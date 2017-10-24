package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Subclass;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Maintains information about registered entity classes<p>
 *
 * <p>There logic here is convoluted by polymorphic hierarchies.  Entity classes can
 * be registered in any particular order, requiring some considerable care.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Registrar
{
	/** Needed to obtain the converters */
	protected ObjectifyFactory fact;

	/** This maps kind to EntityMetadata */
	protected Map<String, EntityMetadata<?>> byKind = new HashMap<>();

	/** True if any @Cached entities have been registered */
	protected boolean cacheEnabled;

	/** @return true if any entities are cacheable */
	public boolean isCacheEnabled()
	{
		return this.cacheEnabled;
	}

	/**
	 * @param fact is so that the translations can be obtained
	 */
	public Registrar(ObjectifyFactory fact) {
		this.fact = fact;
	}

	/**
	 * <p>All @Entity and @Subclass classes (for both entity and embedded classes)
	 * must be registered before using Objectify to load or save data.  This method
	 * must be called in a single-threaded mode sometime around application initialization.</p>
	 *
	 * <p>Re-registering a class has no effect.</p>
	 *
	 * @param clazz must be annotated with either @Entity or @Subclass
	 */
	public <T> void register(Class<T> clazz) {
		// There are two possible cases
		// 1) This might be a simple class with @Entity
		// 2) This might be a class annotated with @Subclass

		// @Entity is inherited, but we only create entity metadata for the class with the @Entity declaration
		if (TypeUtils.isDeclaredAnnotationPresent(clazz, Entity.class)) {
			String kind = Key.getKind(clazz);

			// If we are already registered, ignore
			if (this.byKind.containsKey(kind))
				return;

			EntityMetadata<T> cmeta = new EntityMetadata<>(this.fact, clazz);
			this.byKind.put(kind, cmeta);

			if (cmeta.getCacheExpirySeconds() != null)
				this.cacheEnabled = true;
		}
		else if (clazz.isAnnotationPresent(Subclass.class)) {
			// We just need to make sure that a translator was created
			fact.getTranslators().getRoot(clazz);
		}
		else {
			throw new IllegalArgumentException(clazz + " must be annotated with either @Entity or @Subclass");
		}
	}

	/**
	 * @return the metadata for the specified kind, or null if there was nothing appropriate registered
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadata(String kind) {
		return (EntityMetadata<T>)this.byKind.get(kind);
	}

	/**
	 * @return the metadata for the specified class, or null if there was nothing appropriate registered
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadata(Class<T> clazz) {
		return getMetadata(Key.getKind(clazz));
	}

	/**
	 * Gets metadata for the specified kind
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadataSafe(String kind) throws IllegalArgumentException {
		EntityMetadata<T> metadata = this.getMetadata(kind);
		if (metadata == null)
			throw new IllegalArgumentException("No entity class has been registered which matches kind '" + kind + "'");
		else
			return metadata;
	}

	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public <T> EntityMetadata<T> getMetadataSafe(Class<T> clazz) throws IllegalArgumentException {
		EntityMetadata<T> metadata = this.getMetadata(clazz);
		if (metadata == null)
			throw new IllegalArgumentException("No class '" + clazz.getName() + "' was registered");
		else
			return metadata;
	}

}