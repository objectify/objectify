package com.googlecode.objectify.impl;

import com.googlecode.objectify.ObjectifyFactory;
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
	private Transactor<?> transactor;
	private ObjectifyFactory factory;

	/** */
	public DeferredImpl(ObjectifyImpl<?> ofy) {
		this.transactor = ofy.transactor();
		this.factory = ofy.factory();
	}

	@Override
	public DeferredSaver save() {
		return new DeferredSaverImpl(transactor);
	}

	@Override
	public DeferredDeleter delete() {
		return new DeferredDeleterImpl(transactor, factory);
	}
}
