package com.googlecode.objectify.cmd;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.StructuredQuery.Filter;


/**
 * The basic options for a typed Query.  In addition to adding a few methods that are only available for typed
 * queries, this interface overrides the QueryCommon methods to return the full Query<T>.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Query<T> extends SimpleQuery<T>
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
	 * </ul>
	 * 
	 * <p><strong>The space between the property name and the operator is required.</strong>
	 * Filtering a condition of {@code "age>="} will perform an <em>equality</em> test on an entity
	 * property exactly named "age>=".  You can't create properties like this with Objectify, but you
	 * can with the Low-Level API.</p>
	 *
	 * <p>Multiple calls to filter() will produce an AND (intersection) query.</p></p>
	 *
	 * <p>{@code ==} is an alias of {@code =}, {@code <>} is an alias of {@code !=}.</p>
	 * 
	 * <p>See the Google documentation for 
	 * <a href="http://code.google.com/appengine/docs/java/datastore/queries.html#Introduction_to_Indexes">indexes</a>
	 * for an explanation of what you can and cannot filter for.</p>
	 * 
	 * <p>You can <strong>not</strong> filter on @Id or @Parent properties.  Use
	 * {@code filterKey()} or {@code ancestor()} instead.</p>
	 */
	public Query<T> filter(String condition, Object value);

	/**
	 * <p>Create a filter based on the raw low-level Filter. This is a very low-level operation; the values
	 * in the Filter are not translated in any way. For example, this only understands native datastore
	 * {@code Key} objects and not Objectify {@code Key<?>} objects.</p>
	 *
	 * <p>See the Google documentation for
	 * <a href="http://code.google.com/appengine/docs/java/datastore/queries.html#Introduction_to_Indexes">indexes</a>
	 * for an explanation of what you can and cannot filter for.</p>
	 *
	 * <p>You can <strong>not</strong> filter on @Id or @Parent properties.  Use
	 * {@code filterKey()} or {@code ancestor()} instead.</p>
	 */
	public Query<T> filter(Filter filter);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#filterKey(java.lang.String, java.lang.Object)
	 */
	@Override
	public Query<T> filterKey(String condition, Object value);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#filterKey(java.lang.Object)
	 */
	@Override
	public Query<T> filterKey(Object value);

	/**
	 * <p>Sorts based on a property.  Examples:</p>
	 *
	 * <ul>
	 * <li>{@code order("age")}</li>
	 * <li>{@code order("-age")} (descending sort)</li>
	 * </ul>
	 *
	 * <p>You can <strong>not</strong> sort on @Id or @Parent properties. Sort by __key__ or -__key__ instead.</p>
	 */
	public Query<T> order(String condition);

	/**
	 * Shorthand for {@code order("__key__")} or {@code order("-__key__")}
	 * @param descending if true, specifies a descending (aka reverse) sort
	 */
	public Query<T> orderKey(boolean descending);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#ancestor(java.lang.Object)
	 */
	@Override
	public Query<T> ancestor(Object keyOrEntity);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#limit(int)
	 */
	@Override
	public Query<T> limit(int value);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#offset(int)
	 */
	@Override
	public Query<T> offset(int value);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#startAt(com.google.cloud.datastore.Cursor)
	 */
	@Override
	public Query<T> startAt(Cursor value);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#endAt(com.google.cloud.datastore.Cursor)
	 */
	@Override
	public Query<T> endAt(Cursor value);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#chunk(int)
	 */
	@Override
	public Query<T> chunk(int value);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#chunkAll()
	 */
	@Override
	public Query<T> chunkAll();

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#hybrid(boolean)
	 */
	@Override
	public Query<T> hybrid(boolean force);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#project(String...)
	 */
	@Override
	public Query<T> project(String... fields);

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#distinct()
	 */
	@Override
	public Query<T> distinct(boolean value);
}
