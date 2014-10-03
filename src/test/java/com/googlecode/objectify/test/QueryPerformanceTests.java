/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectifyFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of various queries
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryPerformanceTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryPerformanceTests.class.getName());

	/** */
	class CountingProxy implements InvocationHandler {

		AsyncDatastoreService base;

		public CountingProxy(AsyncDatastoreService base) {
			this.base = base;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			if (method.getName().equals("get"))
				getCount++;

			return method.invoke(base, args);
		}
	}

	/** */
	Trivial triv1;
	int getCount;

	/** */
	@BeforeMethod
	public void setUpExtra() {
		getCount = 0;

		// throw away the current factory and replace it with one that tracks calls
		setUpObjectifyFactory(new TestObjectifyFactory() {
			@Override
			protected AsyncDatastoreService createRawAsyncDatastoreService(DatastoreServiceConfig cfg) {
				return (AsyncDatastoreService)Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] { AsyncDatastoreService.class }, new CountingProxy(super.createRawAsyncDatastoreService(cfg)));
			}
		});

		fact().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);

		ofy().save().entity(triv1).now();
		ofy().clear();
	}

	/** */
	@Test
	public void hybridOn() throws Exception {
		Query<Trivial> q = ofy().load().type(Trivial.class).hybrid(true);

		int count = 0;
		for (@SuppressWarnings("unused") Trivial t: q) {
			count++;
		}

		assert count == 1;
		assert getCount == 1;
	}

	/** */
	@Test
	public void hybridOff() throws Exception {
		Query<Trivial> q = ofy().load().type(Trivial.class).hybrid(false);

		int count = 0;
		for (@SuppressWarnings("unused") Trivial t: q) {
			count++;
		}

		assert count == 1;
		assert getCount == 0;
	}
}
