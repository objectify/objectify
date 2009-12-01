/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.Indexed;

/**
 * A trivial entity with some basic data.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TrivialWithId
{
	@Id Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }
	
	long someNumber;
	public long getSomeNumber() { return this.someNumber; }
	public void setSomeNumber(long value) { this.someNumber = value; }

	@Indexed
	String someString;
	public String getSomeString() { return this.someString; }
	public void setSomeString(String value) { this.someString = value; }
	
	/** Default constructor must always exist */
	public TrivialWithId() {}
	
	/** Constructor to use when autogenerating an id */
	public TrivialWithId(long someNumber, String someString)
	{
		this(null, someNumber, someString);
	}

	/** Constructor to use when forcing the id */
	public TrivialWithId(Long id, long someNumber, String someString)
	{
		this.id = id;
		this.someNumber = someNumber;
		this.someString = someString;
	}
}