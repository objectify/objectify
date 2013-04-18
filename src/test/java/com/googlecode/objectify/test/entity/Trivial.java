/*
 */

package com.googlecode.objectify.test.entity;

import java.io.Serializable;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

/**
 * A trivial entity with some basic data.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
public class Trivial implements Serializable
{
	private static final long serialVersionUID = 1L;

	@Id Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }

	@Index
	String someString;
	public String getSomeString() { return this.someString; }
	public void setSomeString(String value) { this.someString = value; }

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

	@Override
	public String toString()
	{
		return this.getClass().getSimpleName() + "(id=" + id + ", someString=" + someString + ", someNumber=" + someNumber + ")";
	}
}