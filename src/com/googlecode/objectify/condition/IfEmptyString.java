package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that returns true if the value is an empty string.
 * Note that a null string still returns false.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfEmptyString extends ValueIf<String>
{
	@Override
	public boolean matches(String value)
	{
		return value != null && value.isEmpty();
	}
}