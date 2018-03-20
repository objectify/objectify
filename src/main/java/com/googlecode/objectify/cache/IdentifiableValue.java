package com.googlecode.objectify.cache;

public interface IdentifiableValue {
	Object getValue();

	IdentifiableValue withValue(final Object value);
}
