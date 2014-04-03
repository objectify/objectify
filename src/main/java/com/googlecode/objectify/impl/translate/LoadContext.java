package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The context of a load operation, which may extend across several entities (for example, a batch).
 */
public class LoadContext
{
	/** */
	private static final Logger log = Logger.getLogger(LoadContext.class.getName());

	/** The loader instance */
	Loader loader;

	/** */
	LoadEngine engine;

	/** Lazily created, but executed at the end of done() */
	List<Runnable> deferred;

	/** The key of the current root entity; will change as multiple entities are loaded */
	Key<?> currentRoot;
	
	/** As we enter and exit embedded contexts, track the objects */
	Deque<Object> owners = new ArrayDeque<Object>();

	/**
	 * If a translator implements the marker interface Recycles, this will be populated with
	 * the existing value of a property.
	 */
	Object recycled;

	/** */
	public LoadContext(Loader loader, LoadEngine batch) {
		this.loader = loader;
		this.engine = batch;
	}

	/** The most recently recycled value */
	public Object getRecycled() {
		return recycled;
	}

	/** */
	public void recycle(Object value) {
		this.recycled = value;
	}

	/** */
	public Loader getLoader() { return this.loader; }

	/** Sets the current root entity */
	public void setCurrentRoot(Key<?> rootEntity) {
		this.currentRoot = rootEntity;
	}

	/**
	 * Call this when a load process completes.  Executes anything in the batch and then executes any delayed operations.
	 */
	public void done() {
		engine.execute();

		while (deferred != null) {
			List<Runnable> runme = deferred;
			deferred = null;	// reset this because it might get filled with more

			for (Runnable run: runme) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Executing " + run);

				run.run();
			}
		}
	}

	/**
	 * Create a Ref for the key, and maybe start a load operation depending on current load groups.
	 */
	public <T> Ref<T> makeRef(Property property, Key<T> key) {
		return engine.makeRef(currentRoot, property, key);
	}

	/**
	 * Delays an operation until the context is done().  Typically this is for lifecycle methods.
	 */
	public void defer(Runnable runnable) {
		if (this.deferred == null)
			this.deferred = new ArrayList<Runnable>();

		if (log.isLoggable(Level.FINEST))
			log.finest("Deferring: " + runnable);

		this.deferred.add(runnable);
	}

	/**
	 * Gets the currently enabled set of load groups
	 */
	public Set<Class<?>> getLoadGroups() {
		return loader.getLoadGroups();
	}

	/**
	 * Get the owner object which is appropriate for the specified property. Go up the chain looking for a compatible
	 * type; the first one found is the owner. If nothing found, throw an exception.
	 */
	public Object getOwner(Type ownerType, Path path) {
		Class<?> ownerClass = GenericTypeReflector.erase(ownerType);
		
		Iterator<Object> ownersIt = owners.descendingIterator();

		// We have always entered the current 'this' context when processing properties, so the first thing
		// we get will always be 'this'. So skip that and the first matching owner should be what we want.
		ownersIt.next();

		while (ownersIt.hasNext()) {
			Object potentialOwner = ownersIt.next();
			
			if (ownerClass.isAssignableFrom(potentialOwner.getClass()))
				return potentialOwner;
		}
		
		throw new IllegalStateException("No owner matching " + ownerType + " in " + owners + " at path " + path);
	}
	
	/**
	 * Enter an "owner" context; this is the context of the object that we are processing right now.
	 */
	public void enterOwnerContext(Object owner) {
		owners.addLast(owner);
	}
	
	/**
	 * Exit an "owner" context. The parameter is just a sanity check to make sure that the value popped off is the same
	 * as the value we expect.
	 */
	public void exitOwnerContext(Object expectedOwner) {
		Object popped = owners.removeLast();
		assert popped == expectedOwner;
	}
}