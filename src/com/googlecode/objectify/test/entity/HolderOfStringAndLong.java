/*
 * $Id$
 * $URL$
 */

package com.googlecode.objectify.test.entity;


/**
 * A holder of a string, and a Long.
 * 
 * @author Scott Hernandez
 */
public class HolderOfStringAndLong extends HolderOfString
{
	protected Long myPrecious;
	
	/** Default constructor must always exist */
	public HolderOfStringAndLong() {}

	public HolderOfStringAndLong(String s, Long l) {super(s); this.myPrecious = l; }

	public Long getMyPrecious()
	{
		return this.myPrecious;
	}
}