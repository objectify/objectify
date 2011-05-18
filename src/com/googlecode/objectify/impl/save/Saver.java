package com.googlecode.objectify.impl.save;

import com.google.appengine.api.datastore.Entity;

/**
 * <p>A saver knows how to save part (or all) of a pojo to a datastore entity.  Any
 * given Saver will be responsible for all sub-parts; at the top you have a Saver
 * for a root entity and it will be composed of savers for all of its parts.</p>
 *
 * <p>Keep in mind that Savers are NOT a parallel hierarchy to Setters.  They work
 * completely differently.</p>
 */
public interface Saver
{
	/**
	 * Saves the part of a pojo that we are responsible for to an entity.
	 * @param pojo might be a root pojo or it might be an embedded class.
	 * @param path is the entity path to this class, ie "field1.field2" for an embedded field1 
	 *             containing a field2 of the type of this class.  The root pathPrefix is null.
	 * @param index whether or not the parent thinks this value should be indexed when saved.
	 */
	public void save(Object pojo, Entity entity, Path path, boolean index);
}
