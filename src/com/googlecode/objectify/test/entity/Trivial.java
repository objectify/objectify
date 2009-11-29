/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.Indexed;

/**
 * A trivial entity with some basic data.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Trivial
{
	@Id
	Key key;
	public Key getKey() { return this.key; }
	public void setKey(Key value) { this.key = value; }
	
	long someNumber;
	public long getSomeNumber() { return this.someNumber; }
	public void setSomeNumber(long value) { this.someNumber = value; }

	@Indexed
	String someString;
	public String getSomeString() { return this.someString; }
	public void setSomeString(String value) { this.someString = value; }
	
	/** Default constructor must always exist */
	public Trivial() {}
	
	public Trivial(Key key, long someNumber, String someString)
	{
		this.key = key;
		this.someNumber = someNumber;
		this.someString = someString;
	}
}