package com.googlecode.objectify.impl;


/**
 * The interface that lets a Loader get the site upon which a value
 * should be set given a root pojo.  The objects that implement this
 * interface navigate an entity graph (possibly constructing objects)
 * to get to the actual target object.
 */
public interface Navigator<T>
{
	/**
	 * Given a root pojo, navigate into the object structure to get the *actual* object
	 * that should be populated.  This might involve creating objects along the chain.
	 */
	public Object navigateToTarget(T root);
}
