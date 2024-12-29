package com.googlecode.objectify.impl;

import com.google.protobuf.ByteString;
import com.googlecode.objectify.TxnOptions;

import java.util.Optional;

/**
 * The new datastore SDK has a neat structure of interfaces and implementations (transaction, datastorereader, etc)
 * but doesn't currently support async operations. We need to shim in a Future-based API so that we can seamlessly
 * support it when it becomes available. We'll remove this parallel hierarchy then.
 */
public interface AsyncDatastore extends AsyncDatastoreReaderWriter {

	@Deprecated
	default AsyncTransaction newTransaction(Runnable afterCommit) {
		return newTransaction(TxnOptions.deflt(), afterCommit, Optional.empty());
	}

	@Deprecated
	default AsyncTransaction newTransaction(Runnable afterCommit, ByteString prevTxnHandle) {
		return newTransaction(TxnOptions.deflt(), afterCommit, Optional.ofNullable(prevTxnHandle));
	}

	AsyncTransaction newTransaction(TxnOptions options, Runnable afterCommit, Optional<ByteString> prevTxnHandle);
}