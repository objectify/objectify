package com.googlecode.objectify.cache.tmp;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class MemcacheService {
	public Map<String, IdentifiableValue> getIdentifiables(final Collection<String> stringify) {
		return null;
	}

	public Map<String, Object> getAll(final Collection<String> stringify) {
		return null;
	}

	public void putAll(final Map<String, Object> stringify) {

	}

	public Set<String> putIfUntouched(final Map<String, CasValues> stringify) {
		return null;
	}

	public void deleteAll(final Collection<String> stringify) {

	}

	public void put(final String thing, final Object fetched) {

	}

	public Object get(final String thing) {
		return null;
	}

	public static class CasValues {
		public CasValues(final IdentifiableValue iv, final Object nextToStore, final Expiration expiration) {

		}
	}

	public static class IdentifiableValue {
		public Object getValue() {
			return null;
		}
	}
}
