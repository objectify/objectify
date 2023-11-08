package com.googlecode.objectify.impl;

import lombok.RequiredArgsConstructor;
import lombok.Value;

/**
 * <p>Encapsulates the various options that can be twiddled in an objectify session. Immutable/functional.</p>
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Value
@RequiredArgsConstructor
public class ObjectifyOptions {

	boolean cache;
	boolean mandatoryTransactions;
	String namespace;

	ObjectifyOptions() {
		this(true, false, null);
	}

	public ObjectifyOptions cache(final boolean cache) {
		return new ObjectifyOptions(cache, mandatoryTransactions, namespace);
	}

	public ObjectifyOptions mandatoryTransactions(final boolean mandatoryTransactions) {
		return new ObjectifyOptions(cache, mandatoryTransactions, namespace);
	}

	public ObjectifyOptions namespace(final String namespace) {
		return new ObjectifyOptions(cache, mandatoryTransactions, namespace);
	}
}