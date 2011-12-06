package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;


/** 
 * A wrapper for partial entities.  When a partial entity which might be upgradable gets returned from
 * a translater, it gets wrapped in one of these.  Higher-level translators (say, EntityClassTranslator
 * or the collection, array, or map translators) understand this indicates that an upgrade should be
 * registered before the content is actually placed in the intended field/collection/etc.
 */
public class Partial<T>
{
	Key<T> key;
	T value;
	
	public Partial(Key<T> key, T value) {
		this.key = key;
		this.value = value;
	}
	
	public Key<T> getKey() { return key; }
	public T getValue() { return value; }
}