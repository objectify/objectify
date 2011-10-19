/*
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.cache.TriggerFutureHook;
import com.googlecode.objectify.test.entity.Apple;
import com.googlecode.objectify.test.entity.Banana;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Criminal;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.HasAlsoLoads;
import com.googlecode.objectify.test.entity.HasArrays;
import com.googlecode.objectify.test.entity.HasCollections;
import com.googlecode.objectify.test.entity.HolderOfString;
import com.googlecode.objectify.test.entity.HolderOfStringAndLong;
import com.googlecode.objectify.test.entity.NamedTrivial;
import com.googlecode.objectify.test.entity.Town;
import com.googlecode.objectify.test.entity.Trivial;

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
	protected ObjectifyFactory fact;
	
	/** */
	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(
					// Our tests assume strong consistency
					new LocalDatastoreServiceTestConfig(),//.setDefaultHighRepJobPolicyUnappliedJobPercentage(100),
					new LocalMemcacheServiceTestConfig(),
					new LocalTaskQueueTestConfig());
	/** */
	@BeforeMethod()
	public void setUp()
	{
		this.helper.setUp();
		
		this.fact = new ObjectifyFactory() {
			@Override
			public Objectify begin(ObjectifyOpts opts)
			{
				// This can be used to enable/disable the memory cache globally.
				opts.setGlobalCache(true);
				
				// This can be used to enable/disable the session caching objectify
				// Note that it will break several unit tests that check for transmutation
				// when entities are run through the DB (ie, unknown List types become
				// ArrayList).  These failures are ok.
				opts.setSessionCache(false);
				
				return super.begin(opts);
			}
		};
		
		this.fact.register(Trivial.class);
		this.fact.register(NamedTrivial.class);
		this.fact.register(HasAlsoLoads.class);
		this.fact.register(Child.class);
		this.fact.register(Employee.class);
		this.fact.register(HasArrays.class);
		this.fact.register(HasCollections.class);
		this.fact.register(Apple.class);
		this.fact.register(Banana.class);
		this.fact.register(HolderOfString.class);
		this.fact.register(HolderOfStringAndLong.class);
		this.fact.register(Town.class);
		this.fact.register(Criminal.class);
	}

	/** */
	@AfterMethod
	public void tearDown()
	{
		// This normally is done in the AsyncCacheFilter but that doesn't exist for tests
		TriggerFutureHook.completeAllPendingFutures();
		this.helper.tearDown();
	}
	
	/** Utility methods that puts and immediately gets an entity */
	protected <T> T putAndGet(T saveMe)
	{
		Objectify ofy = this.fact.begin();
		
		Key<T> key = ofy.put(saveMe);

		try
		{
			Entity ent = ofy.getDatastore().get(fact.getRawKey(key));
			System.out.println(ent);
		}
		catch (EntityNotFoundException e) { throw new RuntimeException(e); }

		return ofy.find(key);
	}
}
