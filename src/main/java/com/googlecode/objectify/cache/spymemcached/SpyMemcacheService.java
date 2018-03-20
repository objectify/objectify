package com.googlecode.objectify.cache.spymemcached;

import com.googlecode.objectify.cache.IdentifiableValue;
import com.googlecode.objectify.cache.MemcacheService;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClient;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Among the issues this impl needs to be concerned with is that memcached doesn't store nulls. We have to replace it
 * with something (in our case, an empty string).
 */
@RequiredArgsConstructor
public class SpyMemcacheService implements MemcacheService {
	/** Stored as a value to indicate that this is a null; memcached doesn't store actual nulls */
	private static final String NULL_VALUE = "";

	private final MemcachedClient client;

	private Object toCacheValue(final Object thing) {
		return thing == null ? NULL_VALUE : thing;
	}

	private Object fromCacheValue(final Object thing) {
		return NULL_VALUE.equals(thing) ? null : thing;
	}

	public Object get(final String key) {
		return fromCacheValue(client.get(key));
	}

	public Map<String, IdentifiableValue> getIdentifiables(final Collection<String> keys) {
		// Can't use streams because they don't allow nulls
		//return keys.stream().collect(Collectors.toMap(Functions.identity(), this::getIdentifiable));
		final Map<String, IdentifiableValue> result = new LinkedHashMap<>();

		for (final String key : keys) {
			final IdentifiableValue iv = getIdentifiable(key);
			result.put(key, iv);
		}

		return result;
	}

	private IdentifiableValue getIdentifiable(final String key) {
		final CASValue<Object> casValue = client.gets(key);
		if (casValue != null) {
			return new SpyIdentifiableValue(casValue);
		} else {
			client.set(key, 0, NULL_VALUE);	// use the fake null so that no other fetches get confused
			final CASValue<Object> try2 = client.gets(key);
			return try2 == null ? null : new SpyIdentifiableValue(try2);
		}
	}

	public Map<String, Object> getAll(final Collection<String> keys) {
		final Map<String, Object> map = client.getBulk(keys);

		final Map<String, Object> translated = new LinkedHashMap<>();
		map.forEach((key, value) -> translated.put(key, fromCacheValue(value)));
		return translated;
	}

	public void put(final String key, final Object value) {
		client.set(key, 0, toCacheValue(value));
	}

	public Set<String> putIfUntouched(final Map<String, CasPut> values) {
		final Set<String> successes = new HashSet<>();

		values.forEach((key, vals) -> {
			final long cas = ((SpyIdentifiableValue)vals.getIv()).getCasValue().getCas();
			final CASResponse response = client.cas(key, cas, vals.getExpirationSeconds(), toCacheValue(vals.getNextToStore()));
			if (response == CASResponse.OK) {
				successes.add(key);
			}
		});

		return successes;
	}

	public void putAll(final Map<String, Object> values) {
		// There is no bulk put
		values.forEach(this::put);
	}

	public void deleteAll(final Collection<String> keys) {
		// There is no bulk delete
		keys.forEach(client::delete);
	}

}
