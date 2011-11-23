package com.googlecode.objectify.impl.conv;



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
}