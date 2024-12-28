package com.googlecode.objectify.impl;

import com.google.cloud.datastore.DatastoreException;
import com.google.common.base.Preconditions;
import com.google.protobuf.ByteString;
import com.google.rpc.Code;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import lombok.extern.slf4j.Slf4j;

/**
 * Transactor which represents the absence of a transaction.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
class TransactorNo extends Transactor {

	TransactorNo(final ObjectifyFactory factory) {
		super(factory);
	}

	TransactorNo(final ObjectifyFactory factory, final Session session) {
		super(factory, session);
	}

	@Override
	public AsyncTransactionImpl getTransaction() {
		// This version doesn't have a transaction, always null.
		return null;
	}

	@Override
	public ObjectifyImpl transactionless(ObjectifyImpl parent) {
		return parent;
	}

	@Override
	public <R> R execute(final ObjectifyImpl parent, final TxnType txnType, final Work<R> work) {
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

	@Override
	public <R> R transactionless(final ObjectifyImpl parent, final Work<R> work) {
		return work.run();
	}

	@Override
	public <R> R transact(final ObjectifyImpl parent, final Work<R> work) {
		return this.transactNew(parent, DEFAULT_TRY_LIMIT, work);
	}

	@Override
	public <R> R transactReadOnly(final ObjectifyImpl parent, final Work<R> work) {
		// It shouldn't matter if we call transactNew since it's not supposed to retry, but short-circuiting
		// out the retry logic seems wise.
		return transactOnce(parent, work, true, new AtomicReference<>());
	}

	@Override
	public <R> R transactNew(final ObjectifyImpl parent, int limitTries, final Work<R> work) {
		Preconditions.checkArgument(limitTries >= 1);
		final int ORIGINAL_TRIES = limitTries;

		final AtomicReference<ByteString> prevTxnHandle = new AtomicReference<>();
		while (true) {
			try {
				return transactOnce(parent, work, false, prevTxnHandle);
			} catch (DatastoreException ex) {

				if (!isRetryable(ex)) {
					throw ex;
				}

				if (--limitTries > 0) {
					log.warn("Retrying {} failure for {}: {}", ex.getReason(), work, ex);
					log.trace("Details of transaction failure", ex);
					try {
						// Do increasing backoffs with randomness
						Thread.sleep(Math.min(10000, (long) ((0.5 * Math.random() + 0.5) * 200 * (ORIGINAL_TRIES - limitTries + 2))));
					} catch (InterruptedException ignored) {
					}
				} else {
					throw new DatastoreException(ex.getCode(), "Failed retrying datastore " + ORIGINAL_TRIES + " times ", ex.getReason(), ex);
				}
			}
		}
	}

    private static boolean isRetryable(DatastoreException ex) {
        // ex.isRetryable() doesn't work because the SDK considers all transactions to be non-retryable. Objectify
        // has always assumed that transactions are idempotent and retries accordingly. So we have to explicitly
        // check against code 10, which is ABORTED. https://cloud.google.com/datastore/docs/concepts/errors
        // I hate this so much. Sometimes the transaction gets closed by the datastore during contention, and
        // then it proceeds to freak out and 503.
        return Code.ABORTED.getNumber() == ex.getCode() || ( // Behavior in the cloud
                Code.INVALID_ARGUMENT.getNumber() == ex.getCode() && (
                        // Behavior in Datastore emulator
                        ex.getMessage().contains("transaction closed") ||
                                // Behavior in Firestore emulator in Datastore mode
                                ex.getMessage().contains("Transaction is invalid or closed")
                )
        );
    }

	@Override
	public AsyncDatastoreReaderWriter asyncDatastore(final ObjectifyImpl ofy) {
		return ofy.factory().asyncDatastore(ofy.getOptions().isCache());
	}

	/**
	 * One attempt at executing a transaction
	 */
	private <R> R transactOnce(final ObjectifyImpl parent, final Work<R> work, final boolean readOnly, final AtomicReference<ByteString> prevTxnHandle) {
		final ObjectifyImpl txnOfy = parent.factory().open(
			parent.getOptions(),
			new TransactorYes(parent.factory(), readOnly, parent.getOptions().isCache(), this, Optional.ofNullable(prevTxnHandle.get()))
		);
		prevTxnHandle.set(txnOfy.getTransaction().getTransactionHandle());

		boolean committedSuccessfully = false;
		try {
			final R result = work.run();
			txnOfy.flush();
			txnOfy.getTransaction().commit();
			committedSuccessfully = true;
			return result;
		}
		finally {
			if (txnOfy.getTransaction().isActive()) {
				try {
					txnOfy.getTransaction().rollback();
				} catch (RuntimeException ex) {
					log.error("Rollback failed, suppressing error", ex);
				}
			}

			txnOfy.close();

			if (committedSuccessfully) {
				((PrivateAsyncTransaction)txnOfy.getTransaction()).runCommitListeners();
			}
		}
	}
}
