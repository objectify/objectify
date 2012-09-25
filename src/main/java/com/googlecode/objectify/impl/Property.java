package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

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
	
	/** Get an annotation on this type, or null if there is no annotation of that type */
	<A extends Annotation> A getAnnotation(Class<A> annoType);

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
	 * Also factors in any index instruction on the class as a default.  However, explicit index instruction
	 * on the field overrides the class.
	 * 
	 * @return true if this field should be indexed, false if it should be unindexed, null is "no information, continue with defaults".
	 */
	Boolean getIndexInstruction(Object onPojo);
	
	/**
	 * @return true if the property has @IgnoreSave with conditions; there are some cases where this
	 *  won't work (ie in embedded collections) so we need to throw an exception at registration time.
	 */
	boolean hasIgnoreSaveConditions();
	
	/**
	 * @return true if the property should be loaded when the given loadgroups are active
	 */
	boolean shouldLoad(Set<Class<?>> groups);
}