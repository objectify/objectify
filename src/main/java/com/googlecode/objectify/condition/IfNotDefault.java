package com.googlecode.objectify.condition;

import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Field;


/**
 * <p>The opposite of IfDefault</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfNotDefault extends ValueIf<Object> implements InitializeIf
{
	IfDefault opposite = new IfDefault();
	
	@Override
	public void init(ObjectifyFactory fact, Field field) {
		opposite.init(fact, field);
	}
	
	@Override
	public boolean matchesValue(Object value) {
		return !opposite.matchesValue(value);
	}
}