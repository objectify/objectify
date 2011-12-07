package com.googlecode.objectify.cmd;

import com.google.appengine.api.datastore.Cursor;
import com.googlecode.objectify.Ref;


/**
 * A restricted set of query operations that apply to both kindless queries and typed queries.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface SimpleQuery<T> extends QueryExecute<T>
{
	/**
	 * <p>Create a filter on the key of an entity. Examples:</p>
	 * 
	 * <ul>
	 * <li>{@code filterKey(">=", key)} (standard inequalities)</li>
	 * <li>{@code filterKey("=", key)} (wouldn't you rather do a load-by-key?)</li>
	 * <li>{@code filterKey("", key)} (if no operator, = is assumed)</li>
	 * <li>{@code filterKey("!=", key)}</li>
	 * <li>{@code filterKey("in", keyList)} (wouldn't you rather do a batch load-by-key?)</li>
	 * </ul>
	 * 
	 * <p>The key parameter can be anything key-ish; a Key<?>, a native datastore key, a Ref, a pojo entity, etc.</p>
	 * 
	 * <p>See the Google documentation for 
	 * <a href="http://code.google.com/appengine/docs/java/datastore/queries.html#Introduction_to_Indexes">indexes</a>
	 * for an explanation of what you can and cannot filter for.</p>
	 */
	public SimpleQuery<T> filterKey(String condition, Object value);
	
	/**
	 * Restricts result set only to objects which have the given ancestor
	 * somewhere in the chain.  Doesn't need to be the immediate parent.
	 * 
	 * @param keyOrEntity can be a Key, a Key<T>, or an Objectify entity object.
	 */
	public SimpleQuery<T> ancestor(Object keyOrEntity);
	
	/**
	 * Limit the fetched result set to a certain number of values.
	 * 
	 * @param value must be >= 0.  A value of 0 indicates no limit.
	 */
	public SimpleQuery<T> limit(int value);
	
	/**
	 * Starts the query results at a particular zero-based offset.  This can be extraordinarily
	 * expensive because each skipped entity is billed as a "minor datastore operation".  If you
	 * can, you probably want to use cursors instead.
	 * 
	 * @param value must be >= 0
	 */
	public SimpleQuery<T> offset(int value);
	
	/**
	 * Starts query results at the specified Cursor.  You can obtain a Cursor from
	 * a QueryResultIterator by calling the getCursor() method.
	 * 
	 * Note that limit() and offset() are NOT encoded within a cursor; they operate
	 * on the results of the query after a cursor is established.
	 */
	public SimpleQuery<T> startAt(Cursor value);
	
	/**
	 * Ends query results at the specified Cursor.  You can obtain a Cursor from
	 * a QueryResultIterator by calling the getCursor() method.
	 * 
	 * Note that limit() and offset() are NOT encoded within a cursor; they operate
	 * on the results of the query after a cursor is established.
	 */
	public SimpleQuery<T> endAt(Cursor value);
	
	/**
	 * Sets the internal chunking and prefetching strategy within the low-level API.  Affects
	 * performance only; the result set will be the same.
	 *  
	 * @param value must be >= 0
	 */
	public SimpleQuery<T> chunk(int value);
	
	/**
	 * Switches to a keys-only query.  Keys-only responses are billed as "minor datastore operations"
	 * which are significantly cheaper (~7X) than fetching whole entities.
	 */
	public QueryKeys<T> keys();
	
	/**
	 * <p>Count the total number of values in the result.  <em>limit</em> and <em>offset</em> are obeyed.
	 * This is somewhat faster than fetching, but the time still grows with the number of results.
	 * The datastore actually walks through the result set and counts for you.</p>
	 * 
	 * <p>Immediately executes the query; there is no async version of this method.</p>
	 * 
	 * <p>WARNING:  Each counted entity is billed as a "datastore minor operation".  Counting large numbers
	 * of entities can quickly create massive bills.</p>
	 */
	public int count();

	/**
	 * Gets the first entity in the result set.  Obeys the offset value.
	 * 
	 * @return an asynchronous Ref containing the first result.  The Ref will hold null if the result set is empty.
	 */
	public Ref<T> first();
	
	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions of Objectify.</p>
	 * 
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p> 
	 */
	public String toString();
}
