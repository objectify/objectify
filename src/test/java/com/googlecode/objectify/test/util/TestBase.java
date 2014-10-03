/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.appengine.tools.development.testing.LocalTaskQueueTestConfig;
import com.googlecode.objectify.util.Closeable;
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

	/** Tear down every method */
	private Closeable rootService;

	/** */
	@BeforeMethod
	public void setUp() {
		this.helper.setUp();

		this.setUpObjectifyFactory(new TestObjectifyFactory());
	}

	/** */
	@AfterMethod
	public void tearDown() {
		// This is normally done in ObjectifyFilter but that doesn't exist for tests
		rootService.close();
		rootService = null;

		this.helper.tearDown();
	}

	protected void setUpObjectifyFactory(TestObjectifyFactory factory) {
		if (rootService != null)
			rootService.close();

		TestObjectifyService.setFactory(factory);
		rootService = TestObjectifyService.begin();
	}

	/** */
	protected EmbeddedEntity makeEmbeddedEntityWithProperty(String name, Object value) {
		EmbeddedEntity emb = new EmbeddedEntity();
		emb.setProperty(name, value);
		return emb;
	}

}
