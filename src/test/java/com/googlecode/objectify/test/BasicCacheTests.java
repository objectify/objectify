/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.cache.spymemcached.SpyMemcacheService;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.LocalDatastoreExtension;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.Closeable;
import net.spy.memcached.MemcachedClient;
import org.junit.After;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

import java.io.IOException;
import java.net.InetSocketAddress;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.init;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Doesn't extend TestBase so we have to set up the factory by hand
 */
class BasicCacheTests {
	private static class NeverAllowCacheFactory extends ObjectifyFactory {
		public NeverAllowCacheFactory(final Datastore datastore, final MemcacheService memcache) {
			super(datastore, memcache);
		}

		@Override
		public AsyncDatastore asyncDatastore(final boolean enableGlobalCache) {
			assertThat(enableGlobalCache).isFalse();
			return super.asyncDatastore(enableGlobalCache);
		}
	}

	private Closeable rootService;

	@BeforeEach
	void setUp() throws IOException, InterruptedException {
		final LocalDatastoreHelper helper = LocalDatastoreHelper.create(1.0);
		helper.start();
		final Datastore datastore = helper.getOptions().getService();
		final MemcachedClient client = new MemcachedClient(new InetSocketAddress("localhost", 11211));

		ObjectifyService.init(new NeverAllowCacheFactory(datastore, new SpyMemcacheService(client)));

		rootService = ObjectifyService.begin();
	}

	@AfterEach
	void tearDown() {
		rootService.close();
	}

	/** */
	@Test
	void cacheOptionWorks() throws Exception {
		factory().register(Trivial.class);

		final Trivial triv = new Trivial("foo", 5);
		final Key<Trivial> k = ofy().cache(false).save().entity(triv).now();

		final Trivial fetched = ofy().cache(false).load().key(k).now();
		assertThat(fetched.getId()).isEqualTo(k.getId());

		ofy().clear();
		final Trivial fetched2 = ofy().cache(false).load().key(k).now();
		assertThat(fetched2.getId()).isEqualTo(k.getId());
	}
}