package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;

/**
 * Information we track internally about a Ref<?> on a cached entity.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionReference
{
	/** */
	final LoadConditions loadConditions;
	public LoadConditions getLoadConditions() { return loadConditions; }

	/** */
	final Key<?> key;
	public Key<?> getKey() { return key; }

	/** */
	public SessionReference(Key<?> key, LoadConditions loadConditions) {
		this.key = key;
		this.loadConditions = loadConditions;
	}
}
