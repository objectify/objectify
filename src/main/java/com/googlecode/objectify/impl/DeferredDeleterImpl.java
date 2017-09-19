package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
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
	private Transactor<?> transactor;
	private ObjectifyFactory factory;

	/** */
	DeferredDeleterImpl(Transactor<?> transactor, ObjectifyFactory factory) {
		this.transactor = transactor;
		this.factory = factory;
	}

	@Override
	public DeferredDeleteType type(Class<?> type) {
		return new DeferredDeleteTypeImpl(this, type);
	}

	@Override
	public void key(Key<?> key) {
		transactor.getDeferrer().deferDelete(key);
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
		key(factory().keys().anythingToKey(entity));
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

	public ObjectifyFactory factory() {
		return factory;
	}
}
