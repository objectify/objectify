package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Objectify;

/** 
 * The context of a load or save operation to a single entity. 
 */
public class LoadContext
{
	/** The objectify instance */
	Objectify ofy;
	public Objectify getObjectify() { return this.ofy; }
	
	/** */
	public LoadContext(Objectify ofy)
	{
		this.ofy = ofy;
	}
}