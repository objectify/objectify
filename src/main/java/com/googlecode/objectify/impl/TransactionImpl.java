package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.FutureHelper;
import com.googlecode.objectify.util.SimpleFutureWrapper;
import com.googlecode.objectify.util.cmd.TransactionWrapper;

/** */
public class TransactionImpl extends TransactionWrapper {
	/**
	 * Holds the session data and knows what to do with it.
	 */
	private final TransactorYes<?> transactor;

	/**
	 * Operations which modify the session must be enlisted in the transaction and completed
	 * before the transaction commits.  This is so that the session reaches a consistent state
	 * before it is propagated to the parent session.
	 */
	private List<Result<?>> enlisted = new ArrayList<Result<?>>();

	/** */
	public TransactionImpl(Transaction raw, TransactorYes<?> transactor) {
		super(raw);
		this.transactor = transactor;
	}

	/**
	 * Enlist any operations that modify the session.
	 */
	public void enlist(Result<?> result) {
		enlisted.add(result);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.util.cmd.TransactionWrapper#commit()
	 */
	@Override
	public void commit() {
		FutureHelper.quietGet(commitAsync());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.util.cmd.TransactionWrapper#commitAsync()
	 */
	@Override
	public Future<Void> commitAsync() {
		// Complete any enlisted operations so that the session becomes consistent. Note that some of the
		// enlisted load operations might result in further enlistment... so we have to do this in a loop
		// that protects against concurrent modification exceptions
		while (!enlisted.isEmpty()) {
			List<Result<?>> last = enlisted;
			enlisted = new ArrayList<Result<?>>();

			for (Result<?> result: last)
				result.now();
		}

		return new SimpleFutureWrapper<Void, Void>(super.commitAsync()) {
			@Override
			protected Void wrap(Void nothing) throws Exception {
				transactor.committed();
				return nothing;
			}
		};
	}
}