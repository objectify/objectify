package com.googlecode.objectify.condition;

import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Field;

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
	 * @param field is the field which has the annotation with this condition.
	 */
	void init(ObjectifyFactory fact, Field field);
}