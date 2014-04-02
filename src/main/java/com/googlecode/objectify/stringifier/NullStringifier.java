package com.googlecode.objectify.stringifier;


/**
 * <p>No-op stringifier used as a null object.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NullStringifier implements Stringifier<String>
{
	@Override
	public String toString(String obj) {
		return obj;
	}

	@Override
	public String fromString(String str) {
		return str;
	}
}