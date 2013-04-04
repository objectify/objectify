package com.googlecode.objectify.impl;

import com.googlecode.objectify.Ref;

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
	final Ref<?> ref;
	public Ref<?> getRef() { return ref; }

	/** */
	public Reference(Property property, Ref<?> ref) {
		this.property = property;
		this.ref = ref;
	}
}
