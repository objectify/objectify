/*
 */

package com.googlecode.objectify.util.cmd;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;


/**
 * <p>Simple wrapper/decorator for an Objectify interface.</p>
 *
 * <p>Use by subclassing like this: {@code class MyObjectify extends ObjectifyWrapper<MyObjectify, MyFactory>}</p>
 *
 * <p>Be aware that chained settings require the wrapper to be cloned.</p>
 *
 * @author Jeff Schnitzer
 */
public class ObjectifyWrapper<W extends ObjectifyWrapper<W, F>, F extends ObjectifyFactory> extends ObjectifyImpl
{
	/** */
	public ObjectifyWrapper(ObjectifyFactory fact) {
		super(fact);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public F getFactory() {
		return (F)super.getFactory();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#consistency(com.google.appengine.api.datastore.ReadPolicy.Consistency)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public W consistency(Consistency policy) {
		return (W)super.consistency(policy);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#deadline(java.lang.Double)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public W deadline(Double value) {
		return (W)super.deadline(value);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#cache(boolean)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public W cache(boolean value) {
		return (W)super.cache(value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public W transactionless() {
		return (W)super.transactionless();
	}
}