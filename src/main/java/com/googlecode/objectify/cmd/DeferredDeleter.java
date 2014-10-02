package com.googlecode.objectify.cmd;

import com.googlecode.objectify.Key;


/**
 * <p>Element in the command chain for deferred deleting entities from the datastore. Note that all methods return void;
 * there is no way to force synchronous execution.</p>
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
public interface DeferredDeleter
{
	/**
	 * Begin construction of a key or keys to delete by specifying a kind.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param type is the kind of object to delete.
	 * @return the next step in the immutable command chain where you specify a parent and/or ids.
	 */
	DeferredDeleteType type(Class<?> type);

	/**
	 * <p>Defer deletion of a specific entity.</p>
	 *
	 * @param key defines which entity to delete 
	 */
	void key(Key<?> key);

	/**
	 * <p>Defer deletion of specific entities.</p>
	 *
	 * @param keys defines which entities to delete 
	 */
	void keys(Iterable<? extends Key<?>> keys);
	
	/**
	 * Convenient substitute for keys(Iterable)
	 */
	void keys(Key<?>... keys);

	/**
	 * <p>Defer deletion of a specific entity.</p>
	 *
	 * @param entity can be an entity or any key-like structure; a Key<?>, a native datastore Key, or an entity object with valid id/parent fields. 
	 */
	void entity(Object entity);

	/**
	 * <p>Defer deletion of specific entities.</p>
	 *
	 * @param entities can be entity instances or any key-like structure; a Key<?>, a native datastore Key, or an entity object with valid id/parent fields. 
	 */
	void entities(Iterable<?> entities);
	
	/**
	 * Convenient substitute for entities(Iterable)
	 */
	void entities(Object... entities);
}
