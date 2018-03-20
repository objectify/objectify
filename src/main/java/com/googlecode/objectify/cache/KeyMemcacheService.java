package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Key;
import com.google.common.collect.Collections2;
import com.googlecode.objectify.cache.MemcacheService.CasPut;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Like MemcacheService but translates keys and values into forms more palatable to the low level service. Also protects
 * against no-ops (empty collections).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class KeyMemcacheService
{
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

	public Map<Key, IdentifiableValue> getIdentifiables(final Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
		
		final Map<String, IdentifiableValue> map = service.getIdentifiables(toCacheKeys(keys));

		final Map<Key, IdentifiableValue> dataForApp = new LinkedHashMap<>();
		map.forEach((key, value) -> dataForApp.put(fromCacheKey(key), value));
		return dataForApp;
	}

	public Map<Key, Object> getAll(final Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
			
		final Map<String, Object> map = service.getAll(toCacheKeys(keys));

		final Map<Key, Object> dataForApp = new LinkedHashMap<>();
		map.forEach((key, value) -> dataForApp.put(fromCacheKey(key), value));
		return dataForApp;
	}

	public void putAll(final Map<Key, Object> map) {
		if (map.isEmpty())
			return;

		final Map<String, Object> dataForCache = new LinkedHashMap<>();
		map.forEach((key, value) -> dataForCache.put(toCacheKey(key), value));

		service.putAll(dataForCache);
	}

	public Set<Key> putIfUntouched(final Map<Key, CasPut> map) {
		if (map.isEmpty())
			return Collections.emptySet();

		final Map<String, CasPut> dataForCache = new LinkedHashMap<>();
		map.forEach((key, value) -> {
			final CasPut actualPut = new CasPut(value.getIv(), value.getNextToStore(), value.getExpirationSeconds());
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
