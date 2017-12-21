package com.googlecode.objectify.cache;

import lombok.RequiredArgsConstructor;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.MemcachedClient;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public class MemcacheServiceImpl implements MemcacheService {

	private final MemcachedClient client;

	public Object get(final String key) {
		return client.get(key);
	}

	public Map<String, CASValue<Object>> getIdentifiables(final Collection<String> keys) {
		// Can't use streams because they don't allow nulls
		//return keys.stream().collect(Collectors.toMap(Functions.identity(), this::getIdentifiable));
		final Map<String, CASValue<Object>> result = new LinkedHashMap<>();

		for (final String key : keys) {
			final CASValue<Object> casValue = getIdentifiable(key);
			result.put(key, casValue);
		}

		return result;
	}

	private CASValue<Object> getIdentifiable(final String key) {
		final CASValue<Object> casValue = client.gets(key);
		if (casValue != null) {
			return casValue;
		} else {
			client.set(key, 0, KeyMemcacheService.NULL_VALUE);	// use the fakenull so that no other fetches get confused
			return client.gets(key);
		}
	}

	public Map<String, Object> getAll(final Collection<String> keys) {
		return client.getBulk(keys);
	}

	public void put(final String key, final Object value) {
		client.set(key, 0, value);
	}

	public Set<String> putIfUntouched(final Map<String, CasPut> values) {
		final Set<String> successes = new HashSet<>();

		values.forEach((key, vals) -> {
			final CASResponse response = client.cas(key, vals.getIv().getCas(), vals.getExpirationSeconds(), vals.getNextToStore());
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
