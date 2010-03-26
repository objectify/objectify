package com.google.appengine.api.datastore;


/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class EntityNotFoundException extends Exception
{
	private final Key key;

	public EntityNotFoundException(Key key)
	{
		super("No entity was found matching the key: " + key);
		
		this.key = key;
	}
	
	public Key getKey()
	{
		return this.key;
	}
}
