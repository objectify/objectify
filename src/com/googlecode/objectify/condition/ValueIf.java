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
abstract public class ValueIf<V> implements If<V, Object>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.condition.If#matchesPojo(java.lang.Object)
	 */
	@Override
	final public boolean matchesPojo(Object value) {
		return false;
	}
}