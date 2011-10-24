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
	/** @return the datastore kind associated with this metadata */
	public String getKind();
	
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
	
	/**
	 * Gets the class associated with this entity. For concrete metadata it will be the actual class;
	 * for polymorphic metadata this will be the base class.
	 */
	public Class<T> getEntityClass(); 
}
