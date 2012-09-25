package com.googlecode.objectify.impl.translate;

/**
 * <p>Thrown by any Translator that wants its value to be skipped.  Unlike most exceptions, this one
 * is not initialized with a stacktrace.  This eliminates almost all of the cost of using an exception
 * mechanism for a common operation.</p>
 * 
 * <p>For example, if you're translating a value during save() and you don't want to store a null,
 * you can {@code throw new SkipException();} and this particular value will be skipped.</p>
 */
public class SkipException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
	/** */
	public SkipException() { }
	
	/** No need for a stacktrace */
	@Override
	public synchronized Throwable fillInStackTrace() { return this; }
}
