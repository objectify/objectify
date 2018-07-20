package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Transaction.Response;
import com.google.protobuf.ByteString;
import com.googlecode.objectify.Result;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** */
public class AsyncTransactionImpl extends AsyncDatastoreReaderWriterImpl implements PrivateAsyncTransaction {

	private final Transaction transaction;

	/**
	 * Hook that is run immediately after commit
	 */
	private final Runnable afterCommit;

	/**
	 * Operations which modify the session must be enlisted in the transaction and completed
	 * before the transaction commits.  This is so that the session reaches a consistent state
	 * before it is propagated to the parent session.
	 */
	private List<Result<?>> enlisted = new ArrayList<>();


	/** Listeners that will be executed _after_ a commit completes successfully */
	private final List<Runnable> listeners = new ArrayList<>();

	/**
	 * An arbitrary bag of stuff that can be associated with the current transaction context. Not used
	 * by Objectify internally but useful sometimes for application programming.
	 */
	@Getter
	private final Map<Object, Object> bag = new HashMap<>();

	/** */
	public AsyncTransactionImpl(final Transaction raw, final Runnable afterCommit) {
		super(raw);
		this.transaction = raw;
		this.afterCommit = afterCommit;
	}

	/**
	 * Enlist any operations that modify the session.
	 */
	@Override
	public void enlist(final Result<?> result) {
		enlisted.add(result);
	}

	/**
	 * Add a listener to be called after the transaction commits.
	 */
	@Override
	public void listenForCommit(final Runnable listener) {
		listeners.add(listener);
	}

	@Override
	public ByteString getTransactionHandle() {
		return transaction.getTransactionId();
	}

	@Override
	public void runCommitListeners() {
		for (final Runnable listener : listeners) {
			listener.run();
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.util.cmd.TransactionWrapper#commit()
	 */
	@Override
	public Response commit() {
		// Complete any enlisted operations so that the session becomes consistent. Note that some of the
		// enlisted load operations might result in further enlistment... so we have to do this in a loop
		// that protects against concurrent modification exceptions
		while (!enlisted.isEmpty()) {
			final List<Result<?>> last = enlisted;
			enlisted = new ArrayList<>();

			for (final Result<?> result: last)
				result.now();
		}

		final Response response = transaction.commit();
		afterCommit.run();
		return response;
	}

	@Override
	public boolean isActive() {
		return transaction.isActive();
	}

	@Override
	public void rollback() {
		transaction.rollback();
	}
}