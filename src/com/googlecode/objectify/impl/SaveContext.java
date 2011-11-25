package com.googlecode.objectify.impl;

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
	public Objectify getObjectify() { return this.ofy; }
	
	boolean inEmbeddedCollection;
	public boolean isInEmbeddedCollection() { return inEmbeddedCollection; }
	public void setInEmbeddedCollection(boolean value) { this.inEmbeddedCollection = value; }
	
	/** */
	public SaveContext(Objectify ofy) {
		this.ofy = ofy;
	}
}