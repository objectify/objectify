package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Datastore;

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
		return new AsyncTransactionImpl(datastore.newTransaction(), afterCommit);
	}
}