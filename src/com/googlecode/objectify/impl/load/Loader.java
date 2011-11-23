package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;

/**
 * <p>A loader knows how to load a subtree of a datastore entity into a POJO object.</p>
 * 
 * <p>Loaders are composed of other loaders; through a chain of these a whole entity
 * object is assembled.</p>
 *
 * <p>Before the load process is started, the Entity must be broken down into an EntityNode
 * (which itself may be composed of EntityNodes).  This allows each Loader to process only
 * the piece of the Entity it cares about.</p>
 */
public interface Loader
{
	/**
	 * Loads the content of the specified node, returning the generated value.
	 * 
	 * @param node is the part of the entity tree we are transforming.
	 * @param ctx holds state information during an entity load.  
	 * @return an assembled pojo corresponding to the node subtree
	 */
	public Object load(EntityNode node, LoadContext ctx);
}
