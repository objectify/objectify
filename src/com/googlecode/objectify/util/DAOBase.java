/*
 * $Id$
 */

package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.ObjectifyService;


/**
 * <p>Useful class for creating a basic DAO.  Typically you would extend this class
 * and register your entites in a static initializer, then provide higher-level
 * data manipulation methods as desired.</p>
 * 
 * <p>As you can see from the implementation, there isn't much to it.  You can easily
 * make your own DAO class without DAOBase if you so choose.</p>
 * 
 * <p>See <a href="http://code.google.com/p/objectify-appengine/wiki/BestPractices">BestPractices</a>.
 * for more guidance.</p>
 * 
 * @author Jeff Schnitzer
 */
public class DAOBase
{
	/** Need to create the lazy Objectify object */
	private ObjectifyOpts opts;
	
	/** A single objectify interface, lazily created */
	private Objectify lazyOfy;
	
	/** Creates a DAO without a transaction */
	public DAOBase()
	{
		this.opts = new ObjectifyOpts();
	}
	
	/**
	 * Creates a DAO possibly with a transaction.
	 */
	@Deprecated
	public DAOBase(boolean transactional)
	{
		this.opts = new ObjectifyOpts();
		if (transactional)
			opts.setTransactionOptions(TransactionOptions.Builder.withDefaults());
	}
	
	/**
	 * Creates a DAO with a certain set of options
	 */
	public DAOBase(ObjectifyOpts opts)
	{
		this.opts = opts;
	}
	
	/**
	 * Easy access to the factory object.  This is convenient shorthand for
	 * {@code ObjectifyService.factory()}.
	 */
	public ObjectifyFactory fact()
	{
		return ObjectifyService.factory();
	}

	/**
	 * Easy access to the objectify object (which is lazily created).
	 */
	public Objectify ofy()
	{
		if (this.lazyOfy == null)
			this.lazyOfy = ObjectifyService.factory().begin(this.opts);

		return this.lazyOfy;
	}
}