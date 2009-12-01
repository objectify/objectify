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

import com.google.appengine.api.datastore.dev.LocalDatastoreService;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.TrivialWithId;
import com.googlecode.objectify.test.entity.TrivialWithName;
import com.googlecode.objectify.test.entity.WithOldNames;

/**
 * All tests should extend this class to set up the GAE environment.
 * @see http://code.google.com/appengine/docs/java/howto/unittesting.html
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TestBase.class);
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());

		ApiProxyLocal proxy = new ApiProxyLocalImpl(new File("target")) {};
		proxy.setProperty(LocalDatastoreService.NO_STORAGE_PROPERTY, Boolean.TRUE.toString());
		ApiProxy.setDelegate(proxy);
		
		// Register all our entity types.  It's ok that we do this multiple times.
		ObjectifyFactory.register(TrivialWithId.class);
		ObjectifyFactory.register(TrivialWithName.class);
		ObjectifyFactory.register(WithOldNames.class);
		ObjectifyFactory.register(Child.class);
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
}