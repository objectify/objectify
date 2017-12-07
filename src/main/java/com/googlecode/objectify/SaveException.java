package com.googlecode.objectify;


/**
 * Thrown when something went wrong during the save translation process.  Indicates what went
 * wrong with which entity.
 */
public class SaveException extends TranslateException
{
	private static final long serialVersionUID = 1L;
	
	private final Object pojo;

	/** Constructor to use when you're saving an entity with a known key */
	public SaveException(final Object pojo, final Throwable cause) {
		super("Error saving " + pojo + ": " + cause, cause);
		
		this.pojo = pojo;
	}
	
	/** The complete version of what we couldn't translate */
	public Object getPojo() {
		return this.pojo;
	}
}
