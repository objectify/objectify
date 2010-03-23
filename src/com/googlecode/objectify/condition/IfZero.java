package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that returns true if the value is a numeric zero.
 * Note that a null value still returns false.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfZero extends ValueIf<Number>
{
	@Override
	public boolean matches(Number value)
	{
		return value != null && value.doubleValue() == 0;
	}
}