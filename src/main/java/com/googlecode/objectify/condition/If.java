package com.googlecode.objectify.condition;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>A simple interface that defines a condition test for a field value or whole
 * entity.  For example, you could have a class that tests against null values called IfNull.
 * This interface is used by the @IgnoreSave, @Index, and @Unindex annotations.</p>
 * 
 * <p>The matching engine will call both methods; if either return true the condition
 * is considered true.</p>
 * 
 * <p>All implementations of this interface will be created with {@code ObjectifyFactory.construct()}.
 * If the implementation also implements the {@code InitializeIf} interface, the {@code init()} method
 * will be called immediately after construction.</p>
 * 
 * @see InitializeIf
 * @see ObjectifyFactory#construct(Class)
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface If<V, P>
{
	/**
	 * Test a simple property value.
	 * 
	 * @param value is the actual value of a particular field
	 * @return true if the value matches the condition defined by an instance of this interface.
	 */
	public boolean matchesValue(V value);

	/**
	 * Override this method to test a whole pojo for your condition.  The pojo might
	 * be an entity or an embedded class object - whichever holds the field being tested. 
	 * 
	 * @param pojo is the entity object on which the field/value exists
	 * @return true if the value matches the condition defined by an instance of this interface.
	 */
	public boolean matchesPojo(P pojo);
}