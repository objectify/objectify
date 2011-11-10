package com.googlecode.objectify.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.CasValues;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

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
public class EntityMemcache
{
	private static final Logger log = Logger.getLogger(EntityMemcache.class.getName());
	
	/**
	 * A bucket represents memcache information for a particular Key.  It might have an entity,
	 * it might be a negative cache result, it might be empty.
	 * 
	 * Buckets can be hash keys; they hash to their Key value.
	 */
	public class Bucket
	{
		/** Identifies the bucket */
		private Key key;
		
		/**
		 * If null, this means the key is uncacheable (possibly because the cache is down). 
		 * If not null, the IV holds the Entity or NEGATIVE or EMPTY.
		 */
		private IdentifiableValue iv;
		
		/**
		 * The Entity to store in this bucket in a put().  Can be null to indicate a negative cache
		 * result.  The Entity key *must* match the bucket key.
		 */
		private Entity next;
		
		/**
		 * Crate a bucket with an uncacheable key.  Same as this(key, null).
		 */
		public Bucket(Key key)
		{
			this(key, null);
		}
		
		/**
		 * @param iv can be null to indicate an uncacheable key
		 */
		public Bucket(Key key, IdentifiableValue iv)
		{
			this.key = key;
			this.iv = iv;
		}
		
		/** */
		public Key getKey() { return this.key; }
		
		/** @return true if we can cache this bucket; false if the key isn't cacheable or the memcache was down when we created the bucket */
		public boolean isCacheable() { return this.iv != null; }

		/** @return true if this is a negative cache result */
		public boolean isNegative() { return this.isCacheable() && NEGATIVE.equals(iv.getValue()); }
		
		/**
		 * "Empty" means we don't know the value - it could be null, it could be uncacheable, or we could have some
		 * really weird unknown data in the cache.  Basically, anything other than "yes we have an entity/negative"
		 * is considered empty.
		 *  
		 * @return true if this is empty or uncacheable or something other than a nice entity or negative result.
		 */
		public boolean isEmpty()
		{
			return !this.isCacheable() || (!this.isNegative() && !(iv.getValue() instanceof Entity));
		}
		
		/** Get the entity stored at this bucket, possibly the one that was set */
		public Entity getEntity()
		{
			if (iv != null && iv.getValue() instanceof Entity)
				return (Entity)iv.getValue();
			else
				return null;
		}
		
		/**
		 * Prepare the value that will be set in memcache in the next putAll().
		 * Null (or not calling this method) will put a negative result in the cache.
		 */
		public void setNext(Entity value)
		{
			this.next = value;
		}
		
		/**
		 * @return the actual value we should store in memcache based on the next value, ie possibly NEGATIVE 
		 */
		private Object getNextToStore()
		{
			return (this.next == null) ? NEGATIVE : this.next;
		}

		/** */
		@Override
		public boolean equals(Object obj) { return this.key.equals(obj); }
		
		/** */
		@Override
		public int hashCode() { return this.key.hashCode(); }
	}
	
	/**
	 * The value stored in the memcache for a negative cache result.
	 */
	private static final String NEGATIVE = "NEGATIVE";
	
	/** */
	MemcacheService memcache;
	MemcacheService memcacheWithRetry;
	MemcacheStats stats;
	CacheControl cacheControl;
	
	/**
	 * Creates a memcache which caches everything without expiry and doesn't record statistics.
	 */
	public EntityMemcache(String namespace)
	{
		this(namespace, new CacheControl() {
			@Override
			public Integer getExpirySeconds(Key key) { return 0; }
		});
	}

	/**
	 * Creates a memcache which doesn't record stats
	 */
	public EntityMemcache(String namespace, CacheControl cacheControl)
	{
		this(namespace, cacheControl, new MemcacheStats() {
			@Override public void recordHit(Key key) { }
			@Override public void recordMiss(Key key) { }
		});
	}

	/**
	 */
	public EntityMemcache(String namespace, CacheControl cacheControl, MemcacheStats stats)
	{
		this.memcache = MemcacheServiceFactory.getMemcacheService(namespace);
		this.memcacheWithRetry = MemcacheServiceRetryProxy.createProxy(MemcacheServiceFactory.getMemcacheService(namespace));
		this.stats = stats;
		this.cacheControl = cacheControl;
	}

	/**
	 * <p>Gets the Buckets for the specified keys.  A bucket is built around an IdentifiableValue so you can
	 * putAll() them without the risk of overwriting other threads' changes.  Buckets also hide the
	 * underlying details of storage for negative, empty, and uncacheable results.</p>
	 * 
	 * <p>Note that worst case (a cold cache), obtaining each bucket might require three memcache requests:
	 * a getIdentifiable() which returns null, a put(EMPTY), and another getIdentifiable().  Since
	 * there is no batch getIdentifiable(), this is *per key*.</p>
	 * 
	 * <p>When keys are uncacheable (per CacheControl) or the memcache is down, you will still get an empty
	 * bucket back.  The bucket will have null IdentifiableValue so we can identify it as uncacheable.</p>
	 * 
	 * @return the buckets requested.  Buckets will never be null.  You will always get a bucket for every key.
	 */
	public Map<Key, Bucket> getAll(Iterable<Key> keys)
	{
		Map<Key, Bucket> result = new HashMap<Key, Bucket>();
		
		// Sort out the ones that are uncacheable
		Set<Key> potentials = new HashSet<Key>();
		
		for (Key key: keys)
		{
			if (cacheControl.getExpirySeconds(key) == null)
				result.put(key, new Bucket(key));
			else
				potentials.add(key);
		}

		Map<Key, IdentifiableValue> ivs = null;
		try {
			ivs = this.memcache.getIdentifiables(potentials);
		} catch (Exception ex) {
			// This should really only be a problem if the serialization format for an Entity changes,
			// or someone put a badly-serializing object in the cache underneath us.
			log.log(Level.WARNING, "Error obtaining cache for " + potentials, ex);
			ivs = new HashMap<Key, IdentifiableValue>();
		}
		
		// Figure out cold cache values
		Map<Key, Object> cold = new HashMap<Key, Object>();
		for (Key key: potentials)
			if (ivs.get(key) == null)
				cold.put(key, null);
		
		if (!cold.isEmpty())
		{
			// The cache is cold for those values, so start them out with nulls that we can make an IV for
			this.memcache.putAll(cold);
			
			try {
				Map<Key, IdentifiableValue> ivs2 = this.memcache.getIdentifiables(cold.keySet());
				ivs.putAll(ivs2);
			} catch (Exception ex) {
				// At this point we should just not worry about it, the ivs will be null and uncacheable
			}
		}
		
		// Now create the remaining buckets
		for (Key key: keys)
		{
			// iv might still be null, which is ok - that means uncacheable
			IdentifiableValue iv = ivs.get(key);
			Bucket buck = (iv == null) ? new Bucket(key) : new Bucket(key, iv);
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
	public void putAll(Collection<Bucket> updates)
	{
		Set<Key> good = this.cachePutIfUntouched(updates);
		
		if (good.size() == updates.size())
			return;

		// Figure out which ones were bad
		List<Key> bad = new ArrayList<Key>();
		
		for (Bucket bucket: updates)
			if (!good.contains(bucket.getKey()))
				bad.add(bucket.getKey());

		if (!bad.isEmpty())
		{
			// So we had some collisions.  We need to reset these back to null, but do it in a safe way - if we
			// blindly set null something already null, it will break any putIfUntouched() which saw the first null.
			// This could result in write contention starving out a real write.  The solution is to only reset things
			// that are not already null.
			
			Map<Key, Object> cached = this.cacheGetAll(bad);
			
			// Remove the stuff we don't care about
			Iterator<Object> it = cached.values().iterator();
			while (it.hasNext())
			{
				Object value = it.next();
				if (value == null)
					it.remove();
			}
			
			this.empty(cached.keySet());
		}
	}

	/**
	 * Revert a set of keys to the empty state.  Will loop on this several times just in case
	 * the memcache write fails - we don't want to leave the cache in a nasty state.
	 */
	public void empty(Iterable<Key> keys)
	{
		Map<Key, Object> updates = new HashMap<Key, Object>();
		
		for (Key key: keys)
			if (cacheControl.getExpirySeconds(key) != null)
				updates.put(key, null);
		
		this.memcacheWithRetry.putAll(updates);
	}
	
	/**
	 * Put buckets in the cache, checking for cacheability and collisions.
	 * @return the set of keys that were *successfully* put without collision
	 */
	private Set<Key> cachePutIfUntouched(Iterable<Bucket> buckets)
	{
		Map<Key, CasValues> payload = new HashMap<Key, CasValues>();

		for (Bucket buck: buckets)
		{
			if (!buck.isCacheable())
				continue;
			
			Integer expirySeconds = cacheControl.getExpirySeconds(buck.getKey());
			if (expirySeconds == null)
				continue;
			
			Expiration expiration = expirySeconds == 0 ? null : Expiration.byDeltaSeconds(expirySeconds);
			
			payload.put(buck.getKey(), new CasValues(buck.iv, buck.getNextToStore(), expiration));
		}

		return this.memcache.putIfUntouched(payload);
	}

	/**
	 * Bulk get on keys, getting the raw objects
	 */
	private Map<Key, Object> cacheGetAll(Collection<Key> keys)
	{
		try {
			return this.memcache.getAll(keys);
		} catch (Exception ex) {
			// Some sort of serialization error, just wipe out the values
			log.log(Level.WARNING, "Error fetching values from memcache, deleting keys", ex);
			
			this.memcache.deleteAll(keys);
			
			return new HashMap<Key, Object>();
		}
	}

	/**
	 * Basically a list comprehension of the keys for convenience.
	 */
	public static Set<Key> keysOf(Iterable<Bucket> buckets)
	{
		Set<Key> keys = new HashSet<Key>();
		
		for (Bucket buck: buckets)
			keys.add(buck.getKey());
		
		return keys;
	}
}


