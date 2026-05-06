package com.googlecode.objectify.test.valkey;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.cache.valkey.ValkeyCacheService;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.util.Closeable;
import glide.api.GlideClient;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Mirrors {@link com.googlecode.objectify.test.BasicCacheTests} but boots its own Valkey container
 * and {@link GlideClient} so it can wire a custom {@link ObjectifyFactory} subclass — the same
 * pattern the memcached version uses to assert the global-cache flag is honored.
 */
class ValkeyBasicCacheTests {

	private static class NeverAllowCacheFactory extends ObjectifyFactory {
		NeverAllowCacheFactory(final Datastore datastore, final MemcacheService memcache) {
			super(datastore, memcache);
		}

		@Override
		public AsyncDatastore asyncDatastore(final boolean enableGlobalCache) {
			assertThat(enableGlobalCache).isFalse();
			return super.asyncDatastore(enableGlobalCache);
		}
	}

	@SuppressWarnings("resource")
	private static final GenericContainer<?> VALKEY = new GenericContainer<>(DockerImageName.parse("valkey/valkey:9.0"))
			.withExposedPorts(6379);

	private static GlideClient client;
	private Closeable rootService;

	@BeforeAll
	static void startValkey() throws Exception {
		VALKEY.start();
		client = GlideClient.createClient(
				GlideClientConfiguration.builder()
						.address(NodeAddress.builder().host(VALKEY.getHost()).port(VALKEY.getMappedPort(6379)).build())
						.requestTimeout(5000)
						.build()
		).get();
	}

	@AfterAll
	static void stopValkey() throws Exception {
		if (client != null) {
			client.close();
		}
		VALKEY.stop();
	}

	@BeforeEach
	void setUp() throws IOException, InterruptedException {
		client.customCommand(new String[]{"FLUSHALL"});

		final LocalDatastoreHelper helper = LocalDatastoreHelper.create(1.0);
		helper.start();
		final Datastore datastore = helper.getOptions().getService();

		ObjectifyService.init(new NeverAllowCacheFactory(datastore, new ValkeyCacheService(client)));

		rootService = ObjectifyService.begin();
	}

	@AfterEach
	void tearDown() {
		rootService.close();
	}

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
