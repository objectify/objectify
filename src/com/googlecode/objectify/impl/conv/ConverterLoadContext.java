package com.googlecode.objectify.impl.conv;

import com.googlecode.objectify.impl.Wrapper;


/** 
 * <p>Provides some limited context information to a Converter during load/set.</p>
 */
public interface ConverterLoadContext
{
	/**
	 * @return the field/method wrapper that we are trying to set
	 */
	Wrapper getField();
}