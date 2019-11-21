package com.googlecode.objectify.stringifier;


/**
 * <p>Stringifier that escapes dots because they cannot be used as keys in embedded objects</p>
 * 
 * @author Alex Rhomberg
 */
public class DotEscapingStringifier implements Stringifier<String>
{
	@Override
	public String toString(String obj) {
		return obj == null ? obj : obj.replaceAll("%", "%25").replaceAll("\\.", "%2E");
	}

	@Override
	public String fromString(String str) {
		return str == null ? str : str.replaceAll("%2E", ".").replaceAll("%25", "%");
	}
}