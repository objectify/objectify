/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * An entity that has several array types.  Left off getters and setters
 * for convenience.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Cached
public class HasArrays
{
	public @Id Long id;
	
	public String[] strings;
	
	@Unindexed
	public long[] longs;
	
	@Unindexed
	public int[] ints;

	@Unindexed
	public Integer[] integers;

	/** Default constructor must always exist */
	public HasArrays() {}
}