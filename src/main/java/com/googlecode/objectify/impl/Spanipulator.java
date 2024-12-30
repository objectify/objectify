package com.googlecode.objectify.impl;

import io.opentelemetry.api.trace.Span;

import java.util.function.Consumer;

/**
 * Interface that allows span manipulation in optional otel tracing. If otel is not enabled,
 * pass the NOOP version and the manipulate callbacks will not be applied.
 */
public interface Spanipulator {
	Spanipulator NOOP = new Spanipulator() {
		@Override
		public void update(final Consumer<Span> manipulate) {}
	};

	/** Call this to update a span */
	void update(Consumer<Span> manipulate);
}
