/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.googlecode.objectify.ObjectifyService;

/**
 * Gives us our custom version rather than the standard Objectify one.
 *
 * @author Jeff Schnitzer
 */
public class TestObjectifyService
{
	public static void initialize() {
		ObjectifyService.setFactory(new TestObjectifyFactory());
	}

	/**
	 * @return our extension to Objectify
	 */
	public static TestObjectify ofy() {
		return (TestObjectify)ObjectifyService.ofy();
	}

	/**
	 * @return our extension to ObjectifyFactory
	 */
	public static TestObjectifyFactory fact() {
		return (TestObjectifyFactory)ObjectifyService.factory();
	}

	/**
	 * Get a DatastoreService
	 */
	public static DatastoreService ds() {
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

//		Collection<Transaction> active = ds.getActiveTransactions();
//		if (active.size() > 0)
//			throw new IllegalStateException("Active is: " + active);

		return ds;
	}

}