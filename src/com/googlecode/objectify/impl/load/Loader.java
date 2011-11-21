package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.save.Path;

/**
 * <p>A loader knows how to load part (or all) of a datastore entity into a pojo.  Any
 * given Loader will be responsible for all sub-parts; at the top you have a Loader
 * for a root entity and it will be composed of loaders for all of its parts.</p>
 *
 * <p>Before the load process is started, the Entity must be broken down into an EntityNode
 * (which itself may be composed of EntityNodes).  This allows each Loader to process only
 * the piece of the Entity it cares about.</p>
 */
public interface Loader
{
	/**
	 * Loads a value into the part of a pojo that we are responsible for loading.
	 * 
	 * @param value might be a leaf value or might be a Map<String, Object>
	 * @param pojo might be a root pojo or it might be an embedded class.
	 * @param path is the entity path to this value, ie "field1.field2" for an embedded field1 
	 *  containing a field2 of the type of this class.  The root pathPrefix is null.
	 */
	public void load(EntityNode parent, Object pojo, Path path);
}
