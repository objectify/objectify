package com.googlecode.objectify.impl;


/** 
 * Wrapper for a partial entity that still might be loadable in the future.  Partial entities are
 * pojo entities whose key fields have been set but are otherwise uninitialized.  If a deep translator
 * returns one of these, the ClassTranslator will recognize it and possibly note the property for update
 * sometime later.
 */
public class Partial<T> {
	T value;
	
	public Partial(T value) {
		this.value = value;
	}
	
	public T get() {
		return value;
	}
}