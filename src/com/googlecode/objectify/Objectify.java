package com.googlecode.objectify;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.Put;

/**
 * <p>This is the main "business end" of Objectify.  It lets you find, put, and delete your typed POJO entities.</p>
 * 
 * <p>You can create an {@code Objectify} instance using {@code ObjectifyFactory.begin()}
 * or {@code ObjectifyFactory.beginTransaction()}.  A transaction (or lack thereof)
 * will be associated with the instance; by using multiple instances, you can interleave
 * calls between several different transactions.</p>
 * 
 * <p>Objectify instances are immutable but they are NOT thread-safe.  The instance may contain, for example,
 * a session cache of entities that have been loaded from the instance.  You should not access an Objectify
 * from more than one thread simultaneously.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Objectify
{
	/**
	 * <p>Start a load command chain.  This is where you begin for any request that fetches data from
	 * the datastore: gets and queries.  Note that all command objects are immutable.</p>
	 * 
	 * <p>A quick example:
	 * {@code Map<Key<Thing>, Thing> things = ofy.load().type(Thing.class).parent(par).ids(123L, 456L);}</p>
	 * 
	 * @return the next step in the immutable command chain.
	 */
	LoadCmd load();
	
	/**
	 * <p>Start a put command chain.  Allows you to save (or re-save) entity objects.  Note that all command
	 * chain objects are immutable.</p>
	 * 
	 * <p>Puts do NOT cascade; if you wish to save an object graph, you must save each individual entity.</p>
	 * 
	 * <p>A quick example:
	 * {@code ofy.put().entities(e1, e2, e3).now();}</p>
	 * 
	 * @return the next step in the immutable command chain.
	 */
	Put put();
	
	/**
	 * <p>Start a delete command chain.  Lets you delete entities or keys.  Note that all command chain
	 * objects are immutable.</p>
	 * 
	 * <p>Deletes do NOT cascade; if you wish to delete an object graph, you must delete each individual entity.</p>
	 * 
	 * <p>A quick example:
	 * {@code ofy.delete().entities(e1, e2, e3).now();}</p>
	 * 
	 * @return the next step in the immutable command chain.
	 */
	Delete delete();
	
	/**
	 * <p>Get the underlying transaction object associated with this Objectify instance.</p>
	 * 
	 * <p>Note that this is *not* the same as {@code DatastoreService.getCurrentTransaction()},
	 * which uses implicit transaction management.  Objectify does not use implicit (thread
	 * local) transactions.</p>
	 * 
	 * @return the transaction associated with this Objectify instance,
	 *  or null if no transaction is associated with this instance.
	 */
	public Transaction getTxn();

	/**
	 * Obtain the ObjectifyFactory from which this Objectify instance was created.
	 * 
	 * @return the ObjectifyFactory associated with this Objectify instance.
	 */
	public ObjectifyFactory getFactory();

	/**
	 * <p>Provides a new Objectify instance with the specified Consistency.  Generally speaking, STRONG consistency
	 * provides more consistent results more slowly; EVENTUAL consistency produces results quickly but they
	 * might be out of date.  See the 
	 * <a href="http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/ReadPolicy.Consistency.html">Appengine Docs</a>
	 * for more explanation.</p>
	 * 
	 * <p>The new instance will inherit all other characteristics (transaction, cache policy, session cache contents, etc)
	 * from this instance.</p>
	 *  
	 * @param policy the consistency policy to use.  STRONG load()s are more consistent but EVENTUAL load()s
	 *  are faster.
	 * @return a new Objectify instance with the consistency policy replaced
	 */
	Objectify consistency(Consistency policy);
	
	/**
	 * <p>Provides a new Objectify instance with a limit, in seconds, for datastore calls.  If datastore calls take longer
	 * than this amount, a timeout exception will be thrown.</p>
	 * 
	 * <p>The new instance will inherit all other characteristics (transaction, cache policy, session cache contents, etc)
	 * from this instance.</p>
	 *  
	 * @param value - limit in seconds, or null to indicate no deadline (other than the standard whole request deadline of 30s/10m).
	 * @return a new Objectify instance with the specified deadline
	 */
	Objectify deadline(Double value);
	
	/**
	 * <p>Provides a new Objectify instance with (or without) a session cache.  If true,
	 * a new session cache is started (even if there was a pre-existing one).  If false,
	 * the new Objectify will not have a session cache.</p>
	 * 
	 * <p>With a session cache, all entities fetched from the datastore (or the 2nd level memcache)
	 * will be stored as-is in a hashmap within the Objectify instance.  Repeated
	 * get()s or queries for the same entity will return the same object.</p>
	 * 
	 * @return a new Objectify instance with an empty or disabled cache
	 */
	Objectify sessionCache(boolean value);

	/**
	 * <p>Provides a new Objectify instance which uses (or doesn't use) a 2nd-level memcache.
	 * If true, Objectify will obey the @Cache annotation on entity classes,
	 * saving entity data to the GAE memcache service.  Fetches from the datastore
	 * for @Cache entities will look in the memcache service first.  This cache
	 * is shared across all versions of your application across the entire GAE
	 * cluster.</p>
	 * 
	 * @return a new Objectify instance which will (or won't) use the global cache
	 */
	Objectify globalCache(boolean value);
	
	/**
	 * Creates a new Objectify instance that wraps a transaction.  The instance inherits any
	 * settings (including the session cache).
	 * 
	 * @return a new Objectify instance with a fresh transaction
	 */
	Objectify transaction();

	/**
	 * Creates a new Objectify instance that does not have a transaction.  The instance inherits any
	 * settings (including the session cache).  This can be useful to continue an existing session
	 * cache beyond the commit() of a transaction.
	 * 
	 * @return a new Objectify instance without a transaction
	 */
	Objectify transactionless();
}
