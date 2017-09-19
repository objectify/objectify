package com.googlecode.objectify;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.cmd.Deferred;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.util.Closeable;

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
public interface Objectify extends Closeable
{
	/**
	 * <p>Start a load command chain.  This is where you begin for any request that fetches data from
	 * the datastore: gets and queries.</p>
	 *
	 * <p>A quick example:
	 * {@code Map<Key<Thing>, Thing> things = ofy().load().type(Thing.class).parent(par).ids(123L, 456L);}</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
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
	 * {@code ofy().save().entities(e1, e2, e3).now();}</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @return the next step in the immutable command chain.
	 */
	Saver save();

	/**
	 * <p>Start a delete command chain.  Lets you delete entities or keys.</p>
	 *
	 * <p>Deletes do NOT cascade; if you wish to delete an object graph, you must delete each individual entity.</p>
	 *
	 * <p>A quick example:
	 * {@code ofy().delete().entities(e1, e2, e3).now();}</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @return the next step in the immutable command chain.
	 */
	Deleter delete();

	/**
	 * <p>Start a deferred command chain, which lets you make multiple save or delete calls on a single
	 * entity without incurring multiple datastore operations. Deferred operations are executed at the
	 * end of a unit-of-work (transaction, or http request if not in a transaction).</p>
	 *
	 * <p>Deferred operations are reflected in the session cache immediately. However query operations
	 * may not reflect these changes. For example, newly indexed entities may not show up, even with
	 * an otherwise strongly consistent ancestor query. This should not be surprising since the actual
	 * save operation has not occurred yet.</p>
	 *
	 * <p>In the case of deferred save() and delete() operations on the same entity, the last one wins.</p>
	 *
	 * @return the next step in the immutable command chain.
	 */
	Deferred defer();

	/**
	 * Obtain the ObjectifyFactory from which this Objectify instance was created.
	 *
	 * @return the ObjectifyFactory associated with this Objectify instance.
	 */
	ObjectifyFactory factory();

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
	Transaction getTransaction();

	/**
	 * <p>Executes work outside of a transaction.  If you are in a transaction, a new non-transaction context will be
	 * created using the session prior to the current transaction start. If you are not in a transaction, the work
	 * is executed in the current context.</p>
	 *
	 * <p>Within {@code Work.run()}, obtain the correct transactional {@code Objectify} instance by calling
	 * {@code ObjectifyService.ofy()}</p>
	 *
	 * @param work defines the work to be done outside of a transaction.
	 * @return the result of the work
	 */
	<R> R transactionless(Work<R> work);

	/**
	 * <p>Exactly the same behavior as the Work version, but doesn't return anything. Convenient for Java8
	 * so you don't have to return something from the lambda.</p>
	 */
	void transactionless(Runnable work);

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
	 * <p>Exactly the same behavior as the Work version, but doesn't return anything. Convenient for Java8
	 * so you don't have to return something from the lambda.</p>
	 */
	void transact(Runnable work);

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
	 * @param limitTries is the max # of tries. Must be > 0. A value of 1 means "try only once".
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
	 * Synchronously flushes any deferred operations to the datastore. Objectify does this for you at the end
	 * of transactions and requests, but if you need data to be written immediately - say, you're about to perform
	 * a strongly-consistent ancestor query and you need to see the updated indexes immediately - you can call this
	 * method. If there are no deferred operations, this does nothing.
	 */
	void flush();

	/**
	 * <p>Clears the session; all subsequent requests (or Ref<?>.get() calls) will go to the datastore/memcache
	 * to repopulate the session. This should rarely, if ever be necessary. Note that if you iterate query results
	 * you should only perform this action on chunk boundaries, otherwise performance will suffer. This is a "use
	 * only if you really know what you are doing" feature.</p>
	 */
	void clear();

	/**
	 * @return true if the key has been loaded into the session; false if loading the key would result in a datastore
	 * (or memcache) fetch.
	 */
	boolean isLoaded(Key<?> key);
}
