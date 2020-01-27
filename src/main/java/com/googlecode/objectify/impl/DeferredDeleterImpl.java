package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.DeferredDeleteType;
import com.googlecode.objectify.cmd.DeferredDeleter;


/**
 * Implementation of the Delete command.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeferredDeleterImpl implements DeferredDeleter
{
	/** */
	final ObjectifyImpl ofy;

	/** */
	DeferredDeleterImpl(final ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	@Override
	public DeferredDeleteType type(final Class<?> type) {
		return new DeferredDeleteTypeImpl(this, type);
	}

	@Override
	public void key(final Key<?> key) {
		ofy.deferDelete(key);
	}

	@Override
	public void keys(final Key<?>... keys) {
		for (final Key<?> key: keys)
			key(key);
	}

	@Override
	public void keys(final Iterable<? extends Key<?>> keys) {
		for (final Key<?> key: keys)
			key(key);
	}

	@Override
	public void entity(final Object entity) {
		key(ofy.factory().keys().anythingToKey(entity, ofy.getOptions().getNamespace()));
	}

	@Override
	public void entities(final Iterable<?> entities) {
		for (final Object entity: entities)
			entity(entity);
	}

	@Override
	public void entities(final Object... entities) {
		for (final Object entity: entities)
			entity(entity);
	}
}
