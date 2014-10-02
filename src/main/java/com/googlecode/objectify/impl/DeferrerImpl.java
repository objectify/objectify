package com.googlecode.objectify.impl;

import com.googlecode.objectify.cmd.DeferredDeleter;
import com.googlecode.objectify.cmd.DeferredSaver;
import com.googlecode.objectify.cmd.Deferrer;


/**
 * Implementation of the Deferrer interface.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeferrerImpl implements Deferrer
{
	/** */
	ObjectifyImpl<?> ofy;

	/** */
	public DeferrerImpl(ObjectifyImpl<?> ofy) {
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
