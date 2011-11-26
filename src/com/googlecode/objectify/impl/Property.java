package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/** 
 * Basic interface so we can wrap fields and methods so they look more or less the same.
 * This makes @AlsoLoad methods look just like fields. 
 */
public interface Property
{
	/** Get the primary name associated with this property */
	String getName();
	
	/** Get all the names associated with this property (ie, due to @AlsoLoad). Includes the primary name unless it is @IgnoreLoad. */
	String[] getLoadNames();
	
	/** Get all the annotations associated with this property; ie on the field or the parameter */
	Annotation[] getAnnotations();

	/** Get the real generic type of the field */
	Type getType();

	/** Actually set the property (field or method) on an object */
	void set(Object onPojo, Object value);

	/** Get the value of the property (field) if possible, or null if not possible (method) */
	Object get(Object onPojo);
	
	/**
	 * @return true if this field should be saved, false if not
	 */
	boolean isSaved(Object onPojo);
	
	/**
	 * Gets the index instruction for this property, if there is one.  Properties do not necessarily have a
	 * specific index or unindex instruction, and even if they do, the instruction might be conditional.
	 * Note that this is just the index instruction on the property itself, not the class.  If the class
	 * has an index instruction, it will override this one.
	 * 
	 * @return true if this field should be indexed, false if it should be unindexed, null is "no information, continue with defaults".
	 */
	Boolean getIndexInstruction(Object onPojo);
	
	/**
	 * @return true if the property has @IgnoreSave with conditions; there are some cases where this
	 *  won't work (ie in embedded collections) so we need to throw an exception at registration time.
	 */
	boolean hasIgnoreSaveConditions();
}