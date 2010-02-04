package com.googlecode.objectify;


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
	 * Limit the fetched result set to a certain number of values.  The largest value
	 * allowed is 1000, reduced by the offset value.
	 * 
	 * @param value must be between 0 and 1000, inclusive.  A value of 0 indicates no limit.
	 */
	public Query<T> limit(int value);
	
	/**
	 * Starts the query results at a particular zero-based offset.  The appengine-wide limit of
	 * 1000 items must be taken into account; if you specify an offset of 999 you will get
	 * one item back (assuming there are 1000 items or more).
	 * 
	 * @param value must be >= 0
	 */
	public Query<T> offset(int value);
	
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
	 * Execute the query and get the results.  This method is provided for orthogonality;
	 * Query.fetch().iterator() is identical to Query.iterator().
	 */
	public Iterable<T> fetch();
	
	/**
	 * Execute the query and get the keys of the results.  This is more efficient than
	 * fetching the actual results.
	 */
	public Iterable<Key<T>> fetchKeys();
	
	/**
	 * <p>Count the total number of values in the result, <strong>ignoring <em>limit</em> and
	 * <em>offset</em>.</p>
	 * <p>This is somewhat faster than fetching, but the time still grows with the number
	 * of results.  The largest number returned will be 1000 as per GAE limits.</p>
	 */
	public int countAll();
}