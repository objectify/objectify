package com.googlecode.objectify.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.Key;

/**
 * The basic session cache.  A lot easier than passing the generic arguments around!
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Session
{
	/** */
	private static final Logger log = Logger.getLogger(Session.class.getName());

	/** */
	private Map<Key<?>, SessionValue<?>> map = new HashMap<Key<?>, SessionValue<?>>();

	/**
	 * Add/overwrite a SV.
	 */
	public void add(Key<?> key, SessionValue<?> value) {
		if (log.isLoggable(Level.FINEST))
			log.finest("Adding to session: " + key + " -> " + value.getResult());

		map.put(key, value);
	}

	/** Add all entries in the other session to this one */
	public void addAll(Session other) {
		if (log.isLoggable(Level.FINEST))
			log.finest("Adding all values to session: " + other.map.keySet());

		map.putAll(other.map);
	}

	/** */
	@SuppressWarnings("unchecked")
	public <T> SessionValue<T> get(Key<T> key) {
		return (SessionValue<T>)map.get(key);
	}

	/** */
	public boolean contains(Key<?> key) {
		return map.containsKey(key);
	}

	/** */
	public void clear() {
		if (log.isLoggable(Level.FINEST))
			log.finest("Clearing session");

		map.clear();
	}

	/** Convenient for debugging */
	@Override
	public String toString() {
		return map.toString();
	}
}
