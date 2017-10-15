/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.cache.CachingAsyncDatastoreService;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.test.util.MockAsyncDatastoreService;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import static com.google.common.truth.Truth.assertThat;

/**
 * Tests of the caching datastore directly, outside of the rest of Objectify. Tries to create as few
 * future wrappers as possible so we can easily see what is going on.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class CachingDatastoreTests extends TestBase {
	/** Caching */
	private CachingAsyncDatastoreService cads;
	
	/** No datastore */
	private CachingAsyncDatastoreService nods;

	/** Simple bits of data to use */
	private Key key;
	private Set<Key> keyInSet;
	private Entity entity;
	private List<Entity> entityInList;
	
	/**
	 */
	@BeforeEach
	void setUpExtra() {
		final EntityMemcache mc = new EntityMemcache(null);
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
	void negativeCacheWorks() throws Exception {
		final Future<Map<Key, Entity>> fent = cads.get(null, keyInSet);
		assertThat(fent.get()).isEmpty();
		
		// Now that it's called, make sure we have a negative cache entry
		final Future<Map<Key, Entity>> cached = nods.get(null, keyInSet);
		assertThat(cached.get()).isEmpty();
	}

	/** */
	@Test
	void basicCacheWorks() throws Exception {
		final Future<List<Key>> fkey = cads.put(null, entityInList);
		final List<Key> putResult = fkey.get();

		final Future<Map<Key, Entity>> fent = cads.get(null, putResult);
		final Entity fentEntity = fent.get().values().iterator().next();
		assertThat(fentEntity.getProperty("foo")).isEqualTo("bar");
		
		// Now make sure it is in the cache
		final Future<Map<Key, Entity>> cached = nods.get(null, putResult);
		final Entity cachedEntity = cached.get().values().iterator().next();
		assertThat(cachedEntity.getProperty("foo")).isEqualTo("bar");
	}
}