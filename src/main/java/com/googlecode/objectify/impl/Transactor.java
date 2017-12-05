package com.googlecode.objectify.impl;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import lombok.Getter;

/**
 * Determines the transactional behavior of an ObjectifyImpl instance. There are transactional and non-transactional
 * subclasses.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract class Transactor
{
	/** */
	protected final ObjectifyFactory factory;

	/** */
	protected final ObjectifyImpl ofy;

	/** The session associated with this transaction state */
	@Getter
	private final Session session;

	/** Any deferred operations in this context */
	@Getter
	private final Deferrer deferrer;

	/**
	 * Construct a transactor with a fresh session
	 */
	Transactor(final ObjectifyImpl ofy) {
		this(ofy, new Session());
	}

	/**
	 * Construct a transactor with an explicit session
	 */
	Transactor(final ObjectifyImpl ofy, final Session session) {
		this.ofy = ofy;
		this.factory = ofy.factory();
		this.session = session;
		this.deferrer = new Deferrer(ofy, session);
	}

	/**
	 * @return the transaction appropriate to this transaction state, or null if there is no transaction.
	 */
	abstract public AsyncTransaction getTransaction();

	/**
	 * @param parent is the parent objectify instance; the one being transitioned from
	 * @return an Objectify instance that is suitable for transactionless execution. In the case of a
	 * transactor which is not in a transaction, probably this is the same as getObjectify().
	 */
	@Deprecated
	abstract public ObjectifyImpl transactionless(ObjectifyImpl parent);

	/**
	 * @see Objectify#execute(TxnType, Work)
	 */
	abstract public <R> R execute(ObjectifyImpl parent, TxnType txnType, Work<R> work);

	/**
	 * @see Objectify#transactionless(Work)
	 */
	abstract public <R> R transactionless(ObjectifyImpl parent, Work<R> work);

	/**
	 * @see Objectify#transact(Work)
	 */
	abstract public <R> R transact(ObjectifyImpl parent, Work<R> work);

	/**
	 * @see Objectify#transactNew(int, Work)
	 */
	abstract public <R> R transactNew(ObjectifyImpl parent, int limitTries, Work<R> work);

	/** */
	abstract public AsyncDatastoreReaderWriter asyncDatastore();
}