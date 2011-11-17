/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;


/**
 * A holder of a string, and a Long.
 * 
 * @author Scott Hernandez
 */
@Entity
@Cache
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