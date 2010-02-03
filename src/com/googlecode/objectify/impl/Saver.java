package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

/**
 * <p>A saver knows how to get a value from a field and set it into a datastore Entity.</p>
 */
public interface Saver
{
	/**
	 * Saves one part of a pojo object graph into an entity.
	 * @param pojo might be a root pojo or it might be an embedded class.
	 */
	public void save(Object pojo, Entity entity);
}
