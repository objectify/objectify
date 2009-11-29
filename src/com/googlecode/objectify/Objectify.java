package com.googlecode.objectify;

import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * This interface mimics DatastoreService, except that instead of working with
 * Entity you work with real typed objects.  However, many of the methods
 * (allocating ids, transactions, delete) are included on this interface
 * for convenience.
 */
public interface Objectify
{
	/**
	 * Just like the DatastoreService method, but returns your typed objects.
	 * @see DatastoreService#get(Iterable) 
	 */
	<T> Map<Key, T> get(Iterable<Key> keys);
	
	/**
	 * Just like the DatastoreService method, but returns your typed object.
	 * @see DatastoreService#get(Key) 
	 */
	<T> T get(Key key) throws EntityNotFoundException;
	
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
	 * with that key.
	 * @see DatastoreService#put(Iterable) 
	 */
	List<Key> put(Iterable<Object> objs);
	
	/**
	 * Prepares a query for execution.  Uses the same Query object as the
	 * native datastore.  The resulting ObjPreparedQuery allows the result
	 * set to be iterated through in a typesafe way.
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
	 *  with Entity objects if you so choose.
	 */
	public DatastoreService getDatastore();
	
	//
	//
	// The remaining methods simply pass through to the underlying DatastoreService
	// implementation, and are offered as a convenient shorthand.
	//
	//

	/** @see DatastoreService#allocateIds(String, long) */
	public KeyRange allocateIds(String kind, long num);

	/** @see DatastoreService#allocateIds(Key, String, long) */
	public KeyRange allocateIds(Key parent, String kind, long num);

	/** @see DatastoreService#delete(Key...) */
	public void delete(Key... keys);

	/** @see DatastoreService#delete(Iterable) */
	public void delete(Iterable<Key> keys);
}