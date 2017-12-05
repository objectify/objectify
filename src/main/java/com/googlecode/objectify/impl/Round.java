package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.util.ResultCache;
import com.googlecode.objectify.util.ResultNow;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Each round in the series of fetches required to complete a batch.  A round executes when
 * the value is obtained (via now()) for a Result that was created as part of this round.
 * When a round executes, a new round is created.
 */
@Slf4j
class Round {
	/** */
	private final LoadEngine loadEngine;

	/** The depth of the rounds of execution, for debugging. 0 is first round, 1 is second round, etc */
	private final int depth;

	/** The keys we will need to fetch; might not be any if everything came from the session */
	private final Set<com.google.cloud.datastore.Key> pending = new HashSet<>();

	/** Sometimes we get a bunch of Entity data from queries that eliminates our need to go to the backing datastore */
	private final Map<com.google.cloud.datastore.Key, Entity> stuffed = new HashMap<>();

	/** Entities that have been fetched and translated this round. There will be an entry for each pending. */
	private Result<Map<Key<?>, Object>> translated;

	/**
	 */
	Round(LoadEngine loadEngine, int depth) {
		this.loadEngine = loadEngine;
		this.depth = depth;
	}

	/**
	 * Gets a result, using the session cache if possible.
	 */
	public <T> Result<T> get(final Key<T> key) {
		assert !isExecuted();

		SessionValue<T> sv = getSession().get(key);
		if (sv == null) {
			log.trace("Adding to round (session miss): {}", key);

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

			sv = new SessionValue<>(result, getLoadArrangement());
			getSession().add(key, sv);

		} else {
			log.trace("Adding to round (session hit): {}", key);

			if (sv.loadWith(getLoadArrangement())) {
				log.trace("New load group arrangement, checking for upgrades: {}", getLoadArrangement());

				// We are looking at a brand-new arrangement for something that already existed in the session.
				// We need to go through any Ref<?>s that might be in need of loading. We find those refs by
				// actually saving the entity into a custom SaveContext.
				T thing = sv.getResult().now();
				if (thing != null) {
					SaveContext saveCtx = new SaveContext() {
						@Override
						public boolean skipLifecycle() {
							return true;
						}

						@Override
						public com.google.cloud.datastore.Key saveRef(Ref<?> value, LoadConditions loadConditions) {
							com.google.cloud.datastore.Key key = super.saveRef(value, loadConditions);

							if (loadEngine.shouldLoad(loadConditions)) {
								log.trace("Upgrading key {}", key);
								loadEngine.load(value.key());
							}

							return key;
						}
					};

					// We throw away the saved entity and we are done
					loadEngine.ofy.factory().getMetadataForEntity(thing).save(thing, saveCtx);
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
			log.trace("Executing round: {}", pending);

			Result<Map<com.google.cloud.datastore.Key, Entity>> fetched = fetchPending();
			translated = loadEngine.translate(fetched);

			// If we're in a transaction (and beyond the first round), force all subsequent rounds to complete.
			// This effectively means that only the first round can be asynchronous; all other rounds are
			// materialized immediately. The reason for this is that there are some nasty edge cases with @Load
			// annotations in transactions getting called after the transaction closes. This is possibly not the
			// best solution to the problem, but it solves the problem now.
			if (loadEngine.ofy.getTransaction() != null && depth > 0)
				translated.now();
		}
	}

	/** Possibly pulls some values from the stuffed collection */
	private Result<Map<com.google.cloud.datastore.Key, Entity>> fetchPending() {
		// We don't need to fetch anything that has been stuffed

		final Map<com.google.cloud.datastore.Key, Entity> combined = new HashMap<>();
		Set<com.google.cloud.datastore.Key> fetch = new HashSet<>();

		for (com.google.cloud.datastore.Key key: pending) {
			Entity ent = stuffed.get(key);
			if (ent == null)
				fetch.add(key);
			else
				combined.put(key, ent);
		}

		if (fetch.isEmpty()) {
			return new ResultNow<>(combined);
		} else {
			final Result<Map<com.google.cloud.datastore.Key, Entity>> fetched = loadEngine.fetch(fetch);

			return () -> {
				combined.putAll(fetched.now());
				return combined;
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
		log.trace("Creating new round, going from depth {} to {}", depth, (depth+1));
		return new Round(loadEngine, depth+1);
	}

	/**
	 * Stuffs an Entity into a place where values in the round can be obtained instead of going to the datastore.
	 * Called by non-hybrid queries to add results and eliminate batch fetching.
	 */
	public void stuff(Entity ent) {
		stuffed.put(ent.getKey(), ent);
	}

	/** */
	private Session getSession() {
		return loadEngine.getSession();
	}

	/** */
	private LoadArrangement getLoadArrangement() {
		return loadEngine.getLoadArrangement();
	}
}