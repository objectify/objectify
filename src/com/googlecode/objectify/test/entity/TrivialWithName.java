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
public class TrivialWithName
{
	@Id String name;
	public String getName() { return this.name; }
	public void setId(String value) { this.name = value; }
	
	long someNumber;
	public long getSomeNumber() { return this.someNumber; }
	public void setSomeNumber(long value) { this.someNumber = value; }

	@Indexed
	String someString;
	public String getSomeString() { return this.someString; }
	public void setSomeString(String value) { this.someString = value; }
	
	/** Default constructor must always exist */
	public TrivialWithName() {}
	
	/** You cannot autogenerate a name */
	public TrivialWithName(String id, long someNumber, String someString)
	{
		this.name = id;
		this.someNumber = someNumber;
		this.someString = someString;
	}
}