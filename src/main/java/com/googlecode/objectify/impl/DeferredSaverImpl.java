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
	private Transactor<?> transactor;

	/** */
	public DeferredSaverImpl(Transactor<?> transactor) {
		this.transactor = transactor;
	}

	@Override
	public void entity(Object entity) {
		transactor.getDeferrer().deferSave(entity);
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
