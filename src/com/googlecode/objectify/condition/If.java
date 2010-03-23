package com.googlecode.objectify.condition;

/**
 * <p>A simple interface that defines a condition test for a field value or whole
 * entity.  For example, you could have a class that tests against null values called IfNull.
 * This interface is used by the @NotSaved, @Indexed, and @Unindexed annotations.</p>
 * 
 * <p>Because the entity object is provided, partial indexes can be based on values
 * other than the actual field in question.</p>
 * 
 * <p>All concrete instances of this interface must have either a no-arg constructor
 * or a constructor that takes {@code Class<?>, Field} parameters.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface If<T, V>
{
	/**
	 * Test a value (for a field) and/or an entity.
	 * 
	 * @param value is the actual value of a particular field
	 * @param onPojo is the entity object on which the field/value exists
	 * @return true if the value matches the condition defined by an instance of this interface.
	 */
	public boolean matches(T value, V onPojo);
}