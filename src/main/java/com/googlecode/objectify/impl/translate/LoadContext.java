package com.googlecode.objectify.impl.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.engine.LoadEngine;

/**
 * The context of a load or save operation to a single entity.
 */
public class LoadContext
{
	/** */
	private static final Logger log = Logger.getLogger(LoadContext.class.getName());

	/** The loader instance */
	Loader loader;

	/** */
	LoadEngine batch;

	/** Lazily created, but executed at the end of done() */
	List<Runnable> deferredA;

	/** Lazily created, but executed at the end of done() */
	List<Runnable> deferredB;

	/** The key of the current root entity; will change as multiple entities are loaded */
	Key<?> currentRoot;

	/** */
	public LoadContext(Loader loader, LoadEngine batch) {
		this.loader = loader;
		this.batch = batch;
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
		batch.execute();

		while (deferredA != null) {
			List<Runnable> runme = deferredA;
			deferredA = null;	// reset this because it might get filled with more

			for (Runnable run: runme) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Executing " + run);

				run.run();
			}
		}

		while (deferredB != null) {
			List<Runnable> runme = deferredB;
			deferredB = null;	// reset this because it might get filled with more

			for (Runnable run: runme) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Executing " + run);

				run.run();
			}
		}
	}

	/**
	 * Create a Ref for the key, and maybe initialize the value depending on the load annotation and the current
	 * state of load groups.  If appropriate, this will also register the ref for upgrade.
	 */
	public <T> Ref<T> makeRef(Property property, Key<T> key) {
		final Ref<T> ref = Ref.create(key);

		if (batch.shouldLoad(property)) {
			batch.loadRef(ref);
		}

		return ref;
	}

	/**
	 * Delays an operation until the context is done().  Executes before B.
	 */
	public void deferA(Runnable runnable) {
		if (this.deferredA == null)
			this.deferredA = new ArrayList<Runnable>();

		if (log.isLoggable(Level.FINEST))
			log.finest("Deferring priority A: " + runnable);

		this.deferredA.add(runnable);
	}

	/**
	 * Delays an operation until the context is done().  Executes after A.  This is for lifecycle methods.
	 */
	public void deferB(Runnable runnable) {
		if (this.deferredB == null)
			this.deferredB = new ArrayList<Runnable>();

		if (log.isLoggable(Level.FINEST))
			log.finest("Deferring priority B: " + runnable);

		this.deferredB.add(runnable);
	}

	/**
	 * Gets the currently enabled set of load groups
	 */
	public Set<Class<?>> getLoadGroups() {
		return loader.getLoadGroups();
	}
}