package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.ObjectifyFactory;

/** 
 * The context while creating translator factories. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CreateContext
{
	/** The objectify factory instance */
	ObjectifyFactory factory;
	public ObjectifyFactory getFactory() { return this.factory; }
	
	boolean inEmbed;
	public boolean isInEmbed() { return inEmbed; }
	public void setInEmbed(boolean value) { this.inEmbed = value; }
	
	boolean inCollection;
	public boolean isInCollection() { return inCollection; }
	public void setInCollection(boolean value) { this.inCollection = value; }
	
	/** */
	public CreateContext(ObjectifyFactory fact) {
		this.factory = fact;
	}
}