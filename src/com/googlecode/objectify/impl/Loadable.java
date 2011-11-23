package com.googlecode.objectify.impl;

import java.lang.reflect.Type;

/** 
 * Basic interface so we can wrap fields and methods so they look more or less the same.
 * This makes @AlsoLoad methods look just like fields. 
 */
interface Loadable
{
	/** Get all the names associated with this Loadable (ie, due to @AlsoLoad). Includes the primary name. */
	String[] getNames();

	/** Actually set the thing (field or method) on an object */
	void set(Object pojo, Object value);

	/** Get the value of the thing thing (field) if possible, or null if not possible (method) */
	Object get(Object pojo);

	/** Get the real generic type of the field */
	Type getType();
	
	/** @return true if the value should be deserialized from blob */
	boolean isSerialize();
	
	/** @return true if the value is flagged as @Embed */
	boolean isEmbed();
}