package com.googlecode.objectify.impl;

/**
 * The new datastore SDK has a neat structure of interfaces and implementations (transaction, datastorereader, etc)
 * but doesn't currently support async operations. We need to shim in a Future-based API so that we can seamlessly
 * support it when it becomes available. We'll remove this parallel hierarchy then.
 */
public interface AsyncDatastore extends AsyncDatastoreReaderWriter {

	/**
	 */
	AsyncTransaction newTransaction(Runnable afterCommit);
}