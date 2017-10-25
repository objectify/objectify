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
	private final ObjectifyImpl ofy;

	/** */
	public DeferredSaverImpl(final ObjectifyImpl ofy) {
		this.ofy = ofy;
	}

	@Override
	public void entity(final Object entity) {
		ofy.deferSave(entity);
	}

	@Override
	public void entities(final Object... entities) {
		for (final Object entity: entities)
			entity(entity);
	}

	@Override
	public void entities(final Iterable<?> entities) {
		for (final Object entity: entities)
			entity(entity);
	}
}
