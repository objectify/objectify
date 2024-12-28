package com.googlecode.objectify.impl;

import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;

import java.util.Optional;

/**
 * Implementation for when we start a transaction.  Maintains a separate session, but then copies all
 * data into the original session on successful commit.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TransactorYes extends Transactor {

	/** Our transaction. */
	private final AsyncTransaction transaction;

	/** The non-transactional transactor that spawned us */
	private final TransactorNo parentTransactor;

	/**
	 * The low-level transaction object is created here.
	 */
	TransactorYes(final ObjectifyFactory factory, final boolean readOnly, final boolean cache, final TransactorNo parentTransactor, final Optional<ByteString> prevTxnHandle) {
		super(factory);

		this.transaction = factory.asyncDatastore(cache).newTransaction(readOnly, this::committed, prevTxnHandle);
		this.parentTransactor = parentTransactor;
	}

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
				throw new IllegalStateException("NEVER transaction type but transaction present");

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

	@Override
	public <R> R transact(final ObjectifyImpl parent, final Work<R> work) {
		return work.run();
	}

	@Override
	public <R> R transactReadOnly(final ObjectifyImpl parent, final Work<R> work) {
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
