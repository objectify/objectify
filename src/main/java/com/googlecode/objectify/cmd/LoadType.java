package com.googlecode.objectify.cmd;



/**
 * Once you have narrowed your interest to a type (via {@code load().type(SomeType.class)}), the command
 * pattern can diverge into two directions:  Either defining a parent or ids (which corresponds to a
 * batch get) or calling query-related methods (which will produce a query).
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface LoadType<T> extends LoadIds<T>, Query<T>
{
	/**
	 * Define a parent for a get-by-key operation.  After this, you must define an id() or ids().
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param keyOrEntity - a Key<?>, datastore Key, or entity pojo of the relevant entity to use as the key parent
	 * @return the next immutable step in the command chain, which allows you to define ids.
	 */
	LoadIds<T> parent(Object keyOrEntity);
}
