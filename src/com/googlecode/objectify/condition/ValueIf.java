package com.googlecode.objectify.condition;

/**
 * <p>Base class for If classes that test against a simple value.  This is the
 * most common case; IfNull, IfFalse, IfDefault, etc.</p>
 * 
 * <p>All concrete instances of this interface must have either a no-arg constructor
 * or a constructor that takes {@code Class<?>, Field} parameters.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ValueIf<T> implements If<T, Object>
{
	/**
	 * Override this method to test a field value for your condition.
	 * 
	 * For example, for a class IfNull, return true if the value is null.
	 */
	abstract public boolean matches(T value);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.condition.If#matches(java.lang.Object, java.lang.Object)
	 */
	@Override
	final public boolean matches(T value, Object onPojo)
	{
		return this.matches(value);
	}
}