package com.googlecode.objectify.test.valkey;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.googlecode.objectify.cache.CachingAsyncDatastore;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.test.util.valkey.ValkeyTestBase;
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
 * Valkey-backed mirror of {@link com.googlecode.objectify.test.CachingDatastoreTests}.
 */
class ValkeyCachingDatastoreTests extends ValkeyTestBase {

	private CachingAsyncDatastore cads;
	private CachingAsyncDatastore nods;

	private Key key;
	private Set<Key> keyInSet;
	private Entity entity;
	private List<Entity> entityInList;

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
		return (AsyncDatastore) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{AsyncDatastore.class},
				(p, m, a) -> { throw new UnsupportedOperationException(); });
	}

	@Test
	void negativeCacheWorks() throws Exception {
		final Future<Map<Key, Entity>> fent = cads.get(key);
		assertThat(fent.get()).isEmpty();

		final Future<Map<Key, Entity>> cached = nods.get(key);
		assertThat(cached.get()).isEmpty();
	}

	@Test
	void basicCacheWorks() throws Exception {
		final Future<List<Key>> fkey = cads.put(entityInList);
		final List<Key> putResult = fkey.get();

		final Future<Map<Key, Entity>> fent = cads.get(putResult);
		final Entity fentEntity = fent.get().values().iterator().next();
		assertThat(fentEntity.getString("foo")).isEqualTo("bar");

		final Future<Map<Key, Entity>> cached = nods.get(putResult);
		final Entity cachedEntity = cached.get().values().iterator().next();
		assertThat(cachedEntity.getString("foo")).isEqualTo("bar");
	}
}
