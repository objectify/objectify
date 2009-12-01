package com.googlecode.objectify;

import java.util.List;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;


/**
 * This interface mimics the GAE PreparedQuery interface, but instead of
 * iterating through Entity objects, it iterates through your typesafe
 * objects.  Note also that if the original Query was setKeysOnly(),
 * this interface will output Key objects instead of your model objects.
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
	T asSingle();
	
	/**
	 * @see PreparedQuery#countEntities() 
	 */
	int count();
}