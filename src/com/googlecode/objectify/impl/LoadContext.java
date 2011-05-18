package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	/**
	 * <p>Whenver we stumble across an embedded multivalue, we build it up in an ArrayList *before* putting it in
	 * the final collection.  This is because the embedded objects need to be fully reconsitituted
	 * first - if the embedded collection is of type Set, we can't add objects that don't have their
	 * hashable fields ready.</p>
	 * 
	 * <p>This also helps to track which fields have been processed.  The EmbeddedNullIndexSetter needs
	 * this to cleanup an edge case.</p>
	 * 
	 * <p>Key is the base path to the embedded collection/array.</p>
	 */
	Map<String, ArrayList<Object>> pendingEmbeddedMultivalues;
	
	/**
	 * The key of an entry that is stored in an embedded map. Extracted by Transmog and then used in the 
	 * EmbeddedMapSetter, as only Transmog knows which keys matched.
	 * <p>
	 * E.g. in map.entry.property.value this would be "entry".
	 */
	String currentMapEntry;
	/**
	 * The remaining setter string after setting a map entry, e.g. in map.entry.property.value this would
	 * be "property.value".
	 */
	String currentMapSuffix;
	
	/** Things that get run when we are done() */
	List<Runnable> doneHandlers;
	
	/** */
	public LoadContext(Object pojo, Entity entity)
	{
		this.pojo = pojo;
		this.entity = entity;
	}
	
	/**
	 * @return true if there is a pending array for the specified embedded multivalue.  False means
	 * that we haven't seen an overt value for it (yet). 
	 */
	public boolean hasPendingEmbeddedMultivalue(String path)
	{
		return this.pendingEmbeddedMultivalues != null && this.pendingEmbeddedMultivalues.containsKey(path);
	}
	
	/**
	 * Gets the temporary storage list for an embedded multivalue, instantiating
	 * one if necessary.
	 */
	public ArrayList<Object> getPendingEmbeddedMultivalue(String path, int length)
	{
		if (this.pendingEmbeddedMultivalues == null)
			this.pendingEmbeddedMultivalues = new HashMap<String, ArrayList<Object>>();
		
		ArrayList<Object> list = this.pendingEmbeddedMultivalues.get(path);
		if (list == null)
		{
			list = new ArrayList<Object>(length);
			this.pendingEmbeddedMultivalues.put(path, list);
		}
		
		return list;
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

	public String getMapEntryName()
	{
		return currentMapEntry;
	}
	
	public String getMapSuffix()
	{
		return currentMapSuffix;
	}
}