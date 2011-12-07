package com.googlecode.objectify.condition;

import java.lang.reflect.Field;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>If an If<?, ?> condition class implements this interface, it will be called once just after construction.
 * This is a poor-man's injection system.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface InitializeIf
{
	/**
	 * Instructs the condition instance which field it lives on.
	 * 
	 * @param fact is just handy to have around
	 * @param concreteClass is the class that was registered when the field was discovered
	 * @param field is the field which has the annotation with this condition.  The declaring class of
	 *  the field might be different from the concreteClass if the field was declared on a superclass.
	 */
	void init(ObjectifyFactory fact, Class<?> concreteClass, Field field);
}