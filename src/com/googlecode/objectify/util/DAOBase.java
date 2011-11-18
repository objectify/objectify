/*
 * $Id$
 */

package com.googlecode.objectify.util;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
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
	/** A single objectify interface */
	private Objectify ofy;
	
	/** Creates a DAO without a transaction */
	public DAOBase() {
		this(false);
	}
	
	/**
	 * Creates a DAO possibly with a transaction.
	 */
	public DAOBase(boolean transactional) {
		ofy = fact().begin();
		if (transactional)
			ofy = ofy.transaction();
	}
	
	/**
	 * Easy access to the factory object.  This is convenient shorthand for
	 * {@code ObjectifyService.factory()}.
	 */
	public ObjectifyFactory fact() {
		return ObjectifyService.factory();
	}

	/**
	 * Easy access to the objectify object
	 */
	public Objectify ofy() {
		return ofy;
	}
}