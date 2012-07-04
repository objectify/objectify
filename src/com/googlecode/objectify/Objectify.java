package com.googlecode.objectify;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;

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
	Loader load();
	
	/**
	 * <p>Start a save command chain.  Allows you to save (or re-save) entity objects.  Note that all command
	 * chain objects are immutable.</p>
	 * 
	 * <p>Saves do NOT cascade; if you wish to save an object graph, you must save each individual entity.</p>
	 * 
	 * <p>A quick example:
	 * {@code ofy.save().entities(e1, e2, e3).now();}</p>
	 * 
	 * @return the next step in the immutable command chain.
	 */
	Saver save();
	
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
	Deleter delete();
	
	/**
	 * <p>Get the underlying transaction object associated with this Objectify instance.  You typically
	 * do not need to use this; use transact() instead.</p>
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
	 * <p>Provides a new Objectify instance which uses (or doesn't use) a 2nd-level memcache.
	 * If true, Objectify will obey the @Cache annotation on entity classes,
	 * saving entity data to the GAE memcache service.  Fetches from the datastore
	 * for @Cache entities will look in the memcache service first.  This cache
	 * is shared across all versions of your application across the entire GAE
	 * cluster.</p>
	 * 
	 * @return a new Objectify instance which will (or won't) use the global cache
	 */
	Objectify cache(boolean value);
	
	/**
	 * <p>Creates a new Objectify instance that wraps a transaction.  The instance inherits any
	 * settings, but the session cache will be empty.  Upon successful commit, the contents
	 * of the session cache will be loaded back into the main session.</p>
	 * 
	 * <p>You typically don't need to use this.  Use the transact() method instead.</p>
	 * 
	 * @return a new Objectify instance with a fresh transaction
	 */
	Objectify transaction();

	/**
	 * <p>If you are in a transaction, this provides you an objectify instance which is outside of the
	 * current transaction and works with the session prior to the transaction start.  Inherits any
	 * settings (consistency, deadline, etc) from the present Objectify instance.</p>
	 * 
	 * <p>If you are not in a transaction, this simply returns "this".
	 * 
	 * @return an Objectify instance outside of a transaction, with the session as it was before txn start. 
	 */
	Objectify transactionless();

	/**
	 * The Work interface for an Objectify transaction.  This is typically what you will use to execute
	 * a transaction in transact().  It shortens the amount of typing you need to perform. 
	 */
	interface Work<R> extends TxnWork<Objectify, R> {}
	
	/**
	 * <p>ConcurrentModificationExceptions will cause the transaction to repeat as many times as necessary to
	 * finish the job. Work <b>MUST</b> idempotent.</p>
	 * 
	 * <p>Note that this is equivalent to execute(TxnType.REQUIRED_NEW, work);</p>
	 * 
	 * @param work defines the work to be done in a transaction.  After the method exits, the transaction will commit.
	 * @return the result of the work
	 */
	<O extends Objectify, R> R transact(TxnWork<O, R> work);

	/**
	 * <p>Executes the work in a new transaction, repeating up to limitTries times when a ConcurrentModificationException
	 * is thrown.  This requires your Work to be idempotent; otherwise limit tries to 1.
	 * 
	 * <p>The Objectify instance passed in to the Work run() method will have a transaction
	 * associated with it.  Typically you will pass in a subclass of Work, not TxnWork.</p>
	 * 
	 * @param work defines the work to be done in a transaction.  After the method exits, the transaction will commit.
	 * @return the result of the work
	 */
	<O extends Objectify, R> R transact(int limitTries, TxnWork<O, R> work);

	/**
	 * <p>Executes the work with the transactional behavior defined by the parameter txnType.  This is very similar
	 * to EJB semantics.  The work can inherit a transaction, create a new transaction, prevent transactions, etc.</p>
	 * 
	 * <p>ConcurrentModificationExceptions will cause the transaction to repeat as many times as necessary to
	 * finish the job. Work <b>MUST</b> idempotent.</p>
	 * 
	 * @param txnType defines what kind of transaction context the work should be executed in.
	 * @param work defines the work to be done in a transaction.  After the method exits, the transaction will commit.
	 * @return the result of the work
	 */
	<O extends Objectify, R> R execute(TxnType txnType, TxnWork<O, R> work);

	/**
	 * <p>Clears the session.  If, for example, you are iterating through large quantities of data
	 * you should clear the session after every iteration to prevent memory problems.</p>
	 */
	void clear();

	/**
	 * Sets the object instance that should be passed on by the base implementation in subsequent actions; for
	 * example, the Objectify instance that is passed to transact() Work.  You probably don't need to worry about
	 * this method; just subclass ObjectifyWrapper. 
	 */
	void setWrapper(Objectify ofy);
	
	/**
	 * Convert a POJO object to a native datastore Entity.  This is like a save() operation but without actually saving
	 * the data to the datastore.
	 * 
	 * @param pojo must be an instance of a registered pojo entity type.
	 * @return the native datastore Entity equivalent of the pojo; exactly what Objectify would save if you saved the POJO normally.
	 */
	Entity toEntity(Object pojo);
	
	/**
	 * Convert a native datastore Entity into a typed POJO.  This is like a load() operation except that you start with
	 * the native datastore type instead of fetching it from the datastore.  However, note that because of @Load annotations,
	 * it is possible that datastore operations will be executed during the translation.
	 * 
	 * @param entity is a native datastore entity which has an appropriate kind registered in the ObjectifyFactory.
	 * @return the POJO equivalent, just as if you had loaded the entity directly from Objectify.
	 */
	<T> T toPojo(Entity entity);
}
