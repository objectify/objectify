package com.googlecode.objectify;

import com.google.appengine.api.datastore.PreparedQuery;

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
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Query<T> extends Iterable<T>
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
	 * <p>You can filter on id properties <strong>if</strong> this query is
	 * restricted to a Class<T> and the entity has no @Parent.  If you are
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
	 * <li>{@code sort("age")}</li>
	 * <li>{@code sort("-age")} (descending sort)</li>
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
	 * @param keyOrEntity can be an Key, a Key, or an Objectify entity object.
	 */
	public Query<T> ancestor(Object keyOrEntity);
	
	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions of Objectify.</p>
	 * 
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p> 
	 */
	public String toString();
	
	/**
	 * @return the only instance in the result, or null if the result set is empty.
	 * @throws PreparedQuery.TooManyResultsException if there are more than one result.
	 */
	public T get();
	
	/**
	 * @return the key of the only instance in the result, or null if the result set is empty.
	 * @throws PreparedQuery.TooManyResultsException if there are more than one result.
	 */
	public Key<T> getKey();
	
	/**
	 * Execute the query and get the results.  This method is provided for orthogonality;
	 * Query.fetch().iterator() is identical to Query.iterator().
	 */
	public Iterable<T> fetch();
	
	/**
	 * Execute the query and get the results.  The usual 1000-entry limit applies.
	 * @param limit is the max number of entities to return.
	 * @param offset is where to start; an offset of 0 is the first normal response.
	 */
	public Iterable<T> fetch(int limit, int offset);

	/**
	 * Execute the query and get the keys of the results.  This is more efficient than
	 * fetching the actual results.
	 */
	public Iterable<Key<T>> fetchKeys();
	
	/**
	 * Execute the query and get the keys of the results.  This is more efficient than
	 * fetching the actual results.  The usual 1000-entry limit applies.
	 * @param limit is the max number of keys to return.
	 * @param offset is where to start; an offset of 0 is the first normal result.
	 */
	public Iterable<Key<T>> fetchKeys(int limit, int offset);
	
	/**
	 * Count the number of values in the result.  This is somewhat faster than fetching,
	 * but the time still grows with the number of results.  The largest number returned
	 * will be 1000 as per GAE limits. 
	 */
	public int count();
}