package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Field;


/** 
 * <p>When we are identifying the proper TypeConverter for a field, this provides some
 * context to the ConverterFactory instances.</p>
 */
public interface ConverterCreateContext
{
	/**
	 * @return true if the field we are converting data for is inside an embedded collection.
	 */
	boolean inEmbeddedCollection();
	
	/**
	 * @return the field that is being saved, useful for exception messages.
	 */
	Field getField();
	
	/**
	 * Creates and throws an error message with details about where the problem happened.
	 */
	void throwErrorMessage(String detail);
}