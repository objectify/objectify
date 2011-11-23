package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;


/**
 * The interface by which POJOs and datastore Entity objects are translated back and forth.
 * Subclasses implement specific mapping, including polymorphic mapping.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface EntityMetadata<T>
{
	/**
	 * Get the expiry associated with this kind, defined by the @Cached annotation.
	 * For polymorphic types, this is always the instruction on the root @Entity - you
	 * cannot provide per-type caching.
	 *  
	 * @return null means DO NOT CACHE, 0 means "no limit", otherwise # of seconds
	 */
	public Integer getCacheExpirySeconds();
	
	/**
	 * Converts an entity to an object of the appropriate type for this metadata structure.
	 * Does not check that the entity is appropriate; that should be done when choosing
	 * which EntityMetadata to call.
	 */
	public T load(Entity ent, Objectify ofy);

	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity save(T pojo, Objectify ofy);

	/**
	 * Gets the class associated with this entity.
	 */
	public Class<T> getEntityClass(); 
	
	/**
	 * Get specific metadata about the key for this type.
	 */
	public KeyMetadata<T> getKeyMetadata();
}
