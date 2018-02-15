package com.googlecode.objectify.cmd;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.ReadOption;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;

import java.util.Map;
import java.util.Set;


/**
 * <p>The top element in the command chain for retrieving entities from the datastore.</p>
 *
 * <p>At this point you can enable load groups with {@code group()}, start all-kinds
 * queries by calling query-related methods (see SimpleQuery), load entities by key or ref,
 * or narrow your interest to a specific kind by calling {@code type()}.</p>
 *
 * <p>All command objects are immutable.</p>
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
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param groups are one or more load groups to enable.  They can be any arbitrary class.
	 * @return a continuation of the immutable command pattern, enabled for fetching this group.
	 */
	Loader group(Class<?>... groups);

	/**
	 * Enable the specified read option for this load. For example, {@code ReadOption.eventualConsistency()}
	 * Note that requests for eventual consistency will be ignored inside a transaction.
	 *
	 * @param option is defined by the Google Cloud SDK.
	 * @return a continuation of the immutable command pattern, with the specified option enabled
	 */
	Loader option(ReadOption... option);

	/**
	 * <p>Restricts the find operation to entities of a particular type.  The type may be the
	 * base of a polymorphic class hierarchy.  This is optional.</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param type is the type of entity (or entities) to retrieve, possibly a base class for a polymorphic hierarchy
	 * @return the next step in the immutable command chain, which allows you to start a query or define
	 *  keys for a batch get.
	 */
	<E> LoadType<E> type(Class<E> type);

	/**
	 * <p>Restricts the find operation to entities of a particular kind. This is similar to type()
	 * but lets you specify any arbitrary kind string. You'll typically only use this if you are
	 * also working with the low level api directly.</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param kind is the kind of entity (or entities) to retrieve
	 * @return the next step in the immutable command chain, which allows you to start a query or define
	 *  keys for a batch get.
	 */
	<E> LoadType<E> kind(String kind);

	/**
	 * <p>Load a single entity ref.  This starts an asynchronous fetch operation.</p>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * can be thrown from {@code Result} operations.</p>
	 *
	 * @param ref holds the key to fetch and will receive the asynchronous result.
	 * @return a result which can materialize the entity.
	 */
	<E> LoadResult<E> ref(Ref<E> ref);

	/**
	 * <p>Load multiple refs in a batch operation.  This starts an asynchronous fetch.</p>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * can be thrown from the map operations.</p>
	 *
	 * @param refs provide the keys to fetch and will receive the asynchronous result.
	 * @return as an alternative to accessing the Refs directly, a Map of the asynchronous result.
	 */
	<E> Map<Key<E>, E> refs(Iterable<Ref<E>> refs);

	/**
	 * <p>A convenient substitute for refs(Iterable)</p>
	 */
	<E> Map<Key<E>, E> refs(Ref<? extends E>... refs);

	/**
	 * <p>Load a single entity by key.  This starts an asynchronous fetch.</p>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * can be thrown from {@code Result} operations.</p>
	 *
	 * @param key defines the entity to fetch
	 * @return a result which can materialize the entity
	 */
	<E> LoadResult<E> key(Key<E> key);

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
	<E> Map<Key<E>, E> keys(Iterable<Key<E>> keys);

	/**
	 * <p>A convenient substitute for keys(Iterable)</p>
	 */
	<E> Map<Key<E>, E> keys(Key<? extends E>... keys);

	/**
	 * <p>Load a single entity which has the same id/parent as the specified entity.  This starts an asynchronous fetch.</p>
	 *
	 * <p>This is a shortcut for {@code key(Key.create(entity))}.</p>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * may be thrown from {@code Result} operations.</p>
	 *
	 * @param entity defines the entity to fetch; it must be of a registered entity type and have valid id/parent fields.
	 * @return a result which can materialize the entity
	 */
	<E> LoadResult<E> entity(E entity);

	/**
	 * <p>Load multiple entities from the datastore in a batch.  This starts an asynchronous fetch.</p>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * may be thrown when the Map is accessed.</p>
	 *
	 * @param entities must be a list of objects which belong to registered entity types, and must have id & parent fields
	 *  properly set.
	 * @return a Map of the asynchronous result.  The fetch will be completed when the Map is first accessed.
	 */
	<E> Map<Key<E>, E> entities(Iterable<E> entities);

	/**
	 * <p>A convenient substitute for entities(Iterable)</p>
	 */
	<E> Map<Key<E>, E> entities(E... entities);

	/**
	 * <p>Load a single entity given any of a variety of acceptable key-like structures.  This starts an asynchronous fetch.</p>
	 *
	 * <p>The parameter can be any key-like structure, including:</p>
	 *
	 * <ul>
	 * <li>Standard Objectify Key<?> objects.</li>
	 * <li>Native datastore Key objects.</li>
	 * <li>Ref<?> objects.</li>
	 * <li>Registered entity instances which have id and parent fields populated.</li>
	 * </ul>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * may be thrown from {@code Result} operations.</p>
	 *
	 * @param key defines the entity to fetch; can be anything that represents a key structure
	 * @return a Ref<?> which holds the asynchronous result
	 */
	<E> LoadResult<E> value(Object key);

	/**
	 * <p>Fetch multiple entities from the datastore in a batch.  This starts an asynchronous fetch.</p>
	 *
	 * <p>The parameters can be any mix of key-like structures, including:</p>
	 *
	 * <ul>
	 * <li>Standard Objectify Key<?> objects.</li>
	 * <li>Native datastore Key objects.</li>
	 * <li>Ref<?> objects.</li>
	 * <li>Registered entity instances which have id and parent fields populated.</li>
	 * </ul>
	 *
	 * <p>Since fetching is asynchronous,
	 * <a href="http://code.google.com/appengine/articles/handling_datastore_errors.html">datastore exceptions</a>
	 * ({@code DatastoreTimeoutException}, {@code ConcurrentModificationException}, {@code DatastoreFailureException})
	 * may be thrown when the Map is accessed.</p>
	 *
	 * @param keysOrEntities defines a possibly heterogeneous mixture of Key<?>, Ref<?>, native datastore Key, or registered
	 * entity instances with valid id/parent fields.
	 * @return a Map of the asynchronous result.  The fetch will be completed when the Map is first accessed.
	 */
	<E> Map<Key<E>, E> values(Iterable<?> keysOrEntities);

	/**
	 * <p>A convenient substitute for values(Iterable)</p>
	 */
	<E> Map<Key<E>, E> values(Object... keysOrEntities);

	/**
	 * @return the parent Objectify instance
	 */
	Objectify getObjectify();

	/**
	 * @return the currently enabled load groups in an unmodifiable list
	 */
	Set<Class<?>> getLoadGroups();

	/**
	 * Convert a native datastore Entity into a typed POJO.  This is like a load() operation except that you start with
	 * the native datastore type instead of fetching it from the datastore.  However, note that because of @Load annotations,
	 * it is possible that datastore operations will be executed during the translation.
	 *
	 * @param entity is a native datastore entity which has an appropriate kind registered in the ObjectifyFactory.
	 * @return the POJO equivalent, just as if you had loaded the entity directly from Objectify.
	 */
	<T> T fromEntity(Entity entity);

	/**
	 * Get the entity for a key immediately. You rarely, if ever, should want to use this; it exists to support
	 * Ref<?> behavior. Value will be loaded from the session if available, but will go to the datastore if necessary.
	 * It is synchronous.
	 */
	<E> E now(Key<E> key);
}
