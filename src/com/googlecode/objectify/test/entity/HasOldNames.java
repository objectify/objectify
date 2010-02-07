/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.googlecode.objectify.annotation.OldName;

/**
 * A simple entity with some @OldName annotations
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class HasOldNames
{
	@Id Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }
	
	@OldName("oldStuff")
	String stuff;
	public String getStuff() { return this.stuff; }
	public void setStuff(String value) { this.stuff = value; }
	
	@OldName("oldOtherStuff")
	String otherStuff;
	public String getOtherStuff() { return this.otherStuff; }
	public void setOtherStuff(String value) { this.otherStuff = value; }

	/** Tests loading with @OldName on a method */
	@Transient Integer weird;
	public Integer getWeird() { return this.weird; }
	void namedAnything(@OldName("weirdStuff") String stuff)
	{
		this.weird = Integer.valueOf(stuff);
	}
	
	/** Default constructor must always exist */
	public HasOldNames() {}
	
	public HasOldNames(String stuff, String otherStuff)
	{
		this.stuff = stuff;
		this.otherStuff = otherStuff;
	}
}