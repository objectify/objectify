package com.googlecode.objectify;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>Factory which allows us to construct implementations of the Objectify interface.
 * Just call {@code ObjectifyFactory.begin()}.</p>
 * 
 * <p>This class exposes a full set of static methods, but the actual implementations are
 * located on a singleton called {@code Factory}.  For further control, you can subclass
 * and inject that class - but for most use, these static methods suffice..</p>
 * 
 * <p>Note that unlike the DatastoreService, there is no implicit transaction management.
 * You either create an Objectify without a transaction (by calling {@code begin()} or you
 * create one with a transaction (by calling {@code beginTransaction()}.  If you create
 * an Objectify with a transaction, you should use it like this:</p>
 * <code><pre>
 * 	Objectify data = ObjectifyFactory.beginTransaction()
 * 	try {
 * 		// do work
 * 		data.getTxn().commit();
 * 	}
 * 	finally {
 * 		if (data.getTxn().isActive()) data.getTxn().rollback();
 * 	}
 * </pre></code>
 * 
 * <p>It would be fairly easy for someone to implement a ScanningObjectifyFactory
 * on top of this class that looks for @Entity annotations based on Scannotation or
 * Reflections, but this would add extra dependency jars and need a hook for
 * application startup.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyFactory
{
	/**
	 * @return an Objectify from the DatastoreService which does NOT use
	 *  transactions.  This is a lightweight operation and can be used freely.
	 */
	public static Objectify begin() { return Factory.instance().begin(); }
	
	/**
	 * @return an Objectify which uses a transaction.  Be careful, you cannot
	 *  access entities across differing entity groups. 
	 */
	public static Objectify beginTransaction() { return Factory.instance().beginTransaction(); }
	
	/**
	 * Use this method when you already have a Transaction object, say one
	 * that was created by using the raw DatastoreService.  This method should
	 * not commonly be used.
	 * 
	 * @return an Objectify which uses the specified transaction.
	 */
	public static Objectify withTransaction(Transaction txn) { return Factory.instance().withTransaction(txn); }
	
	/**
	 * <p>Registers a class with the system so that we can recompose an
	 * object from its key kind.  The default kind is the simplename
	 * of the class, overridden by the @Entity annotation.</p>
	 * 
	 * <p>This method must be called in a single-threaded mode, around the
	 * time of app initialization.  After all types are registered, the
	 * get() method can be called.</p>
	 */
	public static void register(Class<?> clazz) { Factory.instance().register(clazz); }
	
	/**
	 * <p>If this value is set to something greater than zero, Objectify will
	 * retry all calls to the Objectify interface whenever the datastore
	 * returns a DatastoreTimeoutException.  The datastore produces a trickle
	 * of these errors *all the time*, even in good health.  They can be
	 * retried without ill effect.</p>
	 * 
	 * <p>If you want more fine grained control of retries, you can wrap
	 * Objectify instances by calling DatastoreTimeoutRetryProxy.wrap() directly.</p>
	 * 
	 * <p>Beware setting this value to something large; sometimes the datastore
	 * starts choking and returning timeout errors closer to 100% of the time
	 * rather than the usual 0.1%-1%.</p>
	 * 
	 * @param value is the number of retries to attempt; ie 1 means two total tries
	 *  before giving up and propagating the DatastoreTimeoutException.
	 */
	public static void setDatastoreTimeoutRetryCount(int value) { Factory.instance().setDatastoreTimeoutRetryCount(value); }

	/**
	 * @return the current setting for datastore timeout retry count
	 */
	public static int getDatastoreTimeoutRetryCount() { return Factory.instance().getDatastoreTimeoutRetryCount(); }
	
	//
	// Methods equivalent to those on KeyFactory, but which use typed Classes instead of kind.
	//
	
	/** Creates a key for the class with the specified id */
	public static Key createKey(Class<?> kind, long id) { return Factory.instance().createKey(kind, id); }
	
	/** Creates a key for the class with the specified name */
	public static Key createKey(Class<?> kind, String name) { return Factory.instance().createKey(kind, name); }
	
	/** Creates a key for the class with the specified id having the specified parent */
	public static Key createKey(Key parent, Class<?> kind, long id) { return Factory.instance().createKey(parent, kind, id); }
	
	/** Creates a key for the class with the specified name having the specified parent */
	public static Key createKey(Key parent, Class<?> kind, String name) { return Factory.instance().createKey(parent, kind, name); }
	
	/**
	 * <p>Creates a key from a registered entity object that does *NOT* have
	 * a null id.  This method does not have an equivalent on KeyFactory.</p>
	 * 
	 * @throws IllegalArgumentException if the entity has a null id.
	 */
	public static Key createKey(Object entity) { return Factory.instance().createKey(entity); }
	
	//
	// Friendly query creation methods
	//
	
	/**
	 * Creates a new kind-less query that finds entities.
	 * @see Query#Query()
	 */
	public static Query createQuery() { return Factory.instance().createQuery(); }
	
	/**
	 * Creates a query that finds entities with the specified type
	 * @see Query#Query(String)
	 */
	public static Query createQuery(Class<?> entityClazz) { return Factory.instance().createQuery(entityClazz); }
	
	//
	// Stuff which should only be necessary internally.
	//
	
	/**
	 * @return the kind associated with a particular entity class
	 */
	public static String getKind(Class<?> clazz) { return Factory.instance().getKind(clazz); }
	
	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public static EntityMetadata getMetadata(Key key) { return Factory.instance().getMetadata(key); }
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public static EntityMetadata getMetadata(Object obj) { return Factory.instance().getMetadata(obj); }
}