package com.googlecode.objectify.impl;

import com.googlecode.objectify.cmd.Deferred;
import com.googlecode.objectify.cmd.DeferredDeleter;
import com.googlecode.objectify.cmd.DeferredSaver;


/**
 * Implementation of the Deferrer interface.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeferredImpl implements Deferred
{
	/** */
	ObjectifyImpl<?> ofy;

	/** */
	public DeferredImpl(ObjectifyImpl<?> ofy) {
		this.ofy = ofy;
	}

	@Override
	public DeferredSaver save() {
		return new DeferredSaverImpl(ofy);
	}

	@Override
	public DeferredDeleter delete() {
		return new DeferredDeleterImpl(ofy);
	}
}
