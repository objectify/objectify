package com.googlecode.objectify.impl;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Value;
import lombok.RequiredArgsConstructor;

import java.util.Map;

/**
 * Implementation of the propertycontainer for dealing with Save situations - we adapt a {@code Builder} provided by the SDK.
 */
@RequiredArgsConstructor
public class SavePropertyContainer extends PropertyContainer
{
	private final FullEntity.Builder<IncompleteKey> builder;

	/** Start with a key-less, empty builder */
	public SavePropertyContainer() {
		this(FullEntity.newBuilder());
	}

	@Override
	public Value<?> getProperty(final String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setUnindexedProperty(final String name, final Value<?> value) {
		builder.set(name, value.toBuilder().setExcludeFromIndexes(true).build());
	}

	@Override
	public void setProperty(final String name, final Value<?> value) {
		builder.set(name, value);
	}

	@Override
	public FullEntity<IncompleteKey> toFullEntity() {
		return builder.build();
	}

	@Override
	public boolean hasProperty(final String name) {
		throw new UnsupportedOperationException();
	}

	@Override
	public IncompleteKey getKey() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Map<String, Value<?>> getProperties() {
		throw new UnsupportedOperationException();
	}
}