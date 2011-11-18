package com.googlecode.objectify.impl.engine;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.ResultAdapter;
import com.googlecode.objectify.util.ResultProxy;
import com.googlecode.objectify.util.ResultWrapper;

/**
 * <p>The master logic for loading entities from the datastore.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class GetEngine extends Engine
{
	/** Fetch groups that we are after */
	Set<String> groups;
	
	/** During each round we track the keys we will need to satisfy the request */
	Set<com.google.appengine.api.datastore.Key> needed = new HashSet<com.google.appengine.api.datastore.Key>();
	
	/**
	 */
	public GetEngine(Objectify ofy, AsyncDatastoreService ads, Set<String> groups) {
		super(ofy, ads);
		this.groups = groups;
	}
	
	/**
	 * The fundamental get() operation.
	 */
	public <E, K extends E> Map<Key<K>, E> get(final Iterable<com.google.appengine.api.datastore.Key> rawKeys) {
		// First step is to figure out what keys we are really going to fetch.  The list might expand if we @Load parents.
		for (com.google.appengine.api.datastore.Key key: rawKeys)
			need(key);
		
		Future<Map<com.google.appengine.api.datastore.Key, Entity>> fut = ads.get(ofy.getTxn(), rawKeys);
		Result<Map<com.google.appengine.api.datastore.Key, Entity>> adapted = new ResultAdapter<Map<com.google.appengine.api.datastore.Key, Entity>>(fut);
		
		Result<Map<Key<K>, E>> wrapper = new ResultWrapper<Map<com.google.appengine.api.datastore.Key, Entity>, Map<Key<K>, E>>(adapted) {
			@Override
			protected Map<Key<K>, E> wrap(Map<com.google.appengine.api.datastore.Key, Entity> base) {
				Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>(base.size() * 2);
				
				// We preserve the order of the original keys
				for (com.google.appengine.api.datastore.Key rawKey: rawKeys) {
					Entity entity = base.get(rawKey);
					if (entity != null) {
						EntityMetadata<E> metadata = ofy.getFactory().getMetadata(rawKey);
						result.put(Key.<K>create(rawKey), (E)metadata.toObject(entity, ofy));
					}
				}
				
				return result;
			}
		};

		return ResultProxy.create(Map.class, wrapper);
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