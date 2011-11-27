package com.googlecode.objectify;


/**
 * Thrown when something went wrong during the entity translation process; for example, the data in the
 * datastore might be in a format incompatible with the intended pojo field.
 */
public class TranslateException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
	public TranslateException(String message, Throwable cause) {
		super(message, cause);
	}
}
