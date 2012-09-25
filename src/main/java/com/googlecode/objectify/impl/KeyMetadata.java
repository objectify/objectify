package com.googlecode.objectify.impl;

import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.impl.translate.LoadContext;


/**
 * Interface for mapping key/parent fields on pojo entities.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface KeyMetadata<T>
{
	/**
	 * Sets the key onto the POJO id/parent fields
	 */
	public void setKey(T pojo, com.google.appengine.api.datastore.Key key, LoadContext ctx);
	
	/** @return the datastore kind associated with this metadata */
	public String getKind();
	
	/**
	 * <p>This hides all the messiness of trying to create an Entity from an object that:</p>
	 * <ul>
	 * <li>Might have a long id, might have a String name</li>
	 * <li>If it's a Long id, might be null and require autogeneration</li>
	 * <li>Might have a parent key</li>
	 * </ul>
	 * 
	 * @return an empty Entity object whose key has been set but no other properties.
	 */
	public Entity initEntity(T pojo);

	/**
	 * Gets a key composed of the relevant id and parent fields in the object.
	 * 
	 * @param pojo must be of the entityClass type for this metadata.
	 * @throws IllegalArgumentException if pojo has a null id
	 */
	public com.google.appengine.api.datastore.Key getRawKey(T pojo);
		
	/** @return the name of the parent field, or null if there wasn't one */
	public String getParentFieldName();

	/** @return the name of the id field */
	public String getIdFieldName();
	
	/**
	 * @return true if the entity has a parent field
	 */
	public boolean hasParentField();
	
	/**
	 * @return true if the parent should be loaded given the enabled fetch groups
	 */
	public boolean shouldLoadParent(Set<Class<?>> enabledGroups);
	
	/**
	 * @return true if the id field is uppercase-Long, which can be genearted.
	 */
	public boolean isIdGeneratable();

	/**
	 * Sets the numeric id field
	 */
	public void setLongId(T pojo, Long id);
}
