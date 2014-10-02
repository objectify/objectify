package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.DeferredDeleteType;
import com.googlecode.objectify.cmd.DeferredDeleter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


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
		this.keys(key);
	}

	@Override
	public void keys(Key<?>... keys) {
		this.keys(Arrays.asList(keys));
	}

	@Override
	public void keys(Iterable<? extends Key<?>> keys) {
		this.entities(keys);
	}

	@Override
	public void entity(Object entity) {
		this.entities(entity);
	}

	@Override
	public void entities(Iterable<?> entities) {
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<>();
		for (Object obj: entities)
			keys.add(ofy.factory().keys().anythingToRawKey(obj));

		//return ofy.createWriteEngine().delete(keys);
	}

	@Override
	public void entities(Object... entities) {
		this.entities(Arrays.asList(entities));
	}
}
