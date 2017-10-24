package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.util.ResultNow;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The basic session cache.  A lot easier than passing the generic arguments around!
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class Session
{
	/** */
	private final Map<Key<?>, SessionValue<?>> map = new HashMap<>();

	/**
	 * Add/overwrite a SV.
	 */
	public void add(final Key<?> key, final SessionValue<?> value) {
		if (log.isTraceEnabled())
			log.trace("Adding to session: {} -> {}", key, value.getResult());

		map.put(key, value);
	}

	/**
	 * Convenience method
	 */
	public void addValue(final Key<?> key, final Object value) {
		add(key, new SessionValue<>(new ResultNow<>(value)));
	}

	/** Add all entries in the other session to this one */
	public void addAll(final Session other) {
		if (log.isTraceEnabled())
			log.trace("Adding all values to session: {}", other.map.keySet());

		map.putAll(other.map);
	}

	/** */
	@SuppressWarnings("unchecked")
	public <T> SessionValue<T> get(final Key<T> key) {
		return (SessionValue<T>)map.get(key);
	}

	/** */
	public boolean contains(final Key<?> key) {
		return map.containsKey(key);
	}

	/** */
	public void clear() {
		log.trace("Clearing session");
		map.clear();
	}

	/** Convenient for debugging */
	@Override
	public String toString() {
		return map.toString();
	}

	/**
	 * @return all the keys currently in the session. If you really want this data, subclass ObjectifyImpl and
	 * use the protected getSession() method.
	 */
	public Set<Key<?>> keys() { return map.keySet(); }
}
