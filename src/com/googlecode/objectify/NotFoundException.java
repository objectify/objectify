package com.googlecode.objectify;


/**
 * Exception thrown from Objectify.get() when there is no entity with the
 * specified key.  This is exactly like the datastore EntityNotFoundException,
 * however it is a RuntimeException and it contains the generic Key<?>.
 */
public class NotFoundException extends RuntimeException
{
	private static final long serialVersionUID = 1L;
	
	private final Key<?> key;

	public NotFoundException(Key<?> key)
	{
		super("No entity was found matching the key: " + key);
		
		this.key = key;
	}
	
	public Key<?> getKey()
	{
		return this.key;
	}
}
