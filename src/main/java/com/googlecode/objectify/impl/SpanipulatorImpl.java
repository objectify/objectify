package com.googlecode.objectify.impl;

import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@RequiredArgsConstructor
public class SpanipulatorImpl implements Spanipulator {
	private final Span span;

	@Override
	public void update(final Consumer<Span> manipulate) {
		manipulate.accept(span);
	}
}
