package com.googlecode.objectify;

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

}
