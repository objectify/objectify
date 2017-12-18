/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.googlecode.objectify.cache.CachingAsyncDatastore;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Proxy;
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
	private CachingAsyncDatastore cads;
	
	/** No datastore */
	private CachingAsyncDatastore nods;

	/** Simple bits of data to use */
	private Key key;
	private Set<Key> keyInSet;
	private Entity entity;
	private List<Entity> entityInList;
	
	/**
	 */
	@BeforeEach
	void setUpExtra() throws IOException {
		final EntityMemcache mc = new EntityMemcache(memcache(), "somenamespace");
		cads = new CachingAsyncDatastore(asyncDatastore(), mc);
		nods = new CachingAsyncDatastore(allOperationsUnsupported(), mc);
		
		key = datastore().newKeyFactory().setKind("thing").newKey(1);
		keyInSet = Collections.singleton(key);
		entity = Entity.newBuilder(key).set("foo", "bar").build();
		entityInList = Collections.singletonList(entity);
	}

	private AsyncDatastore allOperationsUnsupported() {
		return (AsyncDatastore)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { AsyncDatastore.class }, (p, m, a) -> { throw new UnsupportedOperationException(); });
	}
	
	/** */
	@Test
	void negativeCacheWorks() throws Exception {
		final Future<Map<Key, Entity>> fent = cads.get(key);
		assertThat(fent.get()).isEmpty();
		
		// Now that it's called, make sure we have a negative cache entry
		final Future<Map<Key, Entity>> cached = nods.get(key);
		assertThat(cached.get()).isEmpty();
	}

	/** */
	@Test
	void basicCacheWorks() throws Exception {
		final Future<List<Key>> fkey = cads.put(entityInList);
		final List<Key> putResult = fkey.get();

		final Future<Map<Key, Entity>> fent = cads.get(putResult);
		final Entity fentEntity = fent.get().values().iterator().next();
		assertThat(fentEntity.getString("foo")).isEqualTo("bar");
		
		// Now make sure it is in the cache
		final Future<Map<Key, Entity>> cached = nods.get(putResult);
		final Entity cachedEntity = cached.get().values().iterator().next();
		assertThat(cachedEntity.getString("foo")).isEqualTo("bar");
	}
}