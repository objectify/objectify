package com.googlecode.objectify.cache;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreAttributes;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.Index.IndexState;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.util.FutureHelper;

/**
 * <p>A synchronous facade for the CachingAsyncDatastoreService.  Please see the documentation
 * for that class for details.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingDatastoreService implements DatastoreService
{
	DatastoreService ds;
	CachingAsyncDatastoreService async;
	
	/**
	 */
	public CachingDatastoreService(DatastoreService ds, CachingAsyncDatastoreService async)
	{
		this.ds = ds;
		this.async = async;
	}
	
	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#allocateIds(java.lang.String, long)
	 */
	@Override
	public KeyRange allocateIds(String kind, long num)
	{
		return this.ds.allocateIds(kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#allocateIds(com.google.appengine.api.datastore.Key, java.lang.String, long)
	 */
	@Override
	public KeyRange allocateIds(Key parent, String kind, long num)
	{
		return this.ds.allocateIds(parent, kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#allocateIdRange(com.google.appengine.api.datastore.KeyRange)
	 */
	@Override
	public KeyRangeState allocateIdRange(KeyRange range)
	{
		return this.ds.allocateIdRange(range);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#beginTransaction()
	 */
	@Override
	public Transaction beginTransaction()
	{
		return FutureHelper.quietGet(this.async.beginTransaction());
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#beginTransaction(com.google.appengine.api.datastore.TransactionOptions)
	 */
	@Override
	public Transaction beginTransaction(TransactionOptions options)
	{
		return FutureHelper.quietGet(this.async.beginTransaction(options));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public void delete(Key... keys)
	{
		FutureHelper.quietGet(this.async.delete(keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<Key> keys)
	{
		FutureHelper.quietGet(this.async.delete(keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public void delete(Transaction txn, Key... keys)
	{
		FutureHelper.quietGet(this.async.delete(txn, keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#delete(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public void delete(Transaction txn, Iterable<Key> keys)
	{
		FutureHelper.quietGet(this.async.delete(txn, keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public Entity get(Key key) throws EntityNotFoundException
	{
		return this.get(null, key);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(java.lang.Iterable)
	 */
	@Override
	public Map<Key, Entity> get(Iterable<Key> keys)
	{
		return FutureHelper.quietGet(this.async.get(keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Key)
	 */
	@Override
	public Entity get(Transaction txn, Key key) throws EntityNotFoundException
	{
		// This one is a little tricky because of the declared exception
		Map<Key, Entity> result = this.get(txn, Collections.singleton(key));
		Entity ent = result.get(key);
		if (ent == null)
			throw new EntityNotFoundException(key);
		else
			return ent;
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#get(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public Map<Key, Entity> get(Transaction txn, Iterable<Key> keys)
	{
		return FutureHelper.quietGet(this.async.get(txn, keys));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getActiveTransactions()
	 */
	@Override
	public Collection<Transaction> getActiveTransactions()
	{
		// This would conflict with the wrapped transaction object
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getCurrentTransaction()
	 */
	@Override
	public Transaction getCurrentTransaction()
	{
		// This would conflict with the wrapped transaction object
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getCurrentTransaction(com.google.appengine.api.datastore.Transaction)
	 */
	@Override
	public Transaction getCurrentTransaction(Transaction txn)
	{
		// This would conflict with the wrapped transaction object
		throw new UnsupportedOperationException();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#prepare(com.google.appengine.api.datastore.Query)
	 */
	@Override
	public PreparedQuery prepare(Query query)
	{
		return this.ds.prepare(query);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#prepare(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Query)
	 */
	@Override
	public PreparedQuery prepare(Transaction txn, Query query)
	{
		return this.ds.prepare(txn, query);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public Key put(Entity entity)
	{
		return this.put(null, entity);
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(java.lang.Iterable)
	 */
	@Override
	public List<Key> put(Iterable<Entity> entities)
	{
		return FutureHelper.quietGet(this.async.put(null, entities));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(com.google.appengine.api.datastore.Transaction, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public Key put(Transaction txn, Entity entity)
	{
		return FutureHelper.quietGet(this.async.put(txn, entity));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#put(com.google.appengine.api.datastore.Transaction, java.lang.Iterable)
	 */
	@Override
	public List<Key> put(Transaction txn, Iterable<Entity> entities)
	{
		return FutureHelper.quietGet(this.async.put(txn, entities));
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getDatastoreAttributes()
	 */
	@Override
	public DatastoreAttributes getDatastoreAttributes()
	{
		return this.ds.getDatastoreAttributes();
	}

	/* (non-Javadoc)
	 * @see com.google.appengine.api.datastore.DatastoreService#getIndexes()
	 */
	@Override
	public Map<Index, IndexState> getIndexes()
	{
		return this.ds.getIndexes();
	}
}


