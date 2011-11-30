/*
 */

package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnWork;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.SaveCmd;


/**
 * <p>Simple wrapper/decorator for an Objectify interface.</p>
 * 
 * <p>Use by subclassing like this: {@code class MyObjectify extends ObjectifyWrapper<MyObjectify>}</p>
 * 
 * <p>Be aware that chained settings require the wrapper to be cloned.</p>
 * 
 * @author Jeff Schnitzer
 */
public class ObjectifyWrapper<T extends ObjectifyWrapper<T>> implements Objectify, Cloneable
{
	/** */
	private Objectify base;
	
	/** Wraps the base objectify */
	public ObjectifyWrapper(Objectify ofy) {
		this.base = ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#load()
	 */
	@Override
	public LoadCmd load() {
		return base.load();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put()
	 */
	@Override
	public SaveCmd save() {
		return base.save();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete()
	 */
	@Override
	public Delete delete() {
		return base.delete();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	@Override
	public Transaction getTxn() {
		return base.getTxn();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	@Override
	public ObjectifyFactory getFactory() {
		return base.getFactory();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#consistency(com.google.appengine.api.datastore.ReadPolicy.Consistency)
	 */
	@Override
	public T consistency(Consistency policy) {
		T next = (T)this.clone();
		next.base = base.consistency(policy);
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#deadline(java.lang.Double)
	 */
	@Override
	public T deadline(Double value) {
		T next = (T)this.clone();
		next.base = base.deadline(value);
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#cache(boolean)
	 */
	@Override
	public T cache(boolean value) {
		T next = (T)this.clone();
		next.base = base.cache(value);
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transaction()
	 */
	@Override
	public T transaction() {
		T next = (T)this.clone();
		next.base = base.transaction();
		return next;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	protected T clone()
	{
		try {
			return (T)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // impossible
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#clear()
	 */
	@Override
	public void clear() {
		base.clear();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.TxnWork)
	 */
	@Override
	public <O extends Objectify, R> R transact(TxnWork<O, R> work) {
		return base.transact(work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(int, com.googlecode.objectify.TxnWork)
	 */
	@Override
	public <O extends Objectify, R> R transact(int limitTries, TxnWork<O, R> work) {
		return base.transact(limitTries, work);
	}
}