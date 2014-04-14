/*
 */

package com.googlecode.objectify.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Multimap that stores values in an ArrayList.  It's not worth pulling in a guava dependency just
 * for this one class.
 *
 * @author Jeff Schnitzer
 */
public class MultimapList<K, V> extends HashMap<K, List<V>>
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