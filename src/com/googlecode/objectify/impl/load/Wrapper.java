package com.googlecode.objectify.impl.load;

import java.lang.reflect.Type;

/** 
 * Basic interface so we can wrap fields and methods so they look more or less the same.
 * This makes @OldName methods look just like fields. 
 */
public interface Wrapper
{
	/** Actually set the thing (field or method) on an object */
	void set(Object entity, Object value);

	/** Get the value of the thing thing (field) if possible, or null if not possible (method) */
	Object get(Object entity);

	/** Get the type of the thing */
	Class<?> getType();
	
	/** Get the "generictype", which can be a ParameterizedType */
	Type getGenericType();
}