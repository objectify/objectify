package com.googlecode.objectify.impl.engine;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.impl.cmd.ObjectifyImpl;
import com.googlecode.objectify.util.ResultProxy;
import com.googlecode.objectify.util.ResultWrapper;

/**
 * <p>The master logic for loading entities from the datastore.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class GetEngine extends Engine
{
	/** */
	private static final Logger log = Logger.getLogger(GetEngine.class.getName());
	
	/** Fetch groups that we are after */
	Set<String> groups;
	
	/** During each round we track the keys we will need to satisfy the request */
	Set<com.google.appengine.api.datastore.Key> needed = new HashSet<com.google.appengine.api.datastore.Key>();
	
	/**
	 */
	public GetEngine(ObjectifyImpl ofy, AsyncDatastoreService ads, Map<com.google.appengine.api.datastore.Key, Object> session, Set<String> groups) {
		super(ofy, ads, session);
		this.groups = groups;
	}
	
	/**
	 * The fundamental get() operation.
	 */
	@SuppressWarnings("unchecked")
	public <E, K extends E> Map<Key<K>, E> get(final Iterable<com.google.appengine.api.datastore.Key> keys) {
		
		if (log.isLoggable(Level.FINEST))
			log.finest("Getting " + keys);
		
		final List<com.google.appengine.api.datastore.Key> needFetching = new ArrayList<com.google.appengine.api.datastore.Key>();
		Map<Key<K>, E> foundInCache = new LinkedHashMap<Key<K>, E>();
		
		for (com.google.appengine.api.datastore.Key key: keys) {
			E obj = (E)this.session.get(key);
			if (obj == null)
				needFetching.add(key);
			else if (obj != NEGATIVE_RESULT)
				foundInCache.put(Key.<K>create(key), obj);
		}
		
		if (log.isLoggable(Level.FINEST))
			log.finest("Session contained " + foundInCache.keySet());
		
		if (needFetching.isEmpty()) {
			// We can just use the foundInCache as-is
			return foundInCache;
		} else {
			Result<Map<com.google.appengine.api.datastore.Key, E>> fromDatastore = this.getInternal(needFetching);

			// Needs to add in the cached values, creating a map with the proper order
			Result<Map<Key<K>, E>> together = new ResultWrapper<Map<com.google.appengine.api.datastore.Key, E>, Map<Key<K>, E>>(fromDatastore) {
				@Override
				protected Map<Key<K>, E> wrap(Map<com.google.appengine.api.datastore.Key, E> orig) {
					Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>();
					
					for (com.google.appengine.api.datastore.Key key: keys) {
						
						// Instead of using the foundInCache collection (which has the wrong key), we'll use the session again
						E pojo = (E)session.get(key);
						
						if (pojo != null) {
							if (pojo != NEGATIVE_RESULT)
								result.put(Key.<K>create(key), pojo);
						} else {
							pojo = orig.get(key);
							if (pojo != null) {
								result.put(Key.<K>create(key), pojo);
								session.put(key, pojo);
							} else {
								session.put(key, NEGATIVE_RESULT);
							}
						}
					}
					
					return result;
				}
			};
			
			return ResultProxy.create(Map.class, together);
		}
	}

	/**
	 * Performs the datastore get for actual data, no session cache
	 */
	public <E> Result<Map<com.google.appengine.api.datastore.Key, E>> getInternal(final Iterable<com.google.appengine.api.datastore.Key> rawKeys) {
		if (log.isLoggable(Level.FINEST))
			log.finest("Getting session misses: " + rawKeys);
		
		// First step is to figure out what keys we are really going to fetch.  The list might expand if we @Load parents.
		for (com.google.appengine.api.datastore.Key key: rawKeys)
			need(key);
		
		Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(ofy.getTxnRaw(), rawKeys);
		Result<Map<com.google.appengine.api.datastore.Key, Entity>> adapted = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
		
		return new ResultWrapper<Map<com.google.appengine.api.datastore.Key, Entity>, Map<com.google.appengine.api.datastore.Key, E>>(adapted) {
			@Override
			protected Map<com.google.appengine.api.datastore.Key, E> wrap(Map<com.google.appengine.api.datastore.Key, Entity> base) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Retreived: " + base.keySet());
				
				Map<com.google.appengine.api.datastore.Key, E> result = new LinkedHashMap<com.google.appengine.api.datastore.Key, E>(base.size() * 2);
				
				// We preserve the order of the original keys
				for (com.google.appengine.api.datastore.Key rawKey: rawKeys) {
					Entity entity = base.get(rawKey);
					if (entity != null) {
						EntityMetadata<E> metadata = ofy.getFactory().getMetadata(rawKey);
						result.put(rawKey, (E)metadata.load(entity, ofy));
					}
				}
				
				return result;
			}
		};
	}

	/**
	 * Add this key to needed and check its parents to see if they are needed.
	 */
	private void need(com.google.appengine.api.datastore.Key key) {
		needed.add(key);
		
		// Maybe we recurse
//		EntityMetadata<?> meta = ofy.getFactory().getMetadata(key);
//		if (meta.shouldLoadParent(groups))
//			need(key.getParent());
	}
}