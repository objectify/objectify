package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

/**
 * A Writer knows how to write part of an object graph to a datastore Entity.
 *
 * A Writer may be asked to operate in one of two ways:
 *
 * a) write a single property into the Entity.
 * b) write a list of values into a set of list-properties in the Entity
 */
public interface Writer
{
	/**
	 * Write into entity the value/values from "obj"
	 */
	void addtoEntity(Entity ent, Object obj) throws IllegalAccessException;

	/**
	 * Write into the list-properties in entity at index "i" the value/values from "obj"
	 */
	void addtoArray(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException;

	/**
	 * Called to give this Writer an opportunity to create the list-properties it needs in "entity"
	 */
	void initArray(ListPropertyMap entity);
}
