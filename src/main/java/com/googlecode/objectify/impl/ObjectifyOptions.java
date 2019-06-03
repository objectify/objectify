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
	private final String namespace;

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