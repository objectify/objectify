package com.googlecode.objectify.impl;

import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.TxnType;
import com.googlecode.objectify.Work;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.cache.PendingFutures;
import com.googlecode.objectify.cmd.Deferred;
import com.googlecode.objectify.cmd.Deleter;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.Saver;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.util.Closeable;
import com.googlecode.objectify.util.Values;
import lombok.Getter;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Implementation of the Objectify interface. This is also suitable for subclassing; you
 * can return your own subclass by overriding ObjectifyFactory.begin().</p>
 *
 * <p>Note we *always* use the AsyncDatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify, Closeable
{
	/** The factory that produced us */
	protected final ObjectifyFactory factory;

	/** */
	@Getter
	protected final ObjectifyOptions options;

	/** */
	protected final Transactor transactor;

	/**
	 */
	public ObjectifyImpl(final ObjectifyFactory fact) {
		this.factory = fact;
		this.options = new ObjectifyOptions();
		this.transactor = new TransactorNo(factory);
	}

	public ObjectifyImpl(final ObjectifyFactory factory, final ObjectifyOptions options, final Transactor transactor) {
		this.factory = factory;
		this.options = options;
		this.transactor = transactor;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	public ObjectifyFactory factory() {
		return this.factory;
	}

	@Override
	public Objectify namespace(final String namespace) {
		return options(options.namespace(namespace));
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
	 * @see com.googlecode.objectify.Objectify#defer()
	 */
	@Override
	public Deferred defer() {
		return new DeferredImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#deadline(java.lang.Double)
	 */
	@Override
	public Objectify deadline(final Double value) {
		// A no-op
		return this;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#cache(boolean)
	 */
	@Override
	public Objectify cache(boolean value) {
		return options(options.cache(value));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#mandatoryTransactions(boolean)
	 */
	@Override
	public Objectify mandatoryTransactions(boolean value) {
		return options(options.mandatoryTransactions(value));
	}

	/** Same transactor, different options */
	ObjectifyImpl options(final ObjectifyOptions opts) {
		return makeNew(opts, transactor);
	}

	/** Same options, different transactor */
	ObjectifyImpl transactor(final Transactor transactor) {
		return makeNew(options, transactor);
	}

	/** Can be overriden if you want to subclass the ObjectifyImpl */
	protected ObjectifyImpl makeNew(final ObjectifyOptions opts, final Transactor transactor) {
		return new ObjectifyImpl(factory, opts, transactor);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	public AsyncTransaction getTransaction() {
		return transactor.getTransaction();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#execute(com.googlecode.objectify.TxnType, com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R execute(final TxnType txnType, final Work<R> work) {
		return transactor.execute(this, txnType, work);
	}

	@Override
	public void execute(final TxnType txnType, final Runnable work) {
		execute(txnType, (Work<Void>)() -> {
			work.run();
			return null;
		});
	}

	@Override
	public <R> R transactionless(final Work<R> work) {
		return transactor.transactionless(this, work);
	}

	@Override
	public void transactionless(final Runnable work) {
		transactionless((Work<Void>)() -> {
			work.run();
			return null;
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transact(Work<R> work) {
		return transactor.transact(this, work);
	}

	@Override
	public void transact(final Runnable work) {
		transact((Work<Void>)() -> {
			work.run();
			return null;
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transact(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(Work<R> work) {
		return this.transactNew(Transactor.DEFAULT_TRY_LIMIT, work);
	}

	@Override
	public void transactNew(final Runnable work) {
		transactNew((Work<Void>)() -> {
			work.run();
			return null;
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#transactNew(com.googlecode.objectify.Work)
	 */
	@Override
	public <R> R transactNew(int limitTries, Work<R> work) {
		return transactor.transactNew(this, limitTries, work);
	}

	@Override
	public void transactNew(int limitTries, final Runnable work) {
		transactNew(limitTries, (Work<Void>)() -> {
			work.run();
			return null;
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#clear()
	 */
	@Override
	public void clear() {
		transactor.getSession().clear();
	}

	/**
	 */
	protected AsyncDatastoreReaderWriter asyncDatastore() {
		return transactor.asyncDatastore(this);
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for saving and deleting
	 */
	protected WriteEngine createWriteEngine() {
		if (options.isMandatoryTransactions() && getTransaction() == null)
			throw new IllegalStateException("You have attempted save/delete outside of a transaction, but you have enabled ofy().mandatoryTransactions(true). Perhaps you wanted to start a transaction first?");

		return new WriteEngine(this, asyncDatastore(), transactor.getSession(), transactor.getDeferrer());
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
	protected Value<?> makeFilterable(Object value) {
		if (value == null)
			return NullValue.of();

		// This is really quite a dilemma.  We need to convert that value into something we can filter by, but we don't
		// really have a lot of information about it.  We could use type information from the matched field, but there's
		// no guarantee that there is a field to check - it could be a typeless query or a query on an old property value.
		// The only real solution is to create a (non root!) translator on the fly.  Even that is not straightforward,
		// because erasure wipes out any component type information in a collection. We don't know what the collection
		// contains.
		//
		// The answer:  Check for collections explicitly.  Create a separate translator for every item in the collection;
		// after all, it could be a heterogeneous list.  This is not especially efficient but GAE only allows a handful of
		// items in a IN operation and at any rate processing will still be negligible compared to the cost of a query.

		// If this is an array, make life easier by turning it into a list first.  Because of primitive
		// mismatching we can't trust Arrays.asList().
		if (value.getClass().isArray()) {
			final int len = Array.getLength(value);
			final List<Object> asList = new ArrayList<>(len);
			for (int i=0; i<len; i++)
				asList.add(Array.get(value, i));

			value = asList;
		}

		if (value instanceof Iterable) {
			final List<Value<?>> result = new ArrayList<>(50);	// hard limit is 30, but wth
			for (final Object obj: (Iterable<?>)value)
				result.add(makeFilterable(obj));

			return ListValue.of(result);
		} else {
			// Special case entity pojos that become keys
			if (value.getClass().isAnnotationPresent(Entity.class)) {
				return KeyValue.of(factory().keys().rawKeyOf(value, getOptions().getNamespace()));
			} else {
				// Run it through a translator
				final Translator<Object, Value<?>> translator = factory().getTranslators().get(new TypeKey<>(value.getClass()), new CreateContext(factory()), Path.root());

				// For some reason we have to force all values used as filters to be indexed
				return Values.index(translator.save(value, false, new SaveContext(), Path.root()), true);
			}
		}
	}

	/** */
	protected Session getSession() {
		return this.transactor.getSession();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#isLoaded(com.googlecode.objectify.Key)
	 */
	@Override
	public boolean isLoaded(final Key<?> key) {
		return transactor.getSession().contains(key);
	}

	@Override
	public void flush() {
		transactor.getDeferrer().flush(this);
	}

	/**
	 * Defer the saving of one entity. Updates the session cache with this new value.
	 */
	void deferSave(final Object entity) {
		transactor.getDeferrer().deferSave(getOptions(), entity);
	}

	/**
	 * Defer the deletion of one entity. Updates the session cache with this new value.
	 */
	void deferDelete(final Key<?> key) {
		transactor.getDeferrer().deferDelete(getOptions(), key);
	}

	/**
	 * Ends this transactional scope.
	 */
	@Override
	public void close() {
		// The order of these three operations is significant
		flush();

		PendingFutures.completeAllPendingFutures();

		factory().close(this);
	}
}