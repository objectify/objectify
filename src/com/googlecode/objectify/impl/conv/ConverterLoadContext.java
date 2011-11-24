package com.googlecode.objectify.impl.conv;

import com.googlecode.objectify.impl.Property;


/** 
 * <p>Provides some limited context information to a Converter during load/set.</p>
 */
public interface ConverterLoadContext
{
	/**
	 * @return the field/method wrapper that we are trying to set
	 */
	Property getField();
	
	/**
	 * Gets the pojo instance 
	 */
	Object getPojo();

	/**
	 * Creates and an error message with details about where the problem happened.
	 */
	String createErrorMessage(String detail);
}