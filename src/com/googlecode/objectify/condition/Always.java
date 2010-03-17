package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that always returns true for any value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Always implements If<Object>
{
	@Override
	public boolean matches(Object value)
	{
		return true;
	}
}