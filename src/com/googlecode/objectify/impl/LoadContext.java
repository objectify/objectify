package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.impl.load.EntityNode;
import com.googlecode.objectify.impl.load.Loader;
import com.googlecode.objectify.impl.load.EntityNode.DoneIteratingException;

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
	
	/** When iterating, this holds the current index... otherwise null */
	Integer currentIndex;
	public Integer getCurrentIndex() { return this.currentIndex; }
	
	/**
	 * Creates an iterator that will execute a load() for each step in a collection.
	 * Note that only one of these iterators can execute at once, and it must be
	 * completely exhausted before another is started.  Also this slightly violates
	 * the standard iterator spec; you cannot call next() without calling hasNext()
	 * first.
	 */
	public Iterator<Object> iterateLoad(final EntityNode node, final Loader componentLoader) {
		if (currentIndex != null)
			throw new IllegalStateException("Started an iterateLoad while another was in progress!");
		
		currentIndex = 0;
		
		return new Iterator<Object>() {
			Boolean hasNext;	// if null, this means unknown and we must fetch
			Object next;
			
			@Override
			public boolean hasNext() {
				if (hasNext == null) {
					try {
						next = componentLoader.load(node, LoadContext.this);
						hasNext = true;
					} catch (DoneIteratingException ex) {
						currentIndex = null;
						hasNext = false;
					}
				}
				
				return hasNext;
			}

			@Override
			public Object next() {
				assert hasNext != null && hasNext;	// just a sanity check
				hasNext = null;
				currentIndex++;
				return next;
			}

			@Override
			public void remove() { throw new UnsupportedOperationException(); }
		};
	}

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
	public LoadContext(Entity entity, Objectify ofy)
	{
		this.entity = entity;
		this.ofy = ofy;
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