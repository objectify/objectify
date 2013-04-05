package com.googlecode.objectify.cmd;

import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


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
	 * @return a Ref that wraps the asynchronous Result of the fetch.
	 */
	Ref<T> id(long id);

	/**
	 * <p>Specify the String id of an entity and start asynchronous fetch.</p>
	 * 
	 * @param id - the id of the entity to fetch.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return a Ref that wraps the asynchronous Result of the fetch.
	 */
	Ref<T> id(String id);

	/**
	 * <p>Specify the Key of an entity and start asynchronous fetch.</p>
	 * 
	 * @param id - the id of the entity to fetch.  
	 * @return a Ref that wraps the asynchronous Result of the fetch.
	 */
	Ref<T> id(Key<T> id);
	
	/**
	 * <p>Specify the raw id of an entity and start asynchronous fetch.</p>
	 * 
	 * @param id - the id of the entity to fetch.  
	 * @return a Ref that wraps the asynchronous Result of the fetch.
	 */
	Ref<T> id(com.google.appengine.api.datastore.Key id);
	
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
	 * <p>Specify the Keys of multiple entities and start asynchronous fetch.</p>
	 * 
	 * @param ids - the ids of the entity to fetch.
	 * @return a Map of the asynchronous result.  The first method call on the Map will synchronously finish the call.
	 */

	Map<Key<T>, T> ids(Key<T>... ids);
	
	/**
	 * <p>Specify the raw ids of multiple entities and start asynchronous fetch.</p>
	 * 
	 * @param ids - the ids of the entity to fetch.
	 * @return a Map of the asynchronous result.  The first method call on the Map will synchronously finish the call.
	 */
	Map<com.google.appengine.api.datastore.Key, T> ids(com.google.appengine.api.datastore.Key... ids);
	
	/**
	 * <p>Specify the ids of multiple entities and start asynchronous fetch.</p>
	 * 
	 * @param ids - the ids of the entities to fetch.  The Iterator must provide Long or String.  Note that numeric ids and String ids are not equivalent; 123 and "123" are different ids.  
	 * @return a Map of the asynchronous result.  The first method call on the Map will synchronously finish the call.
	 */
	<S> Map<S, T> ids(Iterable<S> ids);
}
