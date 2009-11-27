package com.googlecode.objectify;

import com.google.appengine.api.datastore.DatastoreServiceFactory;

/**
 * Factory which allows us to construct implementations of the Objectify interface
 */
public class ObjectifyFactory
{
	/**
	 * @return a fresh Objectify from a fresh DatastoreService
	 */
	public static Objectify get()
	{
		return new ObjectifyImpl(DatastoreServiceFactory.getDatastoreService(), null);
	}
}