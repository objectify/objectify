package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Field;


/** 
 * <p>During saving we have a little more context.</p>
 */
public interface ConverterSaveContext
{
	/**
	 * @return true if we are collectionizing the field, as in the case of POJOs
	 * that are part of an embedded collection. 
	 */
	boolean isCollectionizing();
	
	/**
	 * @return the field that is being saved.
	 */
	Field getField();
}