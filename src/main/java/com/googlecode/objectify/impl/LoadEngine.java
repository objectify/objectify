package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.ref.LiveRef;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultCache;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents one "batch" of loading.  Get a number of Result<?> objects, then execute().  Some work is done
 * right away, some work is done on the first get().  There might be multiple rounds of execution to process
 * all the @Load groups, but that is invisible outside this class.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoadEngine
{
	/** */
	private static final Logger log = Logger.getLogger(LoadEngine.class.getName());

	/** */
	LoaderImpl<?> loader;
	ObjectifyImpl<?> ofy;
	AsyncDatastoreService ads;
	Session session;
	LoadArrangement loadArrangement;

	/** The current round, replaced whenever the round executes */
	Round round;

	/**
	 */
	public LoadEngine(LoaderImpl<?> loader, ObjectifyImpl<?> ofy, Session session, AsyncDatastoreService ads, LoadArrangement loadArrangement) {
		this.loader = loader;
		this.ofy = ofy;
		this.session = session;
		this.ads = ads;
		this.loadArrangement = loadArrangement;

		this.round = new Round(this, 0);

		if (log.isLoggable(Level.FINEST))
			log.finest("Starting load engine with groups " + loader.getLoadGroups());
	}

	/**
	 * Gets the result, possibly from the session, putting it in the session if necessary.
	 * Also will recursively prepare the session with @Load parents as appropriate.
	 * @throws NullPointerException if key is null
	 */
	public <T> Result<T> load(Key<T> key) {
		if (key == null)
			throw new NullPointerException("You tried to load a null key!");

		Result<T> result = round.get(key);

		// If we are running a transaction, enlist the result so that it gets processed on commit even
		// if the client never materializes the result.
		if (ofy.getTransaction() != null)
			ofy.getTransaction().enlist(result);

		// Now check to see if we need to recurse and add our parent(s) to the round
		if (key.getParent() != null) {
			KeyMetadata<?> meta = ofy.factory().keys().getMetadata(key);
			// Interesting, is this really possible?
			if (meta != null) {
				if (meta.shouldLoadParent(loader.getLoadGroups())) {
					load(key.getParent());
				}
			}
		}

		return result;
	}

	/**
	 * Starts asychronous fetching of the batch.
	 */
	public void execute() {
		if (round.needsExecution()) {
			Round old = round;
			round = old.next();
			old.execute();
		}
	}

	/**
	 * Create a Ref for the key, and maybe start a load operation depending on current load groups.
	 *
	 * @param rootEntity is the entity key which holds this property (possibly through some level of embedded objects)
	 */
	public <T> Ref<T> makeRef(Key<?> rootEntity, LoadConditions loadConditions, Key<T> key) {
		Ref<T> ref = new LiveRef<T>(key, ofy);

		if (shouldLoad(loadConditions)) {
			load(key);
		}

		return ref;
	}

	/**
	 * @return true if the specified property should be loaded in this batch
	 */
	public boolean shouldLoad(LoadConditions loadConditions) {
		return loadConditions.shouldLoad(loader.getLoadGroups());
	}

	/**
	 * Stuffs an Entity into a place where values in the round can be obtained instead of going to the datastore.
	 * Called by non-hybrid queries to add results and eliminate batch fetching.
	 */
	public void stuff(Entity ent) {
		round.stuff(ent);
	}

	/**
	 * Asynchronously translate raw to processed; might produce successive load operations as refs are filled in
	 */
	public Result<Map<Key<?>, Object>> translate(final Result<Map<com.google.appengine.api.datastore.Key, Entity>> raw) {
		return new ResultCache<Map<Key<?>, Object>>() {

			/** */
			LoadContext ctx;

			/** */
			@Override
			public Map<Key<?>, Object> nowUncached() {
				Map<Key<?>, Object> result = new HashMap<Key<?>, Object>(raw.now().size() * 2);

				ctx = new LoadContext(loader, LoadEngine.this);

				for (Entity ent: raw.now().values()) {
					Key<?> key = Key.create(ent.getKey());
					Object entity = load(ent, ctx);
					result.put(key, entity);
				}

				return result;
			}

			/**
			 * We need to execute the done() after the translated value has been set, otherwise we
			 * can produce an infinite recursion problem.
			 */
			@Override
			protected void postExecuteHook() {
				ctx.done();
				ctx = null;
			}
		};
	}

	/**
	 * Fetch the keys from the async datastore using the current transaction context
	 */
	public Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetch(Set<com.google.appengine.api.datastore.Key> keys) {
		Transaction txn = (ofy.getTransaction() == null) ? null : ofy.getTransaction().getRaw();

		Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(txn, keys);
		return ResultAdapter.create(fut);
	}

	/**
	 * Converts a datastore entity into a typed pojo object
	 * @return an assembled pojo, or the Entity itself if the kind is not registered, or null if the input value was null
	 */
	@SuppressWarnings("unchecked")
	public <T> T load(Entity ent, LoadContext ctx) {
		if (ent == null)
			return null;

		EntityMetadata<T> meta = ofy.factory().getMetadata(ent.getKind());
		if (meta == null)
			return (T)ent;
		else
			return meta.load(ent, ctx);
	}

	/** */
	public Session getSession() {
		return session;
	}

	/** */
	public LoadArrangement getLoadArrangement() {
		return loadArrangement;
	}
}