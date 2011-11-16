package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Delete;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.Put;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.util.ResultProxy;
import com.googlecode.objectify.util.ResultWrapper;
import com.googlecode.objectify.util.TranslatingQueryResultIterator;

/**
 * Implementation of the Objectify interface.  Note we *always* use the AsyncDatastoreService
 * methods that use transactions to avoid the confusion of implicit transactions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyImpl implements Objectify
{
	/** The factory that produced us */
	protected ObjectifyFactory factory;
	
	/** The google object that does the actual heavy lifting */
	protected AsyncDatastoreService ads;
	
	/** The transaction to use.  If null, do not use transactions. */
	protected Transaction txn;
	
	/**
	 * @param txn can be null to not use transactions. 
	 */
	public ObjectifyImpl(ObjectifyFactory fact, AsyncDatastoreService ds, Transaction txn)
	{
		this.factory = fact;
		this.ads = ds;
		this.txn = txn;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getFactory()
	 */
	public ObjectifyFactory getFactory()
	{
		return this.factory;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#getTxn()
	 */
	public Transaction getTxn()
	{
		return this.txn;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#find()
	 */
	@Override
	public LoadCmd load()
	{
		return new LoadingImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#put()
	 */
	@Override
	public Put put()
	{
		return new PutImpl(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#delete()
	 */
	@Override
	public Delete delete()
	{
		return new DeleteImpl(this);
	}

	/**
	 * The fundamental get() operation.
	 */
	public <K, E extends K> Map<Key<K>, E> get(final Iterable<com.google.appengine.api.datastore.Key> rawKeys) {
		
		Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(txn, rawKeys);
		Result<Map<com.google.appengine.api.datastore.Key, Entity>> adapted = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
		
		Result<Map<Key<K>, E>> wrapper = new ResultWrapper<Map<com.google.appengine.api.datastore.Key, Entity>, Map<Key<K>, E>>(adapted) {
			@Override
			protected Map<Key<K>, E> wrap(Map<com.google.appengine.api.datastore.Key, Entity> base) {
				Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>(base.size() * 2);
				
				// We preserve the order of the original keys
				for (com.google.appengine.api.datastore.Key rawKey: rawKeys) {
					Entity entity = base.get(rawKey);
					if (entity != null) {
						EntityMetadata<E> metadata = factory.getMetadata(rawKey);
						result.put(Key.<K>create(rawKey), (E)metadata.toObject(entity, ObjectifyImpl.this));
					}
				}
				
				return result;
			}
		};

		return ResultProxy.create(wrapper, Map.class);
	}

	/**
	 * The fundamental put() operation.
	 */
	public <K, E extends K> Result<Map<Key<K>, E>> put(final Iterable<? extends E> entities)
	{
		List<Entity> entityList = new ArrayList<Entity>();
		for (E obj: entities) {
			EntityMetadata<E> metadata = factory.getMetadataForEntity(obj);
			entityList.add(metadata.toEntity(obj, this));
		}

		Future<List<com.google.appengine.api.datastore.Key>> raw = ads.put(txn, entityList);
		Result<List<com.google.appengine.api.datastore.Key>> adapted = new ResultAdapter<List<com.google.appengine.api.datastore.Key>>(raw);

		return new ResultWrapper<List<com.google.appengine.api.datastore.Key>, Map<Key<K>, E>>(adapted) {
			@Override
			protected Map<Key<K>, E> wrap(List<com.google.appengine.api.datastore.Key> base) {
				Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>(base.size() * 2);
				
				// Patch up any generated keys in the original objects while building new key list
				// Order should be exactly the same
				Iterator<com.google.appengine.api.datastore.Key> keysIt = base.iterator();
				for (E obj: entities)
				{
					com.google.appengine.api.datastore.Key k = keysIt.next();
					EntityMetadata<E> metadata = factory.getMetadataForEntity(obj);
					metadata.setKey(obj, k);
					
					result.put(Key.<K>create(k), obj);
				}
				
				return result;
			}
		};
	}

	/**
	 * The fundamental delete() operation.
	 */
	public Result<Void> delete(Iterable<com.google.appengine.api.datastore.Key> keys) {
		Future<Void> fut = ads.delete(txn, keys);
		return new ResultAdapter<Void>(fut);
	}
	
	/**
	 * The fundamental query() operation that returns full populated object instances.  Might be a keys
	 * only query though.
	 */
	public <T> QueryResultIterable<T> query(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = ads.prepare(txn, query);
		return new ToObjectIterable<T>(pq.asQueryResultIterable(fetchOpts));
	}

	/**
	 * The fundamental query() operation that returns Key<?> instances.
	 * @param query must be keysOnly
	 */
	@SuppressWarnings("unchecked")
	public <T> QueryResultIterable<T> queryKeys(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		assert query.isKeysOnly();
		PreparedQuery pq = ads.prepare(txn, query);
		return (QueryResultIterable<T>)new ToKeyIterable(pq.asQueryResultIterable(fetchOpts));
	}

	/**
	 * The fundamental query count operation.  This is sufficiently different from normal query().
	 */
	public int queryCount(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = ads.prepare(txn, query);
		return pq.countEntities(fetchOpts);
	}

	/**
	 * Iterable that translates from datastore Entity to Keys
	 */
	protected class ToKeyIterable implements QueryResultIterable<Key<?>> {
		QueryResultIterable<Entity> source;

		public ToKeyIterable(QueryResultIterable<Entity> source) {
			this.source = source;
		}

		@Override
		public QueryResultIterator<Key<?>> iterator() {
			return new ToKeyIterator(this.source.iterator());
		}
	}

	/**
	 * Iterator that translates from datastore Entity to Keys
	 */
	protected class ToKeyIterator extends TranslatingQueryResultIterator<Entity, Key<?>> {
		public ToKeyIterator(QueryResultIterator<Entity> source) {
			super(source);
		}

		@Override
		protected Key<?> translate(Entity from) {
			return Key.create(from.getKey());
		}
	}

	/**
	 * Iterable that translates from datastore Entity to POJO
	 */
	protected class ToObjectIterable<T> implements QueryResultIterable<T> {
		QueryResultIterable<Entity> source;

		public ToObjectIterable(QueryResultIterable<Entity> source) {
			this.source = source;
		}

		@Override
		public QueryResultIterator<T> iterator() {
			return new ToObjectIterator<T>(this.source.iterator());
		}
	}

	/**
	 * Iterator that translates from datastore Entity to typed Objects
	 */
	protected class ToObjectIterator<T> extends TranslatingQueryResultIterator<Entity, T> {
		public ToObjectIterator(QueryResultIterator<Entity> source) {
			super(source);
		}

		@Override
		protected T translate(Entity from) {
			EntityMetadata<T> meta = factory.getMetadata(from.getKey());
			return meta.toObject(from, ObjectifyImpl.this);
		}
	}
}