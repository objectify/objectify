package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;


/**
 * The interface by which POJOs and datastore Entity objects are translated back and forth.
 * Subclasses implement specific mapping, including polymorphic mapping.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface EntityMetadata<T>
{
	/** @return the datastore kind associated with this metadata */
	public String getKind();
	
	/**
	 * @param ent is the entity for which to get caching information
	 * @return the Cached instruction for this entity, or null if it should not be cached.
	 */
	public Cached getCached(Entity ent);
	
	/**
	 * This gets a little strange when dealing with polymorphism; if anything in the polymorphic group is cacheable,
	 * this will return true.  This doesn't necessarily mean that data will be written to the cache; that
	 * is strictly determined by the getCached() method.  But it determines whether or not we have to look
	 * since we don't know by Key whether or not something has a Cached annotation.  
	 *  
	 * @return true of things of this kind might be found in the cache.
	 */
	public boolean mightBeInCache();
	
	/**
	 * Converts an entity to an object of the appropriate type for this metadata structure.
	 * Does not check that the entity is appropriate; that should be done when choosing
	 * which EntityMetadata to call.
	 */
	public T toObject(Entity ent, Objectify ofy);


	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity toEntity(T pojo, Objectify ofy);

	/**
	 * Sets the relevant id and parent fields of the object to the values stored in the key.
	 * @param obj must be of the entityClass type for this metadata.
	 */
	public void setKey(T obj, com.google.appengine.api.datastore.Key key);

	/**
	 * Gets a key composed of the relevant id and parent fields in the object.
	 * 
	 * @param obj must be of the entityClass type for this metadata.
	 * @throws IllegalArgumentException if obj has a null id
	 */
	public com.google.appengine.api.datastore.Key getRawKey(Object obj);

	/**
	 * @return true if the property name corresponds to a Long/long @Id
	 *  field.  If the entity has a String name @Id, this will return false.
	 */
	public boolean isIdField(String propertyName);

	/**
	 * @return true if the property name corresponds to a String @Id
	 *  field.  If the entity has a Long/long @Id, this will return false.
	 */
	public boolean isNameField(String propertyName);

	/**
	 * @return true if the entity has a parent field
	 */
	public boolean hasParentField();
}
