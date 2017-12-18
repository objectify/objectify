package com.googlecode.objectify.cache;

import lombok.Data;
import net.spy.memcached.CASValue;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface MemcacheService {

	@Data
	class CasValues {
		private final CASValue<Object> iv;
		private final Object nextToStore;
		private final int expirationSeconds;
	}

	Object get(final String key);

	Map<String, CASValue<Object>> getIdentifiables(final Collection<String> keys);

	Map<String, Object> getAll(final Collection<String> keys);

	void put(final String key, final Object thing);

	void putAll(final Map<String, Object> values);

	Set<String> putIfUntouched(final Map<String, CasValues> values);

	void deleteAll(final Collection<String> keys);
}
