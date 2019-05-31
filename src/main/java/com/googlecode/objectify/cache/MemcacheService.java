package com.googlecode.objectify.cache;

import lombok.Data;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The interface that all memory cache services must implement. In theory you could write a redis (or whatnot)
 * based implementation, but this was designed around memcached.
 *
 * The implementation must handle 'null' as a value. Note that memcached doesn't handle this natively; the impl
 * takes care of translating it to something that can be stored.
 */
public interface MemcacheService {

	@Data
	class CasPut {
		private final IdentifiableValue iv;
		private final Object nextToStore;
		private final int expirationSeconds;
	}

	/**
	 * @return the value in memcache for this, or null if there was nothing there
	 */
	Object get(final String key);

	/**
	 * For cache implementations that don't handle a cold cache for a key (eg memcached), the implementation
	 * of this method needs to hide that behavior (ie, bootstrap an initial value so we can get a CAS value).
	 * That doesn't mean the result will always be a value; the bootstrap may fail (for whatever reason) and
	 * the resulting map value for a key will be null.
	 */
	Map<String, IdentifiableValue> getIdentifiables(final Collection<String> keys);

	Map<String, Object> getAll(final Collection<String> keys);

	/** Values can be null */
	void put(final String key, final Object thing);

	/** Values can be null */
	void putAll(final Map<String, Object> values);

	/**
	 * Values can be null
	 * @return a set of all the keys that succeeded
	 */
	Set<String> putIfUntouched(final Map<String, CasPut> values);

	void deleteAll(final Collection<String> keys);
}
