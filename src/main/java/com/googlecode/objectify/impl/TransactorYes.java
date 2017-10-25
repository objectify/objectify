package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.util.ResultWrapper;

import java.util.concurrent.Future;

/**
 * Implementation for when we start a transaction.  Maintains a separate session, but then copies all
 * data into the original session on successful commit.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TransactorYes extends Transactor
{
	/** Our transaction. */
	protected Result<TransactionImpl> transaction;

	/** The non-transactional transactor that spawned us */
	protected TransactorNo parentTransactor;

	/**
	 */
	public TransactorYes(ObjectifyImpl current, TransactorNo parentTransactor) {
		super(current);

		this.parentTransactor = parentTransactor;

		// There is no overhead for XG transactions on a single entity group, so there is
		// no good reason to ever have withXG false when on the HRD.
		Future<Transaction> fut = current.createAsyncDatastoreService().beginTransaction(TransactionOptions.Builder.withXG(true));
		transaction = new ResultWrapper<Transaction, TransactionImpl>(new ResultAdapter<>(fut)) {
			private static final long serialVersionUID = 1L;

			@Override
			protected TransactionImpl wrap(Transaction raw) {
				return new TransactionImpl(raw, TransactorYes.this);
			}
		};
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#getTransaction()
	 */
	@Override
	public TransactionImpl getTransaction() {
		return this.transaction.now();
	}

	/**
	 * This version goes back to life without a transaction, but preserves current state regarding deadline, consistency, etc.
	 * We use the session from the parent, ie life before transactions.
	 */
	@Override
	public ObjectifyImpl transactionless(ObjectifyImpl parent) {
		ObjectifyImpl next = parent.clone();
		next.transactor = new TransactorNo(next, parentTransactor.getSession());
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(ObjectifyImpl parent, TxnType txnType, Work<R> work) {
		switch (txnType) {
			case MANDATORY:
			case REQUIRED:
			case SUPPORTS:
				return work.run();

			case NOT_SUPPORTED:
				final Objectify next = transactionless(parent);
				factory.push(next);
				try {
					return work.run();
				} finally {
					factory.pop(next);
				}

			case NEVER:
				throw new IllegalStateException("MANDATORY transaction but no transaction present");

			case REQUIRES_NEW:
				return transactNew(parent, Integer.MAX_VALUE, work);

			default:
				throw new IllegalStateException("Impossible, some unknown txn type");
		}

	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Transactor#transact(com.googlecode.objectify.impl.ObjectifyImpl, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(ObjectifyImpl parent, Work<R> work) {
		return work.run();
	}

	/**
	 * We need to make sure the parentSession is the transactionless session, not the session
	 * for our transaction.  This gives proper transaction isolation.
	 */
	@Override
	public <R> R transactNew(ObjectifyImpl parent, int limitTries, Work<R> work) {
		return transactionless(parent).transactNew(limitTries, work);
	}

	/**
	 * Called when the associated transaction is committed. Dumps the contents of the transactional session into the parent's
	 * session.
	 */
	public void committed() {
		parentTransactor.getSession().addAll(session);
	}
}