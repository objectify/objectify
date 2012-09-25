/*
 */

package com.googlecode.objectify.test.util;

import java.util.logging.Logger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.Ref;

/**
 * All tests should extend this class to set up the GAE environment.
 * @see <a href="http://code.google.com/appengine/docs/java/howto/unittesting.html">Unit Testing in Appengine</a>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TestBase.class.getName());
	
	/** */
	protected TestObjectifyFactory fact;
	
	/** */
	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(
					// Our tests assume strong consistency
					new LocalDatastoreServiceTestConfig(),//.setDefaultHighRepJobPolicyUnappliedJobPercentage(100),
					new LocalMemcacheServiceTestConfig(),
					new LocalTaskQueueTestConfig());
	/** */
	@BeforeMethod
	public void setUp()
	{
		this.helper.setUp();
		
		this.fact = TestObjectifyService.factory();
	}

	/** */
	@AfterMethod
	public void tearDown()
	{
		// This is normally done in ObjectifyFilter but that doesn't exist for tests
		ObjectifyFilter.complete();
		
		this.helper.tearDown();
	}
	
	/** Utility methods that puts, clears the session, and immediately gets an entity */
	protected <T> T putClearGet(T saveMe)
	{
		Objectify ofy = this.fact.begin();
		
		Key<T> key = ofy.save().entity(saveMe).now();

		try
		{
			Entity ent = ds().get(null, key.getRaw());
			System.out.println(ent);
		}
		catch (EntityNotFoundException e) { throw new RuntimeException(e); }
		
		ofy.clear();

		return ofy.load().key(key).get();
	}
	
	/** Get a DatastoreService */
	protected DatastoreService ds()
	{
		return DatastoreServiceFactory.getDatastoreService();
	}

	/** Useful utility method */
	protected void assertRefUninitialzied(Ref<?> ref) {
		try {
			ref.get();
			assert false;
		} catch (IllegalStateException ex) {}
	}
	
}
