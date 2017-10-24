package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.LoadConditions;
import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * The context of a load operation, which may extend across several entities (for example, a batch).
 */
@Slf4j
public class LoadContext
{
	/** */
	LoadEngine engine;

	/** Lazily created, but executed at the end of done() */
	List<Runnable> deferred;

	/** The key of the current root entity; will change as multiple entities are loaded */
	Key<?> currentRoot;
	
	/** As we enter and exit embedded contexts, track the objects */
	Deque<Object> containers = new ArrayDeque<>();

	/**
	 * If a translator implements the marker interface Recycles, this will be populated with
	 * the existing value of a property.
	 */
	Object recycled;

	/** */
	public LoadContext(LoadEngine engine) {
		this.engine = engine;
	}

	/** The most recently recycled value. It can be used exactly once. */
	public Object useRecycled() {
		Object value = recycled;
		recycled = null;
		return value;
	}

	/** */
	public void recycle(Object value) {
		this.recycled = value;
	}

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
			final List<Runnable> runme = deferred;
			deferred = null;	// reset this because it might get filled with more

			for (final Runnable run: runme) {
				log.trace("Executing {}", run);
				run.run();
			}
		}
	}

	/**
	 * Create a Ref for the key, and maybe start a load operation depending on current load groups.
	 */
	public <T> Ref<T> loadRef(Key<T> key, LoadConditions loadConditions) {
		return engine.makeRef(currentRoot, loadConditions, key);
	}

	/**
	 * Delays an operation until the context is done().  Typically this is for lifecycle methods.
	 */
	public void defer(Runnable runnable) {
		if (this.deferred == null)
			this.deferred = new ArrayList<>();

		log.trace("Deferring: {}", runnable);

		this.deferred.add(runnable);
	}

	/**
	 * Get the container object which is appropriate for the specified property. Go up the chain looking for a compatible
	 * type; the first one found is the container. If nothing found, throw an exception.
	 */
	public Object getContainer(Type containerType, Path path) {
		Class<?> containerClass = GenericTypeReflector.erase(containerType);
		
		Iterator<Object> containersIt = containers.descendingIterator();

		// We have always entered the current 'this' context when processing properties, so the first thing
		// we get will always be 'this'. So skip that and the first matching owner should be what we want.
		containersIt.next();

		while (containersIt.hasNext()) {
			Object potentialContainer = containersIt.next();
			
			if (containerClass.isAssignableFrom(potentialContainer.getClass()))
				return potentialContainer;
		}
		
		throw new IllegalStateException("No container matching " + containerType + " in " + containers + " at path " + path);
	}
	
	/**
	 * Enter a container context; this is the context of the object that we are processing right now.
	 */
	public void enterContainerContext(Object container) {
		containers.addLast(container);
	}
	
	/**
	 * Exit a container context. The parameter is just a sanity check to make sure that the value popped off is the same
	 * as the value we expect.
	 */
	public void exitContainerContext(Object expectedContainer) {
		Object popped = containers.removeLast();
		assert popped == expectedContainer;
	}
}