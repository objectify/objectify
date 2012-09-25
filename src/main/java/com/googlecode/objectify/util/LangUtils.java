package com.googlecode.objectify.util;


/**
 * Dumb tools missing from Java.
 */
public class LangUtils
{
	/** Simple null-safe equality check */
	public static boolean objectsEqual(Object o1, Object o2) {
		return o1 == null ? o2 == null : o1.equals(o2);
	}
}