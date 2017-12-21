package com.googlecode.objectify.cache;

import lombok.Data;
import net.spy.memcached.CASValue;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MemcacheService {

	@Data
	class CasPut {
		private final CASValue<Object> iv;
		private final Object nextToStore;
		private final int expirationSeconds;
	}

	Object get(final String key);

	/**
	 * A special property of this method is that if the cache is cold for the keys, we bootstrap an initial
	 * value so that we can get a CAS value. That doesn't mean the result will always be a value; sometimes
	 * the bootstrap may fail (for whatever reason) and the resulting map value for a key will be null.
	 */
	Map<String, CASValue<Object>> getIdentifiables(final Collection<String> keys);

	Map<String, Object> getAll(final Collection<String> keys);

	void put(final String key, final Object thing);

	void putAll(final Map<String, Object> values);

	Set<String> putIfUntouched(final Map<String, CasPut> values);

	void deleteAll(final Collection<String> keys);
}
