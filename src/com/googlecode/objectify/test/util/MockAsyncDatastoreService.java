package com.googlecode.objectify.test.util;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreAttributes;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.Index.IndexState;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;

/**
 * This datastore service throws UnsupportedOperationException from every method call immediately.  It allows
 * us to test that the cache layer is working properly - when cached, it shouldn't make any calls here.
 */
public class MockAsyncDatastoreService implements AsyncDatastoreService
{

	@Override
	public PreparedQuery prepare(Query query)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public PreparedQuery prepare(Transaction txn, Query query)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Transaction getCurrentTransaction()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Transaction getCurrentTransaction(Transaction returnedIfNoTxn)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Collection<Transaction> getActiveTransactions()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Transaction> beginTransaction()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Transaction> beginTransaction(TransactionOptions options)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Entity> get(Key key)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Entity> get(Transaction txn, Key key)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Map<Key, Entity>> get(Iterable<Key> keys)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Map<Key, Entity>> get(Transaction txn, Iterable<Key> keys)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Key> put(Entity entity)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Key> put(Transaction txn, Entity entity)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<List<Key>> put(Iterable<Entity> entities)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<List<Key>> put(Transaction txn, Iterable<Entity> entities)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> delete(Key... keys)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> delete(Transaction txn, Key... keys)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> delete(Iterable<Key> keys)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Void> delete(Transaction txn, Iterable<Key> keys)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<KeyRange> allocateIds(String kind, long num)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<KeyRange> allocateIds(Key parent, String kind, long num)
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<DatastoreAttributes> getDatastoreAttributes()
	{
		throw new UnsupportedOperationException();
	}

	@Override
	public Future<Map<Index, IndexState>> getIndexes()
	{
		throw new UnsupportedOperationException();
	}

}
