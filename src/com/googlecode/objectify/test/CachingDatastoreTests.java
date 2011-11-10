/*
 */

package com.googlecode.objectify.test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.cache.CachingAsyncDatastoreService;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.test.util.MockAsyncDatastoreService;

/**
 * Tests of the caching datastore directly, outside of the rest of Objectify. Tries to create as few
 * future wrappers as possible so we can easily see what is going on.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingDatastoreTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(CachingDatastoreTests.class.getName());
	
	/** Caching */
	CachingAsyncDatastoreService cads;
	
	/** No datastore */
	CachingAsyncDatastoreService nods;

	/** Simple bits of data to use */
	Key key;
	Set<Key> keyInSet;
	Entity entity;
	List<Entity> entityInList;
	
	/**
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		EntityMemcache mc = new EntityMemcache(null);
		cads = new CachingAsyncDatastoreService(DatastoreServiceFactory.getAsyncDatastoreService(), mc);
		nods = new CachingAsyncDatastoreService(new MockAsyncDatastoreService(), mc);
		
		key = KeyFactory.createKey("thing", 1);
		keyInSet = Collections.singleton(key);
		entity = new Entity(key);
		entity.setProperty("foo", "bar");
		entityInList = Collections.singletonList(entity);
	}
	
	/** */
	@Test
	public void testNegativeCache() throws Exception
	{
		Future<Map<Key, Entity>> fent = cads.get(null, keyInSet);
		assert fent.get().isEmpty();
		
		// Now that it's called, make sure we have a negative cache entry
		Future<Map<Key, Entity>> cached = nods.get(null, keyInSet);
		assert cached.get().isEmpty();
	}

	/** */
	@Test
	public void testBasicCache() throws Exception
	{
		Future<List<Key>> fkey = cads.put(null, entityInList);
		List<Key> putResult = fkey.get();
		
		Future<Map<Key, Entity>> fent = cads.get(null, putResult);
		assert fent.get().values().iterator().next().getProperty("foo").equals("bar");
		
		// Now make sure it is in the cache
		Future<Map<Key, Entity>> cached = nods.get(null, putResult);
		assert cached.get().values().iterator().next().getProperty("foo").equals("bar");
	}
}