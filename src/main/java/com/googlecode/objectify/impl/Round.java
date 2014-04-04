package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.ResultCache;
import com.googlecode.objectify.util.ResultNow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Each round in the series of fetches required to complete a batch.  A round executes when
 * the value is obtained (via now()) for a Result that was created as part of this round.
 * When a round executes, a new round is created.
 */
class Round {

	/** */
	private static final Logger log = Logger.getLogger(Round.class.getName());

	/** */
	private final LoadEngine loadEngine;

	/** */
	private final Session session;

	/** The depth of the rounds of execution, for debugging. 0 is first round, 1 is second round, etc */
	private final int depth;

	/** The keys we will need to fetch; might not be any if everything came from the session */
	private final Set<com.google.appengine.api.datastore.Key> pending = new HashSet<com.google.appengine.api.datastore.Key>();

	/** Sometimes we get a bunch of Entity data from queries that eliminates our need to go to the backing datastore */
	private final Map<com.google.appengine.api.datastore.Key, Entity> stuffed = new HashMap<com.google.appengine.api.datastore.Key, Entity>();

	/** Entities that have been fetched and translated this round. There will be an entry for each pending. */
	Result<Map<Key<?>, Object>> translated;

	/**
	 */
	Round(LoadEngine loadEngine, Session session, int depth) {
		this.loadEngine = loadEngine;
		this.session = session;
		this.depth = depth;
	}

	/**
	 * Gets a result, using the session cache if possible.
	 */
	public <T> Result<T> get(final Key<T> key) {
		assert !isExecuted();

		SessionValue<T> sv = session.get(key);
		if (sv == null) {
			if (log.isLoggable(Level.FINEST))
				log.finest("Adding to round (session miss): " + key);

			this.pending.add(key.getRaw());

			Result<T> result = new ResultCache<T>() {
				@Override
				@SuppressWarnings("unchecked")
				public T nowUncached() {
					// Because clients could conceivably get() in the middle of our operations (see LoadCollectionRefsTest.specialListWorks()),
					// we need to check for early execution. This will perform poorly, but at least it will work.
					//assert Round.this.isExecuted();
					loadEngine.execute();
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

		// Check to see if we need to load any further references
		loadEngine.checkReferences(sv);

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

			Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetched = fetchPending();
			translated = loadEngine.translate(fetched);
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
			final Result<Map<com.google.appengine.api.datastore.Key, Entity>> fetched = loadEngine.fetch(fetch);

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
		return (isExecuted() ? "pending" : "executed") + ", depth=" + depth + ", pending="+ pending.toString();
	}

	/**
	 * @return true if the round has been executed already
	 */
	public boolean isExecuted() {
		return translated != null;
	}

	/** Create the next round */
	public Round next() {
		if (log.isLoggable(Level.FINEST))
			log.finest("Creating new round, going from depth " + depth + " to " + (depth+1));

		return new Round(loadEngine, session, depth+1);
	}

	/**
	 * Stuffs an Entity into a place where values in the round can be obtained instead of going to the datastore.
	 * Called by non-hybrid queries to add results and eliminate batch fetching.
	 */
	public void stuff(Entity ent) {
		stuffed.put(ent.getKey(), ent);
	}

}