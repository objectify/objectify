package com.googlecode.objectify;

import java.util.HashMap;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * <p>Factory which allows us to construct implementations of the Objectify interface.
 * Just call {@code ObjectifyFactory.get()}.</p>
 * 
 * <p>Note that unlike the DatastoreService, there is no implicit transaction management.
 * You either create an Objectify without a transaction (by calling {@code get()} or you
 * create one with a transaction (by calling {@code beginTransaction()}.  If you create
 * an Objectify with a transaction, you should use it like this:</p>
 * <code>
 * 	Objectify data = ObjectifyFactory.beginTransaction()
 * 	try {
 * 		// do work
 * 		data.getTxn().commit();
 * 	}
 * 	finally {
 * 		if (data.getTxn().isActive()) data.getTxn().rollback();
 * 	}
 * </code>
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
	/** */
	private static Map<String, EntityMetadata> types = new HashMap<String, EntityMetadata>();
	
	/**
	 * @return an Objectify from the DatastoreService which does NOT use
	 *  transactions.
	 */
	public static Objectify begin()
	{
		return new ObjectifyImpl(DatastoreServiceFactory.getDatastoreService(), null);
	}
	
	/**
	 * @return an Objectify which uses a transaction.  Be careful, you cannot
	 *  access entities across differing entity groups. 
	 */
	public static Objectify beginTransaction()
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		return new ObjectifyImpl(ds, ds.beginTransaction());
	}
	
	/**
	 * Use this method when you already have a Transaction object, say one
	 * that was created by using the raw DatastoreService.  This method should
	 * not commonly be used.
	 * 
	 * @return an Objectify which uses the specified transaction.
	 */
	public static Objectify withTransaction(Transaction txn)
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		return new ObjectifyImpl(ds, txn);
	}
	
	/**
	 * <p>Registers a class with the system so that we can recompose an
	 * object from its key kind.  The default kind is the simplename
	 * of the class, overridden by the @Entity annotation.</p>
	 * 
	 * <p>This method must be called in a single-threaded mode, around the
	 * time of app initialization.  After all types are registered, the
	 * get() method can be called.</p>
	 */
	public static void register(Class<?> clazz)
	{
		String kind = getKind(clazz);
		types.put(kind, new EntityMetadata(clazz));
	}
	
	//
	// Methods equivalent to those on KeyFactory, but which use typed Classes instead of kind.
	//
	
	/** Creates a key for the class with the specified id */
	public static Key createKey(Class<?> kind, long id)
	{
		return KeyFactory.createKey(getKind(kind), id);
	}
	
	/** Creates a key for the class with the specified name */
	public static Key createKey(Class<?> kind, String name)
	{
		return KeyFactory.createKey(getKind(kind), name);
	}
	
	/** Creates a key for the class with the specified id having the specified parent */
	public static Key createKey(Key parent, Class<?> kind, long id)
	{
		return KeyFactory.createKey(parent, getKind(kind), id);
	}
	
	/** Creates a key for the class with the specified name having the specified parent */
	public static Key createKey(Key parent, Class<?> kind, String name)
	{
		return KeyFactory.createKey(parent, getKind(kind), name);
	}
	
	//
	// Friendly query creation methods
	//
	
	/**
	 * Creates a new kind-less query that finds entities.
	 * @see Query#Query()
	 */
	public static Query createQuery()
	{
		return new Query();
	}
	
	/**
	 * Creates a query that finds entities with the specified ancestor
	 * @see Query#Query(Key)
	 */
	public static Query createQuery(Key ancestor)
	{
		return new Query(ancestor);
	}
	
	/**
	 * Creates a query that finds entities with the specified type
	 * @see Query#Query(String)
	 */
	public static Query createQuery(Class<?> entityClazz)
	{
		return new Query(getKind(entityClazz));
	}
	
	/**
	 * Creates a query that finds entities with the specified type and ancestor
	 * @see Query#Query(String, Key)
	 */
	public static Query createQuery(Class<?> entityClazz, Key ancestor)
	{
		return new Query(getKind(entityClazz), ancestor);
	}

	//
	// Stuff which should only be necessary internally.
	//
	
	/**
	 * @return the kind associated with a particular entity class
	 */
	public static String getKind(Class<?> clazz)
	{
		javax.persistence.Entity entityAnn = clazz.getAnnotation(javax.persistence.Entity.class);
		if (entityAnn == null)
			return clazz.getSimpleName();
		else
			return entityAnn.name();
	}
	
	/**
	 * @return the metadata for a kind of entity based on its key
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public static EntityMetadata getMetadata(Key key)
	{
		EntityMetadata metadata = types.get(key.getKind());
		if (metadata == null)
			throw new IllegalArgumentException("No registered type for kind " + key.getKind());
		else
			return metadata;
	}
	
	/**
	 * @return the metadata for a kind of typed object
	 * @throws IllegalArgumentException if the kind has not been registered
	 */
	public static EntityMetadata getMetadata(Object obj)
	{
		EntityMetadata metadata = types.get(getKind(obj.getClass()));
		if (metadata == null)
			throw new IllegalArgumentException("No registered type for kind " + getKind(obj.getClass()));
		else
			return metadata;
	}
}