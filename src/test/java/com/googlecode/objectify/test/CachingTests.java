/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class CachingTests extends TestBase {
	/** */
	@Entity
	@Data
	@NoArgsConstructor
	private static class Uncached {
		@Id
		private Long id;
		private String stuff;

		Uncached(final String stuff) {
			this.stuff = stuff;
		}
	}

	/** */
	@Entity
	@Cache
	@Data
	@NoArgsConstructor
	private static class Cached {
		@Id
		private Long id;
		private String stuff;

		Cached(final String stuff) {
			this.stuff = stuff;
		}
	}

	/**
	 */
	@BeforeEach
	void setUpExtra() {
		factory().register(Uncached.class);
		factory().register(Cached.class);
	}

	/** */
	@Test
	void batchFetchSomeCachedSomeUncached() throws Exception {
		final Uncached un1 = new Uncached("un1 stuff");
		final Uncached un2 = new Uncached("un2 stuff");
		final Cached ca1 = new Cached("ca1 stuff");
		final Cached ca2 = new Cached("ca2 stuff");

		final List<Object> entities = Arrays.asList(un1, ca1, un2, ca2);

		final Map<Key<Object>, Object> keys = ofy().save().entities(entities).now();
		ofy().clear();
		final Map<Key<Object>, Object> fetched = ofy().load().keys(keys.keySet());

		assertThat(fetched.keySet()).containsExactlyElementsIn(keys.keySet()).inOrder();
	}
	
	/** */
	@Entity
	@Cache(expirationSeconds=1)
	@Data
	private static class Expires {
		@Id
		private Long id;
		private String stuff;
	}

	/** */
	//@Test
	void cacheExpirationWorks() throws Exception {
		factory().register(Expires.class);
		
		final Expires exp = new Expires();
		exp.stuff = "foo";

		final Key<Expires> key = ofy().save().entity(exp).now();
		ofy().clear();
		ofy().load().key(key).now();	// cached now

//		final MemcacheService ms = MemcacheServiceFactory.getMemcacheService(ObjectifyFactory.MEMCACHE_NAMESPACE);
//
//		final Object thing = ms.get(key.toWebSafeString());
//		assertThat(thing).isNotNull();
//
//		Thread.sleep(2000);
//
//		final Object thing2 = ms.get(key.toWebSafeString());
//		assertThat(thing2).isNull();
	}
}