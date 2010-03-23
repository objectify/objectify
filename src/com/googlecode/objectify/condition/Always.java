package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that always returns true for any value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Always implements If<Object, Object>
{
	@Override
	public boolean matches(Object value, Object onPojo)
	{
		return true;
	}
}