package com.googlecode.objectify.cmd;

import com.google.cloud.datastore.Cursor;


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
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable query object that applies the filter
	 */
	SimpleQuery<T> filterKey(String condition, Object value);

	/**
	 * An alias for {@code filterKey("=", value)}
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable query object that applies the filter
	 */
	SimpleQuery<T> filterKey(Object value);

	/**
	 * Orders results by the key.
	 * @param descending if true, specifies a descending (aka reverse) sort
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable query object that applies the sort order
	 */
	SimpleQuery<T> orderKey(boolean descending);

	/**
	 * Restricts result set only to objects which have the given ancestor
	 * somewhere in the chain.  Doesn't need to be the immediate parent. The
	 * specified ancestor itself will be included in the result set (if it
	 * exists).
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param keyOrEntity can be a Key, a Key<T>, or an Objectify entity object.
	 * @return a new immutable query object that applies the ancestor filter
	 */
	SimpleQuery<T> ancestor(Object keyOrEntity);

	/**
	 * Limit the fetched result set to a certain number of values.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param value must be >= 0.  A value of 0 indicates no limit.
	 * @return a new immutable query object that applies the limit
	 */
	SimpleQuery<T> limit(int value);

	/**
	 * Starts the query results at a particular zero-based offset.  This can be extraordinarily
	 * expensive because each skipped entity is billed as a "minor datastore operation".  If you
	 * can, you probably want to use cursors instead.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param value must be >= 0
	 * @return a new immutable query object that applies the offset
	 */
	SimpleQuery<T> offset(int value);

	/**
	 * Starts query results at the specified Cursor.  You can obtain a Cursor from
	 * a QueryResultIterator by calling the getCursor() method.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * Note that limit() and offset() are NOT encoded within a cursor; they operate
	 * on the results of the query after a cursor is established.
	 *
	 * @return a new immutable query object that applies the cursor
	 */
	SimpleQuery<T> startAt(Cursor value);

	/**
	 * Ends query results at the specified Cursor.  You can obtain a Cursor from
	 * a QueryResultIterator by calling the getCursor() method.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * Note that limit() and offset() are NOT encoded within a cursor; they operate
	 * on the results of the query after a cursor is established.
	 *
	 * @return a new immutable query object that applies the cursor
	 */
	SimpleQuery<T> endAt(Cursor value);

	/**
	 * Sets the internal chunking and prefetching strategy within the low-level API.  Affects
	 * performance only; the result set will be the same.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param value must be >= 0
	 * @return a new immutable query object that applies the chunk size
	 */
	SimpleQuery<T> chunk(int value);

	/**
	 * <p>Sets the internal chunking and prefetching strategy within the low-level API to attempt to get all
	 * results at once.  Affects performance only; the result set will be the same.</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * <p>Same as chunk(Integer.MAX_VALUE).</p>
	 *
	 * @return a new immutable query object that applies the chunk size
	 */
	SimpleQuery<T> chunkAll();

	/**
	 * <p>Converts this query into a <a href="https://developers.google.com/appengine/docs/java/datastore/projectionqueries">projection query</a>.
	 * Projection queries allow values to be selected directly out of an index rather than loading the whole entity. While this allows
	 * data to be fetched quickly and cheaply, it is limited to selecting data that exists in an index.</p>
	 *
	 * <p>Entities returned from projection queries are NOT kept in the session cache. However, @Load annotations are
	 * processed normally.</p>
	 *
	 * <p>This method can be called more than once; it will have the same effect as passing all the fields
	 * in to a single call.</p>
	 *
	 * @param fields is one or more field names
	 * @return a new immutable query object that projects the specified fields
	 */
	SimpleQuery<T> project(String... fields);

	/**
	 * Determines whether this is a SELECT DISTINCT query.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable query object that applies the distinct operator
	 */
	SimpleQuery<T> distinct(boolean value);
	
	/**
	 * <p>This method forces Objectify to (or not to) hybridize the query into a keys-only fetch plus batch get.</p>
	 *
	 * <p>If Objectify knows you are fetching an entity type that can be cached, it automatically converts
	 * queries into a "hybrid" of keys-only query followed by a batch fetch of the keys.  This is cheaper (keys-only
	 * results are 1/7th the price of a full fetch) and, if the cache hits, significantly faster.  However,
	 * there are some circumstances in which you may wish to force behavior one way or another:</p>
	 *
	 * <ul>
	 * <li>Issuing a kindless query (which Objectify will not auto-hybridize) when you know a significant portion
	 * of the result set is cacheable.</li>
	 * <li>Some exotic queries cannot be made keys-only, and produce an exception from the Low-Level API when you
	 * try to execute the query.  Objectify tries to detect these cases but since the underlying implementation
	 * may change, you may need to force hybridization off in some cases.</li>
	 * <li>Hybrid queries have a slightly different consistency model than normal queries.  You may wish to
	 * force hybridization off to ensure a weakly consistent query (see below).</li>
	 * </ul>
	 *
	 * <p>For the most part, hybridization only affects the performance and cost of queries.  However, it is possible
	 * for hybridization to alter the content of the result set.  In the HRD, queries
	 * always have EVENTUAL consistency but get operations may have either STRONG or EVENTUAL consistency depending
	 * on the read policy (see {@code Objectify.consistency()}.  The keys-only query results will always be EVENTUAL and may
	 * return data that has been deleted, but Objectify will exclude these results when the more-current batch get fails to return
	 * data.  So the count of the returned data may actually be less than the limit(), even if there are plenty of
	 * actual results.  {@code hybrid(false)} will prevent this behavior.</p>
	 *
	 * <p>Note that in hybrid queries, the chunk size defines the batch size.</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable query object that forces hybridization on or off
	 */
	SimpleQuery<T> hybrid(boolean force);

	/**
	 * Switches to a keys-only query.  Keys-only responses are billed as "minor datastore operations"
	 * which are faster and free compared to fetching whole entities.
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable query object that returns keys rather than whole entities
	 */
	QueryKeys<T> keys();

	/**
	 * <p>Count the total number of values in the result.  <em>limit</em> and <em>offset</em> are obeyed.
	 * This is somewhat faster than fetching, but the time still grows with the number of results.
	 * The datastore actually walks through the result set and counts for you.</p>
	 *
	 * <p>Immediately executes the query; there is no async version of this method.</p>
	 *
	 * <p>WARNING:  Each counted entity is billed as a "datastore minor operation".  Even though these
	 * are free, they may take significant time because they require an index walk.</p>
	 */
	int count();

	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions of Objectify.</p>
	 *
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p>
	 */
	String toString();
}
