/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Unindexed;

/**
 * A trivial entity with some basic data.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Cached
public class Trivial
{
	@Id Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }
	
	String someString;
	public String getSomeString() { return this.someString; }
	public void setSomeString(String value) { this.someString = value; }
	
	@Unindexed
	long someNumber;
	public long getSomeNumber() { return this.someNumber; }
	public void setSomeNumber(long value) { this.someNumber = value; }

	/** Default constructor must always exist */
	public Trivial() {}
	
	/** Constructor to use when autogenerating an id */
	public Trivial(String someString, long someNumber)
	{
		this(null, someString, someNumber);
	}

	/** Constructor to use when forcing the id */
	public Trivial(Long id, String someString, long someNumber)
	{
		this.id = id;
		this.someNumber = someNumber;
		this.someString = someString;
	}
}