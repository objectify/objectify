package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;

/** 
 * The context of a load or save operation to a single entity. 
 */
public class LoadContext
{
	/** The root pojo entity */
	Object pojo;
	public Object getPojo() { return this.pojo; }
	
	/** The datastore entity */
	Entity entity;
	public Entity getEntity() { return this.entity; }

	/** Tracks which embedded collection paths (the Thing[] or Collection<Thing> field have been processed */
	Set<String> processedEmbeddedMultivaluePaths;
	
	/** Things that get run when we are done() */
	List<Runnable> doneHandlers;
	
	/** */
	public LoadContext(Object pojo, Entity entity)
	{
		this.pojo = pojo;
		this.entity = entity;
	}
	
	/**
	 * @return the current set of processed embedded paths, possibly the empty set.
	 *  Do not modify the returned Set.
	 */
	public Set<String> getProcessedEmbeddedMultivaluePaths()
	{
		if (this.processedEmbeddedMultivaluePaths == null)
			return Collections.emptySet();
		else
			return this.processedEmbeddedMultivaluePaths;
	}
	
	/**
	 * Adds a path to the set, instantiating it if necessary
	 */
	public void addProcessedEmbeddedPath(String path)
	{
		if (this.processedEmbeddedMultivaluePaths == null)
			this.processedEmbeddedMultivaluePaths = new HashSet<String>();
		
		this.processedEmbeddedMultivaluePaths.add(path);
	}
	
	/**
	 * Adds a handler that will be called when done() is called.
	 */
	public void addDoneHandler(Runnable handler)
	{
		if (this.doneHandlers == null)
			this.doneHandlers = new ArrayList<Runnable>();
		
		this.doneHandlers.add(handler);
	}
	
	/**
	 * Called at the end of a load, runs all the DoneHandlers.
	 */
	public void done()
	{
		if (this.doneHandlers != null)
		{
			for (Runnable handler: this.doneHandlers)
				handler.run();
		}
	}
}