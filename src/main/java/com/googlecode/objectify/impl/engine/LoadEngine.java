package com.googlecode.objectify.impl.engine;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.impl.SessionValue;
import com.googlecode.objectify.impl.Upgrade;
import com.googlecode.objectify.impl.cmd.LoaderImpl;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultCache;
import com.googlecode.objectify.util.ResultNow;

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

	/**
	 * Each round in the series of fetches required to complete a batch.  A round executes when
	 * the value is obtained (via now()) for a Result that was created as part of this round.
	 * When a round executes, a new round is created.
	 */
	class Round {

		/** The keys we will need to fetch; might not be any if everything came from the session */
		Set<com.google.appengine.api.datastore.Key> pending = new HashSet<com.google.appengine.api.datastore.Key>();

		/** Sometimes we get a bunch of Entity data from queries that eliminates our need to go to the backing datastore */
		Map<com.google.appengine.api.datastore.Key, Entity> stuffed = new HashMap<com.google.appengine.api.datastore.Key, Entity>();

		/** Entities that have been fetched and translated this round. There will be an entry for each pending. */
		Result<Map<Key<?>, Object>> translated;

		/**
		 * Gets a result, using the session cache if possible.
		 */
		public <T> Result<T> get(final Key<T> key) {
			SessionValue<T> sv = session.get(key);
			if (sv == null) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Adding to round (session miss): " + key);

				this.pending.add(key.getRaw());

				Result<T> result = new ResultCache<T>() {
					@Override
					@SuppressWarnings("unchecked")
					public T nowUncached() {
						execute();	// just in case the round hasn't executed yet
						return (T)translated.now().get(key);
					}

					@Override
					public String toString() {
						return "(Fetch result for " + key + ")";
					}
				};

				sv = new SessionValue<T>(result);
				session.add(key, sv);

			} else {
				if (log.isLoggable(Level.FINEST))
					log.finest("Adding to round (session hit): " + key);
			}

			// Check for any upgrades
			if (!sv.getUpgrades().isEmpty()) {
				Iterator<Upgrade> it = sv.getUpgrades().iterator();
				while (it.hasNext()) {
					Upgrade up = it.next();
					if (shouldLoad(up.getProperty())) {
						it.remove();
						loadRef(up.getRef());
					}
				}
			}

			return sv.getResult();
		}

		/** @return true if this round needs execution */
		public boolean needsExecution() {
			return translated == null && !pending.isEmpty();
		}

		/** Turn this into a result set */
		public void execute() {
			if (needsExecution()) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Executing round: " + pending);

				final Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetched = fetchPending();

				translated = new ResultCache<Map<Key<?>, Object>>() {

					/** */
					LoadContext ctx;

					/** */
					@Override
					public Map<Key<?>, Object> nowUncached() {
						Map<Key<?>, Object> result = new HashMap<Key<?>, Object>(fetched.now().size() * 2);

						ctx = new LoadContext(loader, LoadEngine.this);

						for (Entity ent: fetched.now().values()) {
							Key<?> key = Key.create(ent.getKey());
							Object entity = ofy.load(ent, ctx);
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
		}

		/** Possibly pulls some values from the stuffed collection */
		private Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetchPending() {
			// We don't need to fetch anything that has been stuffed

			final Map<com.google.appengine.api.datastore.Key, Entity> combined = new HashMap<com.google.appengine.api.datastore.Key, Entity>();
			Set<com.google.appengine.api.datastore.Key> fetch = new HashSet<com.google.appengine.api.datastore.Key>();

			for (com.google.appengine.api.datastore.Key key: pending) {
				Entity ent = stuffed.get(key);
				if (ent == null)
					fetch.add(key);
				else
					combined.put(key, ent);
			}

			if (fetch.isEmpty()) {
				return new ResultNow<Map<com.google.appengine.api.datastore.Key, Entity>>(combined);
			} else {
				Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(ofy.getTxnRaw(), fetch);
				final Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetched = ResultAdapter.create(fut);

				return new Result<Map<com.google.appengine.api.datastore.Key, Entity>>() {
					@Override
					public Map<com.google.appengine.api.datastore.Key, Entity> now() {
						combined.putAll(fetched.now());
						return combined;
					}
				};
			}
		}

		/** */
		@Override
		public String toString() {
			return (translated == null ? "pending:" : "executed:") + pending.toString();
		}
	}

	/** */
	LoaderImpl loader;
	ObjectifyImpl ofy;
	AsyncDatastoreService ads;
	Session session;

	/** The current round, replaced whenever the round executes */
	Round round = new Round();

	/**
	 */
	public LoadEngine(LoaderImpl loader) {
		this.loader = loader;
		this.ofy = loader.getObjectifyImpl();
		this.session = loader.getObjectifyImpl().getSession();
		this.ads = loader.getObjectifyImpl().createAsyncDatastoreService();

		if (log.isLoggable(Level.FINEST))
			log.finest("Starting load engine with groups " + loader.getLoadGroups());
	}

	/**
	 * The fundamental ref() operation.
	 */
	@SuppressWarnings("unchecked")
	public void loadRef(Ref<?> ref) {
		Result<Object> result = (Result<Object>)this.getResult(ref.key());
		((Ref<Object>)ref).set(result);
	}

	/**
	 * Convenience method that creates a new ref and loads it
	 */
	public <T> Ref<T> getRef(Key<T> key) {
		Ref<T> ref = Ref.create(key);
		loadRef(ref);
		return ref;
	}

	/**
	 * Gets the result, possibly from the session, putting it in the session if necessary.
	 * Also will recursively prepare the session with @Load parents as appropriate.
	 */
	public <T> Result<T> getResult(Key<T> key) {
		Result<T> result = round.get(key);

		// Now check to see if we need to recurse and add our parent(s) to the round
		if (key.getParent() != null) {
			KeyMetadata<?> meta = Keys.getMetadata(key);
			if (meta != null) {
				if (meta.shouldLoadParent(loader.getLoadGroups())) {
					getResult(key.getParent());
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
			round = new Round();
			old.execute();
		}
	}

	/**
	 * Create a Ref for the key, and maybe initialize the value depending on the load annotation and the current
	 * state of load groups.  If appropriate, this will also register the ref for upgrade.
	 *
	 * @param rootEntity is the entity key which holds this property (possibly thorugh some level of embedded objects)
	 */
	public <T> Ref<T> makeRef(Key<?> rootEntity, Property property, Key<T> key) {
		Ref<T> ref = Ref.create(key);

		if (shouldLoad(property)) {
			loadRef(ref);
		} else {
			// Only if there is any potential for upgrade
			Load load = property.getAnnotation(Load.class);
			if (load != null) {
				// add it to the possible list of upgrades
				SessionValue<?> sv = session.get(rootEntity);
				if (sv != null) {
					sv.addUpgrade(new Upgrade(property, ref));
				}
			}
		}

		return ref;
	}

	/**
	 * @return true if the specified property should be loaded in this batch
	 */
	public boolean shouldLoad(Property property) {
		return property.shouldLoad(loader.getLoadGroups());
	}

	/**
	 * Stuffs an Entity into a place where values in the round can be obtained instead of going to the datastore.
	 * Called by non-hybrid queries to add results and eliminate batch fetching.
	 */
	public void stuff(Entity ent) {
		round.stuffed.put(ent.getKey(), ent);
	}
}