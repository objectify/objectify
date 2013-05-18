package com.googlecode.objectify;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;

/**
 * <p>This is the main "business end" of Objectify.  It lets you load, save, and delete your typed POJO entities.</p>
 *
 * <p>{@code Objectify} instances are obtained by calling the static method {@code ObjectifyService.ofy()}.  This method
 * will always provide the correct {@code Objectify} instance for a given transactional context.  You can run
 * transactions by calling {@code Objectify.transact()} or {@code Objectify.transactNew()}; calling {@code ObjectifyService.ofy()}
 * within {@code Work.run()} will produce the correct {@code Objectify} instance associated with the correct transaction.</p>
 *
 * <p>Objectify instances are immutable but they are NOT thread-safe.  The instance contains
 * a session cache of entities that have been loaded from the instance.  You should never access an Objectify
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
	 * Obtain the ObjectifyFactory from which this Objectify instance was created.
	 *
	 * @return the ObjectifyFactory associated with this Objectify instance.
	 */
	public ObjectifyFactory factory();

	/**
	 * Use factory() instead.
	 */
	@Deprecated
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
	 * <p>Objectify instances are cache(true) by default.</p>
	 *
	 * @return a new Objectify instance which will (or won't) use the global cache
	 */
	Objectify cache(boolean value);

	/**
	 * <p>Get the underlying transaction object associated with this Objectify instance.  You typically
	 * do not need to use this; use transact() instead.</p>
	 *
	 * <p>Note that this is *not* the same as {@code DatastoreService.getCurrentTransaction()},
	 * which uses the Low-Level API's implicit transaction management.  Every transactional {@code Objectify}
	 * instance is associated with a specific {@code Transaction} object.</p>
	 *
	 * @return the low-level transaction associated with this Objectify instance,
	 *  or null if no transaction is associated with this instance.
	 */
	public Transaction getTransaction();

	/**
	 * <p>If you are in a transaction, this provides you an objectify instance which is outside of the
	 * current transaction and works with the session prior to the transaction start.  Inherits any
	 * settings (consistency, deadline, etc) from the present Objectify instance.</p>
	 *
	 * <p>If you are not in a transaction, this simply returns "this".</p>
	 *
	 * <p>This allows code to quickly "escape" a transactional context for the purpose of loading
	 * manipulating data without creating or affecting XG transactions.</p>
	 *
	 * @return an Objectify instance outside of a transaction, with the session as it was before txn start.
	 */
	Objectify transactionless();

	/**
	 * <p>Executes work in a transaction.  If there is already a transaction context, that context will be inherited.
	 * If there is not already a transaction context, a new transaction will be started.</p>
	 *
	 * <p>Within {@code Work.run()}, obtain the correct transactional {@code Objectify} instance by calling
	 * {@code ObjectifyService.ofy()}</p>
	 *
	 * <p>ConcurrentModificationExceptions will cause the transaction to repeat as many times as necessary to
	 * finish the job. Work <b>MUST</b> idempotent.</p>
	 *
	 * @param work defines the work to be done in a transaction.  If this method started a new transaction, it
	 * will be committed when work is complete.  If transactional context was inherited, no commit is issued
	 * until the full transaction completes normally.
	 * @return the result of the work
	 */
	<R> R transact(Work<R> work);

	/**
	 * <p>Executes work in a new transaction.  Note that this is equivalent to {@code transactNew(Integer.MAX_VALUE, work);}</p>
	 *
	 * <p>ConcurrentModificationExceptions will cause the transaction to repeat as many times as necessary to
	 * finish the job. Work <b>MUST</b> idempotent.</p>
	 *
	 * <p>Within {@code Work.run()}, obtain the new transactional {@code Objectify} instance by calling {@code ObjectifyService.ofy()}</p>
	 *
	 * @param work defines the work to be done in a transaction.  After the method exits, the transaction will commit.
	 * @return the result of the work
	 */
	<R> R transactNew(Work<R> work);

	/**
	 * <p>Executes the work in a new transaction, repeating up to limitTries times when a ConcurrentModificationException
	 * is thrown.  This requires your Work to be idempotent; otherwise limit tries to 1.
	 *
	 * <p>Within {@code Work.run()}, obtain the new transactional {@code Objectify} instance by calling {@code ObjectifyService.ofy()}</p>
	 *
	 * @param work defines the work to be done in a transaction.  After the method exits, the transaction will commit.
	 * @return the result of the work
	 */
	<R> R transactNew(int limitTries, Work<R> work);

	/**
	 * <p>Executes the work with the transactional behavior defined by the parameter txnType.  This is very similar
	 * to EJB semantics.  The work can inherit a transaction, create a new transaction, prevent transactions, etc.</p>
	 *
	 * <p>This method principally exists to facilitate implementation of AOP interceptors that provide EJB-like behavior.
	 * Usually you will call {@code transact()} or {@code transactNew()} when writing code.</p>
	 *
	 * <p>Note that ConcurrentModificationExceptions will cause the transaction to repeat as many times as necessary to
	 * finish the job. Work <b>MUST</b> idempotent.</p>
	 *
	 * <p>Within {@code Work.run()}, obtain the correct {@code Objectify} instance by calling {@code ObjectifyService.ofy()}</p>
	 *
	 * @param txnType defines what kind of transaction context the work should be executed in.
	 * @param work defines the work to be done; possibly in a transaction, possibly not as defined by txnType
	 * @return the result of the work
	 */
	<R> R execute(TxnType txnType, Work<R> work);

	/**
	 * <p>Clears the session; all subsequent requests (or Ref<?>.get() calls) will go to the datastore/memcache
	 * to repopulate the session. This should rarely, if ever be necessary. Note that if you iterate query results
	 * you should only perform this action on chunk boundaries, otherwise performance will suffer. This is a "use
	 * only if you really know what you are doing" feature.</p>
	 */
	void clear();

	/**
	 * Use save().toEntity() instead.
	 */
	@Deprecated
	Entity toEntity(Object pojo);

	/**
	 * Use load().fromEntity() instead.
	 */
	@Deprecated
	<T> T toPojo(Entity entity);

	/**
	 * @return true if the key has been loaded into the session; false if loading the key would result in a datastore
	 * (or memcache) fetch.
	 */
	boolean isLoaded(Key<?> key);
}
