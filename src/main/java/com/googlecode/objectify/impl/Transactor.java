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
	/** Our owner */
	protected ObjectifyImpl<O> ofy;

	/** Our session */
	protected Session session;

	/**
	 * Construct a transactor with a fresh session
	 */
	public Transactor(ObjectifyImpl<O> ofy) {
		this(ofy, new Session());
	}

	/**
	 * Construct a transactor with an explicit session
	 */
	public Transactor(ObjectifyImpl<O> ofy, Session session) {
		this.ofy = ofy;
		this.session = session;
	}

	/**
	 * @return the objectify impl instance associated with this transaction state
	 */
	public ObjectifyImpl<O> getObjectify() {
		return ofy;
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
	 * @return an Objectify instance that is suitable for transactionless execution. In the case of a
	 * transactor which is not in a transaction, probably this is the same as getObjectify().
	 */
	abstract public ObjectifyImpl<O> transactionless();

	/**
	 * @see Objectify#execute(TxnType, Work)
	 */
	abstract public <R> R execute(TxnType txnType, Work<R> work);

	/**
	 * @see Objectify#transact(Work)
	 */
	abstract public <R> R transact(Work<R> work);

	/**
	 * @see Objectify#transactNew(int, Work)
	 */
	abstract public <R> R transactNew(int limitTries, Work<R> work);
}