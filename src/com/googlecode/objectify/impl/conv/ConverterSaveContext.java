package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Field;


/** 
 * <p>During saving we have a little more context.</p>
 */
public interface ConverterSaveContext
{
	/**
	 * @return true if the field we are converting data for is inside an embedded collection.
	 */
	boolean inEmbeddedCollection();
	
	/**
	 * @return the field that is being saved.
	 */
	Field getField();
}