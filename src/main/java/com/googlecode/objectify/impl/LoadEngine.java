package com.googlecode.objectify.impl;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.ReadOption;
import com.google.common.collect.ImmutableSet;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.ref.LiveRef;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultCache;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * Represents one "batch" of loading.  Get a number of Result<?> objects, then execute().  Some work is done
 * right away, some work is done on the first get().  There might be multiple rounds of execution to process
 * all the @Load groups, but that is invisible outside this class.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class LoadEngine
{
	/** */
	final ObjectifyImpl ofy;
	private final AsyncDatastoreReaderWriter datastore;

	@Getter
	private final Session session;

	@Getter
	private final LoadArrangement loadArrangement;

	private final ImmutableSet<ReadOption> readOptions;

	/** The current round, replaced whenever the round executes */
	Round round;

	/**
	 */
	public LoadEngine(
			final ObjectifyImpl ofy,
			final Session session,
			final AsyncDatastoreReaderWriter datastore,
			final LoadArrangement loadArrangement,
			final ImmutableSet<ReadOption> readOptions) {
		this.ofy = ofy;
		this.session = session;
		this.datastore = datastore;
		this.loadArrangement = loadArrangement;
		this.readOptions = readOptions;

		this.round = new Round(this, 0);

		log.trace("Starting load engine with groups {}", loadArrangement);
	}

	/**
	 * Gets the result, possibly from the session, putting it in the session if necessary.
	 * Also will recursively prepare the session with @Load parents as appropriate.
	 * @throws NullPointerException if key is null
	 */
	public <T> Result<T> load(final Key<T> key) {
		if (key == null)
			throw new NullPointerException("You tried to load a null key!");

		final Result<T> result = round.get(key);

		// If we are running a transaction, enlist the result so that it gets processed on commit even
		// if the client never materializes the result.
		if (ofy.getTransaction() != null)
			((PrivateAsyncTransaction)ofy.getTransaction()).enlist(result);

		// Now check to see if we need to recurse and add our parent(s) to the round
		if (key.getParent() != null) {
			final KeyMetadata<?> meta = ofy.factory().keys().getMetadata(key);
			// Is it really possible for this to be null?
			if (meta != null) {
				if (meta.shouldLoadParent(loadArrangement)) {
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
	public <T> Ref<T> makeRef(final Key<?> rootEntity, final LoadConditions loadConditions, final Key<T> key) {
		final Ref<T> ref = new LiveRef<>(key, ofy);

		if (shouldLoad(loadConditions)) {
			load(key);
		}

		return ref;
	}

	/**
	 * @return true if the specified property should be loaded in this batch
	 */
	public boolean shouldLoad(final LoadConditions loadConditions) {
		return loadConditions.shouldLoad(loadArrangement, ofy.getTransaction() != null);
	}

	/**
	 * Stuffs an Entity into a place where values in the round can be obtained instead of going to the datastore.
	 * Called by non-hybrid queries to add results and eliminate batch fetching.
	 */
	public void stuff(final Entity ent) {
		round.stuff(ent);
	}

	/**
	 * Asynchronously translate raw to processed; might produce successive load operations as refs are filled in
	 */
	public Result<Map<Key<?>, Object>> translate(final Result<Map<com.google.cloud.datastore.Key, Entity>> raw) {
		return new ResultCache<Map<Key<?>, Object>>() {

			/** */
			private LoadContext ctx;

			/** */
			@Override
			public Map<Key<?>, Object> nowUncached() {
				final Map<Key<?>, Object> result = new HashMap<>(raw.now().size() * 2);

				ctx = new LoadContext(LoadEngine.this);

				for (final Entity ent: raw.now().values()) {
					final Key<?> key = Key.create(ent.getKey());
					final Object entity = load(ent, ctx);
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
	public Result<Map<com.google.cloud.datastore.Key, Entity>> fetch(Set<com.google.cloud.datastore.Key> keys) {
		log.debug("Fetching {} keys: {}", keys.size(), keys);

		final Future<Map<com.google.cloud.datastore.Key, Entity>> fut = datastore.get(keys, readOptions.toArray(new ReadOption[readOptions.size()]));
		return ResultAdapter.create(fut);
	}

	/**
	 * Converts a datastore entity into a typed pojo object
	 * @return an assembled pojo, or the Entity itself if the kind is not registered, or null if the input value was null
	 */
	@SuppressWarnings("unchecked")
	public <T> T load(final BaseEntity<com.google.cloud.datastore.Key> ent, final LoadContext ctx) {
		if (ent == null)
			return null;

		final EntityMetadata<T> meta = ofy.factory().getMetadata(ent.getKey().getKind());
		if (meta == null)
			return (T)ent;
		else
			return meta.load(ent, ctx);
	}
}