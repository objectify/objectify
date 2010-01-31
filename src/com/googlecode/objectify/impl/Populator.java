package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

/**
 * A Populator knows how to read a datastore Entity into part a of an object graph.
 *
 * A populator may be asked to operate in one of two ways:
 *
 * a) populate a single property into the field of an object
 * b) populate a list-property into each of the elements of a list
 */
public interface Populator
{
	/**
	 * Populate from the entity into the object in the holder.
	 *
	 * If this Populator has nothing to do, the destination object may not be
	 * {@link ObjectHolder#wasCreated() created}.
	 */
	public void populateIntoObject(Entity ent, ObjectHolder dest)
			throws InstantiationException, IllegalAccessException;

	/**
	 * Populate from a list property in the entity into a destination list.
	 */
	public void populateFromList(Entity ent, ListHolder dest)
			throws IllegalAccessException, InstantiationException;
}
