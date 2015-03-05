/*
 */

package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalMemcacheServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 * All tests should extend this class to set up the GAE environment.
 * @see <a href="http://code.google.com/appengine/docs/java/howto/unittesting.html">Unit Testing in Appengine</a>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class GAETestBase
{
	/** */
	private final LocalServiceTestHelper helper =
			new LocalServiceTestHelper(
					// Our tests assume strong consistency
					new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy(),
					new LocalMemcacheServiceTestConfig());

	/** */
	@BeforeMethod
	public void setUpGAE() {
		this.helper.setUp();
	}

	/** */
	@AfterMethod
	public void tearDownGAE() {
		this.helper.tearDown();
	}

	/** */
	protected EmbeddedEntity makeEmbeddedEntityWithProperty(String name, Object value) {
		EmbeddedEntity emb = new EmbeddedEntity();
		emb.setProperty(name, value);
		return emb;
	}

}
