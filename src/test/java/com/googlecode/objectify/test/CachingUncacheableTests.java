/*
 */

package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.cache.tmp.IMemcacheServiceFactory;
import com.googlecode.objectify.cache.tmp.MemcacheService;
import com.googlecode.objectify.impl.CacheControlImpl;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.Closeable;
import lombok.Data;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

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

	private static class TestObjectifyFactory extends ObjectifyFactory {
		/** Only used for one test */
		void setMemcacheFactory(final IMemcacheServiceFactory factory) {
			this.entityMemcache = new EntityMemcache(MEMCACHE_NAMESPACE, new CacheControlImpl(this), this.memcacheStats, factory);
		}
	}

	@Data
	private static class CallCounter implements InvocationHandler {
		private final Object base;
		private int count;

		@Override
		public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
			if (!method.getName().equals("setErrorHandler"))
				count++;

			return method.invoke(base, args);
		}
	}

	private CallCounter counter;

	/** TODO: kill when stack in factory */
	private Closeable rootService;

	/**
	 */
	@BeforeEach
	void setUpExtra() {
		counter = new CallCounter(null);//(MemcacheServiceFactory.getAsyncMemcacheService(ObjectifyFactory.MEMCACHE_NAMESPACE));

		final MemcacheService proxy = (MemcacheService)Proxy.newProxyInstance(
				this.getClass().getClassLoader(),
				new Class<?>[]{MemcacheService.class},
				counter);

		final TestObjectifyFactory factory = new TestObjectifyFactory();
		factory.setMemcacheFactory(new IMemcacheServiceFactory() {
			@Override
			public MemcacheService getMemcacheService(final String s) {
				return proxy;
			}
		});

		ObjectifyService.setFactory(factory);
		factory().register(Uncacheable.class);
		factory().register(Cacheable.class);	// needed to get caching in the code path

		// TODO: kill when stack in factory
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
		assertThat(counter.getCount()).isEqualTo(0);
	}
}