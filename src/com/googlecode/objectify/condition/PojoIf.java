package com.googlecode.objectify.condition;

/**
 * <p>Base class for If classes that test against a whole POJO object.  This allows
 * partial indexes to test against field values which are not the field being indexed.</p>
 * 
 * <p>The pojo will be an entity if the field is on an entity, or an embedded class
 * if the field is on an embedded class.</p>
 * 
 * <p>All concrete instances of this interface must have either a no-arg constructor
 * or a constructor that takes {@code Class<?>, Field} parameters.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class PojoIf<P> implements If<Object, P>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.condition.If#matchesPojo(java.lang.Object)
	 */
	@Override
	final public boolean matchesValue(Object onPojo) {
		return false;
	}
}