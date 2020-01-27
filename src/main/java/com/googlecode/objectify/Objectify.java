package com.googlecode.objectify;

import com.googlecode.objectify.cmd.Deferred;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.impl.AsyncTransaction;

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
	 * <p>Explicitly sets the namespace for keys generated in load, save, delete operations.</p>
	 *
	 * <p>This overrides the setting of {@code NamespaceManager.set()}.</p>
	 *
	 * <p>Note that this only affects implicitly created keys; if you pass key objects into load or delete methods, the
	 * namespace contained in the key will be used.</p>
	 *
	 * <pre>
	 *     final Key&lt;Foo&gt; key = Key.create(Foo.class, 123);
	 *     ofy().namespace("blah").load().key(key);  // namespace() has no effect! The key already has the default namespace.
	 *     ofy().namespace("blah").load().type(Foo.class).id(123);	// namespace() works as expected
	 * </pre>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object instead of modifying the
	 * current command object.</b></p>
	 *
	 * @param namespace is the namespace to pick; "" (empty string) forces the datastore default namespace; null
	 *                  restores the check for {@code NamespaceManager.set()}
	 * @return the next step in the immutable command chain, which allows you to start an operation.
	 */
	Objectify namespace(String namespace);

	/**
	 * <p>Provides a new Objectify instance with a limit, in seconds, for datastore calls.  If datastore calls take longer
	 * than this amount, a timeout exception will be thrown.</p>
	 *
	 * <p>The new instance will inherit all other characteristics (transaction, cache policy, session cache contents, etc)
	 * from this instance.</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @param value - limit in seconds, or null to indicate no deadline (other than the standard whole request deadline of 30s/10m).
	 * @return a new immutable Objectify instance with the specified deadline
	 *
	 * @deprecated This no longer does anything. Transport-level behavior is set via DatastoreOptions when you create
	 * the ObjectifyFactory. Altering this would require tearing down and re-establishing connections, which will have
	 * a negative performance impact. For better or worse, deadline is now a global setting.
	 */
	@Deprecated
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
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable Objectify instance which will (or won't) use the global cache
	 */
	Objectify cache(boolean value);

	/**
	 * <p>Provides a new Objectify instance which throws an exception whenever save() or delete() is
	 * called from outside a transaction context. This is a reasonable sanity check for most business
	 * workloads; you may wish to enable it globally by overriding ObjectifyFactory.begin() to
	 * twiddle this flag on the returned object.</p>
	 *
	 * <p>Objectify instances are mandatoryTransactions(false) by default.</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @return a new immutable Objectify instance which will (or won't) require transactions for save() and delete().
	 */
	Objectify mandatoryTransactions(boolean value);

	/**
	 * <p>This used to have meaning in the old GAE SDK but no longer does. Right now this is pretty much
	 * only useful as a null test to see if you are currently in a transaction. This method will probably
	 * be removed.</p>
	 */
	AsyncTransaction getTransaction();

	/**
	 * <p>Executes work outside of a transaction. This is a way to "escape" from a transaction and perform
	 * datastore operations that would otherwise not be allowed (or perhaps to load data without hitting entity group
	 * limits). If there is not already a transaction running, the work is executed normally.
	 * If there is not already a transaction context, a new transaction will be started.</p>
	 *
	 * <p>For example, to return an entity fetched outside of a transaction:
	 * {@code Thing th = ofy().transactionless(() -> ofy().load().key(thingKey).now())}</p>
	 *
	 * @param work defines the work to be done outside of a transaction
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
	 * <p>Exactly the same behavior as the Work version, but doesn't return anything. Convenient for Java8
	 * so you don't have to return something from the lambda.</p>
	 */
	void transactNew(Runnable work);

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
	 * <p>Exactly the same behavior as the Work version, but doesn't return anything. Convenient for Java8
	 * so you don't have to return something from the lambda.</p>
	 */
	void transactNew(int limitTries, Runnable work);

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
	 * <p>Exactly the same behavior as the Work version, but doesn't return anything. Convenient for Java8
	 * so you don't have to return something from the lambda.</p>
	 */
	void execute(TxnType txnType, Runnable work);

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
