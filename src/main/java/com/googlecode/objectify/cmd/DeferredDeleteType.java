package com.googlecode.objectify.cmd;



/**
 * <p>After a type is specified, the next step in a delete chain is to specify an optional parent
 * or a set of ids.  This is part of </p>
 * 
 * <p>Deletes do NOT cascade; you must delete each individual entity in an object graph.</p>
 * 
 * <p>All command objects are immutable.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface DeferredDeleteType extends DeferredDeleteIds
{
	/**
	 * Defines the parent part of a key (or set of keys) when building a delete request.
	 * After this you will define the id or ids to delete.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param keyOrEntity - a Key<?>, datastore Key, or pojo entity of the parent
	 * @return the next immutable step in the command build process
	 */
	DeferredDeleteIds parent(Object keyOrEntity);
}
