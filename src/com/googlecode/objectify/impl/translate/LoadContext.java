package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.engine.Batch;

/** 
 * The context of a load or save operation to a single entity. 
 */
public class LoadContext
{
	/** The objectify instance */
	Objectify ofy;
	
	/** */
	Batch batch;
	
	/** */
	public LoadContext(Objectify ofy, Batch batch)
	{
		this.ofy = ofy;
		this.batch = batch;
	}
	
	/** */
	public Objectify getObjectify() { return this.ofy; }
	
	/** Call this when a load process completes */
	public void done() {
		batch.execute();
		
		// Now put in place any result values
	}
	
	/**
	 * Maybe load the given ref, depending on configured load groups and the characteristics of the particular Load annotation.
	 * @param load is the annotation on the relevant field; defines which load groups are necessary
	 * @param ref is the ref to load if the right load groups are appropriate
	 */
	public void maybeLoadRef(Load load, Ref<?> ref) {
		if (batch.shouldLoad(load))
			batch.loadRef(ref);
	}
}