package com.googlecode.objectify.impl;

import com.googlecode.objectify.ObjectifyFactory;


/**
 * <p>A Loader knows how to populate one <strong>leaf</strong> entity property into an object.
 * For example, you might have a Person entity with a property "name.firstName"; the Loader
 * uses the Navigator to get (possibly construct) the Name object and then sets the property.</p>
 * 
 * <p>Remember that Loaders only load <strong>leaf</strong> properties.  To make this especially
 * confusing, arrays and collections of basic types are leaf properties!</p>
 */
abstract public class Loader<T>
{
	ObjectifyFactory factory;
	Navigator<T> navigator;
	
	/** */
	public Loader(ObjectifyFactory fact, Navigator<T> nav)
	{
		this.factory = fact;
		this.navigator = nav;
	}
	
	/**
	 * Called by the Transmog to set a value somewhere deep in the structure of the pojo.
	 */
	public void loadIntoRoot(T rootPojo, Object value)
	{
		Object target = this.navigator.navigateToTarget(rootPojo);
		this.load(target, value);
	}
	
	/**
	 * Actually load the value into a specific object.
	 */
	abstract protected void load(Object intoTarget, Object value);
}
