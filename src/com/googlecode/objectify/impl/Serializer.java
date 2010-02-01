package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

/**
 * A Serializer knows how to save a datastore Entity into part a of an object graph, and
 * to load that same part of an object graph back into an Entity.
 *
 * A populator may be asked to operate in one of two ways:
 *
 * a) load/save a single property into the field of an object
 * b) load/save a list-property into each of the elements of a list
 * 
 */
public interface Serializer
{
	/**
	 * Load from the entity into the object in the holder.
	 *
	 * If this implementation has nothing to do, the destination object may not be
	 * {@link ObjectHolder#wasCreated() created}.
	 */
	public void loadIntoObject(Entity ent, ObjectHolder dest)
			throws InstantiationException, IllegalAccessException;

	/**
	 * Load from a list property in the entity into a destination list.
	 */
	public void loadIntoList(Entity ent, ListHolder dest)
			throws IllegalAccessException, InstantiationException;

	/**
	 * save into entity the value/values from "obj"
	 */
	void saveObject(Entity ent, Object obj) throws IllegalAccessException;

	/**
	 * Save into the list-properties in entity at index "i" the value/values from "obj"
	 */
	void saveIntoList(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException;

	/**
	 * Called to give this implementation an opportunity to create the list-properties it needs in "entity"
	 */
	void prepareListForSave(ListPropertyMap entity);

	/**
	 * Call to check if the way this class has been configured confirms to all the rules.
	 */
	void verify();
}
