/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

/**
 * An entity that has several array types.  Left off getters and setters
 * for convenience.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
public class HasArrays
{
	public @Id Long id;
	
	public String[] strings;
	
	@Unindex
	public long[] longs;
	
	@Unindex
	public int[] ints;

	@Unindex
	public Integer[] integers;

	/** Default constructor must always exist */
	public HasArrays() {}
}