package com.googlecode.objectify.condition;


/**
 * <p>Simple If condition that always returns true for any value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Always implements If<Object, Object>
{
	@Override
	public boolean matchesValue(Object value) {
		return true;
	}

	@Override
	public boolean matchesPojo(Object pojo) {
		return true;
	}
}