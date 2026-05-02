package com.googlecode.objectify.test.valkey;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.test.util.valkey.ValkeyTestBase;
import com.googlecode.objectify.util.Closeable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Valkey-backed mirror of {@link com.googlecode.objectify.test.CachingUncacheableTests}. Verifies
 * that an entity without {@link Cache} never causes a call into the cache service.
 */
class ValkeyCachingUncacheableTests extends ValkeyTestBase {

	@Entity
	private static class Uncacheable {
		@Id Long id;
		String stuff;
	}

	@Entity
	@Cache
	private static class Cacheable {
		@Id Long id;
		String stuff;
	}

	private Closeable rootService;

	@Mock
	private MemcacheService memcacheService;

	@BeforeEach
	void setUpExtra() {
		ObjectifyService.init(new ObjectifyFactory(datastore(), memcacheService));
		factory().register(Uncacheable.class);
		factory().register(Cacheable.class);

		rootService = ObjectifyService.begin();
	}

	@AfterEach
	void tearDownExtra() {
		rootService.close();
	}

	@Test
	void ensureUncacheableThingsDoNotTouchCache() {
		final Uncacheable un1 = new Uncacheable();
		un1.stuff = "un1 stuff";

		final Key<Uncacheable> key = ofy().save().entity(un1).now();
		ofy().clear();
		final Uncacheable fetched = ofy().load().key(key).now();

		assertThat(fetched).isNotNull();
		verifyNoInteractions(memcacheService);
	}
}
