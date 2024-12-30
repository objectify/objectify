package com.googlecode.objectify.impl;

import io.opentelemetry.api.trace.Span;

import java.util.function.Consumer;

/**
 * Interface that allows span manipulation in optional otel tracing. If otel is not enabled,
 * pass the NOOP version and the manipulate callbacks will not be applied.
 */
public interface Spanipulator {
	Spanipulator NOOP = manipulate -> {};

	/** Call this to arbitrarily update a span */
	void update(Consumer<Span> manipulate);

	/** Add the query to the span */
	default void attach(final QueryDef queryDef) {
		update(span -> {
			span.setAttribute("db.query.summary", queryDef.toStringSummary());
			span.setAttribute("db.query.text", queryDef.toStringSanitized());

			if (queryDef.getNamespace() != null)
				span.setAttribute("db.namespace", queryDef.getNamespace());

			if (queryDef.getKind() != null)
				span.setAttribute("db.collection.name", queryDef.getKind());
		});
	}

}
