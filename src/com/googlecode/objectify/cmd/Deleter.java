package com.googlecode.objectify.cmd;

import com.googlecode.objectify.Result;


/**
 * <p>The top element in the command chain for deleting entities from the datastore.</p>
 * 
 * <p>You can delete entities by either passing in the POJO or their keys.  Note that deletes do NOT cascade;
 * you must delete each individual entity in an object graph.</p>
 * 
 * <p>The {@code type()} method allows you to construct keys fluently.</p>
 * 
 * <p>Note that all command objects are immutable.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Deleter
{
	/**
	 * Begin construction of a key or keys to delete by specifying a kind.
	 * 
	 * @param type is the kind of object to delete.
	 * @return the next step in the command chain where you specify a parent and/or ids.
	 */
	DeleteType type(Class<?> type);

	/**
	 * <p>Begin asynchronous deletion of a specific entity.</p>
	 * <p>To force synchronous delete, call now() on the returned Result.</p>
	 * 
	 * @param keyOrEntity can be any key-like structure; a Key<?>, a native datastore Key, or an entity object with valid id/parent fields. 
	 * @return an asynchronous Result.  Call now() to force synchronous deletion.
	 */
	Result<Void> key(Object keyOrEntity);

	/**
	 * <p>Begin asynchronous deletion of specific entities.</p>
	 * <p>To force synchronous delete, call now() on the returned Result.</p>
	 * 
	 * @param keyOrEntities can be any key-like structures; Key<?>s, native datastore Keys, or entity objects with valid id/parent fields. 
	 * @return an asynchronous Result.  Call now() to force synchronous deletion.
	 */
	Result<Void> keys(Iterable<?> keysOrEntities);
	
	/**
	 * Convenient substitute for entities(Iterable)
	 */
	Result<Void> keys(Object... keysOrEntities);
}
