package com.googlecode.objectify.cmd;

import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;


/**
 * <p>The top element in the command chain for retrieving entities from the datastore.</p>
 * 
 * <p>At this point you can enable load groups with {@code group()}, start all-kinds
 * queries by calling query-related methods (see KindlessQuery), load entities by key or ref,
 * or narrow your interest to a specific kind by calling {@code type()}.</p>
 * 
 * <p>All command objects are immutable.</p>
 * 
 * @see KindlessQuery
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Loader extends SimpleQuery<Object>
{
	/**
	 * <p>Enables one or more fetch groups.  This will cause any entity fields (or Ref fields) which
	 * are annotated with @Load(XYZGroup.class) to be fetched along with your entities.  The class
	 * definition can be any arbitrary class, but inheritance is respected - if you have a
	 * {@code class Foo extends Bar}, then {@code group(Foo.class)} will cause loading of all {@code @Load(Bar.class)}
	 * properties.</p>
	 * 
	 * <p>Calling this method multiple times is the same as passing all the groups into one call.</p>
	 * 
	 * @param groups are one or more load groups to enable.  They can be any arbitrary class.
	 * @return a continuation of the immutable command pattern, enabled for fetching this group.
	 */
	Loader group(Class<?>... groups);
	
	/**
	 * <p>Restricts the find operation to entities of a particular type.  The type may be the
	 * base of a polymorphic class hierarchy.  This is optional.</p>
	 * 
	 * @param type is the type of entity (or entities) to retrieve, possibly a base class for a polymorphic hierarchy
	 * @return the next step in the immutable command chain, which allows you to start a query or define
	 *  keys for a batch get.
	 */
	<E> LoadType<E> type(Class<E> type);

	/**
	 * <p>Load a single entity ref.  This starts an asynchronous fetch operation which will be available as ref.get().</p>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown from {@code Ref<?>.get()}.</p>
	 * 
	 * @param ref holds the key to fetch and will receive the asynchronous result.  
	 */
	void ref(Ref<?> ref);
	
	/**
	 * <p>Load multiple refs in a batch operation.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown from {@code Ref<?>.get()}.</p>
	 * 
	 * @param refs provide the keys to fetch and will receive the asynchronous result.
	 */
	void refs(Iterable<? extends Ref<?>> refs);
	
	/**
	 * <p>A convenient substitute for refs(Iterable)</p>
	 */
	void refs(Ref<?>... refs);

	/**
	 * <p>Load a single entity by key.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown from {@code Ref<?>.get()}.</p>
	 * 
	 * @param key defines the entity to fetch
	 * @return a Ref<?> which holds the asynchronous result 
	 */
	<K> Ref<K> key(Key<K> key);

	/**
	 * <p>Load multiple entities by key from the datastore in a batch.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown when the Map is accessed.</p>
	 * 
	 * @param keys are the keys to fetch
	 * @return a Map of the asynchronous result.  The fetch will be completed when the Map is first accessed. 
	 */
	<K, E extends K> Map<Key<K>, E> keys(Iterable<Key<E>> keys);
	
	/**
	 * <p>A convenient substitute for keys(Iterable)</p>
	 */
	<K, E extends K> Map<Key<K>, E> keys(Key<E>... keys);

	/**
	 * <p>Load a single entity which has the same id/parent as the specified entity.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>This is typically used to retrieve "unfetched" entities which have been returned as fields in other entities.
	 * These unfetched entities will have their key fields (id/parent) filled but otherwise be uninitialized.</p>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown from {@code Ref<?>.get()}.</p>
	 * 
	 * @param entity defines the entity to fetch; it must be of a registered entity type and have valid id/parent fields.
	 * @return a Ref<?> which holds the asynchronous result 
	 */
	<K, E extends K> Ref<K> entity(E entity);
	
	/**
	 * <p>Load multiple entities from the datastore in a batch.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>This is typically used to retrieve "unfetched" entities which have been returned as fields in other entities.
	 * These unfetched entities will have their key fields (id/parent) filled but otherwise be uninitialized.</p>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown when the Map is accessed.</p>
	 * 
	 * @param entities must be a list of objects which belong to registered entity types, and must have id & parent fields
	 *  properly set.
	 * @return a Map of the asynchronous result.  The fetch will be completed when the Map is first accessed. 
	 */
	<K, E extends K> Map<Key<K>, E> entities(Iterable<E> entities);
	
	/**
	 * <p>A convenient substitute for entities(Iterable)</p>
	 */
	<K, E extends K> Map<Key<K>, E> entities(E... entities);

	/**
	 * <p>Load a single entity given any of a variety of acceptable key-like structures.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>The parameter can be any key-like structure, including:</p>
	 * 
	 * <ul>
	 * <li>Standard Objectify Key<?> objects.</li>
	 * <li>Native datastore Key objects.</li>
	 * <li>Ref<?> objects.</li>
	 * <li>Registered entity instances which have id and parent fields populated.  This is typically used to retrieve "unfetched"
	 * entities which have been returned as fields in other entities. These unfetched entities will have their key fields
	 * (id/parent) filled but otherwise be uninitialized.</li>
	 * </ul>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown from {@code Ref<?>.get()}.</p>
	 * 
	 * @param key defines the entity to fetch; can be anything that represents a key structure
	 * @return a Ref<?> which holds the asynchronous result 
	 */
	<K> Ref<K> value(Object key);

	/**
	 * <p>Fetch multiple entities from the datastore in a batch.  This starts an asynchronous fetch.</p>
	 * 
	 * <p>The parameters can be any mix of key-like structures, including:</p>
	 * 
	 * <ul>
	 * <li>Standard Objectify Key<?> objects.</li>
	 * <li>Native datastore Key objects.</li>
	 * <li>Ref<?> objects.</li>
	 * <li>Registered entity instances which have id and parent fields populated.  This is typically used to retrieve "unfetched"
	 * entities which have been returned as fields in other entities. These unfetched entities will have their key fields
	 * (id/parent) filled but otherwise be uninitialized.</li>
	 * </ul>
	 * 
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * will be thrown when the Map is accessed.</p>
	 * 
	 * @param keysOrEntities defines a possibly heterogeneous mixture of Key<?>, Ref<?>, native datastore Key, or registered
	 * entity instances with valid id/parent fields.
	 * @return a Map of the asynchronous result.  The fetch will be completed when the Map is first accessed. 
	 */
	<K, E extends K> Map<Key<K>, E> values(Iterable<?> keysOrEntities);
	
	/**
	 * <p>A convenient substitute for values(Iterable)</p>
	 */
	<K, E extends K> Map<Key<K>, E> values(Object... keysOrEntities);
	
	/**
	 * @return the parent Objectify instance (possibly the wrapper)
	 */
	public Objectify getObjectify();
	
	/**
	 * @return the currently enabled load groups in an unmodifiable list
	 */
	public Set<Class<?>> getLoadGroups();

	/**
	 * Sets the object instance that should be passed on by the base implementation in subsequent actions.
	 * You probably don't need to worry about this method; just subclass LoaderWrapper.
	 */
	void setWrapper(Loader loader);
}
