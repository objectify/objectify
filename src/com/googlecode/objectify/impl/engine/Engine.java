package com.googlecode.objectify.impl.engine;

import java.util.ArrayList;
import java.util.HashMap;
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
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.util.ResultWrapper;
import com.googlecode.objectify.util.TranslatingQueryResultIterator;

/**
 * This is the master logic for loading, saving, and deleting entities from the datastore.  It provides the
 * fundamental operations that enable the rest of the API.  One of these engines is created for every operation;
 * upon completion, it is thrown away.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Engine
{
	/** Value which gets put in the cache for negative results */
	protected static final Object NEGATIVE_RESULT = new Object();

	/** */
	protected Objectify ofy;
	
	/** */
	protected AsyncDatastoreService ads;
	
	/** */
	protected Map<com.google.appengine.api.datastore.Key, Object> session = new HashMap<com.google.appengine.api.datastore.Key, Object>();
	
	/**
	 * @param txn can be null to not use transactions. 
	 */
	public Engine(Objectify ofy, AsyncDatastoreService ads, Map<com.google.appengine.api.datastore.Key, Object> session) {
		this.ofy = ofy;
		this.ads = ads;
		this.session = session;
	}
	
	/**
	 * The fundamental put() operation.
	 */
	public <K, E extends K> Result<Map<Key<K>, E>> put(final Iterable<? extends E> entities) {
		
		List<Entity> entityList = new ArrayList<Entity>();
		for (E obj: entities) {
			EntityMetadata<E> metadata = ofy.getFactory().getMetadataForEntity(obj);
			entityList.add(metadata.toEntity(obj, ofy));
		}

		Future<List<com.google.appengine.api.datastore.Key>> raw = ads.put(ofy.getTxn(), entityList);
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
					EntityMetadata<E> metadata = ofy.getFactory().getMetadataForEntity(obj);
					metadata.setKey(obj, k);
					
					result.put(Key.<K>create(k), obj);
					session.put(k, obj);
				}
				
				return result;
			}
		};
	}

	/**
	 * The fundamental delete() operation.
	 */
	public Result<Void> delete(final Iterable<com.google.appengine.api.datastore.Key> keys) {
		Future<Void> fut = ads.delete(ofy.getTxn(), keys);
		Result<Void> result = new ResultAdapter<Void>(fut);
		return new ResultWrapper<Void, Void>(result) {
			@Override
			protected Void wrap(Void orig) {
				for (com.google.appengine.api.datastore.Key key: keys)
					session.put(key, NEGATIVE_RESULT);
				
				return orig;
			}
		};
	}
	
	/**
	 * The fundamental query() operation that returns full populated object instances.  Might be a keys
	 * only query though.
	 */
	public <T> QueryResultIterable<T> query(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = ads.prepare(ofy.getTxn(), query);
		return new ToObjectIterable<T>(pq.asQueryResultIterable(fetchOpts));
	}

	/**
	 * The fundamental query() operation that returns Key<?> instances.
	 * @param query must be keysOnly
	 */
	@SuppressWarnings("unchecked")
	public <T> QueryResultIterable<T> queryKeys(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		assert query.isKeysOnly();
		PreparedQuery pq = ads.prepare(ofy.getTxn(), query);
		return (QueryResultIterable<T>)new ToKeyIterable(pq.asQueryResultIterable(fetchOpts));
	}

	/**
	 * The fundamental query count operation.  This is sufficiently different from normal query().
	 */
	public int queryCount(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = ads.prepare(ofy.getTxn(), query);
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
			@SuppressWarnings("unchecked")
			T cached = (T)session.get(from.getKey());
			
			if (cached == null || cached == NEGATIVE_RESULT) {
				EntityMetadata<T> meta = ofy.getFactory().getMetadata(from.getKey());
				cached = meta.toObject(from, ofy);
				session.put(from.getKey(), cached);
			}
			
			return cached;
		}
	}
}