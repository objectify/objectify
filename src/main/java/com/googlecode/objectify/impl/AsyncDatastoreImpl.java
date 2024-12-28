package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Datastore;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.protobuf.ByteString;

import java.util.Optional;

/** */
public class AsyncDatastoreImpl extends AsyncDatastoreReaderWriterImpl implements AsyncDatastore {

	private final Datastore datastore;

	/** */
	public AsyncDatastoreImpl(final Datastore raw) {
		super(raw);
		this.datastore = raw;
	}

	@Override
	public AsyncTransaction newTransaction(final boolean readOnly, final Runnable afterCommit, final Optional<ByteString> prevTxnHandle) {
		final TransactionOptions.Builder txnOptions = TransactionOptions.newBuilder();

		if (readOnly) {
			txnOptions.setReadOnly(ReadOnly.newBuilder().build());
		}

		prevTxnHandle.ifPresent(handle -> {
			if (readOnly) {
				// setPreviousTransaction() doesn't exist on the readonly version, presumably because
				// readonly transactions don't retry.
				//txnOptions.getReadOnlyBuilder().setPreviousTransaction(handle);
				throw new IllegalStateException("This should be impossible; readonly transactions don't retry");
			} else {
				txnOptions.getReadWriteBuilder().setPreviousTransaction(handle);
			}
		});

		return new AsyncTransactionImpl(datastore.newTransaction(txnOptions.build()), afterCommit);
	}
}