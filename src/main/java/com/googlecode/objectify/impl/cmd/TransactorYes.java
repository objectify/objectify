package com.googlecode.objectify.impl.cmd;

import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.util.DatastoreIntrospector;
import com.googlecode.objectify.util.ResultWrapper;

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
	public TransactorYes(ObjectifyImpl ofy, TransactorNo parentTransactor) {
		super(ofy);

		this.parentTransactor = parentTransactor;

		// There is no overhead for XG transactions on a single entity group, so there is
		// no good reason to ever have withXG false when on the HRD.
		Future<Transaction> fut = ofy.createAsyncDatastoreService().beginTransaction(TransactionOptions.Builder.withXG(DatastoreIntrospector.SUPPORTS_XG));
		transaction = new ResultWrapper<Transaction, TransactionImpl>(new ResultAdapter<Transaction>(fut)) {
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
	public Objectify transactionless() {
		ObjectifyImpl next = ofy.clone();
		next.transactor = new TransactorNo(next, parentTransactor.getSession());
		return next;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(TxnType txnType, Work<R> work) {
		switch (txnType) {
			case MANDATORY:
			case REQUIRED:
			case SUPPORTS:
				return work.run();

			case NOT_SUPPORTED:
				try {
					ObjectifyService.push(transactionless());
					return work.run();
				} finally {
					ObjectifyService.pop();
				}

			case NEVER:
				throw new IllegalStateException("MANDATORY transaction but no transaction present");

			case REQUIRES_NEW:
				return transact(work);

			default:
				throw new IllegalStateException("Impossible, some unknown txn type");
		}

	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#transact(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(Work<R> work) {
		return work.run();
	}

	/**
	 * We need to make sure the parentSession is the transactionless session, not the session
	 * for our transaction.  This gives proper transaction isolation.
	 */
	@Override
	public <R> R transactNew(int limitTries, Work<R> work) {
		return transactionless().transactNew(limitTries, work);
	}

	/**
	 * Called when the associated transaction is committed. Dumps the contents of the transactional session into the parent's
	 * session.
	 */
	public void committed() {
		parentTransactor.getSession().addAll(session);
	}
}