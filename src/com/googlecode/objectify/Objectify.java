package com.googlecode.objectify;

import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>This interface is similar to DatastoreService, except that instead of working with
 * Entity you work with real typed objects.</p>
 * 
 * <p>Unlike DatastoreService, none of these methods take a Transaction as a parameter.
 * Instead, a transaction (or lack thereof) is associated with a particular instance of
 * this interface when you create it.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Objectify
{
	/**
	 * Performs a batch get, returning your typed objects.
	 * @param keys are the keys to fetch; you can mix and match the types of objects.
	 * @return a empty map if no keys are found in the datastore.
	 * @see DatastoreService#get(Iterable) 
	 */
	<T> Map<Key, T> get(Iterable<Key> keys);
	
	/**
	 * Gets one instance of your typed object.
	 * @throws EntityNotFoundException if the key does not exist in the datastore
	 * @see DatastoreService#get(Key) 
	 */
	<T> T get(Key key) throws EntityNotFoundException;
	
	/**
	 * This is a convenience method, shorthand for get(ObjectifyFactory.createKey(clazz, id)); 
	 * @throws EntityNotFoundException if the key does not exist in the datastore
	 */
	<T> T get(Class<T> clazz, long id) throws EntityNotFoundException;
	
	/**
	 * This is a convenience method, shorthand for get(ObjectifyFactory.createKey(clazz, name)); 
	 * @throws EntityNotFoundException if the key does not exist in the datastore
	 */
	<T> T get(Class<T> clazz, String name) throws EntityNotFoundException;
	
	/**
	 * Just like the DatastoreService method, but uses your typed object.
	 * If the object has a null key, one will be created.  If the object
	 * has a key, it will overwrite any value formerly stored with that key.
	 * @see DatastoreService#put(com.google.appengine.api.datastore.Entity) 
	 */
	Key put(Object obj);
	
	/**
	 * Just like the DatastoreService method, but uses your typed objects.
	 * If any of the objects have a null key, one will be created.  If any
	 * of the objects has a key, it will overwrite any value formerly stored
	 * with that key.  You can mix and match the types of objects stored.
	 * @see DatastoreService#put(Iterable) 
	 */
	List<Key> put(Iterable<?> objs);
	
	/**
	 * Deletes the entities with the specified Keys.  Same as DatastoreService method.
	 * 
	 * @see DatastoreService#delete(Key...)
	 */
	public void delete(Key... keys);

	/**
	 * Deletes the entities with the specified Keys.  Same as DatastoreService method.
	 * 
	 * @see DatastoreService#delete(Iterable)
	 */
	public void delete(Iterable<Key> keys);
	
	/**
	 * <p>Prepares a query for execution.  The resulting ObjPreparedQuery allows the result
	 * set to be iterated through in a typesafe way.</p>
	 * 
	 * <p>You should create a query by calling one of the {@code ObjectifyFactory.createQuery()}
	 * methods.</p>
	 * 
	 * <p>Note:  If Query is keysOnly, result will be ObjPreparedQuery<Key>.
	 * This behavior differs from how the underlying DatastoreService works.</p>
	 * 
	 * @see DatastoreService#prepare(Query)
	 */
	<T> ObjPreparedQuery<T> prepare(Query query);
	
	/**
	 * <p>Note that this is *not* the same as {@code DatastoreService.getCurrentTransaction()},
	 * which uses implicit transaction management.  Objectify does not use implicit (thread
	 * local) transactions.</p>
	 * 
	 * @return the transaction associated with this Objectify instance,
	 *  or null if no transaction is associated with this instance.
	 */
	public Transaction getTxn();

	/**
	 * @return the underlying DatastoreService implementation so you can work
	 *  with Entity objects if you so choose.  Also allows you to allocateIds
	 *  or examine thread local transactions.
	 */
	public DatastoreService getDatastore();
	
}