package com.googlecode.objectify.impl;

import com.google.common.base.Preconditions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import java.util.ConcurrentModificationException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Transactor which represents the absence of a transaction.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
/**
 * @author jeff
 *
 * @param <O>
 */
public class TransactorNo<O extends Objectify> extends Transactor<O>
{
	/** */
	private static final Logger log = Logger.getLogger(TransactorNo.class.getName());

	/**
	 */
	public TransactorNo(Objectify ofy) {
		super(ofy);
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
	 * @see com.googlecode.objectify.impl.cmd.Transactor#transactionless(com.googlecode.objectify.impl.ObjectifyImpl, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactionless(ObjectifyImpl<O> parent, Work<R> work) {
		return work.run();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.Transactor#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(ObjectifyImpl<O> parent, TxnType txnType, Work<R> work) {
		switch (txnType) {
			case MANDATORY:
				throw new IllegalStateException("MANDATORY transaction but no transaction present");

			case NOT_SUPPORTED:
			case NEVER:
			case SUPPORTS:
				return work.run();

			case REQUIRED:
			case REQUIRES_NEW:
				return transact(parent, work);

			default:
				throw new IllegalStateException("Impossible, some unknown txn type");
		}

	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Transactor#transact(com.googlecode.objectify.impl.ObjectifyImpl, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(ObjectifyImpl<O> parent, Work<R> work) {
		return this.transactNew(parent, Integer.MAX_VALUE, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Transactor#transactNew(com.googlecode.objectify.impl.ObjectifyImpl, int, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(ObjectifyImpl<O> parent, int limitTries, Work<R> work) {
		Preconditions.checkArgument(limitTries >= 1);

		while (true) {
			try {
				return transactOnce(parent, work);
			} catch (ConcurrentModificationException ex) {
				if (--limitTries > 0) {
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
	private <R> R transactOnce(ObjectifyImpl<O> ofy, Work<R> work) {
		TransactorYes<O> transactorYes = new TransactorYes<>(ofy, this);
		ofy.push(transactorYes);

		boolean committedSuccessfully = false;
		try {
			R result = work.run();
			ofy.flush();
			ofy.getTransaction().commit();
			committedSuccessfully = true;
			return result;
		}
		finally
		{
			if (ofy.getTransaction().isActive()) {
				try {
					ofy.getTransaction().rollback();
				} catch (RuntimeException ex) {
					log.log(Level.SEVERE, "Rollback failed, suppressing error", ex);
				}
			}

			ofy.pop(transactorYes);

			if (committedSuccessfully) {
				transactorYes.getTransaction().runCommitListeners();
			}
		}
	}
}