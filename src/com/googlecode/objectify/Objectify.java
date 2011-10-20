package com.googlecode.objectify;

import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>This is the main "business end" of Objectify.  It lets you get(), put(), delete(),
 * and query() your typed POJO entities.</p>
 * 
 * <p>You can create an {@code Objectify} instance using {@code ObjectifyFactory.begin()}
 * or {@code ObjectifyFactory.beginTransaction()}.  A transaction (or lack thereof)
 * will be associated with the instance; by using multiple instances, you can interleave
 * calls between several different transactions.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Objectify
{
	/**
	 * <p>Performs a parallel batch get, returning your entities.  This is faster and
	 * more efficient than fetching entities one at a time.</p>
	 *
	 * <p>You can fetch entities of many different kinds in a single call.
	 * Entities not present in the datastore will be absent from the returned map.
	 * Otherwise, the iteration order of the result will match the order in the parameter.</p>
	 *
	 * @param keys are the keys to fetch; you can mix and match the types of objects.
	 * @return the keys that were found in the datastore, mapped to the related entity.
	 * The iteration order of the map will match the order of the <code>keys</code> argument.
	 * A empty map is returned if no keys are found in the datastore.
	 * 
	 * @see DatastoreService#get(Iterable)
	 */
	<T> Map<Key<T>, T> get(Iterable<? extends Key<? extends T>> keys);
	
	/**
	 * <p>Varargs version of get(Iterable)</p>
	 */
	<T> Map<Key<T>, T> get(Key<? extends T>... keys);
	
	/**
	 * <p>Gets one instance of your entity.</p>
	 * 
	 * @throws NotFoundException if the key does not exist in the datastore
	 * 
	 * @see DatastoreService#get(Key) 
	 */
	<T> T get(Key<? extends T> key) throws NotFoundException;
	
	/**
	 * <p>A convenience method, shorthand for creating a key and calling get()</p> 
	 * @throws NotFoundException if the key does not exist in the datastore
	 */
	<T> T get(Class<? extends T> clazz, long id) throws NotFoundException;
	
	/**
	 * <p>A convenience method, shorthand for creating a key and calling get()</p> 
	 * @throws NotFoundException if the key does not exist in the datastore
	 */
	<T> T get(Class<? extends T> clazz, String name) throws NotFoundException;
	
	/**
	 * <p>A convenience method that prevents you from having to assemble all the Keys
	 * yourself and calling {@code get(Iterable<Key>)}.</p>
	 * 
	 * <p>Note that unlike the standard batch get method, this method only gets a
	 * homogeneous set of objects.</p>
	 * 
	 * @param idsOrNames <b>must</b> be of type Iterable<Long> (which translates to id keys)
	 *  or of type Iterable<String> (which translates to name keys).
	 * @return a map of the id/name to the entity pojo.
	 * @throws IllegalArgumentException if ids is not Iterable<Long> or Iterable<String>
	 */
	<S, T> Map<S, T> get(Class<? extends T> clazz, Iterable<S> idsOrNames);
	
	/**
	 * Convenient varargs alias for get(Class<?>, Iterable<?>)
	 */
	<S, T> Map<S, T> get(Class<? extends T> clazz, S... idsOrNames);
	
	/** Same as {@code get(Key)} but returns null instead of throwing NotFoundException */ 
	<T> T find(Key<? extends T> key);
	
	/** Same as {@code get(Class, long)} but returns null instead of throwing NotFoundException */ 
	<T> T find(Class<? extends T> clazz, long id);
	
	/** Same as {@code get(Class, name)} but returns null instead of throwing NotFoundException */ 
	<T> T find(Class<? extends T> clazz, String name);

	/**
	 * <p>Puts an entity in the datastore.</p>
	 * 
	 * <p>If your entity has a null Long id, a fresh id will be generated and
	 * a new entity will be created in the database.  If your entity already
	 * has an id (either long, Long, or String) value, any existing entity
	 * in the datastore with that id will be overwritten.</p>
	 * 
	 * <p>Generated ids are stored in the entity itself.  If you put() an
	 * entity with a null Long id, it will be set before the method returns.</p>
	 * 
	 * @param obj must be an object of a registered entity type.
	 * @return the key associated with the object.
	 * 
	 * @see DatastoreService#put(com.google.appengine.api.datastore.Entity) 
	 */
	<T> Key<T> put(T obj);
	
	/**
	 * <p>Saves multiple entities to the datastore in a single parallel batch
	 * operation.</p>
	 * 
	 * <p>All the rules regarding generated ids in {@code put()} apply.</p>
	 * 
	 * <p>Note that the iteration order of the return value will be the same
	 * as the order of the parameter.</p>
	 * 
	 * @param objs must all be objects of registered entity type
	 * @return a map of the keys to the very same object instances passed in
	 * 
	 * @see DatastoreService#put(Iterable) 
	 */
	<T> Map<Key<T>, T> put(Iterable<? extends T> objs);

	/**
	 * Convenient varargs alias for put(Iterable<?>)
	 */
	<T> Map<Key<T>, T> put(T... objs);
	
	/**
	 * Deletes the specified entity.
	 * 
	 * @param keysOrEntities can be Key<?>s, datastore Keys, or pojo entities.
	 * If it includes entities, only the id fields are relevant.
	 */
	void delete(Object... keysOrEntities);

	/**
	 * Deletes the specified entities in a parallel batch operation.  This is faster
	 * and more efficient than deleting them one by one.
	 * 
	 * @param keysOrEntities can contain any mix of Key<?>, datastore Key, or pojo
	 * entities.  They need not be of the same type.  If a pojo is used, only its
	 * id fields are relevant.
	 * 
	 * @see DatastoreService#delete(Iterable)
	 */
	void delete(Iterable<?> keysOrEntities);

	/**
	 * A convenience method, shorthand for creating a key and deleting it. 
	 */
	<T> void delete(Class<T> clazz, long id);
	
	/**
	 * A convenience method, shorthand for creating a key and deleting it. 
	 */
	<T> void delete(Class<T> clazz, String name);

	/**
	 * <p>Create a typesafe query across all kinds of entities.</p>
	 */
	<T> Query<T> query();
	
	/**
	 * <p>Create a typesafe query across one specific kind of entity.</p>
	 */
	<T> Query<T> query(Class<T> clazz);
	
	/**
	 * <p>Get the underlying transaction object associated with this Objectify instance.</p>
	 * 
	 * <p>Note that this is *not* the same as {@code DatastoreService.getCurrentTransaction()},
	 * which uses implicit transaction management.  Objectify does not use implicit (thread
	 * local) transactions.</p>
	 * 
	 * @return the transaction associated with this Objectify instance,
	 *  or null if no transaction is associated with this instance.
	 */
	public Transaction getTxn();

	/**
	 * <p>Obtain a DatastoreService with parameters roughly equivalent to this Objectify instance.</p>
	 * 
	 * <p>This should not normally be necessary.  It allows you to work with
	 * raw Entity objects, allocate ids, and examine thread local transactions.</p>
	 * 
	 * <p>Note that Objectify does not actually use this DatastoreService in any way;
	 * all requests go through an AsyncDatastoreService.  Also, even Google's DatastoreService
	 * implementation is just a facade around AsyncDatastoreService.</p>
	 */
	public DatastoreService getDatastore();
	
	/**
	 * Obtain the ObjectifyFactory from which this Objectify instance was created.
	 * 
	 * @return the ObjectifyFactory associated with this Objectify instance.
	 */
	public ObjectifyFactory getFactory();

	/**
	 * Obtain the asynchronous version of the Objectify interface.  Provides async
	 * versions of get/put/delete calls.  Note that all queries are automatically
	 * asynchronous; just create multiple Iterators before iterating them.
	 */
	public AsyncObjectify async();
}
