/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

/**
 * A simple entity with some @AlsoLoad annotations
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Entity
@Cache
public class HasAlsoLoads
{
	@Id Long id;
	public Long getId() { return this.id; }
	public void setId(Long value) { this.id = value; }
	
	@AlsoLoad("oldStuff")
	String stuff;
	public String getStuff() { return this.stuff; }
	public void setStuff(String value) { this.stuff = value; }
	
	@AlsoLoad("oldOtherStuff")
	String otherStuff;
	public String getOtherStuff() { return this.otherStuff; }
	public void setOtherStuff(String value) { this.otherStuff = value; }

	/** Tests loading with @AlsoLoad on a method */
	@Ignore Integer weird;
	public Integer getWeird() { return this.weird; }
	void namedAnything(@AlsoLoad("weirdStuff") String stuff)
	{
		this.weird = Integer.valueOf(stuff);
	}
	
	/** Default constructor must always exist */
	public HasAlsoLoads() {}
	
	public HasAlsoLoads(String stuff, String otherStuff)
	{
		this.stuff = stuff;
		this.otherStuff = otherStuff;
	}
}