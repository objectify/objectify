/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test.entity;

import javax.persistence.Id;

import com.googlecode.objectify.OldName;

/**
 * A simple entity with some @OldName annotations
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class WithOldNames
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
	
	/** Default constructor must always exist */
	public WithOldNames() {}
	
	public WithOldNames(String stuff, String otherStuff)
	{
		this.stuff = stuff;
		this.otherStuff = otherStuff;
	}
}