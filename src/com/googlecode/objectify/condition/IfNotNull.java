package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that returns true if the value is not null.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfNotNull extends ValueIf<Object>
{
	@Override
	public boolean matches(Object value)
	{
		return value != null;
	}
}