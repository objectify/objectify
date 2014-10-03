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
	ObjectifyImpl<?> ofy;

	/** */
	DeferredDeleterImpl(ObjectifyImpl<?> ofy) {
		this.ofy = ofy;
	}

	@Override
	public DeferredDeleteType type(Class<?> type) {
		return new DeferredDeleteTypeImpl(this, type);
	}

	@Override
	public void key(Key<?> key) {
		ofy.deferDelete(key);
	}

	@Override
	public void keys(Key<?>... keys) {
		for (Key<?> key: keys)
			key(key);
	}

	@Override
	public void keys(Iterable<? extends Key<?>> keys) {
		for (Key<?> key: keys)
			key(key);
	}

	@Override
	public void entity(Object entity) {
		key(ofy.factory().keys().anythingToKey(entity));
	}

	@Override
	public void entities(Iterable<?> entities) {
		for (Object entity: entities)
			entity(entity);
	}

	@Override
	public void entities(Object... entities) {
		for (Object entity: entities)
			entity(entity);
	}
}
