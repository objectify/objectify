package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Objectify;

/** 
 * The context of a save operation to a single entity. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SaveContext
{
	/** The objectify instance */
	Objectify ofy;
	
	/** */
	public SaveContext(Objectify ofy) {
		this.ofy = ofy;
	}

	/** */
	public Objectify getObjectify() { return this.ofy; }

}