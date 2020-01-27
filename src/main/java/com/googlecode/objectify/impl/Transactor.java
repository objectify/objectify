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
public abstract class Transactor
{
	// Limit default number of retries to something high but non-infinite
	public static final int DEFAULT_TRY_LIMIT = 200;

	/** The session associated with this transaction state */
	@Getter
	private final Session session;

	/** Any deferred operations in this context */
	@Getter
	private final Deferrer deferrer;

	/**
	 * Construct a transactor with a fresh session
	 */
	Transactor(final ObjectifyFactory factory) {
		this(factory, new Session());
	}

	/**
	 * Construct a transactor with an explicit session
	 */
	Transactor(final ObjectifyFactory factory, final Session session) {
		this.session = session;
		this.deferrer = new Deferrer(factory, session);
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

	/**
	 * @param ofy */
	abstract public AsyncDatastoreReaderWriter asyncDatastore(final ObjectifyImpl ofy);
}