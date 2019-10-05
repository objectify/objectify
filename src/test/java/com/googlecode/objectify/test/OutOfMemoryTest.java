package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.cache.CachingAsyncDatastoreService;
import com.googlecode.objectify.test.util.GAETestBase;
import com.googlecode.objectify.test.util.TestObjectifyFactory;
import com.googlecode.objectify.test.util.TestObjectifyService;
import com.googlecode.objectify.util.Closeable;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * In unfortunate situations, async get operations can throw OutOfMemoryErrors.
 * If this occurs in an App Engine environment with HTTP requests using
 * ObjectifyFilter, when using a caching datastore service, ObjectifyService
 * will retry the problematic operation when completing futures registered in
 * PendingFutures - where it's very likely to get an OutOfMemoryException again.
 * This can prevent the stack {@link ObjectifyService} to be popped, thus
 * leaking a huge amount of memory. As GAE uses a threadpool, this leak will
 * then stick with the instance. This test asserts that the polluted Objectify
 * instance is not kept in the stack.
 */
public class OutOfMemoryTest extends GAETestBase {

	@AfterMethod
	public void tearDown() {
		// TestObjectifyService.setFactory(new TestObjectifyFactory());
	}

	@BeforeMethod
	public void setUp() throws Exception {
		// set up an Objectify with a datastore service that throws OutOfMemoryErrors on
		// get operations
		TestObjectifyService.setFactory(new TestObjectifyFactory() {
			@Override
			public AsyncDatastoreService createAsyncDatastoreService(DatastoreServiceConfig cfg, boolean globalCache) {
				final AsyncDatastoreService ads = super.createRawAsyncDatastoreService(cfg);
				AsyncDatastoreService throwingAds = (AsyncDatastoreService) Proxy.newProxyInstance(
						getClass().getClassLoader(),
						new Class[] { AsyncDatastoreService.class },
						new InvocationHandler() {
							@Override
							public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
								if (method.getName().equals("get")) {
									return CompletableFuture.runAsync(new Runnable() {

										@Override
										public void run() {
											throw new OutOfMemoryError();
										}
									});
								}
								return method.invoke(ads, args);
					}
						});
				// use a caching wrapper so that the futures are registered in PendingFutures
				return new CachingAsyncDatastoreService(throwingAds, entityMemcache);
			}
		});

		fact().register(MemoryConsumingEntity.class);
	}

	@Test
	public void stackClearedOnOutOfMemoryError() throws Exception {
		// save an entity so that we can load it
		try (Closeable root = TestObjectifyService.begin()) {
			MemoryConsumingEntity entity = new MemoryConsumingEntity(1l);
			ofy().defer().save().entity(entity);
		}
		try {
			// trigger a load as if it was started in a web request with ObjectifyFilter
			try (Closeable root = TestObjectifyService.begin()) {
				ofy().load().type(MemoryConsumingEntity.class).id(1l).now();
			}
		} catch (Throwable t) {
			Assert.assertTrue(t instanceof OutOfMemoryError);
		}
		try {
			TestObjectifyService.pop();
			Assert.fail();
		} catch (NoSuchElementException e) {
			// expected, the stack is cleared in the Closeable
		}
	}

	@Entity
	@Cache
	@Data
	@NoArgsConstructor
	public static class MemoryConsumingEntity implements Serializable {
		private static final long serialVersionUID = 1L;

		@Id
		private Long id;

		public MemoryConsumingEntity(Long id) {
			this.id = id;
		}

	}

}
