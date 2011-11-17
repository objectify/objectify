/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;


/**
 * A holder of a string.
 * 
 * @author Scott Hernandez
 */
@Entity
@Cache
public class HolderOfString extends Holder<String>
{
	/** Default constructor must always exist */
	public HolderOfString() {}

	public HolderOfString(String s) {super(s);}

	public void setMyThing(String s)
	{
		this.thing = s;
	}

	public String getMyThing()
	{
		return this.thing;
	}
	
}