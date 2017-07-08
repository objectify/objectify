package com.googlecode.objectify.cache;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService.CasValues;
import com.google.appengine.api.memcache.MemcacheService.IdentifiableValue;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Subset of MemcacheService used by EntityMemcache, but smart enough to translate Key into the stringified
 * version so that the memcache keys are intelligible. Also guards against calling through to the underlying
 * service when the operation is a no-op (ie, the collection of keys to operate on is empty).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyMemcacheService
{
	/** */
	private static final Function<Key, String> STRINGIFY = new Function<Key, String>() {
		@Override
		public String apply(Key input) {
			return KeyFactory.keyToString(input);
		}
	};

	/** */
	MemcacheService service;
  AsyncMemcacheService asyncService;
 
	/** */
	public KeyMemcacheService(MemcacheService service) {
		this.service = service;
		this.asyncService = MemcacheServiceFactory.getAsyncMemcacheService(service.getNamespace());
	}

	private <T> Map<Key, T> keyify(Map<String, T> stringified) {
		Map<Key, T> result = Maps.newLinkedHashMap();
		for (Map.Entry<String, T> entry: stringified.entrySet())
			result.put(KeyFactory.stringToKey(entry.getKey()), entry.getValue());

		return result;
	}

	private Set<Key> keyify(Set<String> stringified) {
		Set<Key> result = Sets.newLinkedHashSet();
		for (String str: stringified)
			result.add(KeyFactory.stringToKey(str));

		return result;
	}

	private <T> Map<String, T> stringify(Map<Key, T> keyified) {
		Map<String, T> result = Maps.newLinkedHashMap();
		for (Map.Entry<Key, T> entry: keyified.entrySet())
			result.put(KeyFactory.keyToString(entry.getKey()), entry.getValue());

		return result;
	}

	private Collection<String> stringify(Collection<Key> keys) {
		return Collections2.transform(keys, STRINGIFY);
	}

	public Map<Key, IdentifiableValue> getIdentifiables(Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
		
		Map<String, IdentifiableValue> map = service.getIdentifiables(stringify(keys));
		return keyify(map);
	}

	public Map<Key, Object> getAll(Collection<Key> keys) {
		if (keys.isEmpty())
			return Collections.emptyMap();
			
		Map<String, Object> map = service.getAll(stringify(keys));
		return keyify(map);
	}

	public void putAll(Map<Key, Object> map) {
		if (map.isEmpty())
			return;
		
//		service.putAll(stringify(map));
		asyncService.putAll(stringify(map));
	}

	public Set<Key> putIfUntouched(Map<Key, CasValues> map) {
		if (map.isEmpty())
			return Collections.emptySet();
		
		Set<String> result = service.putIfUntouched(stringify(map));
		return keyify(result);
	}

	public void deleteAll(Collection<Key> keys) {
		if (keys.isEmpty())
			return;
		
		service.deleteAll(stringify(keys));
	}

	@SuppressWarnings("deprecation")
	public void setErrorHandler(com.google.appengine.api.memcache.ErrorHandler handler) {
		service.setErrorHandler(handler);
	}
}
