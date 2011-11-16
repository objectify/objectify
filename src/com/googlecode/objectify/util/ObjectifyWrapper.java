/*
 */

package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.Put;


/**
 * <p>Simple wrapper/decorator for an Objectify interface.</p>
 * 
 * @author Jeff Schnitzer
 */
public class ObjectifyWrapper implements Objectify
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
}