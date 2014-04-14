/*
 */

package com.googlecode.objectify.util;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Identity-based Multimap that stores values in an ArrayList.
 *
 * @author Jeff Schnitzer
 */
public class IdentityMultimapList<K, V> extends IdentityHashMap<K, List<V>>
{
	private static final long serialVersionUID = 1L;

	/**
	 * Adds a value to the set associated with the key.
	 */
	public boolean add(K key, V value) {

		List<V> list = this.get(key);
		if (list == null) {
			list = new ArrayList<>();
			this.put(key, list);
		}

		return list.add(value);
	}
}