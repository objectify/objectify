package com.googlecode.objectify.impl;

import com.google.appengine.labs.repackaged.com.google.common.base.Preconditions;
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

	/**
	 */
	public TransactorNo(Objectify ofy, Session session) {
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
	public ObjectifyImpl<O> transactionless(ObjectifyImpl<O> parent) {
		return parent;
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
	private <R> R transactOnce(ObjectifyImpl<O> parent, Work<R> work) {
		ObjectifyImpl<O> txnOfy = startTransaction(parent);
		ObjectifyService.push(txnOfy);

		try {
			R result = work.run();
			txnOfy.flush();
			txnOfy.getTransaction().commit();
			return result;
		}
		finally
		{
			if (txnOfy.getTransaction().isActive()) {
				try {
					txnOfy.getTransaction().rollback();
				} catch (RuntimeException ex) {
					log.log(Level.SEVERE, "Rollback failed, suppressing error", ex);
				}
			}

			ObjectifyService.pop();
		}
	}

	/**
	 * Create a new transactional session by cloning this instance and resetting the transactor component.
	 */
	ObjectifyImpl<O> startTransaction(ObjectifyImpl<O> parent) {
		ObjectifyImpl<O> cloned = parent.clone();
		cloned.transactor = new TransactorYes<>(cloned, this);
		return cloned;
	}
}