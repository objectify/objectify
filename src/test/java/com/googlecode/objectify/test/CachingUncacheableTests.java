/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.IMemcacheServiceFactory;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingUncacheableTests extends TestBase
{
	/** */
	@Entity
	static class Uncacheable {
		@Id Long id;
		String stuff;
	}

	@Data
	static class CallCounter implements InvocationHandler {
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

	/**
	 */
	@BeforeMethod
	public void setUpExtra() {
		counter = new CallCounter(MemcacheServiceFactory.getAsyncMemcacheService(ObjectifyFactory.MEMCACHE_NAMESPACE));

		final MemcacheService proxy = (MemcacheService)Proxy.newProxyInstance(
				this.getClass().getClassLoader(),
				new Class<?>[]{MemcacheService.class},
				counter);

		fact().setMemcacheFactory(new IMemcacheServiceFactory() {
			@Override
			public MemcacheService getMemcacheService(final String s) {
				return proxy;
			}

			@Override
			public AsyncMemcacheService getAsyncMemcacheService(final String s) {
				throw new UnsupportedOperationException();
			}
		});

		fact().register(Uncacheable.class);
	}

	/** */
	@Test
	public void ensureUncacheableThingsDoNotTouchMemcache() throws Exception {
		Uncacheable un1 = new Uncacheable();
		un1.stuff = "un1 stuff";

		final Key<Uncacheable> key = ofy().save().entity(un1).now();
		ofy().clear();
		final Uncacheable fetched = ofy().load().key(key).now();

		assert fetched != null;
		assert counter.getCount() == 0;
	}
}