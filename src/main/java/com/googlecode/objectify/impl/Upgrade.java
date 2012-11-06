package com.googlecode.objectify.impl;

import com.googlecode.objectify.Ref;

/**
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Upgrade
{
	/** */
	final Property property;
	public Property getProperty() { return property; }

	/** */
	final Ref<?> ref;
	public Ref<?> getRef() { return ref; }

	/** */
	public Upgrade(Property property, Ref<?> ref) {
		this.property = property;
		this.ref = ref;
	}
}
