package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Key;
import com.google.common.collect.Collections2;
import com.googlecode.objectify.cache.tmp.MemcacheService;
import com.googlecode.objectify.cache.tmp.MemcacheService.CasValues;
import com.googlecode.objectify.cache.tmp.MemcacheService.IdentifiableValue;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Subset of MemcacheService used by EntityMemcache, but smart enough to translate Key into the stringified
 * version so that the memcache keys are intelligible. Also guards against calling through to the underlying
 * service when the operation is a no-op (ie, the collection of keys to operate on is empty).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class KeyMemcacheService
{
	/** */
	private final MemcacheService service;

	private <T> Map<Key, T> keyify(final Map<String, T> stringified) {
		return stringified.entrySet().stream()
				.collect(Collectors.toMap(e -> Key.fromUrlSafe(e.getKey()), Entry::getValue, throwingMerger(), LinkedHashMap::new));
	}

	private Set<Key> keyify(final Set<String> stringified) {
		return stringified.stream()
				.map(Key::fromUrlSafe)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private <T> Map<String, T> stringify(final Map<Key, T> keyified) {
		return keyified.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey().toUrlSafe(), Entry::getValue, throwingMerger(), LinkedHashMap::new));
	}

	private Collection<String> stringify(final Collection<Key> keys) {
		return Collections2.transform(keys, Key::toUrlSafe);
	}

	public Map<Key, IdentifiableValue> getIdentifiables(Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
		
		final Map<String, IdentifiableValue> map = service.getIdentifiables(stringify(keys));
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

	private static <T> BinaryOperator<T> throwingMerger() {
		return (u,v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); };
	}
}
