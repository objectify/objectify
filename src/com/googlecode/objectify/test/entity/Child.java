/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.ObKey;
import com.googlecode.objectify.annotation.Parent;

/**
 * A child entity which references a parent in the same entity group.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Child
{
	@Id Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }
	
	@Parent
	ObKey<Trivial> parent;
	public ObKey<Trivial> getParent() { return this.parent; }
	public void setParent(ObKey<Trivial> value) { this.parent = value; }
	
	String childString;
	public String getChildString() { return this.childString; }
	public void setChildString(String value) { this.childString = value; }
	
	/** Default constructor must always exist */
	public Child() {}
	
	/** Constructor to use when autogenerating an id */
	public Child(ObKey<Trivial> parent, String childString)
	{
		this.parent = parent;
		this.childString = childString;
	}
}