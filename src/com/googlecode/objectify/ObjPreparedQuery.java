package com.googlecode.objectify;

import java.util.List;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;


/**
 * This interface mimics the GAE PreparedQuery interface, but instead of
 * iterating through Entity objects, it iterates through your typesafe
 * objects.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface ObjPreparedQuery<T>
{
	/**
	 * @see PreparedQuery#asIterable() 
	 */
	Iterable<T> asIterable();
	
	/**
	 * @see PreparedQuery#asIterable(FetchOptions)
	 */
	Iterable<T> asIterable(FetchOptions fetchOptions);

	/**
	 * @see PreparedQuery#asList(FetchOptions) 
	 */
	List<T> asList(FetchOptions fetchOptions);
	
	/**
	 * @see PreparedQuery#asSingleEntity() 
	 */
	T asSingleEntity();
	
	/**
	 * @see PreparedQuery#countEntities() 
	 */
	int countEntities();
}