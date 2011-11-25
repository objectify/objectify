package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;

/** 
 * The context of a load or save operation to a single entity. 
 */
public class LoadContext
{
	/** The datastore entity */
	Entity entity;
	public Entity getEntity() { return this.entity; }
	
	/** The objectify instance */
	Objectify ofy;
	public Objectify getObjectify() { return this.ofy; }
	
	/** */
	public LoadContext(Entity entity, Objectify ofy)
	{
		this.entity = entity;
		this.ofy = ofy;
	}
}