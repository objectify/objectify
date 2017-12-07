package com.googlecode.objectify.impl;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Value;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of the propertycontainer for dealing with Load situations - we adapt a {@code BaseEntity<?>} provided by the SDK.
 */
@RequiredArgsConstructor
public class LoadPropertyContainer extends PropertyContainer
{
	private final BaseEntity<?> entity;

	@Override
	public Value<?> getProperty(final String name) {
		return entity.getValue(name);
	}

	@Override
	public void setProperty(final String name, final Value<?> value) {
		throw new UnsupportedOperationException();
	}

	@Override
	public FullEntity<IncompleteKey> toFullEntity() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean hasProperty(final String name) {
		return entity.contains(name);
	}

	@Override
	public IncompleteKey getKey() {
		return entity.getKey();
	}

	@Override
	public Map<String, Value<?>> getProperties() {
		return entity.getNames().stream().collect(Collectors.toMap(Function.identity(), entity::getValue));
	}
}