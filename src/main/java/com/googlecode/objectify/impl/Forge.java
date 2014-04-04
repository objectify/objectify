package com.googlecode.objectify.impl;

/**
 * Interface for something that constructs objects. This is used to abstract the injection/construction
 * process so we don't have to pass ObjectifyFactory around everywhere.
 */
public interface Forge
{
	/**
	 * <p>Construct an instance of the specified type.  Objectify uses this method whenever possible to create
	 * instances of entities, condition classes, or other types.</p>
	 */
	<T> T construct(Class<T> type);
}