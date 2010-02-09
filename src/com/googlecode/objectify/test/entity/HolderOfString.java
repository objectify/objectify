/*
 * $Id$
 * $URL$
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cached;


/**
 * A holder of a string.
 * 
 * @author Scott Hernandez
 */
@Cached
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