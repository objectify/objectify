package com.googlecode.objectify.util.cmd;

import com.google.appengine.api.datastore.Transaction;

import java.util.concurrent.Future;

/**
 * Simple pass-through to the base methods.
 */
public class TransactionWrapper implements Transaction
{
	/** The real implementation */
	Transaction raw;

	/** */
	public TransactionWrapper(Transaction raw) {
		this.raw = raw;
	}

	/** Just in case something needs this */
	public Transaction getRaw() {
		return this.raw;
	}

	@Override
	public void commit() {
		this.raw.commit();
	}

	@Override
	public String getId() {
		return this.raw.getId();
	}

	@Override
	public boolean isActive() {
		return this.raw.isActive();
	}

	@Override
	public void rollback() {
		this.raw.rollback();
	}

	@Override
	public String getApp() {
		return this.raw.getApp();
	}

	@Override
	public Future<Void> commitAsync() {
		return this.raw.commitAsync();
	}

	@Override
	public Future<Void> rollbackAsync() {
		return this.raw.rollbackAsync();
	}
}
