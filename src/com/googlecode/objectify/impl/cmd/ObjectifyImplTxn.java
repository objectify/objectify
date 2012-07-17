package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.TxnWork;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.util.DatastoreIntrospector;
import com.googlecode.objectify.util.FutureHelper;
import com.googlecode.objectify.util.ResultWrapper;
import com.googlecode.objectify.util.SimpleFutureWrapper;
import com.googlecode.objectify.util.cmd.TransactionWrapper;

/**
 * Implementation for when we start a transaction.  Maintains a separate session, but then copies all
 * data into the original session on successful commit.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImplTxn extends ObjectifyImpl
{
	/** */
	public class TransactionImpl extends TransactionWrapper {
		/**
		 * Operations which modify the session must be enlisted in the transaction and completed
		 * before the transaction commits.  This is so that the session reaches a consistent state
		 * before it is propagated to the parent session.
		 */
		List<Result<?>> enlisted = new ArrayList<Result<?>>();
		
		/** */
		public TransactionImpl(Transaction raw) {
			super(raw);
		}
		
		/**
		 * Enlist any operations that modify the session.
		 */
		public void enlist(Result<?> result) {
			enlisted.add(result);
		}
		
		@Override
		public void commit() {
			FutureHelper.quietGet(commitAsync());
		}
		
		@Override
		public Future<Void> commitAsync() {
			// Complete any enlisted operations so that the session becomes consistent
			for (Result<?> result: enlisted)
				result.now();
			
			return new SimpleFutureWrapper<Void, Void>(super.commitAsync()) {
				@Override
				protected Void wrap(Void nothing) throws Exception {
					parentSession.addAll(session);
					return nothing;
				}
			};
		}
	}
	
	/** The transaction to use.  If null, do not use transactions. */
	protected Result<TransactionImpl> txn;
	
	/** The session is a simple hashmap */
	protected Session parentSession;

	/**
	 */
	public ObjectifyImplTxn(ObjectifyImpl parent) {
		super(parent);
		
		// There is no overhead for XG transactions on a single entity group, so there is
		// no good reason to ever have withXG false when on the HRD.
		Future<Transaction> fut = createAsyncDatastoreService().beginTransaction(TransactionOptions.Builder.withXG(DatastoreIntrospector.SUPPORTS_XG));
		txn = new ResultWrapper<Transaction, TransactionImpl>(new ResultAdapter<Transaction>(fut)) {
			@Override
			protected TransactionImpl wrap(Transaction raw) {
				return new TransactionImpl(raw);
			}
		};
		
		// Transactions get a new session, but are still linked to the old one
		parentSession = session;
		session = new Session();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	@Override
	public TransactionImpl getTxn() {
		return this.txn.now();
	}

	/**
	 * A little complicated because we need to make sure the parentSession is the transactionless session, not the session
	 * for our transaction.  This gives proper transaction isolation.
	 */
	@Override
	public Objectify transaction() {
		return transactionless().transaction();
	}
	
	/**
	 * This version goes back to life without a transaction, but preserves current state
	 */
	@Override
	public Objectify transactionless() {
		ObjectifyImpl impl = new ObjectifyImpl(this);
		impl.session = parentSession;
		return impl;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.ObjectifyImpl#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.TxnWork)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <O extends Objectify, R> R execute(TxnType txnType, TxnWork<O, R> work) {
		switch (txnType) {
			case MANDATORY:
			case REQUIRED:
			case SUPPORTS:
				return work.run((O)wrapper);
			
			case NOT_SUPPORTED:
				// TODO:  clean up this hack
				return work.run((O)((ObjectifyImpl)transactionless()).getWrapper());
				
			case NEVER:
				throw new IllegalStateException("MANDATORY transaction but no transaction present");
				 
			case REQUIRES_NEW:
				return transact(work);

			default:
				throw new IllegalStateException("Impossible, some unknown txn type");
		}
		
	}

	@Override
	@SuppressWarnings("unchecked")
	public <O extends Objectify, R> R transact(TxnWork<O, R> work) {
		return work.run((O)wrapper);
	}
}