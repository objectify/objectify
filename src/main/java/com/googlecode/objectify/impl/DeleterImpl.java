package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.DeleteType;
import com.googlecode.objectify.cmd.Deleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Implementation of the Delete command.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeleterImpl implements Deleter {

	final ObjectifyImpl ofy;

	public DeleterImpl(final ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	@Override
	public DeleteType type(final Class<?> type) {
		return new DeleteTypeImpl(this, type);
	}

	@Override
	public Result<Void> key(final Key<?> key) {
		return this.keys(key);
	}

	@Override
	public Result<Void> keys(final Key<?>... keys) {
		return this.keys(Arrays.asList(keys));
	}

	@Override
	public Result<Void> keys(final Iterable<? extends Key<?>> keys) {
		return ofy.factory().span("delete", spanipulator -> {
			final List<com.google.cloud.datastore.Key> rawKeys = new ArrayList<>();
			for (final Key<?> key: keys)
				rawKeys.add(key.getRaw());

			final Result<Void> result = ofy.createWriteEngine().delete(rawKeys);

			// The low level API is not async, so let's ensure work is finished in the span.
			result.now();

			return result;
		});
	}

	@Override
	public Result<Void> entity(final Object entity) {
		return this.entities(entity);
	}

	@Override
	public Result<Void> entities(final Iterable<?> entities) {
		return ofy.factory().span("delete", spanipulator -> {
			final List<com.google.cloud.datastore.Key> keys = new ArrayList<>();
			for (final Object obj : entities)
				keys.add(ofy.factory().keys().anythingToRawKey(obj, ofy.getOptions().getNamespace()));

			final Result<Void> result = ofy.createWriteEngine().delete(keys);

			// The low level API is not async, so let's ensure work is finished in the span.
			result.now();

			return result;
		});
	}

	@Override
	public Result<Void> entities(final Object... entities) {
		return this.entities(Arrays.asList(entities));
	}
}
