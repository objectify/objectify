package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;

/**
 * Information we track internally about a Ref<?> on a cached entity.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Reference
{
	/** */
	final Property property;
	public Property getProperty() { return property; }

	/** */
	final Key<?> key;
	public Key<?> getKey() { return key; }

	/** */
	public Reference(Property property, Key<?> key) {
		this.property = property;
		this.key = key;
	}
}
