package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Datastore;
import com.google.common.base.Preconditions;
import com.google.datastore.v1.TransactionOptions;
import com.google.datastore.v1.TransactionOptions.ReadOnly;
import com.google.protobuf.ByteString;
import com.google.protobuf.Timestamp;
import com.googlecode.objectify.TxnOptions;

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
	public AsyncTransaction newTransaction(final TxnOptions options, final Runnable afterCommit, final Optional<ByteString> prevTxnHandle) {
		final TransactionOptions.Builder txnOptions = TransactionOptions.newBuilder();

		if (options.readOnly()) {
			final ReadOnly.Builder builder = ReadOnly.newBuilder();
			options.readTime().ifPresent(time -> {
				builder.setReadTime(Timestamp.newBuilder().setSeconds(time.getEpochSecond()).setNanos(time.getNano()));
			});
			txnOptions.setReadOnly(builder.build());

		} else {
			Preconditions.checkState(options.readTime().isEmpty(), "readOnly is required if readTime is set");

			prevTxnHandle.ifPresent(handle -> {
				txnOptions.getReadWriteBuilder().setPreviousTransaction(handle);
			});
		}

		return new AsyncTransactionImpl(datastore.newTransaction(txnOptions.build()), afterCommit);
	}
}