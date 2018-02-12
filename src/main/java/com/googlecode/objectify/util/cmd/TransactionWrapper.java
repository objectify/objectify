package com.googlecode.objectify.util.cmd;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.Transaction;
import com.google.protobuf.ByteString;
import lombok.Data;

import java.util.Iterator;
import java.util.List;

/**
 * Simple pass-through to the base methods. 
 */
@Data
public class TransactionWrapper implements Transaction {
	/** The real implementation */
	private final Transaction raw;

	@Override
	public Entity get(final Key key) {
		return raw.get(key);
	}

	@Override
	public Iterator<Entity> get(final Key... key) {
		return raw.get(key);
	}

	@Override
	public List<Entity> fetch(final Key... keys) {
		return raw.fetch(keys);
	}

	@Override
	public <T> QueryResults<T> run(final Query<T> query) {
		return raw.run(query);
	}

	@Override
	public void addWithDeferredIdAllocation(final FullEntity<?>... entities) {
		raw.addWithDeferredIdAllocation(entities);
	}

	@Override
	public Entity add(final FullEntity<?> entity) {
		return raw.add(entity);
	}

	@Override
	public List<Entity> add(final FullEntity<?>... entities) {
		return raw.add(entities);
	}

	@Override
	public void update(final Entity... entities) {
		raw.update(entities);
	}

	@Override
	public void delete(final Key... keys) {
		raw.delete(keys);
	}

	@Override
	public void putWithDeferredIdAllocation(final FullEntity<?>... entities) {
		raw.putWithDeferredIdAllocation(entities);
	}

	@Override
	public Entity put(final FullEntity<?> entity) {
		return raw.put(entity);
	}

	@Override
	public List<Entity> put(final FullEntity<?>... entities) {
		return raw.put(entities);
	}

	@Override
	public Response commit() {
		return this.raw.commit();
	}

	@Override
	public boolean isActive() {
		return this.raw.isActive();
	}

	@Override
	public Datastore getDatastore() {
		return raw.getDatastore();
	}

	@Override
	public ByteString getTransactionId() {
		return raw.getTransactionId();
	}

	@Override
	public void rollback() {
		this.raw.rollback();
	}
}