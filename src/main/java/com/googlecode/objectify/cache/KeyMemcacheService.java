package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Key;
import com.google.common.collect.Collections2;
import com.googlecode.objectify.cache.MemcacheService.CasPut;
import lombok.RequiredArgsConstructor;
import net.spy.memcached.CASValue;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Like MemcacheService but translates keys and values into forms more palatable to the low level service. Also protects
 * against no-ops (empty collections). Also stores a sentinel value for null and replaces it with null on fetch.
 * Memcached doesn't store nulls (the old GAE SDK hid this from us).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class KeyMemcacheService
{
	/** Stored as a value to indicate that this is a null; memcached doesn't store actual nulls */
	static final String NULL_VALUE = "";

	/** */
	private final MemcacheService service;

	private Key fromCacheKey(final String key) {
		return Key.fromUrlSafe(key);
	}

	private String toCacheKey(final Key key) {
		return key.toUrlSafe();
	}

	private Collection<String> toCacheKeys(final Collection<Key> keys) {
		return Collections2.transform(keys, this::toCacheKey);
	}

	private Object toCacheValue(final Object thing) {
		return thing == null ? NULL_VALUE : thing;
	}

	private Object fromCacheValue(final Object thing) {
		return NULL_VALUE.equals(thing) ? null : thing;
	}

	public Map<Key, CASValue<Object>> getIdentifiables(final Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
		
		final Map<String, CASValue<Object>> map = service.getIdentifiables(toCacheKeys(keys));

		final Map<Key, CASValue<Object>> dataForApp = new LinkedHashMap<>();
		map.forEach((key, value) -> {
			final CASValue<Object> transformedValue = (value == null)
					? null
					: new CASValue<>(value.getCas(), fromCacheValue(value.getValue()));
			dataForApp.put(fromCacheKey(key), transformedValue);
		});
		return dataForApp;
	}

	public Map<Key, Object> getAll(final Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
			
		final Map<String, Object> map = service.getAll(toCacheKeys(keys));

		final Map<Key, Object> dataForApp = new LinkedHashMap<>();
		map.forEach((key, value) -> dataForApp.put(fromCacheKey(key), fromCacheValue(value)));
		return dataForApp;
	}

	public void putAll(final Map<Key, Object> map) {
		if (map.isEmpty())
			return;

		final Map<String, Object> dataForCache = new LinkedHashMap<>();
		map.forEach((key, value) -> dataForCache.put(toCacheKey(key), toCacheValue(value)));

		service.putAll(dataForCache);
	}

	public Set<Key> putIfUntouched(final Map<Key, CasPut> map) {
		if (map.isEmpty())
			return Collections.emptySet();

		final Map<String, CasPut> dataForCache = new LinkedHashMap<>();
		map.forEach((key, value) -> {
			final CasPut actualPut = new CasPut(value.getIv(), toCacheValue(value.getNextToStore()), value.getExpirationSeconds());
			dataForCache.put(toCacheKey(key), actualPut);
		});

		final Set<String> result = service.putIfUntouched(dataForCache);
		return result.stream()
				.map(this::fromCacheKey)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public void deleteAll(final Collection<Key> keys) {
		if (keys.isEmpty())
			return;

		final Collection<String> stringKeys = toCacheKeys(keys);
		service.deleteAll(stringKeys);
	}
}
