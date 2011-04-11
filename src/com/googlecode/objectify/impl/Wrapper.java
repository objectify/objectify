package com.googlecode.objectify.impl;

import java.lang.reflect.Type;

/** 
 * Basic interface so we can wrap fields and methods so they look more or less the same.
 * This makes @AlsoLoad methods look just like fields. 
 */
public interface Wrapper
{
	/** Actually set the thing (field or method) on an object */
	void set(Object pojo, Object value);

	/** Get the value of the thing thing (field) if possible, or null if not possible (method) */
	Object get(Object pojo);

	/** Get the type of the thing.  Might return null when unknown (ie content of Collection with no generic type) */
	Class<?> getType();
	
	/** Get the "generictype", which can be a ParameterizedType */
	Type getGenericType();
	
	/** @return true if the value should be deserialized from blob */
	boolean isSerialized();
}