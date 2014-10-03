/*
 */

package com.googlecode.objectify.test.util;

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
public class TestBase extends GAETestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TestBase.class.getName());

	/** Tear down every method */
	private Closeable rootService;

	/** */
	@BeforeMethod
	public void setUp() {
		this.setUpObjectifyFactory(new TestObjectifyFactory());
	}

	/** */
	@AfterMethod
	public void tearDown() {
		// This is normally done in ObjectifyFilter but that doesn't exist for tests
		rootService.close();
		rootService = null;
	}

	protected void setUpObjectifyFactory(TestObjectifyFactory factory) {
		if (rootService != null)
			rootService.close();

		TestObjectifyService.setFactory(factory);
		rootService = TestObjectifyService.begin();
	}
}
