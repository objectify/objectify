package com.googlecode.objectify.impl;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;

/**
 * Determines the transactional behavior of an ObjectifyImpl instance. There are transactional and non-transactional
 * subclasses.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class Transactor<O extends Objectify>
{
	/** Our session */
	protected Session session;

	/**
	 * Construct a transactor with a fresh session
	 */
	public Transactor() {
		this(new Session());
	}

	/**
	 * Construct a transactor with an explicit session
	 */
	public Transactor(Session session) {
		this.session = session;
	}

	/**
	 * @return the session associated with this transaction state
	 */
	public Session getSession() {
		return session;
	}

	/**
	 * @return the transaction appropriate to this transaction state, or null if there is no transaction.
	 */
	abstract public TransactionImpl getTransaction();

	/**
	 * @param parent is the parent objectify instance; the one being transitioned from
	 * @return an Objectify instance that is suitable for transactionless execution. In the case of a
	 * transactor which is not in a transaction, probably this is the same as getObjectify().
	 */
	abstract public ObjectifyImpl<O> transactionless(ObjectifyImpl<O> parent);

	/**
	 * @see Objectify#execute(TxnType, Work)
	 */
	abstract public <R> R execute(ObjectifyImpl<O> parent, TxnType txnType, Work<R> work);

	/**
	 * @see Objectify#transact(Work)
	 */
	abstract public <R> R transact(ObjectifyImpl<O> parent, Work<R> work);

	/**
	 * @see Objectify#transactNew(int, Work)
	 */
	abstract public <R> R transactNew(ObjectifyImpl<O> parent, int limitTries, Work<R> work);
}