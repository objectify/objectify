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
public class DeleterImpl implements Deleter
{
	/** */
	final ObjectifyImpl ofy;

	/** */
	public DeleterImpl(final ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#type(java.lang.Class)
	 */
	@Override
	public DeleteType type(final Class<?> type) {
		return new DeleteTypeImpl(this, type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#key(com.googlecode.objectify.Key)
	 */
	@Override
	public Result<Void> key(final Key<?> key) {
		return this.keys(key);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#keys(com.googlecode.objectify.Key<?>[])
	 */
	@Override
	public Result<Void> keys(final Key<?>... keys) {
		return this.keys(Arrays.asList(keys));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Delete#values(java.lang.Iterable)
	 */
	@Override
	public Result<Void> keys(final Iterable<? extends Key<?>> keys) {
		final List<com.google.cloud.datastore.Key> rawKeys = new ArrayList<>();
		for (final Key<?> key: keys)
			rawKeys.add(key.getRaw());

		return ofy.createWriteEngine().delete(rawKeys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#entity(java.lang.Object)
	 */
	@Override
	public Result<Void> entity(final Object entity) {
		return this.entities(entity);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#entities(java.lang.Iterable)
	 */
	@Override
	public Result<Void> entities(final Iterable<?> entities) {
		final List<com.google.cloud.datastore.Key> keys = new ArrayList<>();
		for (final Object obj: entities)
			keys.add(ofy.factory().keys().anythingToRawKey(obj, ofy.getOptions().getNamespace()));

		return ofy.createWriteEngine().delete(keys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Deleter#entities(java.lang.Object[])
	 */
	@Override
	public Result<Void> entities(final Object... entities) {
		return this.entities(Arrays.asList(entities));
	}
}
