package com.googlecode.objectify;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;


/**
 * <p>This is similar to the datastore Query object, but better understands
 * real class objects - it allows you to filter and sort by the key field
 * normally.</p>
 * 
 * <p>The methods of this class follow the GAE/Python Query class rather than
 * the GAE/Java Query class because the Python version is much more convenient
 * to use.  The Java version seems to have been designed for machines, not
 * humans.  You will appreciate the improvement.</p>
 * 
 * <p>Construct this class by calling {@code Objectify.query()}</p>
 * 
 * <p>Note that this class is Iterable; to get results, call iterator().</p>
 * 
 * <p>To obtain a {@code Cursor} call {@code Query.iterator().getCursor()}.
 * This cursor can be resumed with {@code Query.cursor()}.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Query<T> extends QueryResultIterable<T>
{
	/**
	 * <p>Create a filter based on the specified condition and value, using
	 * the same syntax as the GAE/Python query class. Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code filter("age >=", age)}</li>
	 * <li>{@code filter("age =", age)}</li>
	 * <li>{@code filter("age", age)} (if no operator, = is assumed)</li>
	 * <li>{@code filter("age !=", age)}</li>
	 * <li>{@code filter("age in", ageList)}</li>
	 * </ul>
	 * 
	 * <p><strong>The space is required.</strong>  Filtering a condition of
	 * {@code "age>="} will perform an <em>equality</em> test on an entity property
	 * with that exact name.  You can't create properties like this with Objectify,
	 * but you can with the Low-Level API.</p>
	 * 
	 * <p>See the Google documentation for 
	 * <a href="http://code.google.com/appengine/docs/java/datastore/queries.html#Introduction_to_Indexes">indexes</a>
	 * for an explanation of what you can and cannot filter for.</p>
	 * 
	 * <p>In addition to filtering on indexed properties, you can filter on @Id properties
	 * <strong>if</strong> this query is restricted to a Class<T> and the entity has no @Parent.  If you are
	 * having trouble working around this limitation, please consult the
	 * objectify-appengine google group.</p>
	 * <p>You can <strong>not</strong> filter on @Parent properties.  Use
	 * the {@code ancestor()} method instead.</p>
	 */
	public Query<T> filter(String condition, Object value);
	
	/**
	 * <p>Sorts based on a property.  Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code order("age")}</li>
	 * <li>{@code order("-age")} (descending sort)</li>
	 * </ul>
	 * 
	 * <p>You can sort on id properties <strong>if</strong> this query is
	 * restricted to a Class<T>.  Note that this is only important for
	 * descending sorting; default iteration is key-ascending.</p>
	 * <p>You can <strong>not</strong> sort on @Parent properties.</p>
	 */
	public Query<T> order(String condition);
	
	/**
	 * Restricts result set only to objects which have the given ancestor
	 * somewhere in the chain.  Doesn't need to be the immediate parent.
	 * 
	 * @param keyOrEntity can be a Key, a Key<T>, or an Objectify entity object.
	 */
	public Query<T> ancestor(Object keyOrEntity);
	
	/**
	 * Limit the fetched result set to a certain number of values.
	 * 
	 * @param value must be >= 0.  A value of 0 indicates no limit.
	 */
	public Query<T> limit(int value);
	
	/**
	 * Starts the query results at a particular zero-based offset.
	 * 
	 * @param value must be >= 0
	 */
	public Query<T> offset(int value);
	
	/**
	 * Starts query results at the specified Cursor.  You can obtain a Cursor from
	 * a QueryResultIterator by calling the getCursor() method.
	 * 
	 * Note that limit() and offset() are NOT encoded within a cursor; they operate
	 * on the results of the query after a cursor is established.
	 */
	public Query<T> startCursor(Cursor value);
	
	/**
	 * Ends query results at the specified Cursor.  You can obtain a Cursor from
	 * a QueryResultIterator by calling the getCursor() method.
	 * 
	 * Note that limit() and offset() are NOT encoded within a cursor; they operate
	 * on the results of the query after a cursor is established.
	 */
	public Query<T> endCursor(Cursor value);
	
	/**
	 * Sets the internal chunking strategy within the low-level API.  Affects
	 * performance only; the result set will be the same.
	 *  
	 * @param value must be > 0
	 */
	public Query<T> chunkSize(int value);
	
	/**
	 * Sets the number of results retreived on the first call to the datastore.  Affects
	 * performance only; the result set will be the same.
	 *  
	 * @param value must be >= 0
	 */
	public Query<T> prefetchSize(int value);
	
	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions of Objectify.</p>
	 * 
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p> 
	 */
	public String toString();
	
	/**
	 * Gets the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the only instance in the result, or null if the result set is empty.
	 */
	public T get();
	
	/**
	 * Get the key of the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return the key of the first instance in the result, or null if the result set is empty.
	 */
	public Key<T> getKey();
	
	/**
	 * Starts an asynchronous query.  While the Query itself is iterable, the datastore does not
	 * begin executing your query until iterator() or fetch() is called.  If you do not need
	 * to run multiple queries in parallel, this method is unnecessary; just iterate over the
	 * Query object itself.
	 */
	public QueryResultIterable<T> fetch();
	
	/**
	 * Prepares an Iterable that will obtain the keys of the results.  This is more efficient than
	 * fetching the actual results.  Note that every time iterator() is called on the Iterable,
	 * a fresh query is executed; calling this method does not cause a datastore operation.
	 */
	public QueryResultIterable<Key<T>> fetchKeys();
	
	/**
	 * Execute a keys-only query and then extract parent keys, returning them as a Set.
	 * 
	 * @throws IllegalStateException if any member of the query result does not have a parent. 
	 */
	public <V> Set<Key<V>> fetchParentKeys();
	
	/**
	 * Gets the parent keys and then fetches the actual entities.  This is the same
	 * as calling {@code ofy.get(query.fetchParentKeys())}.
	 * 
	 * @throws IllegalStateException if any member of the query result does not have a parent. 
	 */
	public <V> Map<Key<V>, V> fetchParents();
	
	/**
	 * <p>Count the total number of values in the result.  <em>limit</em> and <em>offset</em> are obeyed.</p>
	 * <p>This is somewhat faster than fetching, but the time still grows with the number of results.
	 * The datastore actually walks through the result set and counts for you.</p>
	 */
	public int count();

	/**
	 * <p>Execute the query and get the results as a List.  The list will be equivalent to a simple ArrayList;
	 * you can iterate through it multiple times without incurring additional datastore cost.</p>
	 * 
	 * <p>Note that you must be careful about limit()ing the size of the list returned; you can
	 * easily exceed the practical memory limits of Appengine by querying for a very large dataset.</p> 
	 */
	public List<T> list();
	
	/**
	 * <p>Execute a keys-only query and get the results as a List.  This is more efficient than
	 * fetching the actual results.</p>
	 * 
	 * <p>The size and scope considerations of list() apply; don't fetch more data than you
	 * can fit in a simple ArrayList.</p>
	 */
	public List<Key<T>> listKeys();
	
	/**
	 * @return a clone of this query object at its current state.  You can then modify
	 * the clone without modifying the original query.
	 */
	public Query<T> clone();
}