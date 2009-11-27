package com.googlecode.objectify;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;

/**
 * Implementation of the Objectify interface.
 */
public class ObjectifyImpl implements Objectify
{
	/** The google object that does the actual heavy lifting */
	DatastoreService ds;
	
	/** If set, this is a transaction override.  Usually null. */
	Transaction txnOverride;
	
	/**
	 * Protected constructor creates a wrapper on the datastore with
	 * the specified txn override.
	 * 
	 * @param txnOverride can be null (and usually will be)
	 */
	ObjectifyImpl(DatastoreService ds, Transaction txnOverride)
	{
		this.ds = ds;
		this.txnOverride = txnOverride;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(java.lang.Iterable)
	 */
	@Override
	public <T> Map<Key, T> get(Iterable<Key> keys)
	{
		//TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#get(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <T> T get(Key key)
	{
		// TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Object)
	 */
	@Override
	public Key put(Object obj)
	{
		// TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#put(java.lang.Iterable)
	 */
	@Override
	public List<Key> put(Iterable<Object> objs)
	{
		//TODO
		return null;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#prepare(com.google.appengine.api.datastore.Query)
	 */
	@Override
	public <T> ObjPreparedQuery<T> prepare(Query query)
	{
		PreparedQuery pq = (this.txnOverride != null)
			? this.ds.prepare(this.txnOverride, query)
			: this.ds.prepare(query);
			
		return new ObjPreparedQueryImpl<T>(pq);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#withTransaction(com.google.appengine.api.datastore.Transaction)
	 */
	@Override
	public Objectify withTransaction(Transaction txn)
	{
		return new ObjectifyImpl(this.ds, txn);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#getDatastore()
	 */
	@Override
	public DatastoreService getDatastore()
	{
		return this.ds;
	}

	//
	//
	// The rest of the methods are simply pass-through to the underlying datastore
	//
	//
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#allocateIds(java.lang.String, long)
	 */
	@Override
	public KeyRange allocateIds(String kind, long num)
	{
		return this.ds.allocateIds(kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#allocateIds(com.google.appengine.api.datastore.Key, java.lang.String, long)
	 */
	@Override
	public KeyRange allocateIds(Key parent, String kind, long num)
	{
		return this.ds.allocateIds(parent, kind, num);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#beginTransaction()
	 */
	@Override
	public Transaction beginTransaction()
	{
		return this.ds.beginTransaction();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(com.google.appengine.api.datastore.Key[])
	 */
	@Override
	public void delete(Key... keys)
	{
		if (this.txnOverride != null)
			this.ds.delete(txnOverride, keys);
		else
			this.ds.delete(keys);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#delete(java.lang.Iterable)
	 */
	@Override
	public void delete(Iterable<Key> keys)
	{
		if (this.txnOverride != null)
			this.ds.delete(txnOverride, keys);
		else
			this.ds.delete(keys);
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#getActiveTransactions()
	 */
	@Override
	public Collection<Transaction> getActiveTransactions()
	{
		return this.ds.getActiveTransactions();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#getCurrentTransaction()
	 */
	@Override
	public Transaction getCurrentTransaction()
	{
		if (this.txnOverride != null)
			return this.txnOverride;
		else
			return this.ds.getCurrentTransaction();
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.Objectify#getCurrentTransaction(com.google.appengine.api.datastore.Transaction)
	 */
	@Override
	public Transaction getCurrentTransaction(Transaction returnedIfNoTxn)
	{
		if (this.txnOverride != null)
			return this.txnOverride;
		else
			return this.ds.getCurrentTransaction(returnedIfNoTxn);
	}

}