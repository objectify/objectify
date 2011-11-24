package com.googlecode.objectify.impl;

import com.googlecode.objectify.Objectify;

/** 
 * The context of a save operation to a single entity. 
 */
public class SaveContext
{
	/** The objectify instance */
	Objectify ofy;
	public Objectify getObjectify() { return this.ofy; }
	
	/** */
	public SaveContext(Objectify ofy)
	{
		this.ofy = ofy;
	}
}