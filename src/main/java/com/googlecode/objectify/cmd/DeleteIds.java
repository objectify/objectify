package com.googlecode.objectify.cmd;

import com.googlecode.objectify.Result;


/**
 * <p>Terminator methods for a delete-by-key command chain which constructs the key implicitly from
 * type, id, and (optionally) parent.</p>
 * 
 * <p>Deletes do NOT cascade; you must delete each individual entity in an object graph.</p>
 * 
 * <p>All command objects are immutable.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface DeleteIds
{
	/**
	 * <p>Specify the numeric id of an entity and start asynchronous deletion.</p>
	 * 
	 * @param id - the id of the entity to delete.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return an asynchronous Result of the deletion.  Force synchronous delete by calling Result.now().
	 */
	Result<Void> id(long id);

	/**
	 * <p>Specify the String id of an entity and start asynchronous deletion.</p>
	 * 
	 * @param id - the id of the entity to delete.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return an asynchronous Result of the deletion.  Force synchronous delete by calling Result.now().
	 */
	Result<Void> id(String id);

	/**
	 * <p>Specify the numeric ids of multiple entities and start asynchronous deletion.</p>
	 * 
	 * @param ids - the ids of the entity to delete.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return an asynchronous Result of the deletion.  Force synchronous delete by calling Result.now().
	 */
	Result<Void> ids(long... ids);

	/**
	 * <p>Specify the String ids of multiple entities and start asynchronous deletion.</p>
	 * 
	 * @param ids - the ids of the entity to delete.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return an asynchronous Result of the deletion.  Force synchronous delete by calling Result.now().
	 */
	Result<Void> ids(String... ids);

	/**
	 * <p>Specify the ids of multiple entities and start asynchronous deletion.</p>
	 * 
	 * @param ids - the ids of the entities to delete.  The Iterator must provide Long or String.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return an asynchronous Result of the deletion.  Force synchronous delete by calling Result.now().
	 */
	<S> Result<Void> ids(Iterable<S> ids);
}
