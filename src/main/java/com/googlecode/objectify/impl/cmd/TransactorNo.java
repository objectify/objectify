package com.googlecode.objectify.impl.cmd;

import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.impl.Session;

/**
 * Transactor which represents the absence of a transaction.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TransactorNo extends Transactor
{
	/** */
	private static final Logger log = Logger.getLogger(TransactorNo.class.getName());

	/**
	 */
	public TransactorNo(ObjectifyImpl ofy) {
		super(ofy);
	}

	/**
	 */
	public TransactorNo(ObjectifyImpl ofy, Session session) {
		super(ofy, session);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	@Override
	public TransactionImpl getTransaction() {
		// This version doesn't have a transaction, always null.
		return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#transactionless()
	 */
	@Override
	public Objectify transactionless() {
		return ofy;
	}

	/** Get the raw transaction we received from the AsyncDatastoreService (or CachingAsyncDatastoreService, if applicable) */
	private Transaction getTxnRaw() {
		if (getTransaction() == null)
			return null;
		else
			return getTransaction().getRaw();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(TxnType txnType, Work<R> work) {
		switch (txnType) {
			case MANDATORY:
				throw new IllegalStateException("MANDATORY transaction but no transaction present");

			case NOT_SUPPORTED:
			case NEVER:
			case SUPPORTS:
				return work.run();

			case REQUIRED:
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
		return this.transactNew(Integer.MAX_VALUE, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#transactNew(int, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(int limitTries, Work<R> work) {
		while (true) {
			try {
				return transactOnce(work);
			} catch (ConcurrentModificationException ex) {
				if (limitTries-- > 0) {
					if (log.isLoggable(Level.WARNING))
						log.warning("Optimistic concurrency failure for " + work + " (retrying): " + ex);

					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "Details of optimistic concurrency failure", ex);
				} else {
					throw ex;
				}
			}
		}
	}

	/**
	 * One attempt at executing a transaction
	 */
	private <R> R transactOnce(Work<R> work) {
		Objectify txnOfy = startTransaction();
		try {
			ObjectifyService.push(txnOfy);

			R result = work.run();
			txnOfy.getTransaction().commit();
			return result;
		}
		finally
		{
			ObjectifyService.pop();

			if (txnOfy.getTransaction().isActive()) {
				try {
					txnOfy.getTransaction().rollback();
				} catch (RuntimeException ex) {
					log.log(Level.SEVERE, "Rollback failed, suppressing error", ex);
				}
			}
		}
	}

	/**
	 * Create a new transactional session by cloning this instance and resetting the transactor component.
	 */
	Objectify startTransaction() {
		ObjectifyImpl cloned = ofy.clone();
		cloned.transactor = new TransactorYes(cloned, this);
		return cloned;
	}
}