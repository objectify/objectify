package com.googlecode.objectify;

import com.google.appengine.api.datastore.Key;

/**
 * Thrown when something went wrong during the entity translation process; for example, the data in the
 * datastore might be in a format incompatible with the intended pojo field.  Indicates what went
 * wrong with which entity.
 */
public class TranslateException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
	private final Key key;

	public TranslateException(Key key, String message, Throwable cause)
	{
		super("Error translating " + key + ": " + message, cause);
		
		this.key = key;
	}
	
	public Key getKey()
	{
		return this.key;
	}
}
