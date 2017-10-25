package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
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
	private final Consistency consistency;
	private final Double deadline;
	private final boolean mandatoryTransactions;

	ObjectifyOptions() {
		this(true, Consistency.STRONG, null, false);
	}

	public ObjectifyOptions consistency(final Consistency value) {
		if (value == null)
			throw new IllegalArgumentException("Consistency cannot be null");

		return new ObjectifyOptions(cache, value, deadline, mandatoryTransactions);
	}

	public ObjectifyOptions deadline(final Double value) {
		return new ObjectifyOptions(cache, consistency, value, mandatoryTransactions);
	}

	public ObjectifyOptions cache(final boolean value) {
		return new ObjectifyOptions(value, consistency, deadline, mandatoryTransactions);
	}

	public ObjectifyOptions mandatoryTransactions(final boolean value) {
		return new ObjectifyOptions(cache, consistency, deadline, value);
	}
}