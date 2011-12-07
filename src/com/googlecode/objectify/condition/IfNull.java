package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that returns true if the value is null.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfNull extends ValueIf<Object>
{
	@Override
	public boolean matchesValue(Object value) {
		return value == null;
	}
}