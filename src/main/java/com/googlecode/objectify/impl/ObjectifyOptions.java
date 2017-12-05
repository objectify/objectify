package com.googlecode.objectify.impl;

import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * <p>Encapsulates the various options that can be twiddled in an objectify session. Immutable/functional.</p>
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Data
@RequiredArgsConstructor
public class ObjectifyOptions {

	private final boolean cache;
	private final boolean mandatoryTransactions;

	ObjectifyOptions() {
		this(true, false);
	}

	public ObjectifyOptions cache(final boolean value) {
		return new ObjectifyOptions(value, mandatoryTransactions);
	}

	public ObjectifyOptions mandatoryTransactions(final boolean value) {
		return new ObjectifyOptions(cache, value);
	}
}