package com.googlecode.objectify.condition;


/**
 * <p>A simple interface that defines a condition test for a value.  For example,
 * you could have a class that tests an object value against null called IfNull.
 * This interface is used by the @NotSaved, @Indexed, and @Unindexed annotations.</p>
 * 
 * <p>All concrete instances of this interface must have either a no-arg constructor
 * or a constructor that takes {@code Class<?>, Field} parameters.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface If<T>
{
	/**
	 * @return true if the value matches the condition defined by an instance of this interface.
	 */
	public boolean matches(T value);
}