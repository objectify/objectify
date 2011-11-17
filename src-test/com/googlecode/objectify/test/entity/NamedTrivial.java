/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;

/**
 * A trivial entity with some basic data.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
public class NamedTrivial
{
	@Id String name;
	public String getName() { return this.name; }
	public void setId(String value) { this.name = value; }
	
	String someString;
	public String getSomeString() { return this.someString; }
	public void setSomeString(String value) { this.someString = value; }
	
	@Unindex
	long someNumber;
	public long getSomeNumber() { return this.someNumber; }
	public void setSomeNumber(long value) { this.someNumber = value; }

	/** Default constructor must always exist */
	public NamedTrivial() {}
	
	/** You cannot autogenerate a name */
	public NamedTrivial(String id, String someString, long someNumber)
	{
		this.name = id;
		this.someNumber = someNumber;
		this.someString = someString;
	}
}