/*
 */

package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.Put;


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
	
	/** Wraps the  */
	public ObjectifyWrapper(Objectify ofy)
	{
		this.base = ofy;
	}

	@Override
	public LoadCmd load()
	{
		return base.load();
	}

	@Override
	public Put put()
	{
		return base.put();
	}

	@Override
	public Delete delete()
	{
		return base.delete();
	}

	@Override
	public Transaction getTxn()
	{
		return base.getTxn();
	}

	@Override
	public ObjectifyFactory getFactory()
	{
		return base.getFactory();
	}

	@Override
	public T consistency(Consistency policy)
	{
		T next = (T)this.clone();
		next.base = base.consistency(policy);
		return next;
	}

	@Override
	public T deadline(Double value)
	{
		T next = (T)this.clone();
		next.base = base.deadline(value);
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

}