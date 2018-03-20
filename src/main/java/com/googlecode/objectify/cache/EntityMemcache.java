package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.googlecode.objectify.cache.MemcacheService.CasPut;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>This is the facade used by Objectify to cache entities in the MemcacheService.</p>
 *
 * <p>Entity cacheability and expiration are determined by a {@code CacheControl} object.
 * In addition, hit/miss statistics are tracked in a {@code MemcacheStats}.</p>
 *
 * <p>In order to guarantee cache synchronization, getAll() *must* be able to return
 * an IdentifiableValue, even for entries not present in the cache.  Because empty cache
 * values cannot be made into IdentifiableValue, we immediately replace them with a
 * null value and refetch (null is a valid cache value).  If this refetch doesn't work,
 * we treat the key as uncacheable for the duration of the request.</p>
 *
 * <p>The values put in memcache are Key -> Entity, except for negative cache entries,
 * which are Key -> String (the value NEGATIVE).</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class EntityMemcache
{
	/**
	 * A bucket represents memcache information for a particular Key.  It might have an entity,
	 * it might be a negative cache result, it might be empty.
	 *
	 * Buckets can be hash keys; they hash to their Key value.
	 */
	@EqualsAndHashCode(of="key")
	public class Bucket {
		/** Identifies the bucket */
		private final Key key;

		/**
		 * If null, this means the key is uncacheable (possibly because the cache is down).
		 * If not null, the IV holds the Entity or NEGATIVE.
		 */
		private final IdentifiableValue identifiableValue;

		/**
		 * The Entity to store in this bucket in a put().  Can be null to indicate a negative cache
		 * result.  The Entity key *must* match the bucket key.
		 */
		private Entity next;

		/**
		 * Crate a bucket with an uncacheable key.  Same as this(key, null).
		 */
		public Bucket(final Key key)
		{
			this(key, null);
		}

		/**
		 * @param identifiableValue can be null to indicate an uncacheable key
		 */
		public Bucket(final Key key, final IdentifiableValue identifiableValue) {
			this.key = key;
			this.identifiableValue = identifiableValue;
		}

		/** */
		public Key getKey() { return this.key; }

		/** @return true if we can cache this bucket; false if the key isn't cacheable or the memcache was down when we created the bucket */
		public boolean isCacheable() { return this.identifiableValue != null; }

		/** @return true if this is a negative cache result */
		public boolean isNegative() { return this.isCacheable() && NEGATIVE.equals(identifiableValue.getValue()); }

		/**
		 * "Empty" means we don't know the value - it could be null, it could be uncacheable, or we could have some
		 * really weird unknown data in the cache.  Basically, anything other than "yes we have an entity/negative"
		 * is considered empty.
		 *
		 * @return true if this is empty or uncacheable or something other than a nice entity or negative result.
		 */
		public boolean isEmpty() {
			return !this.isCacheable() || (!this.isNegative() && !(identifiableValue.getValue() instanceof Entity));
		}

		/** Get the entity stored at this bucket, possibly the one that was set */
		public Entity getEntity() {
			if (identifiableValue != null && identifiableValue.getValue() instanceof Entity)
				return (Entity)identifiableValue.getValue();
			else
				return null;
		}

		/**
		 * Prepare the value that will be set in memcache in the next putAll().
		 * Null (or not calling this method) will put a negative result in the cache.
		 */
		public void setNext(final Entity value)
		{
			this.next = value;
		}

		/**
		 * @return the actual value we should store in memcache based on the next value, ie possibly NEGATIVE
		 */
		private Object getNextToStore() {
			return (this.next == null) ? NEGATIVE : this.next;
		}
	}

	/**
	 * The value stored in the memcache for a negative cache result.
	 */
	public static final String NEGATIVE = "NEGATIVE";

	/** */
	private final String namespace;

	/** */
	private final KeyMemcacheService memcache;
	private final KeyMemcacheService memcacheWithRetry;

	@Getter
	private final MemcacheStats stats;

	private final CacheControl cacheControl;

	/**
	 * Creates a memcache which caches everything without expiry and doesn't record statistics.
	 */
	public EntityMemcache(final MemcacheService memcache, final String namespace) {
		this(memcache, namespace, key -> 0);
	}

	/**
	 * Creates a memcache which doesn't record stats
	 */
	public EntityMemcache(final MemcacheService memcache, final String namespace, final CacheControl cacheControl) {
		this(memcache, namespace, cacheControl, new MemcacheStats() {
			@Override public void recordHit(Key key) { }
			@Override public void recordMiss(Key key) { }
		});
	}

	public EntityMemcache(
			final MemcacheService memcacheService,
			final String namespace,
			final CacheControl cacheControl,
			final MemcacheStats stats) {

		this.namespace = namespace;
		this.memcache = new KeyMemcacheService(memcacheService);
		this.memcacheWithRetry = new KeyMemcacheService(MemcacheServiceRetryProxy.createProxy(memcacheService));
		this.stats = stats;
		this.cacheControl = cacheControl;
	}

	/**
	 * <p>Gets the Buckets for the specified keys.  A bucket is built around an IdentifiableValue so you can
	 * putAll() them without the risk of overwriting other threads' changes.  Buckets also hide the
	 * underlying details of storage for negative, empty, and uncacheable results.</p>
	 *
	 * <p>Note that worst case (a cold cache), obtaining each bucket might require three memcache requests:
	 * a getIdentifiable() which returns null, a put(null), and another getIdentifiable().  Since
	 * there is no batch getIdentifiable(), this is *per key*.</p>
	 *
	 * <p>When keys are uncacheable (per CacheControl) or the memcache is down, you will still get an empty
	 * bucket back.  The bucket will have null IdentifiableValue so we can identify it as uncacheable.</p>
	 *
	 * @return the buckets requested.  Buckets will never be null.  You will always get a bucket for every key.
	 */
	public Map<Key, Bucket> getAll(final Iterable<Key> keys) {
		final Map<Key, Bucket> result = new HashMap<>();

		// Sort out the ones that are uncacheable
		final Set<Key> potentials = new HashSet<>();

		for (final Key key: keys) {
			if (!cacheControl.isCacheable(key))
				result.put(key, new Bucket(key));
			else
				potentials.add(key);
		}

		Map<Key, IdentifiableValue> casValues;
		try {
			casValues = this.memcache.getIdentifiables(potentials);
		} catch (Exception ex) {
			// This should really only be a problem if the serialization format for an Entity changes,
			// or someone put a badly-serializing object in the cache underneath us.
			log.warn("Error obtaining cache for " + potentials, ex);
			casValues = new HashMap<>();
		}

		// Now create the remaining buckets
		for (final Key key: keys) {
			final IdentifiableValue casValue = casValues.get(key);	// Might be null, which means uncacheable
			final Bucket buck = new Bucket(key, casValue);
			result.put(key, buck);

			if (buck.isEmpty())
				this.stats.recordMiss(buck.getKey());
			else
				this.stats.recordHit(buck.getKey());
		}

		return result;
	}

	/**
	 * Update a set of buckets with new values.  If collisions occur, resets the memcache value to null.
	 *
	 * @param updates can have null Entity values, which will record a negative cache result.  Buckets must have
	 *  been obtained from getAll().
	 */
	public void putAll(final Collection<Bucket> updates) {
		final Set<Key> good = this.cachePutIfUntouched(updates);

		if (good.size() == updates.size())
			return;

		// Figure out which ones were bad
		final List<Key> bad = updates.stream()
				.map(Bucket::getKey)
				.filter(key -> !good.contains(key))
				.collect(Collectors.toList());

		if (!bad.isEmpty()) {
			// So we had some collisions.  We need to reset these back to null, but do it in a safe way - if we
			// blindly set null something already null, it will break any putIfUntouched() which saw the first null.
			// This could result in write contention starving out a real write.  The solution is to only reset things
			// that are not already null.

			final Map<Key, Object> cached = this.cacheGetAll(bad);

			// Remove the stuff we don't care about
			cached.values().removeIf(Objects::isNull);

			this.empty(cached.keySet());
		}
	}

	/**
	 * Revert a set of keys to the empty state.  Will loop on this several times just in case
	 * the memcache write fails - we don't want to leave the cache in a nasty state.
	 */
	public void empty(final Iterable<Key> keys) {
		final Map<Key, Object> updates = new HashMap<>();

		for (final Key key: keys)
			if (cacheControl.isCacheable(key))
				updates.put(key, null);

		this.memcacheWithRetry.putAll(updates);
	}

	/**
	 * Put buckets in the cache, checking for cacheability and collisions.
	 * @return the set of keys that were *successfully* handled. That includes buckets that were put without collision
	 * and buckets that didn't need to be cached.
	 */
	private Set<Key> cachePutIfUntouched(final Iterable<Bucket> buckets) {
		final Map<Key, CasPut> payload = new HashMap<>();
		final Set<Key> successes = new HashSet<>();

		for (final Bucket buck: buckets) {
			if (!buck.isCacheable()) {
				successes.add(buck.getKey());
				continue;
			}

			final Integer expirySeconds = cacheControl.getExpirySeconds(buck.getKey());
			if (expirySeconds == null) {
				successes.add(buck.getKey());
				continue;
			}

			payload.put(buck.getKey(), new CasPut(buck.identifiableValue, buck.getNextToStore(), expirySeconds));
		}

		successes.addAll(this.memcache.putIfUntouched(payload));

		return successes;
	}

	/**
	 * Bulk get on keys, getting the raw objects
	 */
	private Map<Key, Object> cacheGetAll(final Collection<Key> keys) {
		try {
			return this.memcache.getAll(keys);
		} catch (Exception ex) {
			// Some sort of serialization error, just wipe out the values
			log.warn("Error fetching values from memcache, deleting keys", ex);

			this.memcache.deleteAll(keys);

			return new HashMap<>();
		}
	}

	/**
	 * Basically a list comprehension of the keys for convenience.
	 */
	public static Set<Key> keysOf(final Collection<Bucket> buckets) {
		return buckets.stream().map(Bucket::getKey).collect(Collectors.toSet());
	}
}


