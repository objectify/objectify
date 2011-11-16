package com.googlecode.objectify.cmd;

import java.util.List;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * The basic options for a Query.  Note that this does not include type().
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Query<T>
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
	 * Causes this query to return only key information (id and parent).  This is cheaper and
	 * more efficient than returning all entity data.
	 */
	public Query<T> keysOnly();
	
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
	public Ref<T> first();
	
	/**
	 * Starts an asynchronous query which will return entities.  If keysOnly() is set, the entities
	 * will only have their id/parent fields set.
	 */
	public QueryResultIterable<T> entities();
	
	/**
	 * Starts an asynchronous query which will return key objects.  Implies keysOnly(). 
	 */
	public QueryResultIterable<Key<T>> keys();
	
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
}
