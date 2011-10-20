package com.googlecode.objectify;

import java.util.Map;

import com.google.appengine.api.datastore.AsyncDatastoreService;

/**
 * <p>Provides asynchronous get/put/delete methods.  Behavior is identical to the synchronous
 * versions of these methods except that exceptions will be thrown when {@code Result.get()} is
 * called.</p>
 * 
 * <p>Note that there are no {@code query()} methods here.  This is because queries are already
 * inherently asynchronous; you can construct multiple iterators but the datastore will not block
 * until you call {@code Iterator.hasNext()} or {@code Iterator.next()} for the first time.</p>
 * 
 * <p>You can obtain an instance of this interface by calling {@code Objectify.async()}.  See
 * the javadocs of {@code Objectify} for method-level documentation.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface AsyncObjectify
{
	/**
	 * Get the synchronous version of Objectify.
	 */
	Objectify sync();
	
	/**
	 * @see Objectify#get(Iterable)
	 */
	<T> Result<Map<Key<T>, T>> get(Iterable<? extends Key<? extends T>> keys);
	
	/**
	 * @see Objectify#get(Key...)
	 */
	<T> Result<Map<Key<T>, T>> get(Key<? extends T>... keys);
	
	/**
	 * Note that the Result.get() method will throw NotFoundException if entity wasn't found
	 * @see Objectify#get(Key)
	 */
	<T> Result<T> get(Key<? extends T> key);
	
	/**
	 * Note that the Result.get() method will throw NotFoundException if entity wasn't found
	 * @see Objectify#get(Class, long)
	 */
	<T> Result<T> get(Class<? extends T> clazz, long id);
	
	/**
	 * Note that the Result.get() method will throw NotFoundException if entity wasn't found
	 * @see Objectify#get(Class, String)
	 */
	<T> Result<T> get(Class<? extends T> clazz, String name);
	
	/**
	 * @see Objectify#get(Class, Iterable)
	 */
	<S, T> Result<Map<S, T>> get(Class<? extends T> clazz, Iterable<S> idsOrNames);
	
	/**
	 * @see Objectify#get(Class, Object...)
	 */
	<S, T> Result<Map<S, T>> get(Class<? extends T> clazz, S... idsOrNames);
	
	/**
	 * @see Objectify#find(Key)
	 */ 
	<T> Result<T> find(Key<? extends T> key);
	
	/**
	 * @see Objectify#find(Class, long)
	 */ 
	<T> Result<T> find(Class<? extends T> clazz, long id);
	
	/**
	 * @see Objectify#find(Class, String)
	 */ 
	<T> Result<T> find(Class<? extends T> clazz, String name);

	/**
	 * @see Objectify#put(Object) 
	 */
	<T> Result<Key<T>> put(T obj);
	
	/**
	 * @see Objectify#put(Iterable) 
	 */
	<T> Result<Map<Key<T>, T>> put(Iterable<? extends T> objs);

	/**
	 * @see Objectify#put(Object...)
	 */
	<T> Result<Map<Key<T>, T>> put(T... objs);
	
	/**
	 * @see Objectify#delete(Object...)
	 */
	Result<Void> delete(Object... keysOrEntities);

	/**
	 * @see Objectify#delete(Iterable)
	 */
	Result<Void> delete(Iterable<?> keysOrEntities);

	/**
	 * @see Objectify#delete(Class, long)
	 */
	<T> Result<Void> delete(Class<T> clazz, long id);
	
	/**
	 * @see Objectify#delete(Class, String)
	 */
	<T> Result<Void> delete(Class<T> clazz, String name);
	
	/**
	 * Get the raw AsyncDatastoreService
	 */
	AsyncDatastoreService getAsyncDatastore();
}