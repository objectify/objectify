package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/** 
 * Basic interface so we can wrap fields and methods so they look more or less the same.
 * This makes @AlsoLoad methods look just like fields. 
 */
public interface Loadable
{
	/** Get the primary name associated with this Loadable */
	String getPathName();
	
	/** Get all the names associated with this Loadable (ie, due to @AlsoLoad). Includes the primary name. */
	String[] getNames();
	
	/** Get all the annotations associated with this Loadable; ie on the field or the parameter */
	Annotation[] getAnnotations();

	/** Get the real generic type of the field */
	Type getType();

	/** Actually set the thing (field or method) on an object */
	void set(Object pojo, Object value);

	/** Get the value of the thing thing (field) if possible, or null if not possible (method) */
	Object get(Object pojo);
}