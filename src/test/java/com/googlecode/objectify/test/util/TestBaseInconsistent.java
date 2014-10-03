/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.googlecode.objectify.util.Closeable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * For rare tests which require delayed eventual consistency.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TestBaseInconsistent
{
	/** */
	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(
					new LocalDatastoreServiceTestConfig().setAlternateHighRepJobPolicyClass(NeverApplyJobPolicy.class),
					new LocalMemcacheServiceTestConfig(),
					new LocalTaskQueueTestConfig());

	/** Tear down every method */
	private Closeable rootService;

	/** */
	@BeforeMethod
	public void setUp() {
		this.helper.setUp();

		// Initialize a new factory each time.
		TestObjectifyService.initialize();

		rootService = TestObjectifyService.begin();
	}

	/** */
	@AfterMethod
	public void tearDown() {
		// This is normally done in ObjectifyFilter but that doesn't exist for tests
		rootService.close();

		this.helper.tearDown();
	}

	/** Get a DatastoreService */
	protected DatastoreService ds() {
		return DatastoreServiceFactory.getDatastoreService();
	}
}
