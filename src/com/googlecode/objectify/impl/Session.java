package com.googlecode.objectify.impl;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.Key;

/**
 * The basic session cache.  A lot easier than passing the generic arguments around!
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Session
{
	/** */
	private Map<Key<?>, SessionValue<?>> map = new HashMap<Key<?>, SessionValue<?>>();
	
	/**
	 * Add/overwrite a SE.
	 */
	public void add(SessionValue<?> se) {
		map.put(se.getKey(), se);
	}
	
	/** Add all entries in the other session to this one */
	public void addAll(Session other) {
		map.putAll(other.map);
	}
	
	/** */
	@SuppressWarnings("unchecked")
	public <T> SessionValue<T> get(Key<T> key) {
		return (SessionValue<T>)map.get(key);
	}
	
	/** */
	public void clear() {
		map.clear();
	}
	
	/** Normally this isn't necessary but it is currently used for a hack around save() operations */
	public SessionValue<?> remove(Key<?> key) {
		return map.remove(key);
	}
	
	/** Convenient for debugging */
	@Override
	public String toString() {
		return map.toString();
	}
}
