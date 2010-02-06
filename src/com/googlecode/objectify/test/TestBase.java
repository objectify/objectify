/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Apple;
import com.googlecode.objectify.test.entity.Banana;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Criminal;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.HasArrays;
import com.googlecode.objectify.test.entity.HasCollections;
import com.googlecode.objectify.test.entity.HasEnums;
import com.googlecode.objectify.test.entity.HasOldNames;
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
	private static Logger log = LoggerFactory.getLogger(TestBase.class);
	
	/** */
	protected ObjectifyFactory fact;

	/** */
	@BeforeMethod
	public void setUp()
	{
		ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());

		ApiProxyLocal proxy = new ApiProxyLocalImpl(new File("target")) {};
		proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
		ApiProxy.setDelegate(proxy);

		this.fact = new ObjectifyFactory();
		this.fact.register(Trivial.class);
		this.fact.register(NamedTrivial.class);
		this.fact.register(HasOldNames.class);
		this.fact.register(Child.class);
		this.fact.register(Employee.class);
		this.fact.register(HasArrays.class);
		this.fact.register(HasEnums.class);
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
		ApiProxyLocalImpl proxy = (ApiProxyLocalImpl)ApiProxy.getDelegate();
		LocalDatastoreService datastoreService = (LocalDatastoreService)proxy.getService("datastore_v3");
		datastoreService.clearProfiles();

        // not strictly necessary to null these out but there's no harm either
		ApiProxy.setDelegate(null);
		ApiProxy.setEnvironmentForCurrentThread(null);
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
