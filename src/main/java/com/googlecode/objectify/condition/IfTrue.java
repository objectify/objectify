package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that returns true if the value is a boolean true.  Note
 * that a null is still false.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfTrue extends ValueIf<Boolean>
{
	@Override
	public boolean matchesValue(Boolean value) {
		return value != null && value;
	}
}