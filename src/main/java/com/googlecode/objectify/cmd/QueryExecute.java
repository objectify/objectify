package com.googlecode.objectify.cmd;

import com.googlecode.objectify.LoadResult;

import java.util.List;


/**
 * Most of the various methods that can end the definition of a query and start execution.  The first()
 * method is excluded because it returns a Ref<T> which encapsulates both the regular and keys-only
 * result.  It makes no sense to have a Ref<Key<T>>, but when you do keys() you get a QueryExecute<Key<T>>.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface QueryExecute<T> extends QueryResultIterable<T>
{
	/**
	 * <p>Starts an asynchronous query which will return entities.</p>
	 *
	 * <p>Note that since the Query<T> itself is QueryResultIterable<T>, you can iterate on the query
	 * object itself.  However, if you want to start an async query and iterate on it later, you can
	 * use this method.</p>
	 */
	QueryResultIterable<T> iterable();

	/**
	 * <p>Execute the query and get the results as a List.  The list will be equivalent to a simple ArrayList;
	 * you can iterate through it multiple times without incurring additional datastore cost.</p>
	 *
	 * <p>Note that you must be careful about limit()ing the size of the list returned; you can
	 * easily exceed the practical memory limits of Appengine by querying for a very large dataset.</p>
	 */
	List<T> list();

	/**
	 * Gets the first entity in the result set.  Obeys the offset value.
	 *
	 * @return an asynchronous result containing the first result.  The result will hold null if the result set is empty.
	 */
	LoadResult<T> first();

	/**
	 * <p>Generates a string that consistently and uniquely specifies this query.  There
	 * is no way to convert this string back into a query and there is no guarantee that
	 * the string will be consistent across versions of Objectify.</p>
	 *
	 * <p>In particular, this value is useful as a key for a simple memcache query cache.</p>
	 */
	String toString();
}
