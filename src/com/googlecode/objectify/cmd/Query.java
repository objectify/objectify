package com.googlecode.objectify.cmd;

import com.google.appengine.api.datastore.Cursor;


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
	 * <li>{@code filter("age in", ageList)}</li>
	 * </ul>
	 * 
	 * <p><strong>The space between the property name and the operator is required.</strong>
	 * Filtering a condition of {@code "age>="} will perform an <em>equality</em> test on an entity
	 * property exactly named "age>=".  You can't create properties like this with Objectify, but you
	 * can with the Low-Level API.</p>
	 * 
	 * <p>See the Google documentation for 
	 * <a href="http://code.google.com/appengine/docs/java/datastore/queries.html#Introduction_to_Indexes">indexes</a>
	 * for an explanation of what you can and cannot filter for.</p>
	 * 
	 * <p>In addition to filtering on indexed properties, you can filter on @Id properties
	 * <strong>if</strong> this query is restricted to a Class<T> which has no @Parent. This
	 * is a convenient alias for {@code filterKey()} which builds the key for you.</p>
	 * 
	 * <p>You can <strong>not</strong> filter on @Parent properties.  Use
	 * {@code filterKey()} or {@code ancestor()} instead.</p>
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
	 * <p>You can sort on @Id properties <strong>if</strong> this query is
	 * restricted to a Class<T> which has no @Parent.  Note that this is only important for
	 * descending sorting; default iteration is key-ascending.</p>
	 * 
	 * <p>You can <strong>not</strong> sort on @Parent properties.</p>
	 */
	public Query<T> order(String condition);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#filterKey(java.lang.String, java.lang.Object)
	 */
	@Override
	public Query<T> filterKey(String condition, Object value);
	
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
	 * @see com.googlecode.objectify.cmd.SimpleQuery#startAt(com.google.appengine.api.datastore.Cursor)
	 */
	@Override
	public Query<T> startAt(Cursor value);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.SimpleQuery#endAt(com.google.appengine.api.datastore.Cursor)
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
}
