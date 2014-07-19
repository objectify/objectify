/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.googlecode.objectify.ObjectifyFilter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.logging.Logger;

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
	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(
					// Our tests assume strong consistency
					new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy(),
					new LocalMemcacheServiceTestConfig(),
					new LocalTaskQueueTestConfig());
	/** */
	@BeforeMethod
	public void setUp() {
		this.helper.setUp();

		// Initialize a new factory each time.
		TestObjectifyService.initialize();
	}

	/** */
	@AfterMethod
	public void tearDown() {
		// This is normally done in ObjectifyFilter but that doesn't exist for tests
		ObjectifyFilter.complete();

		this.helper.tearDown();
	}

	/** */
	protected EmbeddedEntity makeEmbeddedEntityWithProperty(String name, Object value) {
		EmbeddedEntity emb = new EmbeddedEntity();
		emb.setProperty(name, value);
		return emb;
	}

}
