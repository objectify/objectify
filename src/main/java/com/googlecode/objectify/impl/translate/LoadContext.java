package com.googlecode.objectify.impl.translate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.Property;

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

	/** */
	public LoadContext(Loader loader, LoadEngine batch) {
		this.loader = loader;
		this.engine = batch;
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
	 * Create a Ref for the key, and maybe initialize the value depending on the load annotation and the current
	 * state of load groups.  If appropriate, this will also register the ref for upgrade.
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
}