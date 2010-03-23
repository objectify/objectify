package com.googlecode.objectify.condition;

/**
 * <p>Base class for If classes that test against a whole POJO object.  This allows
 * partial indexes to test against field values which are not the field being indexed.</p>
 * 
 * <p>The pojo will be an entity of the field is on an entity, or an embedded class
 * if the field is on an embedded class.</p>
 * 
 * <p>All concrete instances of this interface must have either a no-arg constructor
 * or a constructor that takes {@code Class<?>, Field} parameters.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class PojoIf<V> implements If<Object, V>
{
	/**
	 * Override this method to test a whole pojo for your condition.  The pojo might
	 * be an entity or an embedded class object - whichever holds the field being tested. 
	 */
	abstract public boolean matches(V pojo);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.condition.If#matches(java.lang.Object, java.lang.Object)
	 */
	@Override
	final public boolean matches(Object value, V onPojo)
	{
		return this.matches(onPojo);
	}
}