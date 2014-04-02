package com.googlecode.objectify.stringifier;



/**
 * <p>Used with the @Stringify annotation to convert arbitrary objects to Strings.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Stringifier<T>
{
	/** Convert the thing to a string */
	String toString(T obj);

	/** Convert the string back to a thing */
	T fromString(String str);
}