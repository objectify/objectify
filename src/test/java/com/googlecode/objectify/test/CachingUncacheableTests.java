/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.Closeable;
import net.spy.memcached.MemcachedClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class CachingUncacheableTests extends TestBase {
	/** */
	@Entity
	private static class Uncacheable {
		@Id Long id;
		String stuff;
	}

	/** */
	@Entity
	@Cache
	private static class Cacheable {
		@Id Long id;
		String stuff;
	}

	/** */
	private Closeable rootService;

	/** */
	@Mock
	private MemcachedClient memcachedClient;

	/**
	 */
	@BeforeEach
	void setUpExtra() {
		ObjectifyService.init(new ObjectifyFactory(datastore(), memcachedClient));
		factory().register(Uncacheable.class);
		factory().register(Cacheable.class);	// needed to get caching in the code path

		rootService = ObjectifyService.begin();
	}

	@AfterEach
	void tearDownExtra() {
		rootService.close();
	}

	/** */
	@Test
	void ensureUncacheableThingsDoNotTouchMemcache() throws Exception {
		final Uncacheable un1 = new Uncacheable();
		un1.stuff = "un1 stuff";

		final Key<Uncacheable> key = ofy().save().entity(un1).now();
		ofy().clear();
		final Uncacheable fetched = ofy().load().key(key).now();

		assertThat(fetched).isNotNull();
		verifyZeroInteractions(memcachedClient);
	}
}