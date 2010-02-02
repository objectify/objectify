package com.googlecode.objectify.impl;


/**
 * The simplest navigator type, which simply returns the root pojo as-is.
 * Any Loaders at the root level will have this as their only Navigator;
 * any Loaders for embedded properties will be chained eventually to a
 * RootNavigator.
 */
public class RootNavigator<T> implements Navigator<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Navigator#get(java.lang.Object)
	 */
	@Override
	public Object navigateToTarget(T root)
	{
		return root;
	}
}
