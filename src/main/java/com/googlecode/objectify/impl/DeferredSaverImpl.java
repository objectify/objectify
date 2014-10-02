package com.googlecode.objectify.impl;

import com.googlecode.objectify.cmd.DeferredSaver;
import java.util.Arrays;
import java.util.Collections;


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
		this.entities(Collections.singleton(entity));
	}

	@Override
	public void entities(Object... entities) {
		this.entities(Arrays.asList(entities));
	}

	@Override
	public void entities(final Iterable<?> entities) {
		//return ofy.createWriteEngine().<E>save(entities);
	}
}
