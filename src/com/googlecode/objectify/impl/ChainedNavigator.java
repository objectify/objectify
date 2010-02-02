package com.googlecode.objectify.impl;

/**
 * Navigators which are part of a chain shall extend this class.
 */
abstract public class ChainedNavigator<T> implements Navigator<T>
{
	/** */
	protected Navigator<T> chain;

	/**
	 * @param predecessor is the navigator "higher up" in the chain;
	 *  at the end will be a RootNavigator. 
	 */
	public ChainedNavigator(Navigator<T> predecessor)
	{
		this.chain = predecessor;
	}
}
