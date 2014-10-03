package com.googlecode.objectify.impl;

import com.googlecode.objectify.cmd.DeferredSaver;


/**
 * Implementation of the DeferredSaver interface.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeferredSaverImpl implements DeferredSaver
{
	/** */
	ObjectifyImpl<?> ofy;

	/** */
	public DeferredSaverImpl(ObjectifyImpl<?> ofy) {
		this.ofy = ofy;
	}

	@Override
	public void entity(Object entity) {
		ofy.deferSave(entity);
	}

	@Override
	public void entities(Object... entities) {
		for (Object entity: entities)
			entity(entity);
	}

	@Override
	public void entities(final Iterable<?> entities) {
		for (Object entity: entities)
			entity(entity);
	}
}
