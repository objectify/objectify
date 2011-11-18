package com.googlecode.objectify.impl.cmd;

import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.Put;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.util.DatastoreIntrospector;

/**
 * Implementation of the Objectify interface.  Note we *always* use the AsyncDatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify, Cloneable
{
	/** The factory that produced us */
	protected ObjectifyFactory factory;

	/** Our options */
	protected boolean sessionCache = false;
	protected boolean globalCache = true;
	protected Consistency consistency = Consistency.STRONG;
	protected Double deadline;
	
	/** The transaction to use.  If null, do not use transactions. */
	protected Result<Transaction> txn;
	
	/**
	 * @param txn can be null to not use transactions. 
	 */
	public ObjectifyImpl(ObjectifyFactory fact) {
		this.factory = fact;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	public ObjectifyFactory getFactory() {
		return this.factory;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	public Transaction getTxn() {
		return txn == null ? null : txn.now();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find()
	 */
	@Override
	public LoadCmd load() {
		return new LoadingImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put()
	 */
	@Override
	public Put put() {
		return new PutImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete()
	 */
	@Override
	public Delete delete() {
		return new DeleteImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#consistency(com.google.appengine.api.datastore.ReadPolicy.Consistency)
	 */
	@Override
	public Objectify consistency(Consistency value) {
		if (value == null)
			throw new IllegalArgumentException("Consistency cannot be null");
		
		ObjectifyImpl clone = this.clone();
		clone.consistency = value;
		return clone;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#deadline(java.lang.Double)
	 */
	@Override
	public Objectify deadline(Double value) {
		ObjectifyImpl clone = this.clone();
		clone.deadline = value;
		return clone;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#sessionCache(boolean)
	 */
	@Override
	public Objectify sessionCache(boolean value) {
		ObjectifyImpl clone = this.clone();
		clone.sessionCache = value;
		return clone;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#globalCache(boolean)
	 */
	@Override
	public Objectify globalCache(boolean value) {
		ObjectifyImpl clone = this.clone();
		clone.globalCache = value;
		return clone;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transaction()
	 */
	@Override
	public Objectify transaction()
	{
		ObjectifyImpl clone = this.clone();
		// There is no overhead for XG transactions on a single entity group, so there is
		// no good reason to ever have withXG false when on the HRD.
		Future<Transaction> fut = createAsyncDatastoreService().beginTransaction(TransactionOptions.Builder.withXG(DatastoreIntrospector.SUPPORTS_XG));
		clone.txn = new ResultAdapter<Transaction>(fut);
		return clone;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transactionless()
	 */
	@Override
	public Objectify transactionless()
	{
		ObjectifyImpl clone = this.clone();
		clone.txn = null;
		return clone;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	protected ObjectifyImpl clone()
	{
		try {
			return (ObjectifyImpl)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // impossible
		}
	}
	
	/**
	 * Make a datastore service config that corresponds to our options.
	 */
	protected DatastoreServiceConfig createDatastoreServiceConfig() {
		DatastoreServiceConfig cfg = DatastoreServiceConfig.Builder.withReadPolicy(new ReadPolicy(consistency));
		
		if (deadline != null)
			cfg.deadline(deadline);

		return cfg;
	}
	
	/**
	 * Make a datastore service config that corresponds to our options.
	 */
	protected AsyncDatastoreService createAsyncDatastoreService() {
		return factory.createAsyncDatastoreService(this.createDatastoreServiceConfig(), globalCache);
	}
	
	/**
	 * @return the engine that handles fundamental datastore operations for the commands
	 */
	public Engine getEngine() {
		return new Engine(this, createAsyncDatastoreService());
	}
}