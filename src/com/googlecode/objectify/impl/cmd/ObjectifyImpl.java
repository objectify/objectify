package com.googlecode.objectify.impl.cmd;

import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnWork;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.Put;
import com.googlecode.objectify.impl.engine.Engine;
import com.googlecode.objectify.impl.engine.GetEngine;

/**
 * Implementation of the Objectify interface.  Note we *always* use the AsyncDatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify, Cloneable
{
	/** */
	private static final Logger log = Logger.getLogger(ObjectifyImpl.class.getName());
	
	/** The factory that produced us */
	protected ObjectifyFactory factory;

	/** Our options */
	protected boolean cache = true;
	protected Consistency consistency = Consistency.STRONG;
	protected Double deadline;
	
	/** The session is a simple hashmap */
	protected Map<com.google.appengine.api.datastore.Key, Object> session = new HashMap<com.google.appengine.api.datastore.Key, Object>();

	/**
	 * @param txn can be null to not use transactions. 
	 */
	public ObjectifyImpl(ObjectifyFactory fact) {
		this.factory = fact;
	}
	
	/** Copy constructor */
	ObjectifyImpl(ObjectifyImpl other) {
		this.factory = other.factory;
		this.cache = other.cache;
		this.consistency = other.consistency;
		this.deadline = other.deadline;
		this.session = other.session;
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
		// This version doesn't have a transaction
		return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find()
	 */
	@Override
	public LoadCmd load() {
		return new LoadCmdImpl(this);
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
	 * @see com.googlecode.objectify.Objectify#cache(boolean)
	 */
	@Override
	public Objectify cache(boolean value) {
		ObjectifyImpl clone = this.clone();
		clone.cache = value;
		return clone;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transaction()
	 */
	@Override
	public Objectify transaction() {
		return new ObjectifyImplTxn(this);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	protected ObjectifyImpl clone() {
		try {
			return (ObjectifyImpl)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // impossible
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.TxnWork)
	 */
	@Override
	public <O extends Objectify, R> R transact(TxnWork<O, R> work) {
		return this.transact(Integer.MAX_VALUE, work);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.TxnWork)
	 */
	@Override
	public <O extends Objectify, R> R transact(int limitTries, TxnWork<O, R> work) {
		while (true) {
			try {
				return transactOnce(work);
			} catch (ConcurrentModificationException ex) {
				if (limitTries-- > 0) {
					if (log.isLoggable(Level.WARNING))
						log.warning("Optimistic concurrency failure for " + work + " (retrying): " + ex);
				} else {
					throw ex;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.TxnWork)
	 */
	private <O extends Objectify, R> R transactOnce(TxnWork<O, R> work) {
		@SuppressWarnings("unchecked")
		O txnOfy = (O)this.transaction();
		try {
			R result = work.run(txnOfy);
			txnOfy.getTxn().commit();
			return result;
		}
		finally
		{
			if (txnOfy.getTxn().isActive())
				txnOfy.getTxn().rollback();
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#clear()
	 */
	@Override
	public void clear() {
		session.clear();
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
		return factory.createAsyncDatastoreService(this.createDatastoreServiceConfig(), cache);
	}
	
	/**
	 * Use this once for one operation and then throw it away
	 * @param groups is the set of load groups that are active
	 * @return a fresh engine that handles fundamental datastore operations for the commands
	 */
	public GetEngine createGetEngine(Set<String> groups) {
		return new GetEngine(this, createAsyncDatastoreService(), session, groups);
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for the commands
	 */
	public Engine createEngine() {
		return new Engine(this, createAsyncDatastoreService(), session);
	}
}