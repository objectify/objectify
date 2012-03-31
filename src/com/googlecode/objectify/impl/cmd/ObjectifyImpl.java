package com.googlecode.objectify.impl.cmd;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnWork;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.NullProperty;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.impl.cmd.ObjectifyImplTxn.TransactionImpl;
import com.googlecode.objectify.impl.engine.WriteEngine;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;

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
	
	/** */
	protected Objectify wrapper = this;
	
	/** The factory that produced us */
	protected ObjectifyFactory factory;

	/** Our options */
	protected boolean cache = true;
	protected Consistency consistency = Consistency.STRONG;
	protected Double deadline;
	
	/** */
	protected Session session = new Session();

	/**
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
	public TransactionImpl getTxn() {
		// This version doesn't have a transaction
		return null;
	}
	
	/** Get the raw transaction we received from the AsyncDatastoreService (or CachingAsyncDatastoreService, if applicable) */
	public Transaction getTxnRaw() {
		if (getTxn() == null)
			return null;
		else
			return getTxn().getRaw();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find()
	 */
	@Override
	public Loader load() {
		return new LoaderImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put()
	 */
	@Override
	public Saver save() {
		return new SaverImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete()
	 */
	@Override
	public Deleter delete() {
		return new DeleterImpl(this);
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
			ObjectifyImpl clone = (ObjectifyImpl)super.clone();
			clone.wrapper = clone;
			return clone;
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
					
					if (log.isLoggable(Level.FINEST))
						log.log(Level.FINEST, "Details of optimistic concurrency failure", ex);
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
		O txnOfy = (O)wrapper.transaction();
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

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#setWrapper(com.googlecode.objectify.Objectify)
	 */
	@Override
	public void setWrapper(Objectify ofy) {
		this.wrapper = ofy;
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
	public AsyncDatastoreService createAsyncDatastoreService() {
		return factory.createAsyncDatastoreService(this.createDatastoreServiceConfig(), cache);
	}
	
	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for saving and deleting
	 */
	public WriteEngine createWriteEngine() {
		return new WriteEngine(this, createAsyncDatastoreService(), session);
	}

	/**
	 * <p>Translates the value of a filter clause into something the datastore understands.  Key<?> goes to native Key,
	 * entities go to native Key, java.sql.Date goes to java.util.Date, etc.  It uses the same translation system
	 * that is used for standard entity fields, but does no checking to see if the value is appropriate for the field.</p>
	 * 
	 * <p>Unrecognized types are returned as-is.</p>
	 * 
	 * <p>A future version of this method might check for type validity.</p>
	 * 
	 * @return whatever can be put into a filter clause.
	 */
	public Object makeFilterable(Object value) {
		if (value == null)
			return null;

		// This is really quite a dilemma.  We need to convert that value into something we can filter by, but we don't
		// really have a lot of information about it.  We could use type information from the matched field, but there's
		// no guarantee that there is a field to check - it could be a typeless query or a query on an old property value.
		// The only real solution is to create a (non root!) translator on the fly.  Even that is not straightforward,
		// because erasure wipes out any component type information in a collection.
		//
		// The answer:  Check for collections explicitly.  Create a separate translator for every item in the collection;
		// after all, it could be a heterogeneous list.  This is not especially efficient but GAE only allows a handful of
		// items in a IN operation and at any rate processing will still be negligible compared to the cost of a query.

		// If this is an array, make life easier by turning it into a list first.  Because of primitive
		// mismatching we can't trust Arrays.asList().
		if (value.getClass().isArray()) {
			int len = Array.getLength(value);
			List<Object> asList = new ArrayList<Object>(len);
			for (int i=0; i<len; i++)
				asList.add(Array.get(value, i));
			
			value = asList;
		}
		
		if (value instanceof Iterable) {
			List<Object> result = new ArrayList<Object>(50);	// hard limit is 30, but wth
			for (Object obj: (Iterable<?>)value)
				result.add(makeFilterable(obj));
			
			return result;
		} else {
			Translator<Object> translator = getFactory().getTranslators().create(Path.root(), NullProperty.INSTANCE, value.getClass(), new CreateContext(getFactory()));
			Node node = translator.save(value, Path.root(), false, new SaveContext(this));
			return getFilterableValue(node, value);
		}
	}
	
	/** Extracts a filterable value from the node, or throws an illegalstate exception */
	private Object getFilterableValue(Node node, Object originalValue) {
		if (!node.hasPropertyValue())
			throw new IllegalStateException("Don't know how to filter by '" + originalValue + "'");
		
		return node.getPropertyValue();
	}
	
	/**
	 * Converts a datastore entity into a typed pojo object
	 * @return an assembled pojo, or the Entity itself if the kind is not registered, or null if the input value was null
	 */
	@SuppressWarnings("unchecked")
	public <T> T load(Entity ent, LoadContext ctx) {
		if (ent == null)
			return null;
		
		EntityMetadata<T> meta = getFactory().getMetadata(ent.getKind());
		if (meta == null)
			return (T)ent;
		else
			return meta.load(ent, ctx);
	}

	/**
	 * Converts a typed pojo object into a datastore entity
	 * @param pojo can be an Entity, which will be returned as-is
	 */
	@Override
	public Entity toEntity(Object pojo) {
		if (pojo instanceof Entity) {
			return (Entity)pojo;
		} else {
			@SuppressWarnings("unchecked")
			EntityMetadata<Object> meta = (EntityMetadata<Object>)getFactory().getMetadata(pojo.getClass());
			return meta.save(pojo, this);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#toPojo(com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public <T> T toPojo(Entity entity) {
		LoaderImpl loader = (LoaderImpl)this.load();
		return this.load(entity, new LoadContext(loader, loader.createLoadEngine()));
	}

	/**
	 * Get the wrapper instance
	 */
	public Objectify getWrapper() {
		return this.wrapper;
	}
	
	/** */
	public Session getSession() {
		return this.session;
	}
	
	/** @return true if cache is enabled */
	public boolean getCache() {
		return cache;
	}

}