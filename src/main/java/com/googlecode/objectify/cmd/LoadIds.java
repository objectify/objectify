package com.googlecode.objectify.cmd;

import com.googlecode.objectify.LoadResult;

import java.util.Map;


/**
 * <p>Terminator methods for a fetch-by-key command chain which constructs the key implicitly from
 * type, id, and (optionally) parent.</p>
 *
 * <p>All command objects are immutable.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface LoadIds<T>
{
	/**
	 * <p>Specify the numeric id of an entity and start asynchronous fetch.</p>
	 *
	 * @param id - the id of the entity to fetch.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.
	 * @return an asynchronous result that can materialize the entity
	 */
	LoadResult<T> id(long id);

	/**
	 * <p>Specify the String id of an entity and start asynchronous fetch.</p>
	 *
	 * @param id - the id of the entity to fetch.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.
	 * @return an asynchronous result that can materialize the entity
	 */
	LoadResult<T> id(String id);

	/**
	 * <p>Specify the numeric ids of multiple entities and start asynchronous fetch.</p>
	 *
	 * @param ids - the ids of the entity to fetch.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.
	 * @return a Map of the asynchronous result.  The first method call on the Map will synchronously finish the call.
	 */
	Map<Long, T> ids(Long... ids);

	/**
	 * <p>Specify the String ids of multiple entities and start asynchronous fetch.</p>
	 *
	 * @param ids - the ids of the entity to fetch.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.
	 * @return a Map of the asynchronous result.  The first method call on the Map will synchronously finish the call.
	 */
	Map<String, T> ids(String... ids);

	/**
	 * <p>Specify the ids of multiple entities and start asynchronous fetch.</p>
	 *
	 * @param ids - the ids of the entities to fetch.  The Iterator must provide Long or String.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.
	 * @return a Map of the asynchronous result.  The first method call on the Map will synchronously finish the call.
	 */
	<S> Map<S, T> ids(Iterable<S> ids);
}
