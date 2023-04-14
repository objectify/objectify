package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Datastore;
import com.google.datastore.v1.TransactionOptions;
import com.google.protobuf.ByteString;

/** */
public class AsyncDatastoreImpl extends AsyncDatastoreReaderWriterImpl implements AsyncDatastore {

	private final Datastore datastore;

	/** */
	public AsyncDatastoreImpl(final Datastore raw) {
		super(raw);
		this.datastore = raw;
	}

	@Override
	public AsyncTransaction newTransaction(final Runnable afterCommit) {
		return newTransaction(afterCommit, null);
	}

	@Override
	public AsyncTransaction newTransaction(final Runnable afterCommit, final ByteString prevTxnHandle) {
		TransactionOptions.Builder txnOptions = TransactionOptions.newBuilder();
		if (prevTxnHandle != null) {
			txnOptions.getReadWriteBuilder().setPreviousTransaction(prevTxnHandle);
		}
		return new AsyncTransactionImpl(datastore.newTransaction(txnOptions.build()), afterCommit);
	}
}
