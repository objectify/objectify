package com.googlecode.objectify.impl.engine;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterable;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.Session;
import com.googlecode.objectify.impl.SessionValue;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.impl.ref.StdRef;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultNow;
import com.googlecode.objectify.util.ResultWrapper;
import com.googlecode.objectify.util.TranslatingQueryResultIterable;

/**
 * This is the master logic for loading, saving, and deleting entities from the datastore.  It provides the
 * fundamental operations that enable the rest of the API.  One of these engines is created for every operation;
 * upon completion, it is thrown away.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Engine
{
	/** */
	private static final Logger log = Logger.getLogger(Engine.class.getName());
	
	/** */
	protected ObjectifyImpl ofy;
	
	/** */
	protected AsyncDatastoreService ads;
	
	/** */
	protected Session session;
	
	/**
	 * @param txn can be null to not use transactions. 
	 */
	public Engine(ObjectifyImpl ofy, AsyncDatastoreService ads, Session session) {
		this.ofy = ofy;
		this.ads = ads;
		this.session = session;
	}
	
	/**
	 * The fundamental put() operation.
	 */
	public <K, E extends K> Result<Map<Key<K>, E>> save(final Iterable<? extends E> entities) {
		
		if (log.isLoggable(Level.FINEST))
			log.finest("Saving " + entities);
		
		List<Entity> entityList = new ArrayList<Entity>();
		for (E obj: entities) {
			if (obj instanceof Entity) {
				entityList.add((Entity)obj);
			} else {
				EntityMetadata<E> metadata = ofy.getFactory().getMetadataForEntity(obj);
				entityList.add(metadata.save(obj, ofy));
			}
		}

		Future<List<com.google.appengine.api.datastore.Key>> raw = ads.put(ofy.getTxnRaw(), entityList);
		Result<List<com.google.appengine.api.datastore.Key>> adapted = new ResultAdapter<List<com.google.appengine.api.datastore.Key>>(raw);

		return new ResultWrapper<List<com.google.appengine.api.datastore.Key>, Map<Key<K>, E>>(adapted) {
			@Override
			protected Map<Key<K>, E> wrap(List<com.google.appengine.api.datastore.Key> base) {
				Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>(base.size() * 2);

				// We need to take a pass to do two things:
				// 1) Patch up any generated ids in the original objects
				// 2) Add values to session
				
				// Order should be exactly the same
				Iterator<com.google.appengine.api.datastore.Key> keysIt = base.iterator();
				for (E obj: entities)
				{
					com.google.appengine.api.datastore.Key k = keysIt.next();
					if (!(obj instanceof Entity)) {
						KeyMetadata<E> metadata = ofy.getFactory().getMetadataForEntity(obj).getKeyMetadata();
						if (metadata.isIdGeneratable())
							metadata.setLongId(obj, k.getId());
					}
					
					Key<K> key = Key.create(k);
					result.put(key, obj);
					
					@SuppressWarnings("unchecked")
					SessionValue<E> sent = new SessionValue<E>((Key<E>)key, new ResultNow<E>(obj)); 
					session.add(sent);
				}
				
				if (log.isLoggable(Level.FINEST))
					log.finest("Saved " + base);
				
				return result;
			}
		};
	}

	/**
	 * The fundamental delete() operation.
	 */
	public Result<Void> delete(final Iterable<com.google.appengine.api.datastore.Key> keys) {
		Future<Void> fut = ads.delete(ofy.getTxnRaw(), keys);
		Result<Void> result = new ResultAdapter<Void>(fut);
		return new ResultWrapper<Void, Void>(result) {
			@Override
			protected Void wrap(Void orig) {
				for (com.google.appengine.api.datastore.Key key: keys)
					session.add(new SessionValue<Object>(Key.create(key), new ResultNow<Object>(null)));
				
				return orig;
			}
		};
	}
	
	/**
	 * The fundamental query() operation, which provides Refs.  Might be a keys only query.
	 */
	public <T> QueryResultIterable<Ref<T>> query(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = ads.prepare(ofy.getTxnRaw(), query);
		return new ToRefIterable<T>(pq.asQueryResultIterable(fetchOpts));
	}

	/**
	 * The fundamental query count operation.  This is sufficiently different from normal query().
	 */
	public int queryCount(com.google.appengine.api.datastore.Query query, FetchOptions fetchOpts) {
		PreparedQuery pq = ads.prepare(ofy.getTxnRaw(), query);
		return pq.countEntities(fetchOpts);
	}

	/**
	 * Iterable that translates from datastore Entity to Ref
	 */
	protected class ToRefIterable<T> extends TranslatingQueryResultIterable<Entity, Ref<T>> {
		public ToRefIterable(QueryResultIterable<Entity> source) {
			super(source);
		}
		
		@Override
		protected Ref<T> translate(Entity from) {
			Key<T> key = Key.create(from.getKey());
			SessionValue<T> cached = session.get(key);
			
			if (cached == null || cached.getResult().now() == null) {
				T obj = ofy.load(from, new LoadContext(ofy, null));	// todo
				cached = new SessionValue<T>(key, new ResultNow<T>(obj));
				session.add(cached);
			}
			
			return new StdRef<T>(key, cached.getResult().now());
		}
	}
}