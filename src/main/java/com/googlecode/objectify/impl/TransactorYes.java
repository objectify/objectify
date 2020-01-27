package com.googlecode.objectify.impl;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;

/**
 * Implementation for when we start a transaction.  Maintains a separate session, but then copies all
 * data into the original session on successful commit.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TransactorYes extends Transactor
{
	/** Our transaction. */
	private final AsyncTransaction transaction;

	/** The non-transactional transactor that spawned us */
	private final TransactorNo parentTransactor;

	/**
	 */
	TransactorYes(final ObjectifyFactory factory, final boolean cache, final TransactorNo parentTransactor) {
		super(factory);

		this.transaction = factory.asyncDatastore(cache).newTransaction(this::committed);
		this.parentTransactor = parentTransactor;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#getTransaction()
	 */
	@Override
	public AsyncTransaction getTransaction() {
		return this.transaction;
	}

	/**
	 * This version goes back to life without a transaction, but preserves current options.
	 * We use the session from the parent, ie life before transactions.
	 */
	@Override
	public ObjectifyImpl transactionless(final ObjectifyImpl parent) {
		return parent.transactor(new TransactorNo(parent.factory(), parentTransactor.getSession()));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(final ObjectifyImpl parent, final TxnType txnType, final Work<R> work) {
		switch (txnType) {
			case MANDATORY:
			case REQUIRED:
			case SUPPORTS:
				return work.run();

			case NOT_SUPPORTED:
				return transactionless(parent, work);

			case NEVER:
				throw new IllegalStateException("MANDATORY transaction but no transaction present");

			case REQUIRES_NEW:
				return transactNew(parent, Transactor.DEFAULT_TRY_LIMIT, work);

			default:
				throw new IllegalStateException("Impossible, some unknown txn type");
		}
	}

	@Override
	public <R> R transactionless(final ObjectifyImpl parent, final Work<R> work) {
		final ObjectifyImpl ofy = parent.factory().open(parent.getOptions(), new TransactorNo(parent.factory(), parentTransactor.getSession()));
		try {
			return work.run();
		} finally {
			ofy.close();
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Transactor#transact(com.googlecode.objectify.impl.ObjectifyImpl, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(final ObjectifyImpl parent, final Work<R> work) {
		return work.run();
	}

	/**
	 * We need to make sure the parentSession is the transactionless session, not the session
	 * for our transaction.  This gives proper transaction isolation.
	 */
	@Override
	public <R> R transactNew(final ObjectifyImpl parent, final int limitTries, final Work<R> work) {
		return transactionless(parent).transactNew(limitTries, work);
	}

	/**
	 * Called when the associated transaction is committed. Dumps the contents of the transactional session into the parent's
	 * session.
	 */
	public void committed() {
		parentTransactor.getSession().addAll(getSession());
	}

	@Override
	public AsyncDatastoreReaderWriter asyncDatastore(final ObjectifyImpl ofy) {
		return this.transaction;
	}
}