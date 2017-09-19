package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyOptions;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.cmd.Deferred;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * <p>Implementation of the Objectify interface. This is also suitable for subclassing; you
 * can return your own subclass by overriding ObjectifyFactory.begin().</p>
 *
 * <p>Note we *always* use the AsyncDatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl<O extends Objectify> implements Objectify
{
	/** The factory that produced us */
	protected ObjectifyFactory factory;

	/** Our options */
	protected final boolean cache;
	protected final Consistency consistency;
	protected final Double deadline;
	protected final boolean mandatoryTransactions;

	/** */
	protected Deque<Transactor<O>> transactors = new ArrayDeque<>();

	/**
	 */
	public ObjectifyImpl(ObjectifyOptions options, ObjectifyFactory fact) {
		this.factory = fact;
		this.cache = options.cache();
		this.consistency = options.consistency();
		this.deadline = options.deadline();
		this.mandatoryTransactions = options.mandatoryTransactions();
		push(new TransactorNo<O>(this));
	}

	/**
	 * @return The top of transactor in our transaction stack. This ensures that using the Objectify instance within
	 * a transaction operates on the correct transactor object.
	 */
	protected Transactor<O> transactor() {
		return transactors.getLast();
	}

	/** Allow transactors to push themselves onto the transaction stack of their parent Objectify. */
	protected void push(Transactor<O> next) {
		transactors.addLast(next);
	}

	/** Allow transactors to push themselves off of the transaction stack of their parent Objectify. */
	protected void pop(Transactor<O> last) {
		if (last != transactor()) {
			throw new IllegalStateException("Popping off transactors out of order.");
		}
		transactors.removeLast();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	public ObjectifyFactory factory() {
		return this.factory;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find()
	 */
	@Override
	public Loader load() {
		return new LoaderImpl<>(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put()
	 */
	@Override
	public Saver save() {
		return new SaverImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete()
	 */
	@Override
	public Deleter delete() {
		return new DeleterImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#defer()
	 */
	@Override
	public Deferred defer() {
		return new DeferredImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transactionless(com.googlecode.objectify.Work)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <R> R transactionless(Work<R> work) {
		return transactor().transactionless(this, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(java.lang.Runnable)
	 */
	@Override
	public void transactionless(final Runnable work) {
		transactionless(new VoidWork() {
			@Override
			public void vrun() {
				work.run();
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	public TransactionImpl getTransaction() {
		return transactor().getTransaction();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(TxnType txnType, Work<R> work) {
		return transactor().execute(this, txnType, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(Work<R> work) {
		return transactor().transact(this, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(java.lang.Runnable)
	 */
	@Override
	public void transact(final Runnable work) {
		transact(new VoidWork() {
			@Override
			public void vrun() {
				work.run();
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(Work<R> work) {
		return this.transactNew(Integer.MAX_VALUE, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transactNew(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(int limitTries, Work<R> work) {
		return transactor().transactNew(this, limitTries, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#clear()
	 */
	@Override
	public void clear() {
		transactor().getSession().clear();
	}

	/**
	 * Make a datastore service config that corresponds to our options.
	 */
	protected DatastoreServiceConfig createDatastoreServiceConfig() {
		DatastoreServiceConfig cfg = DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(consistency));

		if (deadline != null)
			cfg.deadline(deadline);

		return cfg;
	}

	/**
	 * Make a datastore service config that corresponds to our options.
	 */
	protected AsyncDatastoreService createAsyncDatastoreService() {
		return factory.createAsyncDatastoreService(this.createDatastoreServiceConfig(), cache);
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for saving and deleting
	 */
	protected static WriteEngine createWriteEngine(ObjectifyImpl<?> ofy, Transactor<?> transactor) {
		if (ofy.mandatoryTransactions && transactor.getTransaction() == null)
			throw new IllegalStateException("You have attempted save/delete outside of a transaction, but you have enabled ofy().mandatoryTransactions(true). Perhaps you wanted to start a transaction first?");

		return new WriteEngine(ofy, ofy.createAsyncDatastoreService(), transactor.getSession(), transactor.getDeferrer());
	}

	/** */
	protected Session getSession() {
		return this.transactor().getSession();
	}

	/** @return true if cache is enabled */
	public boolean getCache() {
		return cache;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#isLoaded(com.googlecode.objectify.Key)
	 */
	@Override
	public boolean isLoaded(Key<?> key) {
		return transactor().getSession().contains(key);
	}

	@Override
	public void flush() {
		transactor().getDeferrer().flush();
	}

	@Override
	public void close() {
		flush();
	}}