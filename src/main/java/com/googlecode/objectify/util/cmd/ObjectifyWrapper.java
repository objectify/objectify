/*
 */

package com.googlecode.objectify.util.cmd;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;


/**
 * <p>Simple wrapper/decorator for an Objectify interface.</p>
 *
 * <p>Use by subclassing like this: {@code class MyObjectify extends ObjectifyWrapper<MyObjectify, MyFactory>}</p>
 *
 * <p>Be aware that chained settings require the wrapper to be cloned.</p>
 *
 * @author Jeff Schnitzer
 */
public class ObjectifyWrapper<W extends ObjectifyWrapper<W, F>, F extends ObjectifyFactory> implements Objectify, Cloneable
{
	/** */
	Objectify base;

	/** Wraps the base objectify */
	public ObjectifyWrapper(Objectify ofy) {
		this.base = ofy;
		this.base.setWrapper(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#load()
	 */
	@Override
	public Loader load() {
		return base.load();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put()
	 */
	@Override
	public Saver save() {
		return base.save();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete()
	 */
	@Override
	public Deleter delete() {
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
	@SuppressWarnings("unchecked")
	public F getFactory() {
		return (F)base.getFactory();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#consistency(com.google.appengine.api.datastore.ReadPolicy.Consistency)
	 */
	@Override
	public W consistency(Consistency policy) {
		W next = (W)this.clone();
		next.base = base.consistency(policy);
		next.base.setWrapper(next);
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#deadline(java.lang.Double)
	 */
	@Override
	public W deadline(Double value) {
		W next = (W)this.clone();
		next.base = base.deadline(value);
		next.base.setWrapper(next);
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#cache(boolean)
	 */
	@Override
	public W cache(boolean value) {
		W next = (W)this.clone();
		next.base = base.cache(value);
		next.base.setWrapper(next);
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transaction()
	 */
	@Override
	public W transaction() {
		W next = (W)this.clone();
		next.base = base.transaction();
		next.base.setWrapper(next);
		return next;
	}

	@Override
	public W transactionless() {
		W next = (W)this.clone();
		next.base = base.transactionless();
		next.base.setWrapper(next);
		return next;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	protected W clone()
	{
		try {
			return (W)super.clone();
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
	 * @see com.googlecode.objectify.Objectify#setWrapper(com.googlecode.objectify.Objectify)
	 */
	@Override
	public void setWrapper(Objectify ofy) {
		base.setWrapper(ofy);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(Work<R> work) {
		return base.transact(work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transactNew(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(Work<R> work) {
		return base.transactNew(work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transactNew(int, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(int limitTries, Work<R> work) {
		return base.transactNew(limitTries, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(TxnType txnType, Work<R> work) {
		return base.execute(txnType, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#toEntity(java.lang.Object)
	 */
	@Override
	public Entity toEntity(Object pojo) {
		return base.toEntity(pojo);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#toPojo(com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public <T> T toPojo(Entity entity) {
		return base.toPojo(entity);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#isLoaded(com.googlecode.objectify.Key)
	 */
	@Override
	public boolean isLoaded(Key<?> key) {
		return base.isLoaded(key);
	}

}