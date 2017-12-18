package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Key;
import com.google.common.collect.Collections2;
import com.googlecode.objectify.cache.MemcacheService.CasValues;
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
 * Subset of MemcacheService used by EntityMemcache, but smart enough to translate Key into the stringified
 * version so that the memcache keys are intelligible. Also guards against calling through to the underlying
 * service when the operation is a no-op (ie, the collection of keys to operate on is empty).
 *
 * Also handles storing a sentinel value for null and replacing it will null on fetch. Memcached doesn't store
 * nulls (the old GAE SDK hid this from us).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class KeyMemcacheService
{
	/** */
	private final MemcacheService service;

	private <T> Map<Key, T> keyify(final Map<String, T> stringified) {
		final Map<Key, T> keyified = new LinkedHashMap<>();
		stringified.forEach((key, value) -> keyified.put(Key.fromUrlSafe(key), (T)unhideNulls(value)));
		return keyified;
	}

	private Set<Key> keyify(final Set<String> stringified) {
		return stringified.stream()
				.map(Key::fromUrlSafe)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private <T> Map<String, T> stringify(final Map<Key, T> keyified) {
		final Map<String, T> stringified = new LinkedHashMap<>();
		keyified.forEach((key, value) -> stringified.put(key.toUrlSafe(), (T)hideNulls(value)));
		return stringified;
	}

	private Collection<String> stringify(final Collection<Key> keys) {
		return Collections2.transform(keys, Key::toUrlSafe);
	}

	private Object hideNulls(final Object thing) {
		return thing == null ? "" : null;
	}

	private Object unhideNulls(final Object thing) {
		return "".equals(thing) ? null : thing;
	}

	public Map<Key, CASValue<Object>> getIdentifiables(Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
		
		final Map<String, CASValue<Object>> map = service.getIdentifiables(stringify(keys));
		return keyify(map);
	}

	public Map<Key, Object> getAll(Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
			
		final Map<String, Object> map = service.getAll(stringify(keys));
		return keyify(map);
	}

	public void putAll(final Map<Key, Object> map) {
		if (map.isEmpty())
			return;
		
		service.putAll(stringify(map));
	}

	public Set<Key> putIfUntouched(final Map<Key, CasValues> map) {
		if (map.isEmpty())
			return Collections.emptySet();

		final Set<String> result = service.putIfUntouched(stringify(map));
		return keyify(result);
	}

	public void deleteAll(final Collection<Key> keys) {
		if (keys.isEmpty())
			return;
		
		service.deleteAll(stringify(keys));
	}
}
